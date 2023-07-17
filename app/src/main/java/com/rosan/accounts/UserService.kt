package com.rosan.accounts

import android.accounts.IAccountManager
import android.content.Context
import android.content.pm.IPackageManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.Parcel
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper
import java.io.FileDescriptor

object UserService {
    suspend fun getAccountTypes(context: Context): Array<AccountType> {
        val basePackageManager = context.packageManager
        val accountManager =
            IAccountManager.Stub.asInterface(shizukuBinder(Context.ACCOUNT_SERVICE))
        val packageManager = IPackageManager.Stub.asInterface(shizukuBinder("package"))
        val types = mutableListOf<AccountType>()
        getAccounts().groupBy { it.userId }.forEach { (userId, accounts) ->
            accountManager.getAuthenticatorTypes(userId).map {
                val type = it.type
                val values = accounts.filter { it.type == type }.map { it.name }

                val packageName = it.packageName

                val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                    packageManager.getPackageInfo(packageName, 0L, userId)
                else packageManager.getPackageInfo(packageName, 0, userId)
                val applicationInfo = packageInfo.applicationInfo
                var label = basePackageManager.getApplicationLabel(applicationInfo).toString()
                val accountName =
                    basePackageManager.getText(packageName, it.labelId, applicationInfo)

                if (accountName != null && label != accountName)
                    label += " - $accountName"

                val icon = basePackageManager.getApplicationIcon(applicationInfo)
                types.add(
                    AccountType(
                        userId = userId,
                        packageName = it.packageName,
                        type = type,
                        label = label,
                        icon = icon,
                        values = values
                    )
                )
            }
        }
        return types.filter { it.values.isNotEmpty() }
            .toTypedArray()
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
            shizukuBinder(binder).transact(Binder.DUMP_TRANSACTION, data, reply, 0)
            reply.readException()
        } finally {
            data.recycle()
            reply.recycle()
        }
    }

    private fun shizukuBinder(name: String): IBinder {
        return shizukuBinder(SystemServiceHelper.getSystemService(name))
    }

    private fun shizukuBinder(binder: IBinder): IBinder {
        return ShizukuBinderWrapper(binder)
    }
}
