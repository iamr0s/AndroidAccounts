package com.rosan.accounts.data.service.model

import android.accounts.IAccountManager
import android.content.Context
import android.content.pm.IPackageManager
import android.os.Build
import android.os.IUserManager
import android.util.Log
import com.rosan.accounts.data.common.utils.dumpText
import com.rosan.accounts.data.common.utils.requireShizukuPermissionGranted
import com.rosan.accounts.data.common.utils.shizukuBinder
import com.rosan.accounts.data.service.entity.AccountAuthenticatorEntity
import com.rosan.accounts.data.service.entity.AccountEntity
import com.rosan.accounts.data.service.entity.UserEntity
import com.rosan.accounts.data.service.repo.UserService
import rikka.shizuku.Shizuku

class ShizukuUserService(private val context: Context) : UserService {
    private val basePackageManager by lazy { context.packageManager }

    private val userManager by lazy { IUserManager.Stub.asInterface(shizukuBinder(Context.USER_SERVICE)) }

    private val accountManager by lazy { IAccountManager.Stub.asInterface(shizukuBinder(Context.ACCOUNT_SERVICE)) }

    private val packageManager by lazy { IPackageManager.Stub.asInterface(shizukuBinder("package")) }
    override suspend fun removeUser(user: UserEntity): Boolean = removeUser(user.id)

    override suspend fun removeUser(userId: Int): Boolean = userManager.removeUser(userId)

    override suspend fun getUsers(): List<UserEntity> = requireShizukuPermissionGranted(context) {
        (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            userManager.getUsers(false, false, false)
        else userManager.getUsers(false)).map {
            UserEntity(id = it.id, name = it.name)
        }
    }

    override suspend fun getAccountAuthenticators(userId: Int): List<AccountAuthenticatorEntity> =
        requireShizukuPermissionGranted(context) {
            accountManager.getAuthenticatorTypes(userId).map { description ->
                val packageName = description.packageName
                val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                    packageManager.getPackageInfo(packageName, 0L, userId)
                else packageManager.getPackageInfo(packageName, 0, userId)
                val applicationInfo = packageInfo.applicationInfo

                val name =
                    basePackageManager.getText(packageName, description.labelId, applicationInfo)
                val label = basePackageManager.getApplicationLabel(applicationInfo).toString().let {
                    if (it == name) it
                    else "$it - $name"
                }

                val icon = basePackageManager.getApplicationIcon(applicationInfo)

                AccountAuthenticatorEntity(
                    userId = userId,
                    type = description.type,
                    packageName = description.packageName,
                    label = label,
                    icon = icon
                )
            }
        }

    override suspend fun getAccounts(userId: Int): List<AccountEntity> =
        requireShizukuPermissionGranted(context) {
            if (Shizuku.getUid() == 0) getAccountsByManager(userId)
            else getAccountsByDump(userId)
        }

    private fun getAccountsByManager(userId: Int): List<AccountEntity> {
        return (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val packageName =
                basePackageManager.getPackagesForUid(Shizuku.getUid())?.firstOrNull() ?: "android"
            accountManager.getAccountsAsUser(null, userId, packageName)
        } else accountManager.getAccountsAsUser(null, userId)).map {
            AccountEntity(
                userId = userId,
                type = it.type,
                name = it.name
            )
        }
    }

    private fun getAccountsByDump(userId: Int): List<AccountEntity> {
        val text = accountManager.asBinder().dumpText()
        fun getUserIds() =
            "User UserInfo\\{(\\d+):.*?\\}".toRegex()
                .findAll(text)
                .toList()
                .map {
                    it.groupValues[1].toInt()
                }

        fun getLengthsOfAccounts() =
            "Accounts: (\\d+)".toRegex()
                .findAll(text)
                .toList()
                .map {
                    it.groupValues[1].toInt()
                }

        val userIds = getUserIds()
        val index = userIds.indexOf(userId)

        val lengthsOfAccounts = getLengthsOfAccounts()
        val skipLength = lengthsOfAccounts.slice(0 until index).fold(0) { cur, len ->
            cur + len
        }
        val length = lengthsOfAccounts[index]

        return "Account \\{name=(.*), type=(.*)\\}".toRegex()
            .findAll(text)
            .toList()
            .slice(skipLength until skipLength + length)
            .map {
                val name = it.groupValues[1]
                val type = it.groupValues[2]

                AccountEntity(
                    userId = userId,
                    type = type,
                    name = name
                )
            }
    }
}