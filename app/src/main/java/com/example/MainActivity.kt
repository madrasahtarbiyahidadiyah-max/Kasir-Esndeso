package com.example

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.ArsipPenjualanEntity
import com.example.ui.*
import com.example.ui.theme.MyApplicationTheme
import com.example.utils.ReceiptUtils
import com.example.viewmodel.CashierViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

enum class ScreenTab {
    Dashboard, Kasir, Arsip, Settings
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var darkTheme by remember { mutableStateOf(true) }

            MyApplicationTheme(darkTheme = darkTheme) {
                var isLoggedIn by remember { mutableStateOf(false) }

                if (!isLoggedIn) {
                    LoginScreen(onLoginSuccess = { isLoggedIn = true })
                } else {
                    MainAppScaffold(
                        darkTheme = darkTheme,
                        onThemeToggle = { darkTheme = !darkTheme },
                        onLogout = { isLoggedIn = false }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScaffold(
    darkTheme: Boolean,
    onThemeToggle: () -> Unit,
    onLogout: () -> Unit,
    viewModel: CashierViewModel = viewModel()
) {
    val context = LocalContext.current
    var currentTab by remember { mutableStateOf(ScreenTab.Dashboard) }

    val menuItems by viewModel.allMenuItems.collectAsStateWithLifecycle()
    val arsipList by viewModel.allArsipPenjualan.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val syncMessage by viewModel.syncMessage.collectAsStateWithLifecycle()

    // Dialog state for past month receipt re-printing
    var selectedNotaForReprint by remember { mutableStateOf<String?>(null) }

    // Live Digital clock display
    var timeString by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val days = listOf("Minggu", "Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu")
        val months = listOf("Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Ags", "Sep", "Okt", "Nov", "Des")
        
        while (true) {
            val cal = Calendar.getInstance()
            val dayName = days[cal.get(Calendar.DAY_OF_WEEK) - 1]
            val day = String.format("%02d", cal.get(Calendar.DAY_OF_MONTH))
            val monthName = months[cal.get(Calendar.MONTH)]
            val year = cal.get(Calendar.YEAR)
            val hour = String.format("%02d", cal.get(Calendar.HOUR_OF_DAY))
            val minute = String.format("%02d", cal.get(Calendar.MINUTE))
            val second = String.format("%02d", cal.get(Calendar.SECOND))
            
            timeString = "$dayName, $day $monthName $year | $hour:$minute:$second WIB"
            delay(1000)
        }
    }

    // Monitor sync/error state messages
    LaunchedEffect(syncMessage) {
        syncMessage?.let { msg ->
            if (msg.startsWith("WARNING:")) {
                Toast.makeText(context, msg.removePrefix("WARNING:"), Toast.LENGTH_LONG).show()
            } else if (msg.startsWith("SUCCESS:")) {
                Toast.makeText(context, msg.removePrefix("SUCCESS:"), Toast.LENGTH_LONG).show()
            } else if (msg.startsWith("ERROR:")) {
                Toast.makeText(context, msg.removePrefix("ERROR:"), Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
            viewModel.clearSyncMessage()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White)
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.logo_esndeso),
                                contentDescription = "Logo",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Column {
                            Text(
                                text = "Es Ndeso Pro",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Bismillah Barokah",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                },
                actions = {
                    // Clock Pill (only on wide viewports or simple compact display)
                    Box(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .background(
                                MaterialTheme.colorScheme.onBackground.copy(alpha = 0.04f),
                                RoundedCornerShape(10.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = timeString.takeLast(12), // Display only time on mobile to save space
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    // Spreadsheet Shortcut
                    IconButton(onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://docs.google.com/spreadsheets/d/1DWXJBUHCahfJfgsEn42ZkmctPCWE1IPvESvdp5a_Z_Q/edit?usp=sharing"))
                        context.startActivity(intent)
                    }) {
                        Icon(
                            Icons.Default.Description,
                            contentDescription = "Open Spreadsheet",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Sync Button
                    IconButton(onClick = { viewModel.syncData() }, enabled = !isSyncing) {
                        Icon(
                            Icons.Default.Sync,
                            contentDescription = "Sync Cloud",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    // Theme Toggle
                    IconButton(onClick = onThemeToggle) {
                        Icon(
                            imageVector = if (darkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Change Theme",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    // Logout / Lock Screen
                    IconButton(onClick = onLogout) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = "Lock",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ) {
                NavigationBarItem(
                    selected = currentTab == ScreenTab.Dashboard,
                    onClick = { currentTab = ScreenTab.Dashboard },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = null) },
                    label = { Text("Dashboard", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary
                    )
                )
                NavigationBarItem(
                    selected = currentTab == ScreenTab.Kasir,
                    onClick = { currentTab = ScreenTab.Kasir },
                    icon = { Icon(Icons.Default.Bolt, contentDescription = null) },
                    label = { Text("Kasir Toko", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary
                    )
                )
                NavigationBarItem(
                    selected = currentTab == ScreenTab.Arsip,
                    onClick = { currentTab = ScreenTab.Arsip },
                    icon = { Icon(Icons.Default.Receipt, contentDescription = null) },
                    label = { Text("Arsip Data", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary
                    )
                )
                NavigationBarItem(
                    selected = currentTab == ScreenTab.Settings,
                    onClick = { currentTab = ScreenTab.Settings },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("Pengaturan", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Tab content rendering
            when (currentTab) {
                ScreenTab.Dashboard -> {
                    val stats = viewModel.getDashboardStats(arsipList)
                    DashboardScreen(
                        stats = stats,
                        arsipList = arsipList
                    )
                }
                ScreenTab.Kasir -> {
                    KasirScreen(
                        menuItems = menuItems,
                        cartItems = viewModel.cartItems,
                        namaPembeli = viewModel.namaPembeli,
                        onNamaPembeliChange = { viewModel.namaPembeli = it },
                        qtyStepper = viewModel.qtyStepper,
                        onQtyStepperChange = { viewModel.ubahQty(it - viewModel.qtyStepper) },
                        onAddItem = { viewModel.tambahKeKeranjang(it) },
                        onRemoveItem = { viewModel.hapusDariKeranjang(it) },
                        onBungkus = {
                            viewModel.bungkuskertas { nota ->
                                // Optional auto print/save trigger on success checkout
                            }
                        },
                        onDownloadJpg = {
                            if (viewModel.cartItems.isEmpty()) {
                                Toast.makeText(context, "Keranjang belanja kosong!", Toast.LENGTH_SHORT).show()
                            } else {
                                val tgl = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(java.util.Date())
                                val pEntities = viewModel.cartItems.map { pair ->
                                    val m = pair.first
                                    val q = pair.second
                                    ArsipPenjualanEntity(tanggal = tgl, nota = "NDS-PRO", pembeli = viewModel.namaPembeli, menu = m.nama, harga = m.harga, qty = q, total = m.harga * q)
                                }
                                val b = ReceiptUtils.generateReceiptBitmap(context, "NDS-PENDING", tgl, viewModel.namaPembeli, pEntities)
                                ReceiptUtils.saveBitmapToGallery(context, b, "Nota_${viewModel.namaPembeli}_pending.jpg")
                            }
                        },
                        onPrint = {
                            if (viewModel.cartItems.isEmpty()) {
                                Toast.makeText(context, "Keranjang belanja kosong!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Menghubungkan ke Printer Thermal...", Toast.LENGTH_SHORT).show()
                                val tgl = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(java.util.Date())
                                val pEntities = viewModel.cartItems.map { pair ->
                                    val m = pair.first
                                    val q = pair.second
                                    ArsipPenjualanEntity(tanggal = tgl, nota = "NDS-PRO", pembeli = viewModel.namaPembeli, menu = m.nama, harga = m.harga, qty = q, total = m.harga * q)
                                }
                                val bitmap = ReceiptUtils.generateReceiptBitmap(context, "NDS-PRO", tgl, viewModel.namaPembeli, pEntities)
                                executePrintBitmap(context, bitmap, "Struk_${viewModel.namaPembeli}")
                            }
                        }
                    )
                }
                ScreenTab.Arsip -> {
                    ArsipScreen(
                        arsipList = arsipList,
                        onSelectStruk = { selectedNotaForReprint = it }
                    )
                }
                ScreenTab.Settings -> {
                    SettingsScreen(
                        sheetsUrl = viewModel.sheetsUrl,
                        onSaveUrl = { 
                            viewModel.updateSheetsUrl(it)
                            Toast.makeText(context, "Tautan Google Sheets berhasil disimpan!", Toast.LENGTH_SHORT).show()
                        },
                        isSyncing = isSyncing,
                        onSyncClick = { inputUrl ->
                            viewModel.updateSheetsUrl(inputUrl)
                            viewModel.syncData()
                        }
                    )
                }
            }

            // Sync loading overlay banner
            AnimatedVisibility(
                visible = isSyncing,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Menyinkronkan data...",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal dialogue popup for re-printing
    selectedNotaForReprint?.let { nota ->
        val matchingRecords = arsipList.filter { it.nota == nota }
        if (matchingRecords.isNotEmpty()) {
            val pembeli = matchingRecords.first().pembeli
            val tanggal = matchingRecords.first().tanggal

            AlertDialog(
                onDismissRequest = { selectedNotaForReprint = null },
                title = { Text("Opsi Cetak Ulang", fontWeight = FontWeight.ExtraBold) },
                text = {
                    Text(
                        text = "Pilih format untuk cetak ulang struk nota $nota milik pelanggan $pembeli.",
                        fontSize = 14.sp
                    )
                },
                confirmButton = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                selectedNotaForReprint = null
                                Toast.makeText(context, "Membuka printer dialog...", Toast.LENGTH_SHORT).show()
                                val bitmap = ReceiptUtils.generateReceiptBitmap(context, nota, tanggal, pembeli, matchingRecords)
                                executePrintBitmap(context, bitmap, "Reprint_$nota")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("🖨️ Cetak Printer", fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        Button(
                            onClick = {
                                selectedNotaForReprint = null
                                val bitmap = ReceiptUtils.generateReceiptBitmap(context, nota, tanggal, pembeli, matchingRecords)
                                ReceiptUtils.saveBitmapToGallery(context, bitmap, "Nota_Ulang_${pembeli}_$nota.jpg")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("🖼️ Unduh Gambar JPG", fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        OutlinedButton(
                            onClick = { selectedNotaForReprint = null },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Batal", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun SettingsScreen(
    sheetsUrl: String,
    onSaveUrl: (String) -> Unit,
    isSyncing: Boolean,
    onSyncClick: (String) -> Unit
) {
    var urlInput by remember { mutableStateOf(sheetsUrl) }
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val scriptCode = """
        function doGet(e) {
          var action = e.parameter.action;
          var sheet = SpreadsheetApp.getActiveSpreadsheet();
          
          if (action === "getDaftarMenu") {
            var menuSheet = sheet.getSheetByName("Daftar Menu") || sheet.getSheetByName("Menu") || sheet.insertSheet("Daftar Menu");
            if (menuSheet.getLastRow() === 0) {
              menuSheet.appendRow(["Nama Menu", "Harga"]);
              menuSheet.appendRow(["Es Ndeso Original", 5000]);
              menuSheet.appendRow(["Es Degan Gula Aren", 7000]);
              menuSheet.appendRow(["Es Campur Barokah", 8000]);
              menuSheet.appendRow(["Es Teler Spesial", 10000]);
              menuSheet.appendRow(["Es Jeruk Segar", 6000]);
              menuSheet.appendRow(["Es Susu Segar Ndeso", 7000]);
            }
            
            var data = menuSheet.getDataRange().getValues();
            var headers = data[0];
            var namaIdx = headers.indexOf("Nama Menu") !== -1 ? headers.indexOf("Nama Menu") : (headers.indexOf("nama") !== -1 ? headers.indexOf("nama") : 0);
            var hargaIdx = headers.indexOf("Harga") !== -1 ? headers.indexOf("Harga") : (headers.indexOf("harga") !== -1 ? headers.indexOf("harga") : 1);
            
            var list = [];
            for (var i = 1; i < data.length; i++) {
              if (data[i][namaIdx]) {
                list.push({
                  nama: data[i][namaIdx].toString().trim(),
                  harga: parseFloat(data[i][hargaIdx]) || 0
                });
              }
            }
            return ContentService.createTextOutput(JSON.stringify(list))
              .setMimeType(ContentService.MimeType.JSON);
          }
          
          if (action === "getArsipSebulan") {
            var trxSheet = sheet.getSheetByName("Arsip Penjualan") || sheet.getSheetByName("Transaksi") || sheet.insertSheet("Arsip Penjualan");
            if (trxSheet.getLastRow() === 0) {
              trxSheet.appendRow(["Tanggal", "No Nota", "Nama Pembeli", "Nama Menu", "Harga", "Qyt", "Total"]);
            }
            
            var data = trxSheet.getDataRange().getValues();
            var headers = data[0];
            var tglIdx = headers.indexOf("Tanggal") !== -1 ? headers.indexOf("Tanggal") : (headers.indexOf("tanggal") !== -1 ? headers.indexOf("tanggal") : 0);
            var notaIdx = headers.indexOf("No Nota") !== -1 ? headers.indexOf("No Nota") : (headers.indexOf("nota") !== -1 ? headers.indexOf("nota") : 1);
            var pembeliIdx = headers.indexOf("Nama Pembeli") !== -1 ? headers.indexOf("Nama Pembeli") : (headers.indexOf("pembeli") !== -1 ? headers.indexOf("pembeli") : 2);
            var menuIdx = headers.indexOf("Nama Menu") !== -1 ? headers.indexOf("Nama Menu") : (headers.indexOf("menu") !== -1 ? headers.indexOf("menu") : 3);
            var hargaIdx = headers.indexOf("Harga") !== -1 ? headers.indexOf("Harga") : (headers.indexOf("harga") !== -1 ? headers.indexOf("harga") : 4);
            var qtyIdx = headers.indexOf("Qyt") !== -1 ? headers.indexOf("Qyt") : (headers.indexOf("Qty") !== -1 ? headers.indexOf("Qty") : (headers.indexOf("qty") !== -1 ? headers.indexOf("qty") : 5));
            var totalIdx = headers.indexOf("Total") !== -1 ? headers.indexOf("Total") : (headers.indexOf("total") !== -1 ? headers.indexOf("total") : 6);
            
            var list = [];
            for (var i = 1; i < data.length; i++) {
              if (data[i][tglIdx]) {
                var rawDate = data[i][tglIdx];
                var formattedDateStr = "";
                if (rawDate instanceof Date) {
                  formattedDateStr = Utilities.formatDate(rawDate, "GMT+7", "yyyy-MM-dd HH:mm:ss");
                } else {
                  formattedDateStr = rawDate.toString();
                }
                list.push({
                  tanggal: formattedDateStr,
                  nota: data[i][notaIdx] ? data[i][notaIdx].toString() : "",
                  pembeli: data[i][pembeliIdx] ? data[i][pembeliIdx].toString() : "",
                  menu: data[i][menuIdx] ? data[i][menuIdx].toString() : "",
                  harga: parseFloat(data[i][hargaIdx]) || 0,
                  qty: parseInt(data[i][qtyIdx]) || 0,
                  total: parseFloat(data[i][totalIdx]) || 0
                });
              }
            }
            return ContentService.createTextOutput(JSON.stringify(list))
              .setMimeType(ContentService.MimeType.JSON);
          }
          
          return ContentService.createTextOutput(JSON.stringify({status: "Error", message: "Action tidak dikenal"}))
            .setMimeType(ContentService.MimeType.JSON);
        }

        function doPost(e) {
          try {
            var postData = JSON.parse(e.postData.contents);
            var action = postData.action;
            
            if (action === "simpanTransaksi") {
              var sheet = SpreadsheetApp.getActiveSpreadsheet();
              var trxSheet = sheet.getSheetByName("Arsip Penjualan") || sheet.getSheetByName("Transaksi") || sheet.insertSheet("Arsip Penjualan");
              if (trxSheet.getLastRow() === 0) {
                trxSheet.appendRow(["Tanggal", "No Nota", "Nama Pembeli", "Nama Menu", "Harga", "Qyt", "Total"]);
              }
              
              var data = trxSheet.getDataRange().getValues();
              var headers = data[0];
              var tglIdx = headers.indexOf("Tanggal") !== -1 ? headers.indexOf("Tanggal") : 0;
              var notaIdx = headers.indexOf("No Nota") !== -1 ? headers.indexOf("No Nota") : 1;
              var pembeliIdx = headers.indexOf("Nama Pembeli") !== -1 ? headers.indexOf("Nama Pembeli") : 2;
              var menuIdx = headers.indexOf("Nama Menu") !== -1 ? headers.indexOf("Nama Menu") : 3;
              var hargaIdx = headers.indexOf("Harga") !== -1 ? headers.indexOf("Harga") : 4;
              var qtyIdx = headers.indexOf("Qyt") !== -1 ? headers.indexOf("Qyt") : (headers.indexOf("Qty") !== -1 ? headers.indexOf("Qty") : 5);
              var totalIdx = headers.indexOf("Total") !== -1 ? headers.indexOf("Total") : 6;
              
              var nota = postData.nomorNotaDariClient;
              var dataPesanan = postData.dataPesanan;
              
              var formattedDate = Utilities.formatDate(new Date(), "GMT+7", "yyyy-MM-dd HH:mm:ss");
              
              for (var i = 0; i < dataPesanan.length; i++) {
                var item = dataPesanan[i];
                var rowData = [];
                rowData[tglIdx] = formattedDate;
                rowData[notaIdx] = nota;
                rowData[pembeliIdx] = item.pembeli;
                rowData[menuIdx] = item.nama;
                rowData[hargaIdx] = parseFloat(item.harga) || 0;
                rowData[qtyIdx] = parseInt(item.qty) || 0;
                rowData[totalIdx] = parseFloat(item.total) || 0;
                
                var maxIdx = Math.max(tglIdx, notaIdx, pembeliIdx, menuIdx, hargaIdx, qtyIdx, totalIdx);
                for (var j = 0; j <= maxIdx; j++) {
                  if (rowData[j] === undefined) {
                    rowData[j] = "";
                  }
                }
                trxSheet.appendRow(rowData);
              }
              
              return ContentService.createTextOutput(JSON.stringify({status: "Sukses"}))
                .setMimeType(ContentService.MimeType.JSON);
            }
            
            return ContentService.createTextOutput(JSON.stringify({status: "Error", message: "Action POST tidak dikenal"}))
              .setMimeType(ContentService.MimeType.JSON);
          } catch(err) {
            return ContentService.createTextOutput(JSON.stringify({status: "Error", message: err.message}))
              .setMimeType(ContentService.MimeType.JSON);
          }
        }
    """.trimIndent()

    val isSpreadsheetUrl = urlInput.contains("docs.google.com/spreadsheets")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = "Pengaturan Sinkronisasi ⚙️",
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold
        )

        Text(
            text = "Tempelkan tautan Web App dari Google Apps Script untuk menghubungkan kasir ini secara langsung ke Spreadsheet.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            border = CardDefaults.outlinedCardBorder()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Google Apps Script Web App URL",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    placeholder = { Text("https://script.google.com/macros/s/.../exec") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    isError = isSpreadsheetUrl
                )

                if (isSpreadsheetUrl) {
                    Text(
                        text = "⚠️ PERINGATAN: Tautan ini adalah tautan Spreadsheet biasa, bukan tautan Apps Script Web App! Silakan ikuti Panduan Integrasi di bawah untuk mendapatkan tautan yang benar.",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = {
                        if (urlInput.trim().isEmpty()) {
                            Toast.makeText(context, "Silakan masukkan URL Google Sheets terlebih dahulu!", Toast.LENGTH_SHORT).show()
                        } else if (isSpreadsheetUrl) {
                            Toast.makeText(context, "Harap gunakan URL Google Apps Script Web App!", Toast.LENGTH_SHORT).show()
                        } else {
                            onSaveUrl(urlInput.trim())
                        }
                    },
                    modifier = Modifier.align(Alignment.End),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("SIMPAN TAUTAN", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Button(
            onClick = {
                if (urlInput.trim().isEmpty()) {
                    Toast.makeText(context, "Silakan masukkan URL Google Sheets terlebih dahulu!", Toast.LENGTH_SHORT).show()
                } else if (isSpreadsheetUrl) {
                    Toast.makeText(context, "Harap gunakan URL Google Apps Script Web App!", Toast.LENGTH_SHORT).show()
                } else {
                    onSyncClick(urlInput.trim())
                }
            },
            enabled = !isSyncing,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Sync, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("MULAI SINKRONISASI DATA", fontWeight = FontWeight.Black)
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Documentation/Help Panel
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "💡 Panduan Integrasi",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "1. Buka Google Sheets Anda.\n" +
                           "2. Masuk ke Ekstensi -> Apps Script.\n" +
                           "3. Salin kode template di bawah ini.\n" +
                           "4. Simpan, lalu klik Penerapan -> Penerapan Baru.\n" +
                           "5. Pilih jenis penerapan: Web App.\n" +
                           "6. Konfigurasi akses: Siapa saja (Anyone).\n" +
                           "7. Salin URL Web App yang didapat, lalu tempelkan di atas.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )

                Button(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(scriptCode))
                        Toast.makeText(context, "Kode Google Apps Script berhasil disalin!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("SALIN KODE APPS SCRIPT", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// Launches the native Android print dialog for the given bitmap
fun executePrintBitmap(context: Context, bitmap: Bitmap, jobName: String) {
    try {
        val printHelper = androidx.print.PrintHelper(context)
        printHelper.scaleMode = androidx.print.PrintHelper.SCALE_MODE_FIT
        printHelper.printBitmap(jobName, bitmap)
    } catch (e: Exception) {
        Toast.makeText(context, "Gagal mencetak: ${e.message}", Toast.LENGTH_LONG).show()
    }
}
