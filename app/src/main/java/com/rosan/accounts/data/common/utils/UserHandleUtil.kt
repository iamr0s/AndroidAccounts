package com.rosan.accounts.data.common.utils

import android.os.UserHandle

val UserHandle.id: Int
    get() = this.hashCode()