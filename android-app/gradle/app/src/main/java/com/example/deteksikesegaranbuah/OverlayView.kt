package com.example.deteksikesegaranbuah

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class OverlayView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    private var daftarDeteksi: List<Deteksi> = emptyList()
    private var onTapListener: ((Deteksi) -> Unit)? = null

    private val warnaKelas = mapOf(
        "Pisang Segar" to Color.parseColor("#FFEB3B"),        // kuning
        "Pisang Tidak Segar" to Color.parseColor("#795548"),  // cokelat
        "Tomat Segar" to Color.parseColor("#F44336"),         // merah
        "Tomat Tidak Segar" to Color.parseColor("#9E9E9E"),   // abu-abu kusam
        "Mangga Segar" to Color.parseColor("#4CAF50"),        // hijau
        "Mangga Tidak Segar" to Color.parseColor("#8D6E63")   // cokelat tua
    )

    private fun getBoxPaint(namaKelas: String): Paint {
        return Paint().apply {
            color = warnaKelas[namaKelas] ?: Color.GREEN
            style = Paint.Style.STROKE
            strokeWidth = 6f
        }
    }

    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 36f
        style = Paint.Style.FILL
    }

    fun setDeteksi(list: List<Deteksi>) {
        daftarDeteksi = list
        invalidate() // trigger redraw
    }

    fun setOnTapListener(listener: (Deteksi) -> Unit) {
        onTapListener = listener
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        for (deteksi in daftarDeteksi) {
            val paintBox = getBoxPaint(deteksi.namaKelas)
            canvas.drawRect(deteksi.box, paintBox)

            val label = "${deteksi.namaKelas} ${(deteksi.confidence * 100).toInt()}%"
            val textWidth = textPaint.measureText(label)
            val textHeight = 40f

            val labelBgPaint = Paint().apply {
                color = paintBox.color
                style = Paint.Style.FILL
                alpha = 200
            }

            // Posisi awal label: nempel di atas box
            var labelLeft = deteksi.box.left
            var labelTop = deteksi.box.top - textHeight

            // Clamp kanan: kalau label kepotong di sisi kanan, geser ke kiri
            if (labelLeft + textWidth + 10f > width) {
                labelLeft = width - textWidth - 10f
            }
            // Clamp kiri: kalau masih kepotong di sisi kiri, tempel ke tepi kiri
            if (labelLeft < 0f) {
                labelLeft = 0f
            }
            // Clamp atas: kalau box terlalu dekat tepi atas layar, taruh label di BAWAH box
            if (labelTop < 0f) {
                labelTop = deteksi.box.bottom
            }
            // Clamp bawah: kalau label (setelah dipindah ke bawah box) malah kepotong bawah layar
            if (labelTop + textHeight > height) {
                labelTop = height - textHeight
            }

            canvas.drawRect(
                labelLeft,
                labelTop,
                labelLeft + textWidth + 10f,
                labelTop + textHeight,
                labelBgPaint
            )
            canvas.drawText(label, labelLeft + 5f, labelTop + textHeight - 10f, textPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            for (deteksi in daftarDeteksi) {
                if (deteksi.box.contains(event.x, event.y)) {
                    onTapListener?.invoke(deteksi)
                    return true
                }
            }
        }
        return super.onTouchEvent(event)
    }
}