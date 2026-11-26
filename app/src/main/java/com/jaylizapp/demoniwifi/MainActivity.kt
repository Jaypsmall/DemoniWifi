package com.jaylizapp.demoniwifi

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.jaylizapp.demoniwifi.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke

class MainActivity : ComponentActivity() {
    private lateinit var wifiHelper: WifiHelper
    private var networksState = mutableStateOf(emptyList<WifiNetwork>())

    private val wifiScanReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)
            if (success) {
                networksState.value = wifiHelper.getScanResults()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        wifiHelper = WifiHelper(this)

        enableEdgeToEdge()
        setContent {
            DemonicWifiApp(wifiHelper, networksState)
        }
    }

    override fun onStart() {
        super.onStart()
        val intentFilter = IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        registerReceiver(wifiScanReceiver, intentFilter)
    }

    override fun onStop() {
        super.onStop()
        try {
            unregisterReceiver(wifiScanReceiver)
        } catch (e: Exception) {
            // Already unregistered
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DemonicWifiApp(wifiHelper: WifiHelper, networksState: MutableState<List<WifiNetwork>>) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val networks by networksState
    
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    
    var isDarkTheme by rememberSaveable { mutableStateOf(true) }
    var isEnglish by rememberSaveable { mutableStateOf(false) }

    // New interactive dialog states
    var selectedNetworkForDetails by remember { mutableStateOf<WifiNetwork?>(null) }
    var showChannelAnalyzer by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }

    DemoniWifiTheme(darkTheme = isDarkTheme) {
        var selectedSsids by remember { mutableStateOf(setOf<String>()) }
        var passwordInput by remember { mutableStateOf("") }
        var isConnecting by remember { mutableStateOf(false) }

        var hasPermission by remember {
            mutableStateOf(
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            )
        }

        val launcher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            hasPermission = isGranted
        }

        LaunchedEffect(hasPermission) {
            if (hasPermission) {
                networksState.value = wifiHelper.getScanResults()
            } else {
                launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }

        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    drawerContainerColor = MaterialTheme.colorScheme.background,
                    drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp),
                    modifier = Modifier.width(300.dp)
                ) {
                    DrawerContent(
                        isDarkMode = isDarkTheme,
                        isEnglish = isEnglish,
                        onModeToggle = { isDarkTheme = !isDarkTheme },
                        onLanguageToggle = { isEnglish = !isEnglish },
                        onScanClick = {
                            scope.launch {
                                drawerState.close()
                                wifiHelper.startScan()
                                Toast.makeText(context, if (isEnglish) "Scanning Dimension..." else "Rastreando Dimensión...", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onAtaqueClick = {
                            scope.launch {
                                drawerState.close()
                                if (selectedSsids.isNotEmpty() && !isConnecting) {
                                    isConnecting = true
                                    for (ssid in selectedSsids) {
                                        Toast.makeText(context, if (isEnglish) "Attacking $ssid..." else "Atacando $ssid...", Toast.LENGTH_SHORT).show()
                                        val finalPass = passwordInput.ifEmpty { wifiHelper.getWifiPasswordRoot(ssid) }
                                        wifiHelper.connectToWifi(ssid, finalPass, context)
                                        delay(8000)
                                    }
                                    isConnecting = false
                                    Toast.makeText(context, if (isEnglish) "Ritual Complete" else "Ritual Finalizado", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        onChannelAnalyzerClick = {
                            scope.launch {
                                drawerState.close()
                                showChannelAnalyzer = true
                            }
                        },
                        onExportClick = {
                            scope.launch {
                                drawerState.close()
                                val report = buildString {
                                    append("😈 DEMONIWIFI - GRIMOIRE REPORT 📡\n")
                                    append("==================================\n")
                                    networks.forEach { net ->
                                        append("SSID: ${net.ssid} | BSSID: ${net.bssid} | RSSI: ${net.signalLevel}dBm | Ch: ${net.channel} | Sec: ${net.capabilities}\n")
                                    }
                                }
                                clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(report))
                                Toast.makeText(context, if (isEnglish) "Grimoire copied to clipboard!" else "¡Grimorio copiado al portapapeles!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onHelpClick = {
                            scope.launch {
                                drawerState.close()
                                showHelpDialog = true
                            }
                        }
                    )
                }
            }
        ) {
            Scaffold(
                modifier = Modifier.fillMaxSize().imePadding(),
                topBar = {
                    Column {
                        CenterAlignedTopAppBar(
                            modifier = Modifier.height(100.dp),
                            title = {
                                StyledTitle(fontSize = 32, showDevil = true, showAntenna = true)
                            },
                            navigationIcon = {
                                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                    Icon(Icons.Default.Menu, contentDescription = "Menu", tint = DemonicRed)
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = if (isDarkTheme) Color.Black.copy(alpha = 0.5f) else AshGrey.copy(alpha = 0.9f),
                                titleContentColor = Color.Unspecified
                            )
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(3.dp)
                                .height(1.dp)
                                .background(
                                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                        colors = listOf(ShinySilver, Color.Gray)
                                    )
                                )
                        )
                    }
                },
                containerColor = MaterialTheme.colorScheme.background,
                bottomBar = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(12.dp)
                            .navigationBarsPadding()
                            .border(1.dp, DemonicRed)
                            .padding(6.dp)
                    ) {
                        Text(
                            text = if (selectedSsids.isNotEmpty()) 
                                (if (isEnglish) "SELECTED: ${selectedSsids.size}" else "SELECCIONADAS: ${selectedSsids.size}") 
                            else 
                                (if (isEnglish) "SELECT TARGET NETWORKS" else "SELECCIONA LAS REDES OBJETIVO"),
                            color = PentagramGold,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = passwordInput,
                            onValueChange = { passwordInput = it },
                            label = { Text(if (isEnglish) "ABYSS KEY (GENERAL)" else "CLAVE DEL ABISMO", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedBorderColor = DemonicRed,
                                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                focusedLabelColor = DemonicRed,
                                cursorColor = DemonicRed
                            ),
                            singleLine = true,
                            enabled = !isConnecting
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = {
                                if (selectedSsids.isNotEmpty() && !isConnecting) {
                                    isConnecting = true
                                    scope.launch {
                                        for (ssid in selectedSsids) {
                                            val finalPass = passwordInput.ifEmpty { wifiHelper.getWifiPasswordRoot(ssid) }
                                            wifiHelper.connectToWifi(ssid, finalPass, context)
                                            delay(8000)
                                        }
                                        isConnecting = false
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = if(isConnecting) Color.Gray else DemonicRed),
                            shape = RoundedCornerShape(0.dp),
                            enabled = !isConnecting
                        ) {
                            Text(
                                if(isConnecting) 
                                    (if(isEnglish) "PROCESSING SOULS..." else "PROCESANDO...") 
                                else 
                                    (if(isEnglish) "INVOKE SEQUENTIAL ATTACK" else "ATAQUE SECUENCIAL"), 
                                color = Color.White, 
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .padding(innerPadding)
                        .padding(horizontal = 8.dp)
                        .padding(bottom = 8.dp)
                        .fillMaxSize()
                ) {
                    Button(
                        onClick = { if (hasPermission) wifiHelper.startScan() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDarkTheme) Color(0xFF330000) else Color(0xFFB71C1C).copy(alpha = 0.8f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(0.5.dp, DemonicRed.copy(alpha = 0.6f), RoundedCornerShape(4.dp)),
                        shape = RoundedCornerShape(4.dp),
                        contentPadding = PaddingValues(4.dp)
                    ) {
                        Text(
                            if (isEnglish) "REFRESH WIFI DIMENSION" else "REFRESCAR DIMENSIÓN WIFI", 
                            color = Color.White, 
                            fontSize = 12.sp, 
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, DemonicRed)
                            .padding(4.dp)
                    ) {
                        Text("SEL.", Modifier.weight(0.7f), color = PentagramGold, fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.Center)
                        Text("SSID", Modifier.weight(3f), color = PentagramGold, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("RSSI", Modifier.weight(0.8f), color = PentagramGold, fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.Center)
                        Text(if (isEnglish) "SEC." else "SEGUR.", Modifier.weight(1.2f), color = PentagramGold, fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.Center)
                    }

                    LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        items(networks) { network ->
                            WifiRow(
                                network = network,
                                isSelected = selectedSsids.contains(network.ssid),
                                onSelect = {
                                    selectedSsids = if (selectedSsids.contains(network.ssid)) selectedSsids - network.ssid else selectedSsids + network.ssid
                                },
                                onDetailClick = {
                                    selectedNetworkForDetails = network
                                }
                            )
                        }
                    }
                }
            }
        }

        // --- NETWORK DETAILS & PING TEST DIALOG ---
        selectedNetworkForDetails?.let { net ->
            NetworkDetailDialog(
                network = net,
                isEnglish = isEnglish,
                wifiHelper = wifiHelper,
                onDismiss = { selectedNetworkForDetails = null }
            )
        }

        // --- CHANNEL ANALYZER DIALOG ---
        if (showChannelAnalyzer) {
            ChannelAnalyzerDialog(
                networks = networks,
                isEnglish = isEnglish,
                onDismiss = { showChannelAnalyzer = false }
            )
        }

        // --- HELP DIALOG ---
        if (showHelpDialog) {
            HelpDialog(
                isEnglish = isEnglish,
                onDismiss = { showHelpDialog = false }
            )
        }
    }
}

@Composable
fun NetworkDetailDialog(network: WifiNetwork, isEnglish: Boolean, wifiHelper: WifiHelper, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pingOutput by remember { mutableStateOf<String?>(null) }
    var isPingLoading by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(
                text = "😈 ${network.ssid}",
                color = DemonicRed,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                DetailItem(if (isEnglish) "BSSID:" else "BSSID:", network.bssid)
                DetailItem(if (isEnglish) "Signal (RSSI):" else "Señal (RSSI):", "${network.signalLevel} dBm")
                DetailItem(if (isEnglish) "Frequency:" else "Frecuencia:", "${network.frequency} MHz")
                DetailItem(if (isEnglish) "Channel:" else "Canal:", "${network.channel}")
                DetailItem(if (isEnglish) "Capabilities:" else "Capacidades:", network.capabilities)
                
                Spacer(modifier = Modifier.height(12.dp))
                
                if (pingOutput != null) {
                    Text(
                        text = if (isEnglish) "Gateway Ping Result:" else "Resultado Ping Gateway:",
                        color = PentagramGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.8f))
                            .border(1.dp, DemonicRed)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = pingOutput!!,
                            color = Color.Green,
                            fontSize = 11.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }
                }
            }
        },
        confirmButton = {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Button(
                    onClick = {
                        isPingLoading = true
                        scope.launch {
                            pingOutput = wifiHelper.pingGateway(context)
                            isPingLoading = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF330000)),
                    border = BorderStroke(1.dp, DemonicRed)
                ) {
                    Text(
                        if (isPingLoading) (if (isEnglish) "Pinging..." else "Haciendo Ping...") 
                        else (if (isEnglish) "Abyss Ping" else "Ping al Abismo"),
                        color = Color.White,
                        fontSize = 12.sp
                    )
                }

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = DemonicRed)
                ) {
                    Text(if (isEnglish) "Close" else "Cerrar", color = Color.White)
                }
            }
        }
    )
}

@Composable
fun DetailItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = PentagramGold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Text(value, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
    }
}

