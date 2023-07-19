package com.rosan.accounts.data.common.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.annotation.StringRes

fun runOnUiThread(action: () -> Unit) {
    Handler(Looper.getMainLooper()).post {
        action.invoke()
    }
}

fun Context.copy(text: CharSequence) {
    runOnUiThread {
        val manager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        manager.setPrimaryClip(ClipData.newPlainText("Label", text))
    }
}

fun Context.openUrlInBrowser(url: String) {
    runOnUiThread {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }
}

fun Context.toast(text: CharSequence, duration: Int = Toast.LENGTH_SHORT) {
    runOnUiThread {
        Toast.makeText(this, text, duration).show()
    }
}

fun Context.toast(@StringRes resId: Int, duration: Int = Toast.LENGTH_SHORT) {
    runOnUiThread {
        toast(getString(resId), duration)
    }
}