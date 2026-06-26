package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ArsipPenjualanEntity
import com.example.ui.theme.Gradient1End
import com.example.ui.theme.Gradient1Start
import com.example.viewmodel.DashboardStats
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun DashboardScreen(
    stats: DashboardStats,
    arsipList: List<ArsipPenjualanEntity>,
    modifier: Modifier = Modifier
) {
    // Group records dynamically by receipt (nota) to find recent shoppers
    val recentShoppers = remember(arsipList) {
        arsipList.groupBy { it.nota + "_" + it.pembeli }
            .map { (key, items) ->
                val pembeli = key.substringAfter("_")
                val date = items.firstOrNull()?.tanggal ?: ""
                val total = items.sumOf { it.total }
                RecentShopper(date = date, nama = pembeli, total = total)
            }
            .take(15) // Limit to 15 items for the preview list
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Welcoming Title Header
        item {
            Column(modifier = Modifier.padding(top = 16.dp)) {
                Text(
                    text = "Ikhtisar Penjualan 📈",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Bekerja secara offline. Tekan tombol sinkronisasi untuk mengunggah ke Cloud Spreadsheet.",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        // Bento Card Grid Layout for Statistics
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    KpiCard(
                        title = "Uang Hari Ini",
                        value = formatRupiah(stats.hariIni),
                        icon = Icons.Default.MonetizationOn,
                        indicatorColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    KpiCard(
                        title = "Uang Bulan Ini",
                        value = formatRupiah(stats.bulanIni),
                        icon = Icons.Default.Assessment,
                        indicatorColor = Color(0xFF3B82F6),
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    KpiCard(
                        title = "Transaksi",
                        value = stats.transaksi.toString(),
                        icon = Icons.Default.ShoppingBag,
                        indicatorColor = Color(0xFFF59E0B),
                        modifier = Modifier.weight(1f)
                    )
                    KpiCard(
                        title = "Cup Terjual",
                        value = "${stats.cup} Cup",
                        icon = Icons.Default.LocalDrink,
                        indicatorColor = Color(0xFFEC4899),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Customer ledger
        item {
            Text(
                text = "Catatan Pelanggan Terakhir",
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // Ledger Table Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.horizontalGradient(listOf(Gradient1Start, Gradient1End)),
                        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                    )
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Waktu / Tgl",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.weight(1.2f)
                )
                Text(
                    text = "Konsumen",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.weight(1.5f)
                )
                Text(
                    text = "Total Belanja",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.weight(1.2f),
                    textAlign = TextAlign.End
                )
            }
        }

        // Ledger Table Rows
        if (recentShoppers.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Belum ada aktivitas terekam.",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            itemsIndexed(recentShoppers) { index, shopper ->
                val isLast = index == recentShoppers.size - 1
                val cardShape = if (isLast) {
                    RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)
                } else {
                    RoundedCornerShape(0.dp)
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = cardShape,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 14.dp, horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = cleanDateTime(shopper.date),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1.2f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = shopper.nama,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1.5f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = formatRupiah(shopper.total),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1.2f),
                            textAlign = TextAlign.End
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun KpiCard(
    title: String,
    value: String,
    icon: ImageVector,
    indicatorColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(105.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Lateral Color Strip for Visual Anchor
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(indicatorColor, RoundedCornerShape(2.dp))
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title.uppercase(Locale.getDefault()),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = value,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 4.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = indicatorColor.copy(alpha = 0.8f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

fun formatRupiah(amount: Double): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    return formatter.format(amount)
        .replace("Rp", "Rp ")
        .replace(",00", "")
}

private fun cleanDateTime(rawDate: String): String {
    // Turns "2026-06-25 10:29:00" into "25 Jun, 10:29"
    return try {
        val inFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val date = inFormat.parse(rawDate) ?: return rawDate
        val outFormat = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
        outFormat.format(date)
    } catch (e: Exception) {
        rawDate
    }
}

data class RecentShopper(
    val date: String,
    val nama: String,
    val total: Double
)
