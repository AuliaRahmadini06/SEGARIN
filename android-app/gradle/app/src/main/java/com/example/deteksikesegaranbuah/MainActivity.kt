package com.example.deteksikesegaranbuah

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnMulaiKamera = findViewById<Button>(R.id.btnMulaiKamera)
        val btnPetunjuk = findViewById<Button>(R.id.btnPetunjuk)

        btnMulaiKamera.setOnClickListener {
            val intent = Intent(this, KameraActivity::class.java)
            startActivity(intent)
        }

        btnPetunjuk.setOnClickListener {
            val intent = Intent(this, PetunjukActivity::class.java)
            startActivity(intent)
        }
    }
}