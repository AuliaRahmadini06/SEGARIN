package com.example.deteksikesegaranbuah

import android.graphics.RectF
import kotlin.math.max
import kotlin.math.min

class DeteksiTracker {

    private val iouGabungThreshold = 0.15f

    @Synchronized
    fun update(deteksiBaru: List<Deteksi>): List<Deteksi> {
        return gabungkanDuplikat(deteksiBaru)
    }

    private fun gabungkanDuplikat(deteksiList: List<Deteksi>): List<Deteksi> {
        val hasil = deteksiList.toMutableList()
        var i = 0
        while (i < hasil.size) {
            var j = i + 1
            while (j < hasil.size) {
                val a = hasil[i]
                val b = hasil[j]
                if (hitungIoU(a.box, b.box) > iouGabungThreshold) {
                    // Simpan yang confidence-nya lebih tinggi, buang yang lain
                    if (b.confidence > a.confidence) {
                        hasil[i] = b
                    }
                    hasil.removeAt(j)
                } else {
                    j++
                }
            }
            i++
        }
        return hasil
    }

    @Synchronized
    fun reset() {
        // tidak ada state persisten lagi untuk direset, disimpan untuk kompatibilitas pemanggil
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
}