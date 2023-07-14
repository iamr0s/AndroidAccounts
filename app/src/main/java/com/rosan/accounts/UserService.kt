package com.rosan.accounts

import android.accounts.AccountManager
import android.content.Context
import android.os.Binder
import android.os.IBinder
import android.os.Parcel
import android.os.ParcelFileDescriptor
import com.rosan.accounts.data.common.utils.requireShizukuPermissionGranted
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper
import java.io.FileDescriptor

object UserService {
    suspend fun getAccountTypes(context: Context): Array<AccountType> {
        // Wait for the system cache be refreshed
        delay(1500)

        val dumpsys = kotlin.runCatching {
            requireShizukuPermissionGranted(context) {
                dumpsys()
            }
        }.onFailure { it.printStackTrace() }
            .getOrNull()

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
        val text = dumpsysAccount()
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

    private suspend fun dumpsysAccount(): String = withContext(Dispatchers.IO) {
        val binder = SystemServiceHelper.getSystemService(Context.ACCOUNT_SERVICE)
        val pipe = ParcelFileDescriptor.createPipe()
        val readFD = pipe[0]
        val writeFD = pipe[1]
        writeFD.use {
            binderWrapperDump(binder, writeFD.fileDescriptor)
        }
        return@withContext readFD.use {
            ParcelFileDescriptor.AutoCloseInputStream(it)
                .readBytes()
                .decodeToString()
        }
    }

    private fun binderWrapperDump(
        binder: IBinder,
        fd: FileDescriptor,
        args: Array<String>? = null
    ) {
        val data = Parcel.obtain()
        val reply = Parcel.obtain()
        try {
            data.writeFileDescriptor(fd)
            data.writeStringArray(args)
            ShizukuBinderWrapper(binder).transact(Binder.DUMP_TRANSACTION, data, reply, 0)
            reply.readException()
        } finally {
            data.recycle()
            reply.recycle()
        }
    }
}
