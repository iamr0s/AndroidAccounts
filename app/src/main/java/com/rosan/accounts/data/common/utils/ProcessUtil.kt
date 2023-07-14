package com.rosan.accounts.data.common.utils

fun Process.isActive(): Boolean {
    return try {
        exitValue()
        false
    } catch (e: IllegalArgumentException) {
        true
    }
}