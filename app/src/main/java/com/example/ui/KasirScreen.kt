package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MenuItemEntity

@Composable
fun KasirScreen(
    menuItems: List<MenuItemEntity>,
    cartItems: List<Pair<MenuItemEntity, Int>>,
    namaPembeli: String,
    onNamaPembeliChange: (String) -> Unit,
    qtyStepper: Int,
    onQtyStepperChange: (Int) -> Unit,
    onAddItem: (MenuItemEntity) -> Unit,
    onRemoveItem: (Int) -> Unit,
    onBungkus: () -> Unit,
    onDownloadJpg: () -> Unit,
    onPrint: () -> Unit
) {
    val focusManager = LocalFocusManager.current

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        val isWide = maxWidth > 750.dp

        if (isWide) {
            // TABLET / DESKTOP LAYOUT (Side-by-side)
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Left catalog grid area
                Column(
                    modifier = Modifier
                        .weight(1.8f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    KasirInputHeader(
                        namaPembeli = namaPembeli,
                        onNamaPembeliChange = onNamaPembeliChange,
                        qtyStepper = qtyStepper,
                        onQtyStepperChange = onQtyStepperChange,
                        focusManager = focusManager
                    )

                    CatalogGrid(
                        menuItems = menuItems,
                        onAddItem = onAddItem,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Right bill receipt area
                ActiveBillReceiptCard(
                    cartItems = cartItems,
                    onRemoveItem = onRemoveItem,
                    onBungkus = onBungkus,
                    onDownloadJpg = onDownloadJpg,
                    onPrint = onPrint,
                    modifier = Modifier
                        .weight(1.2f)
                        .fillMaxHeight()
                )
            }
        } else {
            // PHONE MOBILE LAYOUT (Vertical Stack)
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                KasirInputHeader(
                    namaPembeli = namaPembeli,
                    onNamaPembeliChange = onNamaPembeliChange,
                    qtyStepper = qtyStepper,
                    onQtyStepperChange = onQtyStepperChange,
                    focusManager = focusManager
                )

                // Split half screen for grid catalog and active receipt list
                CatalogGrid(
                    menuItems = menuItems,
                    onAddItem = onAddItem,
                    modifier = Modifier.weight(1f)
                )

                ActiveBillReceiptCard(
                    cartItems = cartItems,
                    onRemoveItem = onRemoveItem,
                    onBungkus = onBungkus,
                    onDownloadJpg = onDownloadJpg,
                    onPrint = onPrint,
                    modifier = Modifier.height(310.dp)
                )
            }
        }
    }
}

@Composable
fun KasirInputHeader(
    namaPembeli: String,
    onNamaPembeliChange: (String) -> Unit,
    qtyStepper: Int,
    onQtyStepperChange: (Int) -> Unit,
    focusManager: androidx.compose.ui.focus.FocusManager
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Buyer name field
        OutlinedTextField(
            value = namaPembeli,
            onValueChange = onNamaPembeliChange,
            placeholder = { Text("Ketik nama konsumen...", fontSize = 14.sp) },
            singleLine = true,
            modifier = Modifier.weight(1.8f),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f)
            ),
            shape = RoundedCornerShape(14.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
        )

        // Quantity selector stepper
        Row(
            modifier = Modifier
                .width(130.dp)
                .height(56.dp)
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
                .border(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = { if (qtyStepper > 1) onQtyStepperChange(qtyStepper - 1) },
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.background, RoundedCornerShape(10.dp))
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Decrement", modifier = Modifier.size(16.dp))
            }

            Text(
                text = qtyStepper.toString(),
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )

            IconButton(
                onClick = { onQtyStepperChange(qtyStepper + 1) },
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.background, RoundedCornerShape(10.dp))
            ) {
                Icon(Icons.Default.Add, contentDescription = "Increment", modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun CatalogGrid(
    menuItems: List<MenuItemEntity>,
    onAddItem: (MenuItemEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "Daftar Menu Es Segar ❄️",
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 135.dp),
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(menuItems) { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onAddItem(item) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = item.nama,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.height(38.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // High contrast price label
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Rp ${item.harga.toInt()}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActiveBillReceiptCard(
    cartItems: List<Pair<MenuItemEntity, Int>>,
    onRemoveItem: (Int) -> Unit,
    onBungkus: () -> Unit,
    onDownloadJpg: () -> Unit,
    onPrint: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Beautiful linear green theme that mirrors CSS `--cart-bg`
    val cartGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF10B981), Color(0xFF047857))
    )

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(cartGradient)
                .padding(18.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Receipt header details
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Struk Belanja",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Box(
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "NDS-AKTIF",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // Scrollable shopping list items
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                if (cartItems.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Keranjang Kosong",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Pilih es pada menu di samping",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        itemsIndexed(cartItems) { index, item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.first.nama,
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${item.second} x Rp ${item.first.harga.toInt()}",
                                        color = Color.White.copy(alpha = 0.75f),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Rp ${(item.first.harga * item.second).toInt()}",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    IconButton(
                                        onClick = { onRemoveItem(index) },
                                        modifier = Modifier
                                            .size(26.dp)
                                            .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Hapus",
                                            tint = Color.White,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Checkout and action panel
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                    .padding(12.dp)
            ) {
                // Grand total display row
                val grandTotal = cartItems.sumOf { it.first.harga * it.second }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Total Bayar",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Rp ${grandTotal.toInt()}",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom row with buttons
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onBungkus,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF059669)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Text(
                            text = "BUNGKUS PESANAN!",
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onDownloadJpg,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(alpha = 0.25f), contentColor = Color.White),
                            border = ButtonDefaults.outlinedButtonBorder().copy(brush = Brush.linearGradient(listOf(Color.White.copy(alpha=0.3f), Color.White.copy(alpha=0.3f)))),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("Unduh JPG", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = onPrint,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(alpha = 0.25f), contentColor = Color.White),
                            border = ButtonDefaults.outlinedButtonBorder().copy(brush = Brush.linearGradient(listOf(Color.White.copy(alpha=0.3f), Color.White.copy(alpha=0.3f)))),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("Print Kertas", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
