package com.example.deteksikesegaranbuah

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView

object PenjelasanHelper {

    private val deskripsiMap = mapOf(
        "Mangga Segar" to "Warna kulit hijau kekuningan merata, tekstur kencang saat ditekan, tidak ada bercak coklat atau memar. Aroma khas mangga mulai tercium namun belum menyengat. Kondisi buah masuk kategori segar dan layak konsumsi.",
        "Mangga Tidak Segar" to "Kulit mulai keriput atau muncul bercak coklat kehitaman, tekstur lembek saat ditekan, dan pada beberapa bagian kulit tampak memar atau berair. Aroma fermentasi mulai tercium. Kondisi buah masuk kategori tidak segar.",
        "Pisang Segar" to "Kulit kuning cerah merata dengan sedikit warna hijau di ujung, tekstur kencang dan tidak ada memar. Bintik coklat (jika ada) hanya sedikit dan tersebar, bukan tanda pembusukan. Kondisi buah masuk kategori segar dan layak konsumsi.",
        "Pisang Tidak Segar" to "Kulit dipenuhi bintik atau area coklat kehitaman yang meluas, tekstur lembek dan mudah penyok saat ditekan, pada bagian tertentu kulit mulai mengelupas atau berair. Kondisi buah masuk kategori tidak segar.",
        "Tomat Segar" to "Warna kulit merah merata dan mengkilap, tekstur kencang dengan sedikit kelenturan saat ditekan, tidak ditemukan bercak lunak atau retak. Kondisi buah masuk kategori segar dan layak konsumsi.",
        "Tomat Tidak Segar" to "Warna kulit tidak merata dengan bercak lunak berwarna gelap, tekstur lembek dan berair saat ditekan, pada beberapa bagian mulai muncul keriput atau jamur halus. Kondisi buah masuk kategori tidak segar."
    )

    fun tampilkan(context: Context, namaKelas: String, confidence: Float = 0f) {
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.layout_popup_kondisi, null)

        val tvNamaBuah = view.findViewById<TextView>(R.id.tvNamaBuah)
        val tvConfidence = view.findViewById<TextView>(R.id.tvConfidence)
        val tvDeskripsi = view.findViewById<TextView>(R.id.tvDeskripsi)
        val badgeStatus = view.findViewById<TextView>(R.id.badgeStatus)
        val btnClose = view.findViewById<ImageView>(R.id.btnClose)

        val isSegar = namaKelas.contains("Segar", ignoreCase = true) && !namaKelas.contains("Tidak", ignoreCase = true)
        val namaBuahHanya = namaKelas.replace("Segar", "", ignoreCase = true)
            .replace("Tidak", "", ignoreCase = true)
            .trim()

        tvNamaBuah.text = namaBuahHanya.uppercase()

        // Konversi nilai confidence desimal (misal 0.86) menjadi format persen (86%)
        val confidencePercent = if (confidence > 1f) confidence.toInt() else (confidence * 100).toInt()
        tvConfidence.text = "Tingkat keyakinan: $confidencePercent%"

        tvDeskripsi.text = deskripsiMap[namaKelas] ?: "Deskripsi tidak tersedia untuk kondisi ini."

        if (isSegar) {
            badgeStatus.text = "SEGAR"
            badgeStatus.setTextColor(Color.parseColor("#2E5A44"))
            badgeStatus.setBackgroundResource(R.drawable.bg_circle_segar)
        } else {
            badgeStatus.text = "TIDAK\nSEGAR"
            badgeStatus.setTextColor(Color.parseColor("#C1442D"))
            badgeStatus.setBackgroundResource(R.drawable.bg_circle_tidak_segar)
        }

        val dialog = AlertDialog.Builder(context)
            .setView(view)
            .create()

        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            setDimAmount(0.5f) // Efek background transparan/gelap di belakang modal
            setGravity(Gravity.CENTER) // Mengunci posisi persis di tengah (Portrait / Landscape)
        }

        btnClose.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}