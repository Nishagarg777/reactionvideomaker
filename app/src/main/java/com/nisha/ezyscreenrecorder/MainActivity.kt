package com.nisha.ezyscreenrecorder

import android.Manifest

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.Button
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.nisha.ezyscreenrecorder.Utils.hasPermissions
import com.nisha.ezyscreenrecorder.Utils.showVideoSavedNotification
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.ExecutionException
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min


class MainActivity : AppCompatActivity() {

    companion object {
        const val COUNT_DOWN = 3
        val PARENT_DIRECTORY: String = Environment.DIRECTORY_DOCUMENTS
        const val DIRECTORY = "Ezy Recordings"
    }
    var cameraFacing: Int = CameraSelector.LENS_FACING_FRONT
    private lateinit var mediaProjectionManager: MediaProjectionManager
    var selectedItem:String?=null
    var isRecordingState:Boolean?=false


    private lateinit var cameraview: PreviewView
    private lateinit var endVideo:Button

    private var exoPlayer: ExoPlayer? = null

    private lateinit var playerView: StyledPlayerView


    private val mainAppViewModel: MainAppViewModel by viewModels()
    private val requestPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (Manifest.permission.RECORD_AUDIO in permissions) {
                if (permissions[Manifest.permission.RECORD_AUDIO] == true) {
                    startRecording()
                }
            }
//            if (Manifest.permission.POST_NOTIFICATIONS in permissions) {
//                if (permissions[Manifest.permission.POST_NOTIFICATIONS] == true) {
//
//                }
//            }
        }

    private val isNotificationPermissionGranted: Boolean
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasPermissions(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            true
        }

    private val isRecordAudioPermissionGranted: Boolean
        get() = hasPermissions(Manifest.permission.RECORD_AUDIO)



    private val activityResultLauncher: ActivityResultLauncher<String> =
        registerForActivityResult<String, Boolean>(
            ActivityResultContracts.RequestPermission(),
            { result ->
                if (result) {
                    startCamera(cameraFacing)
                }
            })

    private fun aspectRatio(width: Int, height: Int): Int {
        val previewRatio =
            max(width.toDouble(), height.toDouble()) / min(width.toDouble(), height.toDouble())
        return if (abs(previewRatio - 4.0 / 3.0) <= abs(previewRatio - 16.0 / 9.0)) {
            AspectRatio.RATIO_4_3
        } else AspectRatio.RATIO_16_9
    }
    fun startCamera(cameraFacing: Int) {
        val aspectRatio = aspectRatio(cameraview?.width?:0, 400?:0)
        val listenableFuture = ProcessCameraProvider.getInstance(this)
        listenableFuture.addListener({
            try {
                val cameraProvider =
                    listenableFuture.get() as ProcessCameraProvider
                val preview =
                    Preview.Builder().setTargetAspectRatio(aspectRatio).build()
                val imageCapture =
                    ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .setTargetRotation(windowManager.defaultDisplay.rotation).build()
                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(cameraFacing).build()
                cameraProvider.unbindAll()
                val camera =
                    cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)

                preview.setSurfaceProvider(cameraview?.getSurfaceProvider())
            } catch (e: ExecutionException) {
                e.printStackTrace()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(this))



        if (!isRecordAudioPermissionGranted) {
            requestPermissionsLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
        } else{
            startRecording()
        }
    }



    private fun startForegroundService() {
        val serviceIntent = Intent(InProgressRecordingNotificationService.START_RECORDING).also {
            it.setClass(this, InProgressRecordingNotificationService::class.java)
        }
        ActivityCompat.startForegroundService(this, serviceIntent)
    }

    private fun startForegroundServiceReally() {
        val serviceIntent =
            Intent(InProgressRecordingNotificationService.START_RECORDING_REALLY).also {
                it.setClass(this, InProgressRecordingNotificationService::class.java)
            }
        ActivityCompat.startForegroundService(this, serviceIntent)
    }

    private fun stopForegroundService() {
        val serviceIntent = Intent(InProgressRecordingNotificationService.STOP_RECORDING).also {
            it.setClass(this, InProgressRecordingNotificationService::class.java)
        }
        ActivityCompat.startForegroundService(this, serviceIntent)
    }



    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.getStringExtra("action") == "STOP") {
                stopRecording()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        getSupportActionBar()?.hide();
        endVideo=findViewById(R.id.endVideo1)
        endVideo.setOnClickListener(object :View.OnClickListener {
            override fun onClick(v: View?) {
               stopRecording()
            }
        })

        cameraview = findViewById(R.id.cameraPreview)
        playerView = findViewById(R.id.playerView)

         selectedItem = intent.getStringExtra("selectedItem")

        mediaProjectionManager =
            ContextCompat.getSystemService(this, MediaProjectionManager::class.java)!!







        if (ContextCompat.checkSelfPermission(
                this@MainActivity,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            activityResultLauncher.launch(Manifest.permission.CAMERA)
        } else {
            startCamera(cameraFacing)



        }

//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//            if (isNotificationPermissionGranted) {
//
//            } else {
//                requestPermissionsLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
//            }
//        } else {
//
//        }


        val intentFilter = IntentFilter("$packageName.RECORDING_EVENT")
        val receiverFlags = ContextCompat.RECEIVER_NOT_EXPORTED
        ContextCompat.registerReceiver(this, broadcastReceiver, intentFilter, receiverFlags)





//        stopButton.setOnClickListener {
//            stopRecording()
//        }

//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//            if (isNotificationPermissionGranted) {
//
//            } else {
//                requestPermissionsLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
//            }
//        } else {
//
//        }



    }
    private val requestScreenCapture =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val resultCode = result.resultCode
            if (resultCode != RESULT_OK) {


                return@registerForActivityResult
            }
            val data = result.data ?: return@registerForActivityResult

            startForegroundService()

            lifecycleScope.launch {

                repeat(COUNT_DOWN) {
                    delay(1000)
                }

                val lMediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data)
                val fileName = String.format(
                    "Recording_%s.mp4",
                    SimpleDateFormat("dd_MM_yyyy_hh_mm_ss_a", Locale.ENGLISH).format(
                        Calendar.getInstance().time
                    )
                )
                val folder =
                    File(
                        Environment.getExternalStoragePublicDirectory(PARENT_DIRECTORY),
                        DIRECTORY
                    )
                if (!folder.exists()) {
                    folder.mkdir()
                }
                val file = File(folder, fileName)
                val isStarted =
                    mainAppViewModel.startRecording(this@MainActivity, file, lMediaProjection)

                if (isStarted) {

                    isRecordingState=true
                    startForegroundServiceReally()

                } else {

                    stopForegroundService()
                }
            }

        }
    private fun startRecording() {
        Handler().postDelayed(Runnable {
            preparePlayer()
                                   //    playYouTubeVideo()
        }, 2000)


        requestScreenCapture.launch(mediaProjectionManager.createScreenCaptureIntent())

    }

    private fun playYouTubeVideo() {
        val webView = findViewById<WebView>(R.id.webView)
        val video =
            "<iframe width=\"100%\" height=\"100%\" src=\"https://youtu.be/jLshY-zUfZ4?si=oxihs4EuPRdnP_ey\" title=\"YouTube video player\" frameborder=\"0\" allow=\"accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share\" allowfullscreen></iframe>"
        webView.loadData(video, "text/html", "utf-8")
        webView.getSettings().javaScriptEnabled = true
        webView.setWebChromeClient(WebChromeClient())

    }

    private fun stopRecording() {
        if (mainAppViewModel.isRecording()) {

            if(isRecordingState==true) {
                stopForegroundService()
                try {
                    val outFile = mainAppViewModel.stopRecording()

                    showVideoSavedNotification(outFile)
                }catch (e:Exception){

                }

            }
        }

    }
    private fun preparePlayer() {
        exoPlayer = ExoPlayer.Builder(this).build()
        playerView.player = exoPlayer
        playerView.setUseController(false);

        val mediaItem = MediaItem.fromUri(Uri.parse(selectedItem))

        exoPlayer?.apply {
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = true





        }
        exoPlayer?.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    Player.STATE_READY -> {

                    }
                    Player.STATE_ENDED -> {
                      stopRecording()
                    }
                    // Add more states as needed
                }
            }
        })
        playerView.layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
        playerView.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
    }

    override fun onStop() {
        super.onStop()
        exoPlayer?.release()
        stopRecording()
       // unregisterReceiver(broadcastReceiver)

    }

    override fun onPause() {
        super.onPause()
        exoPlayer?.release()
       stopRecording()
       // unregisterReceiver(broadcastReceiver)
    }

    override fun onDestroy() {
        super.onDestroy()
        exoPlayer?.release()
        stopRecording()
    }

}