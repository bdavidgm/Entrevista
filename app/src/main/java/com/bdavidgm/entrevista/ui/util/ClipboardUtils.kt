package com.bdavidgm.entrevista.ui.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.annotation.StringRes

fun Context.copyTextToClipboard(
    label: String,
    text: String,
    @StringRes toastMessageRes: Int
) {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
    Toast.makeText(this, getString(toastMessageRes), Toast.LENGTH_SHORT).show()
}
