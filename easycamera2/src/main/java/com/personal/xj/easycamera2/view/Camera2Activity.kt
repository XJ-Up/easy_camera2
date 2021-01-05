package com.personal.xj.easycamera2.view

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.media.*
import android.os.*
import android.util.Log
import android.view.*
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.ScaleAnimation
import android.view.animation.TranslateAnimation
import android.widget.FrameLayout
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.personal.xj.easycamera2.EasyCamera2
import com.personal.xj.easycamera2.R
import com.personal.xj.easycamera2.bean.Args
import com.personal.xj.easycamera2.focus.AnimationImageView
import com.personal.xj.easycamera2.monitor.*
import com.personal.xj.easycamera2.utils.OrientationLiveData
import com.personal.xj.easycamera2.utils.Zoom
import com.personal.xj.easycamera2.utils.getPreviewOutputSize
import kotlinx.android.synthetic.main.activity_cammer.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.Closeable
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeoutException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.math.roundToInt

/**
 *
 * @ClassName:      Camera2Activity
 * @Description:     利用Camera2完成录像、拍照、录像中拍照、手指放大缩小、SeekBar放大缩小、点击聚焦
 * @Author:         xj
 */
@SuppressLint("NewApi")
class Camera2Activity : AppCompatActivity(), ISeekBarUpData {
    /** 检测 连接到 CameraDevice（用于所有 相机操作）*/
    private val cameraManager: CameraManager by lazy {
        getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }

    /** 根据相机 id得到 相机的配置参数 */
    private lateinit var characteristic: CameraCharacteristics

    /**  HandlerThread 所有的相机操作都在里面执行*/
    private val cameraThread = HandlerThread("CameraThread").apply {
        start()
    }

    /** [HandlerThread] 运行所有缓冲区读取操作的位置 */
    private val imageReaderThread = HandlerThread("imageReaderThread").apply { start() }

    /** 与 cameraThread 对应的 Handler */
    private val cameraHandler = Handler(cameraThread.looper)

    /** [Handler] 对应于[imageReaderThread] */
    private val imageReaderHandler = Handler(imageReaderThread.looper)

    /** camera实例 */
    private lateinit var camera: CameraDevice

    /** camera预览视图*/
    private lateinit var viewFinder: SquareCameraPreview

    /** 对焦图标*/
    private lateinit var mFocusImage: AnimationImageView

    /**从[CameraDevice]捕获帧以进行视频录制*/
    private lateinit var session: CameraCaptureSession

    /** 测试相机参数 注意：这个参数许根据手机的实际情况获取 */
    private lateinit var args: Args

    /** 可以缩放的最大值*/
    private var maxValue: Float? = 0f

    /** 录制计时器*/
    private var recordingTimer: Int = 0

    /** 缩放实例*/
    private lateinit var zoom: Zoom

    /** 为记录器 设置一个 持久的 面 ，这样就可以作为摄像机对话输出的目标 即 预览*/
    private val recorderSurface: Surface by lazy {
        val surface = MediaCodec.createPersistentInputSurface()
        createRecorder(surface).apply {
            prepare()
            release()
            var delete = outputFile.delete()
            Log.e("测试打折的", delete.toString())
        }
        surface
    }

    /** 保存录像 */
    private val recorder: MediaRecorder by lazy {
        createRecorder(recorderSurface)
    }

    /** 生成文件*/
    private val outputFile: File by lazy {
        val createFile = createFile("mp4")
        if (!createFile.parentFile.exists()) { // 如果父目录不存在，创建父目录
            createFile.parentFile.mkdirs()
        }
        createFile
    }

    /**实时数据侦听器，用于了解设备相对于摄像头方向的变化 */
    private lateinit var relativeOrientation: OrientationLiveData

    /** 仅在[CameraCaptureSession]中用于预览的请求 */
    private lateinit var previewRequestBuilder: CaptureRequest.Builder

    /** 用作照相机静止镜头缓冲区的读取器*/
    private lateinit var imageReader: ImageReader

    /**
     * visible与invisible之间切换的动画
     */
    private val mShowAction: TranslateAnimation by lazy {
        TranslateAnimation(
            Animation.RELATIVE_TO_SELF,
            0.0f,
            Animation.RELATIVE_TO_SELF,
            0.0f,
            Animation.RELATIVE_TO_SELF,
            -1.0f,
            Animation.RELATIVE_TO_SELF,
            0.0f
        )
    }

