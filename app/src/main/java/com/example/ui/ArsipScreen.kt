package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalActivity
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ArsipPenjualanEntity
import com.example.ui.theme.Gradient2End
import com.example.ui.theme.Gradient2Start
import java.util.Locale

@Composable
fun ArsipScreen(
    arsipList: List<ArsipPenjualanEntity>,
    onSelectStruk: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }

    // Filter past month transactions dynamically based on search
    val filteredList = remember(arsipList, searchQuery) {
        if (searchQuery.trim().isEmpty()) {
            arsipList
        } else {
            arsipList.filter {
                it.pembeli.contains(searchQuery, ignoreCase = true) ||
                it.nota.contains(searchQuery, ignoreCase = true) ||
                it.menu.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val totalUangArsip = remember(filteredList) { filteredList.sumOf { it.total } }
    val totalCupArsip = remember(filteredList) { filteredList.sumOf { it.qty } }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Welcoming Header
        item {
            Column(modifier = Modifier.padding(top = 16.dp)) {
                Text(
                    text = "Audit Arsip Bulanan 📂",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Tekan Nomor Struk pada tabel untuk mencetak ulang atau mengunduh nota transaksi.",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        // Live stats aggregates (Total Money & Total Cups)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                KpiCard(
                    title = "Total Uang Arsip",
                    value = formatRupiah(totalUangArsip),
                    icon = Icons.Default.Paid,
                    indicatorColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                KpiCard(
                    title = "Total Cup Arsip",
                    value = "$totalCupArsip Cup",
                    icon = Icons.Default.LocalActivity,
                    indicatorColor = Color(0xFF8B5CF6),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Search panel
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Cari nota, konsumen, atau menu es...", fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f)
                )
            )
        }

        // Table headers (Violet to Blue gradient)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.horizontalGradient(listOf(Gradient2Start, Gradient2End)),
                        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                    )
                    .padding(vertical = 12.dp, horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "No. Struk",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.weight(1.1f)
                )
                Text(
                    text = "Konsumen",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.weight(1.1f)
                )
                Text(
                    text = "Item Menu",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.weight(1.4f)
                )
                Text(
                    text = "Qty",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.weight(0.4f),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Subtotal",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.End
                )
            }
        }

        // Table Rows List
        if (filteredList.isEmpty()) {
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
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Belum ada transaksi terekam pada pencarian ini.",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            itemsIndexed(filteredList) { index, item ->
                val isLast = index == filteredList.size - 1
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
                            .padding(vertical = 12.dp, horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Clippable clickable badge
                        Box(
                            modifier = Modifier
                                .weight(1.1f)
                                .padding(end = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                                    .clickable { onSelectStruk(item.nota) }
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = item.nota,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Text(
                            text = item.pembeli,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1.1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = item.menu,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1.4f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = item.qty.toString(),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(0.4f),
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "Rp ${item.total.toInt()}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.End
                        )
                    }
                }
            }
        }
    }
}
