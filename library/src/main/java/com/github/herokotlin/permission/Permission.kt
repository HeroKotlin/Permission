package com.github.herokotlin.permission

import android.app.Activity
import android.content.pm.PackageManager
import android.os.Environment
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import java.lang.Exception

class Permission(private val requestCode: Int, private val permissions: List<String>) {

    var onRequestPermissions: ((Activity, permissions: Array<out String>, Int) -> Unit)? = null

    var onPermissionsGranted: (() -> Unit)? = null

    var onPermissionsDenied: (() -> Unit)? = null

    var onPermissionsNotGranted: (() -> Unit)? = null

    var onExternalStorageNotWritable: (() -> Unit)? = null

    var onExternalStorageNotReadable: (() -> Unit)? = null

    private var onRequestPermissionsComplete: (() -> Unit)? = null

    fun checkExternalStorageWritable(): Boolean {
        if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
            return true
        }
        onExternalStorageNotWritable?.invoke()
        return false
    }

    fun checkExternalStorageReadable(): Boolean {
        if (Environment.getExternalStorageState() in
            setOf(Environment.MEDIA_MOUNTED, Environment.MEDIA_MOUNTED_READ_ONLY)) {
            return true
        }
        onExternalStorageNotReadable?.invoke()
        return false
    }

    fun requestPermissions(activity: Activity, callback: () -> Unit) {

        var list = arrayOf<String>()

        // 如果是 6.0 以下的手机，ActivityCompat.checkSelfPermission() 会始终等于 PERMISSION_GRANTED
        // 但是如果用户关闭了你申请的权限，ActivityCompat.checkSelfPermission() 则可能会导致程序崩溃

        try {
            permissions.forEach {
                if (ContextCompat.checkSelfPermission(activity, it) != PackageManager.PERMISSION_GRANTED) {
                    list = list.plus(it)
                }
            }
        }
        catch (e: Exception) {
            onPermissionsNotGranted?.invoke()
            return
        }

        if (list.isNotEmpty()) {
            onRequestPermissionsComplete = callback
            if (onRequestPermissions != null) {
                onRequestPermissions?.invoke(activity, list, requestCode)
            }
            else {
                ActivityCompat.requestPermissions(activity, list, requestCode)
            }
            return
        }

        callback()

    }

    fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {

        if (requestCode != this.requestCode) {
            return
        }

        for (i in 0 until permissions.size) {
            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                onPermissionsDenied?.invoke()
                return
            }
        }

        onPermissionsGranted?.invoke()

        onRequestPermissionsComplete?.invoke()
        onRequestPermissionsComplete = null

    }

}