    /**
     * Focus的Scale动画
     */
    private val mScaleFocusAnimation: ScaleAnimation by lazy {
        ScaleAnimation(
            2.0f,
            1.0f,
            2.0f,
            1.0f,
            Animation.RELATIVE_TO_SELF,
            0.5f,
            Animation.RELATIVE_TO_SELF,
            0.5f
        )
    }

    /**
     * popupwindow的字的动画
     */
    private val mScaleWindowAnimation: ScaleAnimation by lazy {
        ScaleAnimation(
            2.0f,
            1.0f,
            2.0f,
            1.0f,
            Animation.RELATIVE_TO_SELF,
            0.5f,
            Animation.RELATIVE_TO_SELF,
            0.5f
        )
    }

    /**
     * 淡入动画
     */
    private val mAlphaInAnimation: AlphaAnimation by lazy {
        AlphaAnimation(0.0f, 1.0f)
    }

    /**
     * 淡出动画
     */
    private val mAlphaOutAnimation: AlphaAnimation by lazy {
        AlphaAnimation(1.0f, 0.0f)
    }

    /**录像控制*/
    private var isVideoTape = false

    /**
     * 用来focus的显示框框之类的
     */
    private val mPreviewSessionCallback: PreviewSessionCallback by lazy {
        PreviewSessionCallback(mFocusImage, mMainHandler, viewFinder)
    }

    /**录制的开始时间*/
    private var recordingStartMillis: Long = 0L

