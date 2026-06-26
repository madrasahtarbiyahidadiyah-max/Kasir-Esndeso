package com.example.viewmodel

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.ArsipPenjualanEntity
import com.example.data.MenuItemEntity
import com.example.data.TransactionRepository
import com.example.data.network.WebSyncManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class CashierViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TransactionRepository

    init {
        val database = AppDatabase.getDatabase(application)
        val webSyncManager = WebSyncManager()
        repository = TransactionRepository(database.transactionDao(), webSyncManager, application)

        // Initialize default items if none exist
        viewModelScope.launch {
            repository.initializeDefaultDataIfEmpty()
        }
    }

    val allMenuItems: StateFlow<List<MenuItemEntity>> = repository.allMenuItems
        .stateInViewModel(emptyList())

    val allArsipPenjualan: StateFlow<List<ArsipPenjualanEntity>> = repository.allArsipPenjualan
        .stateInViewModel(emptyList())

    // Sheets Config
    var sheetsUrl by mutableStateOf(repository.getSheetsUrl())
        private set

    fun updateSheetsUrl(url: String) {
        sheetsUrl = url
        repository.saveSheetsUrl(url)
    }

    // Active Cart State
    var cartItems by mutableStateOf<List<Pair<MenuItemEntity, Int>>>(emptyList())
        private set

    var namaPembeli by mutableStateOf("")
    var qtyStepper by mutableStateOf(1)

    // Sync State
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

    fun clearSyncMessage() {
        _syncMessage.value = null
    }

    fun ubahQty(delta: Int) {
        val next = qtyStepper + delta
        if (next >= 1) {
            qtyStepper = next
        }
    }

    fun tambahKeKeranjang(menuItem: MenuItemEntity) {
        if (namaPembeli.trim().isEmpty()) {
            _syncMessage.value = "WARNING: Ketik nama pembeli terlebih dahulu ya!"
            return
        }

        val existingIndex = cartItems.indexOfFirst { it.first.nama == menuItem.nama }
        if (existingIndex >= 0) {
            val list = cartItems.toMutableList()
            val current = list[existingIndex]
            list[existingIndex] = Pair(current.first, current.second + qtyStepper)
            cartItems = list
        } else {
            cartItems = cartItems + Pair(menuItem, qtyStepper)
        }
        qtyStepper = 1 // Reset quantity stepper to default after adding
    }

    fun hapusDariKeranjang(index: Int) {
        val list = cartItems.toMutableList()
        if (index in list.indices) {
            list.removeAt(index)
            cartItems = list
        }
    }

    fun clearCart() {
        cartItems = emptyList()
        namaPembeli = ""
        qtyStepper = 1
    }

    fun bungkuskertas(notaOverride: String? = null, onComplete: (String) -> Unit) {
        if (cartItems.isEmpty()) {
            _syncMessage.value = "WARNING: Keranjang belanja masih kosong!"
            return
        }

        viewModelScope.launch {
            _isSyncing.value = true
            try {
                val nota = repository.saveNewTransaction(
                    pembeli = namaPembeli.trim(),
                    cartItems = cartItems,
                    notaOverride = notaOverride
                )
                clearCart()
                _syncMessage.value = "SUCCESS: Transaksi berhasil dicatat dengan nota: $nota"
                onComplete(nota)
            } catch (e: Exception) {
                _syncMessage.value = "ERROR: Gagal menyimpan transaksi: ${e.message}"
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun syncData() {
        viewModelScope.launch {
            _isSyncing.value = true
            val result = repository.syncWithGoogleSheets()
            when (result) {
                is com.example.data.SyncResult.Success -> {
                    _syncMessage.value = "SUCCESS: ${result.message}"
                }
                is com.example.data.SyncResult.Error -> {
                    _syncMessage.value = "ERROR: ${result.error}"
                }
            }
            _isSyncing.value = false
        }
    }

    // Dynamic state flows or state derivations for Statistics (matches local first rules)
    fun getDashboardStats(arsip: List<ArsipPenjualanEntity>): DashboardStats {
        val today = Calendar.getInstance()
        var totalHariIni = 0.0
        var totalBulanIni = 0.0
        val uniqueNotas = mutableSetOf<String>()
        var totalCup = 0

        val sdfYearMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val sdfYearMonthDay = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        val todayStr = sdfYearMonthDay.format(today.time)
        val monthStr = sdfYearMonth.format(today.time)

        arsip.forEach { item ->
            val parsedDate = parseDateTolerant(item.tanggal)
            val isToday: Boolean
            val isThisMonth: Boolean

            if (parsedDate != null) {
                val cal = Calendar.getInstance().apply { time = parsedDate }
                isToday = cal.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                          cal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
                isThisMonth = cal.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                              cal.get(Calendar.MONTH) == today.get(Calendar.MONTH)
            } else {
                // String fallback matchers
                isToday = item.tanggal.contains(todayStr) || isTodayIndonesianLocaleFallback(item.tanggal, today.time)
                isThisMonth = item.tanggal.contains(monthStr) || isThisMonthIndonesianLocaleFallback(item.tanggal, today.time)
            }

            uniqueNotas.add(item.nota)
            totalCup += item.qty

            if (isThisMonth) {
                totalBulanIni += item.total
            }
            if (isToday) {
                totalHariIni += item.total
            }
        }

        return DashboardStats(
            hariIni = totalHariIni,
            bulanIni = totalBulanIni,
            transaksi = uniqueNotas.size,
            cup = totalCup
        )
    }

    private fun parseDateTolerant(dateStr: String): Date? {
        val formats = listOf(
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd",
            "dd/MM/yyyy HH:mm:ss",
            "dd/MM/yyyy",
            "dd-MM-yyyy HH:mm:ss",
            "dd-MM-yyyy"
        )
        for (f in formats) {
            try {
                val sdf = SimpleDateFormat(f, Locale.getDefault())
                return sdf.parse(dateStr)
            } catch (e: Exception) {
                // Continue
            }
        }
        return null
    }

    private fun isTodayIndonesianLocaleFallback(dateStr: String, today: Date): Boolean {
        // Formats like "25/06/2026" or "25-06-2026"
        val formats = listOf(
            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()),
            SimpleDateFormat("d/M/yyyy", Locale.getDefault())
        )
        val todayFormatted = formats.map { it.format(today) }
        return todayFormatted.any { dateStr.contains(it) }
    }

    private fun isThisMonthIndonesianLocaleFallback(dateStr: String, today: Date): Boolean {
        val formats = listOf(
            SimpleDateFormat("/MM/yyyy", Locale.getDefault()),
            SimpleDateFormat("-MM-yyyy", Locale.getDefault())
        )
        val monthFormatted = formats.map { it.format(today) }
        return monthFormatted.any { dateStr.contains(it) }
    }

    // Expose dynamic helper to collect flows safely
    private fun <T> Flow<T>.stateInViewModel(initialValue: T): StateFlow<T> {
        return this.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = initialValue
        )
    }
}

data class DashboardStats(
    val hariIni: Double,
    val bulanIni: Double,
    val transaksi: Int,
    val cup: Int
)
