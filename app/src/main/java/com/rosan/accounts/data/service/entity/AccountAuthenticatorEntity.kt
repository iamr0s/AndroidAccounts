package com.rosan.accounts.data.service.entity

import android.graphics.drawable.Drawable

data class AccountAuthenticatorEntity(
    val userId: Int,
    val type: String,
    val packageName: String,
    val label: String,
    val icon: Drawable
)