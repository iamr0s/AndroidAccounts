package com.rosan.accounts

import android.graphics.drawable.Drawable

data class AccountType(
    val label: String,
    val icon: Drawable,
    val packageName: String,
    val type: String,
    val values: List<String>
)

