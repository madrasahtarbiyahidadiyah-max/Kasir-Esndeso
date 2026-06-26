package com.example.data

import android.content.Context
import android.util.Log
import com.example.data.network.WebSyncManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TransactionRepository(
    private val transactionDao: TransactionDao,
    private val webSyncManager: WebSyncManager,
    private val context: Context
) {
    val allMenuItems: Flow<List<MenuItemEntity>> = transactionDao.getAllMenuItems()
    val allArsipPenjualan: Flow<List<ArsipPenjualanEntity>> = transactionDao.getAllArsipPenjualan()

    // Key-value storage for the Google Sheets API URL and Theme configuration using SharedPreferences
    private val sharedPrefs = context.getSharedPreferences("EsNdesoPrefs", Context.MODE_PRIVATE)

    fun getSheetsUrl(): String {
        val saved = sharedPrefs.getString("sheets_url", "") ?: ""
        return if (saved.isEmpty()) {
            "https://script.google.com/macros/s/AKfycbzwj873hizIs6E3cOJeZ1KJ0euXDQs5PSGnIDi-ndzajWP7SjTax2ML3Fl4kO8OyiSAbw/exec"
        } else {
            saved
        }
    }

    fun saveSheetsUrl(url: String) {
        sharedPrefs.edit().putString("sheets_url", url.trim()).apply()
    }

    suspend fun initializeDefaultDataIfEmpty() {
        // Pre-populate standard Indonesian iced drink items if database is empty
        val currentMenu = allMenuItems.first()
        if (currentMenu.isEmpty()) {
            val defaults = listOf(
                MenuItemEntity("Es Ndeso Original", 5000.0),
                MenuItemEntity("Es Degan Gula Aren", 7000.0),
                MenuItemEntity("Es Campur Barokah", 8000.0),
                MenuItemEntity("Es Teler Spesial", 10000.0),
                MenuItemEntity("Es Jeruk Segar", 6000.0),
                MenuItemEntity("Es Susu Segar Ndeso", 7000.0)
            )
            transactionDao.insertMenuItems(defaults)
        }
    }

    suspend fun saveNewTransaction(
        pembeli: String,
        cartItems: List<Pair<MenuItemEntity, Int>>,
        notaOverride: String? = null
    ): String {
        val formatTanggal = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val tanggalStr = formatTanggal.format(Date())
        val nota = notaOverride ?: "NDS-${System.currentTimeMillis().toString().takeLast(6)}"

        val entities = cartItems.map { (menuItem, qty) ->
            ArsipPenjualanEntity(
                tanggal = tanggalStr,
                nota = nota,
                pembeli = pembeli,
                menu = menuItem.nama,
                harga = menuItem.harga,
                qty = qty,
                total = menuItem.harga * qty,
                isSynced = false
            )
        }

        // Save local copy
        entities.forEach {
            transactionDao.insertSingleArsipPenjualan(it)
        }

        // Attempt instant sync in background if URL is present
        val sheetsUrl = getSheetsUrl()
        if (sheetsUrl.isNotEmpty()) {
            try {
                val success = webSyncManager.uploadTransactions(sheetsUrl, entities, nota)
                if (success) {
                    transactionDao.markAsSynced(nota)
                }
            } catch (e: Exception) {
                Log.e("TransactionRepository", "Auto-sync failed, saved locally", e)
            }
        }

        return nota
    }

    suspend fun syncWithGoogleSheets(): SyncResult {
        val sheetsUrl = getSheetsUrl()
        if (sheetsUrl.isEmpty()) {
            return SyncResult.Error("URL Google Sheets belum dikonfigurasi di Pengaturan.")
        }

        try {
            // Step 1: Upload any unsynced local transactions
            val unsynced = transactionDao.getUnsyncedTransactions()
            if (unsynced.isNotEmpty()) {
                val groupedByNota = unsynced.groupBy { it.nota }
                for ((nota, items) in groupedByNota) {
                    val uploadSuccess = webSyncManager.uploadTransactions(sheetsUrl, items, nota)
                    if (uploadSuccess) {
                        transactionDao.markAsSynced(nota)
                    } else {
                        return SyncResult.Error("Gagal mengunggah transaksi tertunda $nota ke Google Sheets.")
                    }
                }
            }

            // Step 2: Fetch and update the menu
            val remoteMenu = webSyncManager.fetchDaftarMenu(sheetsUrl)
            if (remoteMenu != null && remoteMenu.isNotEmpty()) {
                transactionDao.clearMenuItems()
                transactionDao.insertMenuItems(remoteMenu)
            }

            // Step 3: Fetch and update monthly archives
            val remoteArsip = webSyncManager.fetchArsipSebulan(sheetsUrl)
            if (remoteArsip != null) {
                transactionDao.clearArsipPenjualan()
                transactionDao.insertArsipPenjualan(remoteArsip)
                return SyncResult.Success(
                    "Sinkronisasi berhasil! Memuat ${remoteMenu?.size ?: 0} menu dan ${remoteArsip.size} transaksi arsip."
                )
            }

            return SyncResult.Success("Sinkronisasi berhasil dengan beberapa data kosong.")
        } catch (e: Exception) {
            Log.e("TransactionRepository", "Full sync error", e)
            return SyncResult.Error("Gagal terhubung dengan Spreadsheet: ${e.message}")
        }
    }
}

sealed class SyncResult {
    data class Success(val message: String) : SyncResult()
    data class Error(val error: String) : SyncResult()
}