@Composable
fun ChannelAnalyzerDialog(networks: List<WifiNetwork>, isEnglish: Boolean, onDismiss: () -> Unit) {
    val channelCounts = networks.groupingBy { it.channel }.eachCount().toSortedMap()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(
                text = if (isEnglish) "📡 Channel Congestion Analyzer" else "📡 Analizador de Canales",
                color = PentagramGold,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = if (isEnglish) "Detected channel distribution in current dimension:" else "Distribución de canales detectados en la dimensión actual:",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                if (channelCounts.isEmpty()) {
                    Text(
                        text = if (isEnglish) "No networks scanned yet." else "Aún no hay redes escaneadas.",
                        color = DemonicRed,
                        fontSize = 14.sp
                    )
                } else {
                    channelCounts.forEach { (channel, count) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Channel / Canal $channel", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                            Text("$count ${if (isEnglish) "networks" else "redes"}", color = DemonicRed, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = DemonicRed)
            ) {
                Text(if (isEnglish) "Close" else "Cerrar", color = Color.White)
            }
        }
    )
}

@Composable
fun HelpDialog(isEnglish: Boolean, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(
                text = if (isEnglish) "😈 DemoniWifi Help" else "😈 Ayuda de DemoniWifi",
                color = DemonicRed,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                val helpText = if (isEnglish) {
                    "1. Scan Dimension: Refresh nearby Wi-Fi networks.\n\n" +
                    "2. Select Targets: Check boxes of networks you wish to audit.\n\n" +
                    "3. Abyss Key: Input a universal password or leave blank for root extraction.\n\n" +
                    "4. Sequential Attack: Executes automated connection tests on selected targets.\n\n" +
                    "5. Channel Analyzer: Inspect wireless congestion in your current location.\n\n" +
                    "6. Grimoire Export: Copy full scan report to clipboard."
                } else {
                    "1. Escanear Dimensión: Refresca las redes Wi-Fi cercanas.\n\n" +
                    "2. Seleccionar Objetivos: Marca las casillas de las redes a auditar.\n\n" +
                    "3. Clave del Abismo: Introduce una contraseña universal o déjala en blanco para extracción root.\n\n" +
                    "4. Ataque Secuencial: Ejecuta pruebas de conexión automatizadas en los objetivos seleccionados.\n\n" +
                    "5. Analizador de Canales: Inspecciona la congestión inalámbrica en tu ubicación.\n\n" +
                    "6. Exportar Grimorio: Copia el informe completo de red al portapapeles."
                }
                Text(
                    text = helpText,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 13.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = DemonicRed)
            ) {
                Text(if (isEnglish) "Got it" else "Entendido", color = Color.White)
            }
        }
    )
}

