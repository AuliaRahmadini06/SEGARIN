package com.example.deteksikesegaranbuah

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class KameraActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var overlayView: OverlayView
    private lateinit var tvStatus: TextView
    private lateinit var detector: YoloDetector
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var tracker: DeteksiTracker

    private var frameCounter = 0

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startCamera()
        } else {
            Toast.makeText(this, "Izin kamera diperlukan untuk mendeteksi buah", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_kamera)

        previewView = findViewById(R.id.previewView)
        overlayView = findViewById(R.id.overlayView)
        tvStatus = findViewById(R.id.tvStatusKamera)

        detector = YoloDetector(this)
        tracker = DeteksiTracker()
        cameraExecutor = Executors.newSingleThreadExecutor()

        overlayView.setOnTapListener { deteksi ->
            PenjelasanHelper.tampilkan(
                context = this,
                namaKelas = deteksi.namaKelas,
                confidence = deteksi.confidence // <-- Pastikan parameter ini diisi (bisa berupa deteksi.confidence atau deteksi.score)
            )
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        overlayView.invalidate()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val imageAnalysis = ImageAnalysis.Builder()
                .setTargetResolution(android.util.Size(416, 416))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                processFrame(imageProxy)
            }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis)
                runOnUiThread { tvStatus.text = "Kamera aktif" }
            } catch (e: Exception) {
                Log.e("KameraActivity", "Gagal bind kamera", e)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun processFrame(imageProxy: ImageProxy) {
        frameCounter++
        if (frameCounter % 3 != 0) {
            imageProxy.close()
            return
        }

        try {
            val bitmap = imageProxyToBitmap(imageProxy)
            val hasilMentah = detector.deteksi(bitmap)

            // Transformasi koordinat dari ruang bitmap ke ruang tampilan layar (FILL_CENTER)
            val hasilTransformasi = transformKoordinat(hasilMentah, bitmap.width, bitmap.height)

            // Terapkan tracking: gabungkan box yang tumpang tindih (dedup) di frame ini
            val hasilTerlacak = tracker.update(hasilTransformasi)

            runOnUiThread {
                overlayView.setDeteksi(hasilTerlacak)
                tvStatus.text = if (hasilTerlacak.isEmpty()) "Tidak ada buah terdeteksi"
                else "${hasilTerlacak.size} buah terdeteksi"
            }
        } catch (e: Exception) {
            Log.e("KameraActivity", "Error saat proses frame", e)
        } finally {
            imageProxy.close()
        }
    }

    private fun transformKoordinat(
        deteksiList: List<Deteksi>,
        imageWidth: Int,
        imageHeight: Int
    ): List<Deteksi> {
        val viewWidth = overlayView.width.toFloat()
        val viewHeight = overlayView.height.toFloat()

        if (viewWidth == 0f || viewHeight == 0f) return deteksiList

        // FILL_CENTER = scale terbesar biar penuh, lalu crop bagian yang kelebihan (center)
        val scale = maxOf(viewWidth / imageWidth, viewHeight / imageHeight)
        val offsetX = (imageWidth * scale - viewWidth) / 2f
        val offsetY = (imageHeight * scale - viewHeight) / 2f

        return deteksiList.map { deteksi ->
            val newBox = android.graphics.RectF(
                deteksi.box.left * scale - offsetX,
                deteksi.box.top * scale - offsetY,
                deteksi.box.right * scale - offsetX,
                deteksi.box.bottom * scale - offsetY
            )
            deteksi.copy(box = newBox)
        }
    }

    private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap {
        val bitmap = imageProxy.toBitmap()
        val matrix = Matrix()
        matrix.postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    override fun onDestroy() {
        super.onDestroy()
        detector.close()
        cameraExecutor.shutdown()
    }
}