    /**
     * UI线程的handler
     */
    private val mMainHandler: Handler =
        object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                super.handleMessage(msg)
                when (msg.what) {
                    FOCUS_DISAPPEAR -> {
                        if (msg.obj == null) {
                            mFocusImage.stopFocus()
                            return
                        }
                        val valueTimes = msg.obj as Int
                        if (mFocusImage.mTimes == valueTimes) {
                            mFocusImage.stopFocus()
                        }
                    }

                    FOCUS_AGAIN -> {
                        Log.i("FOCUS_AGAIN", "FOCUS_AGAINFOCUS_AGAINFOCUS_AGAIN")
                        previewRequestBuilder.set(
                            CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                            CameraMetadata.CONTROL_AE_PRECAPTURE_TRIGGER_START
                        )
                        updatePreview()
                    }
                }
            }
        }

    /** image监听接口*/
    private val iEasyCamera2OutImagePath: ((String) -> Unit)? by lazy {
        EasyCamera2.getImageOutMonitor()
    }

    /** video监听接口*/
    private val iEasyCamera2OutVideoPath: ((String) -> Unit)? by lazy {
        EasyCamera2.getVideoOutMonitor()
    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cammer)

        args = Args("0", 4000, 3000, 30, 256)
        hour_meter.text = "00:00:00"
        hour_meter.setOnChronometerTickListener {
            recordingTimer++
            it.text = formatMiss(recordingTimer)
        }
        characteristic = cameraManager.getCameraCharacteristics(args.id)
        viewFinder = view_finder as SquareCameraPreview

        mFocusImage = img_focus as AnimationImageView
        mFocusImage.visibility = View.INVISIBLE
        mFocusImage.setmMainHandler(mMainHandler)
        mFocusImage.setmAnimation(mScaleFocusAnimation)
        viewFinder.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceChanged(
                holder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int
            ) = Unit

            override fun surfaceDestroyed(holder: SurfaceHolder) = Unit

            @SuppressLint("ClickableViewAccessibility")
            override fun surfaceCreated(holder: SurfaceHolder) {
                //选择合适的预览尺寸并配置SurfaceView
                val previewSize = getPreviewOutputSize(
                    viewFinder.display, characteristic, SurfaceHolder::class.java
                )
                //设置 宽高
                viewFinder.setAspectRatio(previewSize.width, previewSize.height)

                //为了确保配置完成后打开Camera
                viewFinder.post {
                    //首次初始化摄像头
                    initCamera()
                    //切换摄像头点击事件
                    switchCamera.setOnClickListener {
                        //切换摄像头
                        reOpenCamera()
                    }
                    // 对用户触摸捕获按钮做出反应
                    start_recording.setOnTouchListener { view, event ->
                        hour_meter.start()
                        when (event.action) {

                            MotionEvent.ACTION_DOWN -> lifecycleScope.launch(Dispatchers.IO) {
                                if (isVideoTape.not()) {
                                    // 防止视频录制期间屏幕旋转
                                    requestedOrientation =
                                        ActivityInfo.SCREEN_ORIENTATION_LOCKED
                                    //添加 视频接收面
                                    previewRequestBuilder.addTarget(recorderSurface)
                                    // 开始记录重复的请求，这将停止正在进行的预览
                                    //  重复请求而不必显式调用session.stopRepeating
                                    session.setRepeatingRequest(
                                        previewRequestBuilder.build(),
                                        null,
                                        cameraHandler
                                    )

                                    // 完成录音机设置并开始录音
                                    recorder.apply {
                                        // 在启动时根据当前传感器值设置输出方向
                                        relativeOrientation.value?.let { setOrientationHint(it) }
                                        prepare()
                                        start()
                                    }
                                    recordingStartMillis = System.currentTimeMillis()
                                    Log.d(TAG, "录制开始")


                                }

                            }

                            MotionEvent.ACTION_UP ->

                                lifecycleScope.launch(Dispatchers.IO) {
                                    if (isVideoTape.not()) {
                                        isVideoTape = true
                                        runOnUiThread {
                                            start_recording.setBackgroundResource(R.mipmap.shooting)
                                        }

                                    } else {
                                        // 录制完成后解锁屏幕旋转
                                        requestedOrientation =
                                            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

                                        // 需要至少记录MIN_REQUIRED_RECORDING_TIME_MILLIS
                                        val elapsedTimeMillis =
                                            System.currentTimeMillis() - recordingStartMillis
                                        if (elapsedTimeMillis < MIN_REQUIRED_RECORDING_TIME_MILLIS) {
                                            delay(MIN_REQUIRED_RECORDING_TIME_MILLIS - elapsedTimeMillis)
                                        }
                                        val path = outputFile.path
                                        recorder.stop()


                                        //将媒体文件广播到系统的其余部分
                                        MediaScannerConnection.scanFile(
                                            view.context,
                                            arrayOf(outputFile.absolutePath),
                                            null,
                                            null
                                        )
                                        runOnUiThread {
                                            start_recording.setBackgroundResource(R.mipmap.screen)
                                            iEasyCamera2OutVideoPath?.invoke(path)
                                        }
                                        // 完成当前的相机屏幕
                                        delay(ANIMATION_SLOW_MILLIS)
                                        finish()
                                    }

                                }
                        }

                        true
                    }
                }
                im_right.setOnClickListener {

                    // 禁用点击侦听器以防止同时执行多个请求
                    it.isEnabled = false

                    //在不同的范围内执行I / O繁重的操作
                    lifecycleScope.launch(Dispatchers.IO) {
                        takePhoto().use { result ->
                            Log.d(TAG, "收到结果: $result")
                            previewRequestBuilder.removeTarget(imageReader.surface)

                            updatePreview()
                            // 将结果保存到磁盘
                            val output = saveResult(result)

                            Log.d(TAG, "图片已保存: ${output.absolutePath}")

                            // 如果结果是JPEG文件，请使用方向信息更新EXIF元数据
                            if (output.extension == "jpg") {
                                val exif = ExifInterface(output.absolutePath)
                                exif.setAttribute(
                                    ExifInterface.TAG_ORIENTATION, result.orientation.toString()
                                )
                                exif.saveAttributes()
                                Log.d(TAG, "EXIF 元数据已保存: ${output.absolutePath}")
                            }
                            runOnUiThread {
                                val path = output.absolutePath
                                iEasyCamera2OutImagePath?.invoke(path)
                            }
                        }

                        // 拍照后重新启用点击侦听器
                        it.post { it.isEnabled = true }

                    }
                }
            }

        })
        // 用于旋转输出介质以匹配设备方向
        relativeOrientation = OrientationLiveData(this, characteristic).apply {
            observe(this@Camera2Activity, Observer { orientation ->
                Log.d("当前", "方向改变: $orientation")
            })
        }
        viewFinder.setSeekBarUpData(this)
        seek_bar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                //得到当前seekBar的最大值
                val max = seekBar?.max
                //因为seekbar的起始值为0而手机缩放的起始值为1所以加10
                val a = max?.plus(10)?.let { maxValue?.div(it) }
                val pre = (progress + 10) * a!!

                val format = String.format("%.1f", pre)
                if (format != "0.9") {
                    bigPrint.text = format
                }

                zoom.setZoom(previewRequestBuilder, pre)
                updatePreview()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                bigPrint.visibility = View.VISIBLE
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                bigPrint.visibility = View.GONE
            }

        })
        initAnimation()
        initFocusImage()