@Composable
fun DrawerContent(
    isDarkMode: Boolean, 
    isEnglish: Boolean, 
    onModeToggle: () -> Unit,
    onLanguageToggle: () -> Unit,
    onScanClick: () -> Unit,
    onAtaqueClick: () -> Unit,
    onChannelAnalyzerClick: () -> Unit,
    onExportClick: () -> Unit,
    onHelpClick: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "😈",
            fontSize = 42.sp,
            style = androidx.compose.ui.text.TextStyle(shadow = Shadow(
                color = Color.Black.copy(alpha = 0.8f),
                offset = Offset(6f, 6f),
                blurRadius = 12f
            ))
        )
        StyledTitle(
            fontSize = 28, 
            isCentered = true, 
            modifier = Modifier.padding(bottom = 16.dp),
            showDevil = false,
            showAntenna = true
        )
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.5.dp)
                .background(
                    brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                        colors = listOf(Color.Transparent, DemonicRed, Color.Transparent)
                    )
                )
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        DrawerItem(label = if (isEnglish) "Scan Dimension" else "Escanear Dimensión", icon = Icons.Default.Refresh, onClick = onScanClick)
        Spacer(modifier = Modifier.height(12.dp))
        DrawerItem(label = if (isEnglish) "Sequential Attack" else "Ataque Secuencial", icon = Icons.Default.Warning, onClick = onAtaqueClick)
        Spacer(modifier = Modifier.height(12.dp))
        DrawerItem(label = if (isEnglish) "Channel Analyzer" else "Analizador de Canales", icon = Icons.Default.NetworkWifi, onClick = onChannelAnalyzerClick)
        Spacer(modifier = Modifier.height(12.dp))
        DrawerItem(label = if (isEnglish) "Export Grimoire" else "Exportar Grimorio", icon = Icons.Default.Share, onClick = onExportClick)
        Spacer(modifier = Modifier.height(12.dp))
        DrawerItem(
            label = if (isDarkMode) (if (isEnglish) "Soul Mode" else "Modo Alma") else (if (isEnglish) "Abyss Mode" else "Modo Abismo"), 
            icon = if (isDarkMode) Icons.Default.WbSunny else Icons.Default.NightsStay, 
            onClick = onModeToggle
        )
        Spacer(modifier = Modifier.height(12.dp))
        DrawerItem(label = if (isEnglish) "Language: English" else "Idioma: Español", icon = Icons.Default.Language, onClick = onLanguageToggle)
        Spacer(modifier = Modifier.height(12.dp))
        DrawerItem(label = if (isEnglish) "Help & Ritual" else "Ayuda del Ritual", icon = Icons.AutoMirrored.Filled.Help, onClick = onHelpClick)
        
        Spacer(modifier = Modifier.weight(1f))
        
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp), 
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                            colors = listOf(Color.Transparent, Color.DarkGray, Color.Transparent)
                        )
                    )
            )
            Spacer(modifier = Modifier.height(8.dp))
            val footerColor = if (isDarkMode) AshGrey else Color.DarkGray
            Text(
                text = "BY JAYLIZ & DEMONI-TEAM",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold, 
                color = footerColor,
                letterSpacing = 2.sp
            )
            Text(
                text = "v1.0.666-STABLE", 
                fontSize = 9.sp, 
                color = DemonicRed
            )
        }
    }
}

