package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM menu_items ORDER BY nama ASC")
    fun getAllMenuItems(): Flow<List<MenuItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMenuItems(items: List<MenuItemEntity>)

    @Query("DELETE FROM menu_items")
    suspend fun clearMenuItems()

    @Query("SELECT * FROM arsip_penjualan ORDER BY id DESC")
    fun getAllArsipPenjualan(): Flow<List<ArsipPenjualanEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArsipPenjualan(items: List<ArsipPenjualanEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSingleArsipPenjualan(item: ArsipPenjualanEntity)

    @Query("DELETE FROM arsip_penjualan")
    suspend fun clearArsipPenjualan()

    @Query("SELECT * FROM arsip_penjualan WHERE isSynced = 0")
    suspend fun getUnsyncedTransactions(): List<ArsipPenjualanEntity>

    @Query("UPDATE arsip_penjualan SET isSynced = 1 WHERE nota = :nota")
    suspend fun markAsSynced(nota: String)
}
