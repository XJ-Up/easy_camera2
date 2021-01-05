package com.personal.xj.easycamera2.utils

import android.annotation.SuppressLint
import android.graphics.Point
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.params.StreamConfigurationMap
import android.os.Build
import android.util.Size
import android.view.Display
import androidx.annotation.RequiresApi
import kotlin.math.max
import kotlin.math.min

/**
 *
 * @Description:     用于预先计算[Size]的最短边和最长边的Helper类
 * @Author:         xj
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class SmartSize(width: Int, height: Int) {

    var size = Size(width, height)
    var long = max(size.width, size.height)
    var short = min(size.width, size.height)
    override fun toString() = "SmartSize(${long}x${short})"
}

/** 图片和视频的标准高清尺寸*/
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
val SIZE_1080P: SmartSize = SmartSize(1920, 1080)

/** 返回给定[Display]的[SmartSize]对象*/
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
fun getDisplaySmartSize(display: Display): SmartSize {
    val outPoint = Point()
    display.getRealSize(outPoint)
    return SmartSize(outPoint.x, outPoint.y)
}

/**
 * 返回最大的可用PREVIEW大小。想要查询更多的信息 */
@SuppressLint("Assert")
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
fun <T> getPreviewOutputSize(
    display: Display,
    characteristics: CameraCharacteristics,
    targetClass: Class<T>,
    format: Int? = null
): Size {

    //查找哪个更小：屏幕或1080p
    val screenSize = getDisplaySmartSize(display)
    val hdScreen = screenSize.long >= SIZE_1080P.long || screenSize.short >= SIZE_1080P.short
    val maxSize = if (hdScreen) SIZE_1080P else screenSize

    // 如果提供了图像格式，请使用它来确定支持的尺寸；否则使用目标类
    val config = characteristics.get(
        CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP
    )!!
    if (format == null)
        assert(StreamConfigurationMap.isOutputSupportedFor(targetClass))
    else
        assert(config.isOutputSupportedFor(format))
    val allSizes = if (format == null)
        config.getOutputSizes(targetClass) else config.getOutputSizes(format)

    // 获取可用尺寸并按面积从大到小排序
    val validSizes = allSizes
        .sortedWith(compareBy { it.height * it.width })
        .map { SmartSize(it.width, it.height) }.reversed()

    //然后，获得小于或等于最大尺寸的最大输出尺寸
    return validSizes.first { it.long <= maxSize.long && it.short <= maxSize.short }.size
}