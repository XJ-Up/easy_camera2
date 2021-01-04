package com.personal.xj.easycamera2.utils

import android.graphics.Rect
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureRequest
import android.os.Build
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.annotation.RequiresApi
import androidx.core.math.MathUtils

/**
 *
 * @ClassName:      Zoom
 * @Description:     放大所缩小工具类
 * @Author:         xj
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class Zoom( characteristics: CameraCharacteristics?) {
    @NonNull
    private val mCropRegion = Rect()
    private var maxZoom: Float
    @Nullable
    private val mSensorSize: Rect? = characteristics?.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
    private var hasSupport: Boolean

    init {
        if (mSensorSize == null) {
            maxZoom = DEFAULT_ZOOM_FACTOR
            hasSupport = false
        }else{
            val value =
                characteristics?.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM)
            maxZoom =
                if (value == null || value < DEFAULT_ZOOM_FACTOR) DEFAULT_ZOOM_FACTOR else value
            hasSupport =
                maxZoom.compareTo(DEFAULT_ZOOM_FACTOR) > 0
        }

    }
    fun setZoom(@NonNull builder: CaptureRequest.Builder, zoom: Float) {
        if (!hasSupport) {
            return
        }
        val newZoom: Float =
            MathUtils.clamp(zoom, DEFAULT_ZOOM_FACTOR, maxZoom)
        val centerX = mSensorSize!!.width() / 2
        val centerY = mSensorSize.height() / 2
        val deltaX = (0.5f * mSensorSize.width() / newZoom).toInt()
        val deltaY = (0.5f * mSensorSize.height() / newZoom).toInt()
        mCropRegion[centerX - deltaX, centerY - deltaY, centerX + deltaX] = centerY + deltaY

        builder.set(CaptureRequest.SCALER_CROP_REGION, mCropRegion)

    }
    fun getZoom( builder: CaptureRequest.Builder):Float{
        val get = builder.get(CaptureRequest.SCALER_CROP_REGION)
        val centerX = mSensorSize!!.width() / 2
        val centerY = mSensorSize.height() / 2
        get?.let { rect ->
            return (0.5f * mSensorSize.width()) / (centerX - rect.left)
        }
       return DEFAULT_ZOOM_FACTOR
    }
    companion object {
        private const val DEFAULT_ZOOM_FACTOR = 1.0f
    }


}