package com.ylauncher.util

import android.content.Context
import android.hardware.camera2.CameraManager

object FlashlightHelper {
    private var isOn = false

    fun toggle(context: Context) {
        try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = cameraManager.cameraIdList.firstOrNull() ?: return
            isOn = !isOn
            cameraManager.setTorchMode(cameraId, isOn)
        } catch (_: Exception) {
            isOn = false
        }
    }
}
