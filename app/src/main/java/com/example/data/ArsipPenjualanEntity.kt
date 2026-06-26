package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "arsip_penjualan")
data class ArsipPenjualanEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val tanggal: String,
    val nota: String,
    val pembeli: String,
    val menu: String,
    val harga: Double,
    val qty: Int,
    val total: Double,
    val isSynced: Boolean = false
)
