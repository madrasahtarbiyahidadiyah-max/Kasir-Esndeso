package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "menu_items")
data class MenuItemEntity(
    @PrimaryKey val nama: String,
    val harga: Double
)
