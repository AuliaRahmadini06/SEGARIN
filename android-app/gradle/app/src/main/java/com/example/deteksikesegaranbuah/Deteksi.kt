package com.example.deteksikesegaranbuah

import android.graphics.RectF

data class Deteksi(
    val namaKelas: String,
    val confidence: Float,
    val box: RectF
)