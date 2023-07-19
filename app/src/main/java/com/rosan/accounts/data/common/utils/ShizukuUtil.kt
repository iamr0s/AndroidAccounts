package com.rosan.accounts.data.common.utils

import android.content.Context
import android.content.pm.PackageManager
import android.os.IBinder
import android.os.ServiceManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper
import rikka.sui.Sui

suspend fun <T> requireShizukuPermissionGranted(context: Context, action: suspend () -> T): T {
    callbackFlow {
        Sui.init(context.packageName)
        if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            send(Unit)
            awaitClose()
        } else {
            val requestCode = (Int.MIN_VALUE..Int.MAX_VALUE).random()
            val listener =
                Shizuku.OnRequestPermissionResultListener { _requestCode, grantResult ->
                    if (_requestCode != requestCode) return@OnRequestPermissionResultListener
                    if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED)
                        trySend(Unit)
                    else close(Exception("sui/shizuku permission denied"))
                }
            Shizuku.addRequestPermissionResultListener(listener)
            Shizuku.requestPermission(requestCode)
            awaitClose { Shizuku.removeRequestPermissionResultListener(listener) }
        }
    }.first()
    return action()
}

fun shizukuBinder(name: String): IBinder =
    shizukuBinder(ServiceManager.getService(name))

fun shizukuBinder(binder: IBinder): IBinder = ShizukuBinderWrapper(binder)