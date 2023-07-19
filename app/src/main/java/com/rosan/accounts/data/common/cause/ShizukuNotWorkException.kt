package com.rosan.accounts.data.common.cause

data class ShizukuNotWorkException(
    override val message: String? = null,
    override val cause: Throwable? = null
) : RuntimeException(message, cause)