@Composable
fun DrawerItem(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit = {}) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val containerColor = if (isDark) AbyssBlack else Color.White
    val textColor = if (isDark) SoulWhite else AbyssBlack

    Surface(
        onClick = onClick,
        color = containerColor, 
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .shadow(elevation = 4.dp, shape = RoundedCornerShape(12.dp))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = HellRed, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = label, 
                color = textColor,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp)
            )
        }
    }
}

@Composable
fun StyledTitle(
    showDevil: Boolean = true,
    showAntenna: Boolean = true,
    fontSize: Int = 28, 
    isCentered: Boolean = false,
    modifier: Modifier = Modifier
) {
    val titleShadow = Shadow(
        color = Color.Black.copy(alpha = 0.8f),
        offset = Offset(6f, 6f),
        blurRadius = 12f
    )

    val styledTitle = buildAnnotatedString {
        if (showDevil) {
            withStyle(style = SpanStyle(shadow = titleShadow)) {
                append("😈 ")
            }
        }
        withStyle(style = SpanStyle(
            color = HellRed,
            fontWeight = FontWeight.ExtraBold,
            shadow = titleShadow
        )) {
            append("De")
        }
        withStyle(style = SpanStyle(
            color = PentagramGold,
            fontWeight = FontWeight.ExtraBold,
            shadow = titleShadow
        )) {
            append("moni")
        }
        withStyle(style = SpanStyle(
            color = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color.White else AbyssBlack,
            fontWeight = FontWeight.ExtraBold,
            shadow = titleShadow
        )) {
            append("Wifi")
            if (showAntenna) {
                append(" 📡")
            }
        }
    }

    Text(
        text = styledTitle,
        style = MaterialTheme.typography.headlineMedium.copy(
            fontSize = fontSize.sp,
            fontWeight = FontWeight.ExtraBold
        ),
        textAlign = if (isCentered) TextAlign.Center else TextAlign.Start,
        modifier = modifier.then(if (isCentered) Modifier.fillMaxWidth() else Modifier)
    )
}

