package com.rosan.accounts

import android.accounts.AccountManager
import android.content.Context
import android.os.Binder
import android.os.IBinder
import android.os.Parcel
import android.os.ParcelFileDescriptor
import android.os.Process
import com.rosan.accounts.data.common.utils.id
import com.rosan.accounts.data.common.utils.requireShizukuPermissionGranted
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper
import java.io.FileDescriptor

object UserService {
    suspend fun getAccountTypes(context: Context): Array<AccountType> {
        val accounts = kotlin.runCatching {
            requireShizukuPermissionGranted(context) {
                getAccounts()
                    // filter current user
                    .filter { it.userId == Process.myUserHandle().id }
            }
        }.onFailure { it.printStackTrace() }.getOrNull()

        val accountManager =
            context.getSystemService(Context.ACCOUNT_SERVICE) as AccountManager
        val packageManager = context.packageManager

        val types = accountManager.authenticatorTypes.map {
            val type = it.type

            val values = accounts?.filter { it.type == type }?.map { it.name }
                ?: emptyList()
            if (accounts != null && values.isEmpty()) return@map null

            val packageInfo = packageManager.getPackageInfo(it.packageName, 0)
            val applicationInfo = packageInfo.applicationInfo
            var label = applicationInfo.loadLabel(packageManager).toString()
            val accountName = packageManager.getText(it.packageName, it.labelId, null)
            if (accountName != null && label != accountName)
                label += " - $accountName"
            val icon = applicationInfo.loadIcon(packageManager)

            return@map AccountType(
                packageName = it.packageName,
                type = type,
                label = label,
                icon = icon,
                values = values
            )
        }.filterNotNull()

        return types.sortedBy { it.label }.toTypedArray()
    }

    private suspend fun getAccounts(): List<AccountInfo> {
        val text = dumpsysAccount()

        data class UserInfo(val id: Int, val numberOfAccounts: Int)

        val userIds = "User UserInfo\\{(\\d+):.*\\}".toRegex().findAll(text).toList().map {
            it.groupValues[1].toInt()
        }

        val users = "Accounts: (\\d+)".toRegex().findAll(text).toList().let {
            userIds.mapIndexed { index, userId ->
                val numberOfAccounts = it[index].groupValues[1].toInt()
                UserInfo(id = userId, numberOfAccounts = numberOfAccounts)
            }
        }.toMutableList()

        fun readUser(): UserInfo {
            val user = users.first().let { user ->
                if (user.numberOfAccounts > 0) return@let user
                readUser()
            }.let {
                it.copy(numberOfAccounts = it.numberOfAccounts - 1)
            }
            users[0] = user
            return user
        }

        return "Account \\{name=(.*), type=(.*)\\}".toRegex().findAll(text).toList().map {
            val name = it.groupValues[1]
            val type = it.groupValues[2]
            val userId = readUser().id

            AccountInfo(
                userId = userId,
                type = type,
                name = name
            )
        }
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
