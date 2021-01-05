/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.personal.xj.easycamera2.utils

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.os.Build
import android.util.Size
import android.view.OrientationEventListener
import android.view.Surface
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData


/**
 *
 * @Description:     计算最接近的90度方向以补偿设备*相对于传感器方向的旋转，即，允许用户查看具有预期方向的相机*框架。
 * @Author:         xj
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class OrientationLiveData(
    context: Context,
    characteristics: CameraCharacteristics
) : LiveData<Int>() {

    private val listener = object : OrientationEventListener(context.applicationContext) {
        override fun onOrientationChanged(orientation: Int) {
            val rotation = when {
                orientation <= 45 -> Surface.ROTATION_0
                orientation <= 135 -> Surface.ROTATION_90
                orientation <= 225 -> Surface.ROTATION_180
                orientation <= 315 -> Surface.ROTATION_270
                else -> Surface.ROTATION_0
            }
            val relative = computeRelativeRotation(characteristics, rotation)
            if (relative != value) postValue(relative)
        }
    }

    override fun onActive() {
        super.onActive()
        listener.enable()
    }

    override fun onInactive() {
        super.onInactive()
        listener.disable()
    }

    companion object {

        /**
         * 计算从相机传感器方向转换为*设备当前方向（以度为单位）所需的旋转。
         *
         * @param characteristics 通过[CameraCharacteristics]查询传感器方向。
         * @param surfaceRotation 当前设备方向为表面常数
         * @return 从摄像头传感器到当前设备方向的相对旋转。
         */

        @JvmStatic

        private fun computeRelativeRotation(
            characteristics: CameraCharacteristics,
            surfaceRotation: Int
        ): Int {
            val sensorOrientationDegrees =
                characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)!!

            val deviceOrientationDegrees = when (surfaceRotation) {
                Surface.ROTATION_0 -> 0
                Surface.ROTATION_90 -> 90
                Surface.ROTATION_180 -> 180
                Surface.ROTATION_270 -> 270
                else -> 0
            }

            // 前置摄像头的反向设备方向
            val sign = if (characteristics.get(CameraCharacteristics.LENS_FACING) ==
                CameraCharacteristics.LENS_FACING_FRONT
            ) 1 else -1

            // 计算相对于相机方向的所需JPEG方向，以使
            // 相对于设备方向的垂直图像
            return (sensorOrientationDegrees - (deviceOrientationDegrees * sign) + 360) % 360
        }
    }
}