@Composable
fun WifiRow(network: WifiNetwork, isSelected: Boolean, onSelect: () -> Unit, onDetailClick: () -> Unit) {
    val security = when {
        network.capabilities.contains("WPA3") -> "WPA3"
        network.capabilities.contains("WPA2") -> "WPA2"
        network.capabilities.contains("WPA") -> "WPA"
        else -> "OPEN"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isSelected) Color(0xFF220000).copy(alpha = if(MaterialTheme.colorScheme.background.luminance() < 0.5f) 1f else 0.1f) else Color.Transparent)
            .border(0.5.dp, DemonicRed)
            .clickable { onDetailClick() }
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isSelected,
            onCheckedChange = { onSelect() },
            modifier = Modifier.weight(0.7f).size(24.dp),
            colors = CheckboxDefaults.colors(
                checkedColor = DemonicRed,
                uncheckedColor = Color.Gray,
                checkmarkColor = Color.White
            )
        )
        
        Text(network.ssid, Modifier.weight(3f), color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
        Text("${network.signalLevel}", Modifier.weight(0.8f), color = DemonicRed, fontSize = 12.sp, textAlign = TextAlign.Center)
        Text(security, Modifier.weight(1.2f), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), fontSize = 10.sp, textAlign = TextAlign.Center)
    }
}
