package com.rosan.accounts

import android.accounts.IAccountManager
import android.content.Context
import android.content.pm.IPackageManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.IUserManager
import android.os.Parcel
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper
import java.io.FileDescriptor

object UserService {
    suspend fun removeUser(userId: Int): Boolean {
        return IUserManager.Stub.asInterface(shizukuBinder(Context.USER_SERVICE))
            .removeUser(userId)
    }

    suspend fun getUsers(context: Context): Map<UserInfo, List<AccountType>> {
        val basePackageManager = context.packageManager
        val accountManager =
            IAccountManager.Stub.asInterface(shizukuBinder(Context.ACCOUNT_SERVICE))
        val packageManager = IPackageManager.Stub.asInterface(shizukuBinder("package"))
        val users = mutableMapOf<UserInfo, List<AccountType>>()
        getAccounts().forEach { (user, accounts) ->
            val userId = user.id
            val types = accountManager.getAuthenticatorTypes(userId).map {
                val type = it.type
                val values = accounts.filter { it.type == type }.map { it.name }
                if (values.isEmpty()) return@map null

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
                AccountType(
                    userId = userId,
                    packageName = it.packageName,
                    type = type,
                    label = label,
                    icon = icon,
                    values = values
                )
            }
            users[user] = types.filterNotNull()
        }
        return users
    }

    private suspend fun getAccounts(): Map<UserInfo, List<AccountInfo>> {
        val text = dumpsysAccount()

        val result = mutableMapOf<UserInfo, List<AccountInfo>>()

        val users = "User UserInfo\\{(\\d+):(.*):.*\\}".toRegex().findAll(text).toList().map {
            val userId = it.groupValues[1].toInt()
            val name = it.groupValues[2]
            UserInfo(id = userId, name = name, numberOfAccounts = 0).apply {
                result[this] = listOf()
            }
        }.let { users ->
            "Accounts: (\\d+)".toRegex().findAll(text).toList().let {
                users.mapIndexed { index, user ->
                    val numberOfAccounts = it[index].groupValues[1].toInt()
                    user.copy(numberOfAccounts = numberOfAccounts)
                }
            }
        }.toMutableList()

        fun readUser(): UserInfo {
            val user = users.first().let { user ->
                if (user.numberOfAccounts > 0) return@let user
                return readUser()
            }.let {
                it.copy(numberOfAccounts = it.numberOfAccounts - 1)
            }
            users[0] = user
            return user
        }

        "Account \\{name=(.*), type=(.*)\\}".toRegex().findAll(text).toList().map {
            val name = it.groupValues[1]
            val type = it.groupValues[2]
            val userId = readUser().id

            AccountInfo(
                userId = userId,
                type = type,
                name = name
            )
        }.groupBy { it.userId }.map { (userId, accounts) ->
            result[result.keys.first { userId == it.id }] = accounts
        }

        return result
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