//        getDeployIntent()
    }

//    /**
//     * 获取传递过来的接口对象
//     */
//    private  fun  getDeployIntent(){
//        iEasyCamera2OutImagePath = intent.getSerializableExtra("iEasyCamera2OutImagePath") as IEasyCamera2OutImagePath
//        iEasyCamera2OutVideoPath = intent.getSerializableExtra("iEasyCamera2OutVideoPath") as IEasyCamera2OutVideoPath
//    }
    /**
     * 这样做是为了获得mFocusImage的高度和宽度
     */
    private fun initFocusImage() {
        val layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        )
        layoutParams.gravity = Gravity.CENTER
        mFocusImage.layoutParams = layoutParams
        mFocusImage.initFocus()
    }

    /**
     * 初始化动画效果
     */
    private fun initAnimation() {
        mShowAction.duration = 500
        mScaleFocusAnimation.duration = 200
        mScaleWindowAnimation.duration = 500
        mAlphaInAnimation.duration = 500
        mAlphaOutAnimation.duration = 500
    }

    /** 更新Preview*/
    private fun updatePreview() {

        session.setRepeatingRequest(
            previewRequestBuilder.build(),
            mPreviewSessionCallback,
            cameraHandler
        )

    }

    // 将秒转化成小时分钟秒
    private fun formatMiss(miss: Int): String? {
        val hh =
            if (miss / 3600 > 9) (miss / 3600).toString() + "" else "0" + miss / 3600
        val mm =
            if (miss % 3600 / 60 > 9) (miss % 3600 / 60).toString() + "" else "0" + miss % 3600 / 60
        val ss =
            if (miss % 3600 % 60 > 9) (miss % 3600 % 60).toString() + "" else "0" + miss % 3600 % 60
        return "$hh:$mm:$ss"
    }

    /**
     *使用提供的 surface 作为输入 创建 MediaRecorder实例
     */
    private fun createRecorder(surface: Surface) = MediaRecorder().apply {
        setAudioSource(MediaRecorder.AudioSource.MIC)
        setVideoSource(MediaRecorder.VideoSource.SURFACE)
        setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        setOutputFile(outputFile.absolutePath)
        setVideoEncodingBitRate(RECORDER_VIDEO_BITRATE)
        if (args.fps > 0) setVideoFrameRate(args.fps)
        setVideoSize(args.width, args.height)
        setVideoEncoder(MediaRecorder.VideoEncoder.H264)
        setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        setInputSurface(surface)
    }

    /**
     * 用于使用[CameraDevice.TEMPLATE_STILL_CAPTURE]捕获静止图像的助手功能
     * 模板。它在[CaptureResult]和[Image]结果之间执行同步
     *从单个捕获中获取，并输出一个[CombinedCaptureResult]对象。
     */
    private suspend fun takePhoto():
            CombinedCaptureResult = suspendCoroutine { cont ->

        // 冲洗残留在图像读取器中的所有图像
        @Suppress("ControlFlowWithEmptyBody")
        while (imageReader.acquireNextImage() != null) {
        }

        // 启动新的图像队列
        val imageQueue = ArrayBlockingQueue<Image>(IMAGE_BUFFER_SIZE)
        imageReader.setOnImageAvailableListener({ reader ->
            val image = reader.acquireNextImage()
            Log.d(TAG, "图片在队列中可用: ${image.timestamp}")
            imageQueue.add(image)
        }, imageReaderHandler)
        previewRequestBuilder.addTarget(imageReader.surface)
        session.capture(
            previewRequestBuilder.build(),
            object : CameraCaptureSession.CaptureCallback() {

                override fun onCaptureStarted(
                    session: CameraCaptureSession,
                    request: CaptureRequest,
                    timestamp: Long,
                    frameNumber: Long
                ) {
                    super.onCaptureStarted(session, request, timestamp, frameNumber)

                }

                override fun onCaptureCompleted(
                    session: CameraCaptureSession,
                    request: CaptureRequest,
                    result: TotalCaptureResult
                ) {
                    super.onCaptureCompleted(session, request, result)
                    val resultTimestamp = result.get(CaptureResult.SENSOR_TIMESTAMP)
                    Log.d(TAG, "捕获结果已收到: $resultTimestamp")

                    //设置超时以防捕获的图像从管道中丢失
                    val exc = TimeoutException("图像出队时间过长")
                    val timeoutRunnable = Runnable {
                        cont.resumeWithException(exc)
                    }
                    imageReaderHandler.postDelayed(timeoutRunnable, IMAGE_CAPTURE_TIMEOUT_MILLIS)

                    // 在协程的上下文中循环，直到带有匹配时间戳的图像出现为止
                    // 我们需要再次启动协程上下文，因为回调是在
                    // 提供给`capture`方法的处理程序，不在我们的协程环境中
                    @Suppress("BlockingMethodInNonBlockingContext")
                    lifecycleScope.launch(cont.context) {
                        while (true) {

                            // 时间戳不匹配时出队图像
                            val image = imageQueue.take()
                            // TODO(owahltinez): b/142011420

                            Log.d(TAG, "匹配图像出队: ${image.timestamp}")

                            // 取消设置图片阅读器监听器
                            imageReaderHandler.removeCallbacks(timeoutRunnable)
                            imageReader.setOnImageAvailableListener(null, null)

                            //清除图像队列（如果还有）
                            while (imageQueue.size > 0) {
                                imageQueue.take().close()
                            }

                            // 计算EXIF方向元数据
                            val rotation = relativeOrientation.value ?: 0
                            val mirrored = characteristic.get(CameraCharacteristics.LENS_FACING) ==
                                    CameraCharacteristics.LENS_FACING_FRONT
                            val exifOrientation = computeExifOrientation(rotation, mirrored)

                            //建立结果并恢复进度
                            cont.resume(
                                CombinedCaptureResult(
                                    image, result, exifOrientation, imageReader.imageFormat
                                )
                            )

                            //无需中断，协程将暂停
                        }
                    }
                }
            },
            cameraHandler
        )
    }

    /** 用于将[CombinedCaptureResult]保存到[File]中的Helper函数*/
    private suspend fun saveResult(result: CombinedCaptureResult): File = suspendCoroutine { cont ->
        when (result.format) {

            // 当格式为JPEG我们可以按原样保存字节
            ImageFormat.JPEG -> {
                val buffer = result.image.planes[0].buffer
                val bytes = ByteArray(buffer.remaining()).apply { buffer.get(this) }
                try {
                    val output = createFile("jpg")
                    FileOutputStream(output).use { it.write(bytes) }
                    cont.resume(output)
                } catch (exc: IOException) {
                    Log.e(TAG, "无法将JPEG图像写入文件", exc)
                    cont.resumeWithException(exc)
                }
            }

            // 当格式为RAW时，我们使用DngCreator实用程序库
            ImageFormat.RAW_SENSOR -> {
                val dngCreator = DngCreator(characteristic, result.metadata)
                try {
                    val output = createFile("dng")
                    FileOutputStream(output).use { dngCreator.writeImage(it, result.image) }
                    cont.resume(output)
                } catch (exc: IOException) {
                    Log.e(TAG, "无法将DNG图片写入文件", exc)
                    cont.resumeWithException(exc)
                }
            }

            // 不支持其他格式
            else -> {
                val exc = RuntimeException("图片格式未知: ${result.image.format}")
                Log.e(TAG, exc.message, exc)
                cont.resumeWithException(exc)
            }
        }
    }

    /**
     * 在主线程中的线程中开始所有相机的操作 ，
     */
    private fun initCamera() = lifecycleScope.launch(Dispatchers.Main) {

        //打开选定的相机
        camera = openCamera(cameraManager, args.id, cameraHandler)
        // 初始化图像读取器，该图像读取器将用于捕获静态照片
        val size = characteristic.get(
            CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP
        )!!
            .getOutputSizes(args.pixelFormat).maxBy { it.height * it.width }!!
        imageReader = ImageReader.newInstance(
            size.width, size.height, args.pixelFormat, IMAGE_BUFFER_SIZE
        )
        //创建相机输出帧的面列表
        val targets = listOf(viewFinder.holder.surface, recorderSurface, imageReader.surface)
        //使用 打开的相机和配合好的面开始捕捉会话  //尽可能的频繁发送 捕获请求 ，直到 会话结束 即  session.stopRepeating（）被调用
        session = createCaptureSession(camera, targets, cameraHandler)

        //捕获请求保留对目标曲面的引用
        /*
        *   TEMPLATE_PREVIEW：适用于配置预览的模板。
            TEMPLATE_RECORD：适用于视频录制的模板。
            TEMPLATE_STILL_CAPTURE：适用于拍照的模板。
            TEMPLATE_VIDEO_SNAPSHOT：适用于在录制视频过程中支持拍照的模板。
            TEMPLATE_MANUAL：适用于希望自己手动配置大部分参数的模板。
        * */
        previewRequestBuilder =
            camera.createCaptureRequest(CameraDevice.TEMPLATE_VIDEO_SNAPSHOT).apply {
                // 添加预览和记录曲面目标
                addTarget(viewFinder.holder.surface)
//                // 为所有目标设置用户请求的FPS
//                set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, Range(args.fps, args.fps))
            }

        maxValue = characteristic.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM)

        zoom = Zoom(characteristic)
        viewFinder.setCamera(session, characteristic, previewRequestBuilder, cameraHandler)
        //设置自动对焦模式
        previewRequestBuilder.set(
            CaptureRequest.CONTROL_AF_MODE,
            CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_VIDEO
        )
        //设置白平衡
        previewRequestBuilder.set(
            CaptureRequest.CONTROL_AWB_MODE,
            CameraMetadata.CONTROL_AWB_MODE_AUTO
        )
        viewFinder.setMySurfaceTouchEvent(
            SurfaceTouchEvent(
                characteristic,
                viewFinder,
                previewRequestBuilder,
                session,
                cameraHandler,
                mPreviewSessionCallback
            )
        )
        updatePreview()

    }

    /** 手指缩放同步 SeekBar*/
    override fun progressUpdate(progress: Float) {
        //得到当前seekBar的最大值
        val max = seek_bar.max
        //因为seekbar的起始值为0而手机缩放的起始值为1所以加10
        val a = max.plus(10).let { maxValue?.div(it) }
        val result: Int = (progress / a!! - 10).roundToInt()
        seek_bar.progress = result
    }

    /**
     * 切换前后摄像头
     */
    private fun reOpenCamera() {
        mFocusImage.stopFocus()
        when (args.id) {
            "1" -> {
                args.id = "0"
            }
            "0" -> {
                args.id = "1"
            }
            else -> {
                args.id = "0"
            }
        }
        //关闭相机再开启另外个摄像头
        camera.close()
        // 根据摄像头 id 获取当前摄像头特性
        characteristic = cameraManager.getCameraCharacteristics(args.id)
        //重新进行摄像头初始化
        initCamera()
    }


    /**
     *  创建一个 CameraCaptureSession 并返回会话
     *
     */
    private suspend fun createCaptureSession(
        device: CameraDevice,
        targets: List<Surface>,
        handler: Handler? = null
    ): CameraCaptureSession = suspendCoroutine {

        // 使用预定义的目标创建捕获会话，并定义会话状态
        // 会话配置完成后恢复协程的回调
        device.createCaptureSession(targets, object : CameraCaptureSession.StateCallback() {
            override fun onConfigureFailed(session: CameraCaptureSession) {
                val exc = RuntimeException("Camera ${device.id} 会话配置失败")
                it.resumeWithException(exc)
            }

            override fun onConfigured(session: CameraCaptureSession) {

                it.resume(session)
            }

        }, handler)
    }

    /** 使用协程 打开相机并 返回打开的设备*/

    @SuppressLint("MissingPermission")
    private suspend fun openCamera(
        manager: CameraManager,
        cameraId: String,
        handler: Handler? = null
    ): CameraDevice = suspendCancellableCoroutine {
        manager.openCamera(cameraId, object : CameraDevice.StateCallback() {
            override fun onOpened(camera: CameraDevice) = it.resume(camera)

            override fun onDisconnected(camera: CameraDevice) {
                finish()
            }

            override fun onError(camera: CameraDevice, error: Int) {
                val msg = when (error) {
                    ERROR_CAMERA_DEVICE -> "Fatal (device)"
                    ERROR_CAMERA_DISABLED -> "Device policy"
                    ERROR_CAMERA_IN_USE -> "Camera in use"
                    ERROR_CAMERA_SERVICE -> "Fatal (service)"
                    ERROR_MAX_CAMERAS_IN_USE -> "Maximum cameras in use"
                    else -> "Unknown"
                }
                val exc = RuntimeException("Camera $cameraId error: ($error) $msg")
                if (it.isActive) it.resumeWithException(exc)
            }

        }, handler)

    }

    /** 将旋转和镜像信息转换为[ExifInterface]常量之一*/
    fun computeExifOrientation(rotationDegrees: Int, mirrored: Boolean) = when {
        rotationDegrees == 0 && !mirrored -> ExifInterface.ORIENTATION_NORMAL
        rotationDegrees == 0 && mirrored -> ExifInterface.ORIENTATION_FLIP_HORIZONTAL
        rotationDegrees == 180 && !mirrored -> ExifInterface.ORIENTATION_ROTATE_180
        rotationDegrees == 180 && mirrored -> ExifInterface.ORIENTATION_FLIP_VERTICAL
        rotationDegrees == 270 && mirrored -> ExifInterface.ORIENTATION_TRANSVERSE
        rotationDegrees == 90 && !mirrored -> ExifInterface.ORIENTATION_ROTATE_90
        rotationDegrees == 90 && mirrored -> ExifInterface.ORIENTATION_TRANSPOSE
        rotationDegrees == 270 && mirrored -> ExifInterface.ORIENTATION_ROTATE_270
        rotationDegrees == 270 && !mirrored -> ExifInterface.ORIENTATION_TRANSVERSE
        else -> ExifInterface.ORIENTATION_UNDEFINED
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraThread.quitSafely()
        imageReaderThread.quitSafely()
        recorder.release()
        recorderSurface.release()
    }

    override fun onStop() {
        super.onStop()
        try {
            camera.close()
        } catch (exc: Throwable) {
            Log.e(TAG, "关闭相机时出错", exc)
        }
    }

    companion object {
        private val TAG = Camera2Activity::class.java.simpleName
        private const val RECORDER_VIDEO_BITRATE: Int = 10_000_000
        private const val MIN_REQUIRED_RECORDING_TIME_MILLIS: Long = 1000L
        const val FOCUS_DISAPPEAR = 100
        const val WINDOW_TEXT_DISAPPEAR = 101
        const val FOCUS_AGAIN = 102
        const val ANIMATION_SLOW_MILLIS = 100L

        /** 将保存在读取器缓冲区中的最大图像数 */
        private const val IMAGE_BUFFER_SIZE: Int = 3

        /** Maximum time allowed to wait for the result of an image capture */
        private const val IMAGE_CAPTURE_TIMEOUT_MILLIS: Long = 5000

        /** 帮助程序数据类，用于保存捕获元数据及其关联的图像 */
        data class CombinedCaptureResult(
            val image: Image,
            val metadata: CaptureResult,
            val orientation: Int,
            val format: Int
        ) : Closeable {
            override fun close() = image.close()
        }

        /**
         *创建一个以当前时间为名字的文件 用于存储照相机拍摄的资源
         */
        private fun createFile(extension: String): File {
            val sdf = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss_SSS", Locale.CHINA)
            val absoluteFile = Environment.getExternalStorageDirectory().absoluteFile.path
            return File("$absoluteFile/EasyCamera2/", "VID_${sdf.format(Date())}.$extension")
        }
    }

}