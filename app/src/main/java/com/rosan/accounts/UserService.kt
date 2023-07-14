package com.rosan.accounts

import android.accounts.AccountManager
import android.content.Context

class UserService constructor(private val context: Context) {
    fun getAccountTypes(): Array<AccountType> {
        // Wait for the system cache be refreshed
        Thread.sleep(1500)

        val accountTypes = mutableListOf<AccountType>()
        val accountManager =
            context.getSystemService(Context.ACCOUNT_SERVICE) as AccountManager
        val packageManager = context.packageManager

        accountManager.authenticatorTypes.forEach {
            val packageInfo = packageManager.getPackageInfo(it.packageName, 0)
            val applicationInfo = packageInfo.applicationInfo
            var label = applicationInfo.loadLabel(packageManager).toString()
            val accountName = packageManager.getText(it.packageName, it.labelId, null)
            if (accountName != null && label != accountName)
                label += " - $accountName"
            val icon = applicationInfo.loadIcon(packageManager)

            accountTypes.add(AccountType(label, icon, it.packageName, it.type))
        }

        return accountTypes.sortedBy { it.label }.toTypedArray()
    }
}