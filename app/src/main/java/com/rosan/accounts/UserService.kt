package com.rosan.accounts

import android.accounts.AccountManager
import android.content.Context
import com.rosan.accounts.data.common.utils.isActive
import com.rosan.accounts.data.common.utils.requireShizukuPermissionGranted
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku

object UserService {
    suspend fun getAccountTypes(context: Context): Array<AccountType> {
        // Wait for the system cache be refreshed
        delay(1500)

        val dumpsys = kotlin.runCatching {
            requireShizukuPermissionGranted(context) {
                dumpsys()
            }
        }.getOrNull()

        val accountTypes = mutableListOf<AccountType>()
        val accountManager =
            context.getSystemService(Context.ACCOUNT_SERVICE) as AccountManager
        val packageManager = context.packageManager

        accountManager.authenticatorTypes.forEach {
            val type = it.type

            if (dumpsys != null && !dumpsys.containsKey(type)) return@forEach
            val packageInfo = packageManager.getPackageInfo(it.packageName, 0)
            val applicationInfo = packageInfo.applicationInfo
            var label = applicationInfo.loadLabel(packageManager).toString()
            val accountName = packageManager.getText(it.packageName, it.labelId, null)
            if (accountName != null && label != accountName)
                label += " - $accountName"
            val icon = applicationInfo.loadIcon(packageManager)
            val values = dumpsys?.let { it[type] } ?: emptyList()

            accountTypes.add(AccountType(label, icon, it.packageName, type, values))
        }

        return accountTypes.sortedBy { it.label }.toTypedArray()
    }

    private suspend fun dumpsys(): Map<String, List<String>>? {
        val text = dumpsysAccount() ?: return null
        val size = "Accounts: (\\d+)".toRegex().find(text).let {
            if (it == null) return null
            if (it.groupValues.size < 2) return null
            it.groupValues[1].toIntOrNull() ?: return null
        }

        var count = 0
        val map = mutableMapOf<String, MutableList<String>>()
        "Account \\{name=(.*), type=(.*)\\}".toRegex().findAll(text).forEach {
            if (it.groupValues.size < 3) return null
            val name = it.groupValues[1]
            val type = it.groupValues[2]
            val values = map.getOrElse(type) { mutableListOf() }
            values.add(name)
            count += 1
            map[type] = values
        }
        return if (count < size) null
        else map
    }

    private suspend fun dumpsysAccount(): String? = withContext(Dispatchers.IO) {
        val process = Shizuku.newProcess(arrayOf("dumpsys", "account"), null, null)
        val codeJob = async(Dispatchers.IO) {
            process.waitFor()
        }
        val textJob = async(Dispatchers.IO) {
            var bytes = ByteArray(0)
            val input = process.inputStream
            while (process.isActive()) {
                val length = input.available()
                if (length > 0) {
                    bytes += ByteArray(length).run {
                        sliceArray(0 until input.read(this))
                    }
                }
            }
            bytes.decodeToString()
        }
        return@withContext if (codeJob.await() != 0) null
        else textJob.await()
    }
}
