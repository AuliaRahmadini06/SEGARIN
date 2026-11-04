package com.example.deteksikesegaranbuah

import android.os.Bundle
import android.widget.ImageView // Sesuaikan jika menggunakan TextView atau Button untuk tombol X
import androidx.appcompat.app.AppCompatActivity

class PetunjukActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_petunjuk) // Pastikan nama layout-nya sesuai

        // Cari ID tombol X (misal: R.id.btnClose atau sesuaikan dengan ID di XML petunjukmu)
        val btnClose = findViewById<ImageView>(R.id.btnClose)

        btnClose.setOnClickListener {
            // Menutup activity ini dan kembali ke homepage (MainActivity)
            finish()
        }
    }
}