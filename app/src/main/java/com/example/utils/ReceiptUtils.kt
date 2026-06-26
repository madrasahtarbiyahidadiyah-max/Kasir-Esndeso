package com.example.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Environment
import android.content.ContentValues
import android.net.Uri
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.ArsipPenjualanEntity
import java.io.OutputStream

object ReceiptUtils {
    // Generates a thermal receipt image as a Bitmap
    fun generateReceiptBitmap(
        context: Context, 
        nota: String, 
        tanggal: String, 
        pembeli: String, 
        items: List<ArsipPenjualanEntity>
    ): Bitmap {
        val width = 420
        // Calculate height dynamically based on the number of items
        val itemHeight = 55
        val headerHeight = 260
        val footerHeight = 160
        val height = headerHeight + (items.size * itemHeight) + footerHeight
        
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        
        val paint = Paint().apply {
            color = Color.BLACK
            isAntiAlias = true
            textSize = 15f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        }
        
        val paintBold = Paint().apply {
            color = Color.BLACK
            isAntiAlias = true
            textSize = 16f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }

        val paintTitle = Paint().apply {
            color = Color.BLACK
            isAntiAlias = true
            textSize = 22f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        
        // 1. Draw Logo
        try {
            val drawable = ContextCompat.getDrawable(context, R.drawable.logo_esndeso)
            if (drawable != null) {
                val logoSize = 80
                val left = (width - logoSize) / 2
                val top = 20
                drawable.setBounds(left, top, left + logoSize, top + logoSize)
                drawable.draw(canvas)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        // 2. Draw Header Details
        var y = 135f
        canvas.drawText("KEDAI ES NDESO", width / 2f, y, paintTitle)
        
        y += 24f
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = 13f
        canvas.drawText("Geneng Waru Rembang Pasuruan", width / 2f, y, paint)
        y += 18f
        canvas.drawText("WA. 0821-1511-5224 / 0857-8449-4261", width / 2f, y, paint)
        y += 18f
        canvas.drawText("Bismillah Barokah", width / 2f, y, paint)
        
        // Divider
        y += 15f
        paint.textAlign = Paint.Align.LEFT
        paint.textSize = 14f
        canvas.drawText("----------------------------------", 20f, y, paint)
        
        // Metadata details
        y += 22f
        canvas.drawText("No. Nota: $nota", 20f, y, paint)
        y += 20f
        canvas.drawText("Tanggal : $tanggal", 20f, y, paint)
        y += 20f
        canvas.drawText("Pelanggan: $pembeli", 20f, y, paint)
        
        y += 15f
        canvas.drawText("----------------------------------", 20f, y, paint)
        
        // Render Cart Item Table
        var totalAmount = 0.0
        items.forEach { item ->
            y += 25f
            paintBold.textSize = 15f
            canvas.drawText(item.menu, 20f, y, paintBold)
            
            y += 20f
            val qtyPriceStr = "  ${item.qty} x Rp ${item.harga.toInt()}"
            val subtotalStr = "Rp ${item.total.toInt()}"
            
            paint.textAlign = Paint.Align.LEFT
            canvas.drawText(qtyPriceStr, 20f, y, paint)
            
            val textWidth = paint.measureText(subtotalStr)
            canvas.drawText(subtotalStr, width - 20f - textWidth, y, paint)
            totalAmount += item.total
        }
        
        y += 15f
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("----------------------------------", 20f, y, paint)
        
        // Grand Total Row
        y += 30f
        val grandTotalLabel = "GRAND TOTAL:"
        val grandTotalVal = "Rp ${totalAmount.toInt()}"
        canvas.drawText(grandTotalLabel, 20f, y, paintBold)
        
        val valWidth = paintBold.measureText(grandTotalVal)
        canvas.drawText(grandTotalVal, width - 20f - valWidth, y, paintBold)
        
        y += 15f
        canvas.drawText("----------------------------------", 20f, y, paint)
        
        // Receipt Footer Blessing
        y += 30f
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = 13f
        canvas.drawText("Segar Esnya, Nyaman di Hati.", width / 2f, y, paint)
        y += 18f
        canvas.drawText("Terima kasih atas kunjungan Anda!", width / 2f, y, paint)
        
        return bitmap
    }

    // Saves the thermal receipt bitmap to the local Pictures gallery
    fun saveBitmapToGallery(context: Context, bitmap: Bitmap, fileName: String): Uri? {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/KedaiEsNdeso")
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        
        if (uri != null) {
            var stream: OutputStream? = null
            try {
                stream = resolver.openOutputStream(uri)
                if (stream != null) {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)
                    Toast.makeText(context, "Nota disimpan di Galeri: Pictures/KedaiEsNdeso", Toast.LENGTH_LONG).show()
                    return uri
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                stream?.close()
            }
        }
        return null
    }
}
