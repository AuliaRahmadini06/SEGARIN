package com.example.deteksikesegaranbuah

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import kotlin.math.max
import kotlin.math.min

class YoloDetector(context: Context) {

    private var interpreter: Interpreter
    private val inputSize = 416
    private val numClasses = 6
    private val confThreshold = 0.60f
    private val iouThreshold = 0.45f

    private val namaKelas = listOf(
        "Pisang Segar", "Pisang Tidak Segar",
        "Tomat Segar", "Tomat Tidak Segar",
        "Mangga Segar", "Mangga Tidak Segar"
    )

    init {
        val model = loadModelFile(context, "best.tflite")
        val options = Interpreter.Options()
        interpreter = Interpreter(model, options)

        // Debug: cetak shape input & output ke Logcat, buat verifikasi asumsi
        val inputShape = interpreter.getInputTensor(0).shape()
        val outputShape = interpreter.getOutputTensor(0).shape()
        Log.d("YoloDetector", "Input shape: ${inputShape.contentToString()}")
        Log.d("YoloDetector", "Output shape: ${outputShape.contentToString()}")
    }

    private fun loadModelFile(context: Context, filename: String): ByteBuffer {
        val fileDescriptor = context.assets.openFd(filename)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    fun deteksi(bitmap: Bitmap): List<Deteksi> {
        val (letterboxed, scale, padX, padY) = letterboxResize(bitmap)
        val inputBuffer = bitmapToByteBuffer(letterboxed)

        // Output shape asumsi: (1, 10, 3549) -> 4 box coord + 6 kelas, per anchor
        // PENTING: cek Logcat "Output shape" saat run pertama kali, pastikan urutan
        // dimensinya benar-benar (1, 10, 3549) dan bukan (1, 3549, 10) atau lainnya.
        // Kalau beda, box hasil decode bakal ngaco ukuran & posisinya.
        val numAnchors = 3549
        val output = Array(1) { Array(10) { FloatArray(numAnchors) } }

        interpreter.run(inputBuffer, output)

        val hasilMentah = mutableListOf<Deteksi>()

        for (i in 0 until numAnchors) {
            val cx = output[0][0][i]
            val cy = output[0][1][i]
            val w = output[0][2][i]
            val h = output[0][3][i]

            var bestClassIdx = -1
            var bestClassScore = 0f
            for (c in 0 until numClasses) {
                val score = output[0][4 + c][i]
                if (score > bestClassScore) {
                    bestClassScore = score
                    bestClassIdx = c
                }
            }

            if (bestClassScore >= confThreshold && bestClassIdx >= 0) {
                // Koordinat dalam skala piksel 0-416 (ruang letterbox)
                val cxPixel = if (cx <= 1f) cx * inputSize else cx
                val cyPixel = if (cy <= 1f) cy * inputSize else cy
                val wPixel = if (w <= 1f) w * inputSize else w
                val hPixel = if (h <= 1f) h * inputSize else h

                // Hapus padding letterbox, lalu kembalikan ke skala bitmap asli
                val left = ((cxPixel - wPixel / 2) - padX) / scale
                val top = ((cyPixel - hPixel / 2) - padY) / scale
                val right = ((cxPixel + wPixel / 2) - padX) / scale
                val bottom = ((cyPixel + hPixel / 2) - padY) / scale

                hasilMentah.add(
                    Deteksi(
                        namaKelas = namaKelas[bestClassIdx],
                        confidence = bestClassScore,
                        box = RectF(left, top, right, bottom)
                    )
                )
            }
        }

        return terapkanNMS(hasilMentah)
    }

    private fun terapkanNMS(deteksiList: List<Deteksi>): List<Deteksi> {
        val hasilAkhir = mutableListOf<Deteksi>()
        val terurut = deteksiList.sortedByDescending { it.confidence }.toMutableList()

        while (terurut.isNotEmpty()) {
            val terbaik = terurut.removeAt(0)
            hasilAkhir.add(terbaik)

            val iterator = terurut.iterator()
            while (iterator.hasNext()) {
                val lainnya = iterator.next()
                // Class-agnostic: box lain yang tumpang tindih posisi ikut dibuang,
                // TIDAK peduli kelasnya sama atau beda. Ini penting karena pasangan
                // kelas "Segar"/"Tidak Segar" bisa saling numpuk di objek fisik yang sama
                // ketika model masih ragu-ragu -> tanpa ini, satu buah bisa dapat 2+ box.
                if (hitungIoU(terbaik.box, lainnya.box) > iouThreshold) {
                    iterator.remove()
                }
            }
        }
        return hasilAkhir
    }

    private fun hitungIoU(a: RectF, b: RectF): Float {
        val interLeft = max(a.left, b.left)
        val interTop = max(a.top, b.top)
        val interRight = min(a.right, b.right)
        val interBottom = min(a.bottom, b.bottom)

        val interArea = max(0f, interRight - interLeft) * max(0f, interBottom - interTop)
        val areaA = (a.right - a.left) * (a.bottom - a.top)
        val areaB = (b.right - b.left) * (b.bottom - b.top)

        return interArea / (areaA + areaB - interArea + 1e-6f)
    }

    private fun bitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val buffer = ByteBuffer.allocateDirect(4 * inputSize * inputSize * 3)
        buffer.order(ByteOrder.nativeOrder())

        val pixels = IntArray(inputSize * inputSize)
        bitmap.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize)

        // Format NCHW: semua nilai R dulu, baru semua G, baru semua B
        for (pixel in pixels) {
            buffer.putFloat(((pixel shr 16) and 0xFF) / 255.0f) // R
        }
        for (pixel in pixels) {
            buffer.putFloat(((pixel shr 8) and 0xFF) / 255.0f)  // G
        }
        for (pixel in pixels) {
            buffer.putFloat((pixel and 0xFF) / 255.0f)          // B
        }

        buffer.rewind()
        return buffer
    }

    private data class LetterboxResult(
        val bitmap: Bitmap,
        val scale: Float,
        val padX: Float,
        val padY: Float
    )

    private fun letterboxResize(bitmap: Bitmap): LetterboxResult {
        val scale = min(
            inputSize.toFloat() / bitmap.width,
            inputSize.toFloat() / bitmap.height
        )
        val newWidth = (bitmap.width * scale).toInt()
        val newHeight = (bitmap.height * scale).toInt()

        val resized = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)

        val padX = (inputSize - newWidth) / 2f
        val padY = (inputSize - newHeight) / 2f

        val output = Bitmap.createBitmap(inputSize, inputSize, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(output)
        canvas.drawColor(android.graphics.Color.rgb(114, 114, 114)) // abu-abu, sama seperti default Ultralytics
        canvas.drawBitmap(resized, padX, padY, null)

        return LetterboxResult(output, scale, padX, padY)
    }

    fun close() {
        interpreter.close()
    }
}