package com.rosan.accounts

import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable

data class AccountType(
    val userId: Int = 0,
    val packageName: String,
    val type: String = packageName,
    val label: String = packageName,
    val icon: Drawable = ColorDrawable(0x00000000),
    val values: List<String> = emptyList()
)
