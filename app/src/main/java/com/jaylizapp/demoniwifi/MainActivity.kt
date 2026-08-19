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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.jaylizapp.demoniwifi.ui.theme.DemoniWifiTheme
import com.jaylizapp.demoniwifi.ui.theme.DemonicRed
import com.jaylizapp.demoniwifi.ui.theme.EvilBlack
import com.jaylizapp.demoniwifi.ui.theme.PentagramGold
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
            DemoniWifiTheme {
                DemonicWifiApp(wifiHelper, networksState)
            }
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
            // Ya estaba desregistrado
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DemonicWifiApp(wifiHelper: WifiHelper, networksState: MutableState<List<WifiNetwork>>) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val networks by networksState
    
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

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

    val titleShadow = Shadow(
        color = Color.Black.copy(alpha = 0.8f), // Negro potente
        offset = Offset(6f, 6f),               // Desplazamiento (X, Y)
        blurRadius = 12f                       // Suavizado de la sombra
    )

    val demoniTitle = buildAnnotatedString {
        // Parte "Demoni" (Rojo)
        withStyle(style = SpanStyle(
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.ExtraBold,
            shadow = titleShadow
        )) {
            append("Demoni")
        }

        // Parte final y Emoji 😈
        withStyle(style = SpanStyle(
            color = Color.White,
            fontWeight = FontWeight.ExtraBold,
            shadow = titleShadow
        )) {
            append("Wifi 😈")
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color(0xFF1A0000),
                drawerShape = RoundedCornerShape(0.dp),
                modifier = Modifier.width(280.dp)
            ) {
                Spacer(modifier = Modifier.height(48.dp))
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = demoniTitle,
                        style = MaterialTheme.typography.headlineMedium
                    )
                    HorizontalDivider(color = DemonicRed, thickness = 1.dp, modifier = Modifier.padding(vertical = 16.dp))
                }
                
                NavigationDrawerItem(
                    label = { Text("ESCANEAR DIMENSIÓN", color = Color.White) },
                    selected = false,
                    onClick = {
                        scope.launch { 
                            drawerState.close()
                            wifiHelper.startScan()
                            Toast.makeText(context, "Rastreando espectros...", Toast.LENGTH_SHORT).show()
                        }
                    },
                    icon = { Icon(Icons.Default.Refresh, contentDescription = null, tint = PentagramGold) },
                    colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
                )
                NavigationDrawerItem(
                    label = { Text("ATAQUE MASIVO", color = Color.White) },
                    selected = false,
                    onClick = { 
                        scope.launch { 
                            drawerState.close() 
                            if (selectedSsids.isNotEmpty() && !isConnecting) {
                                isConnecting = true
                                for (ssid in selectedSsids) {
                                    Toast.makeText(context, "Atacando red: $ssid...", Toast.LENGTH_SHORT).show()
                                    val finalPass = passwordInput.ifEmpty { wifiHelper.getWifiPasswordRoot(ssid) }
                                    wifiHelper.connectToWifi(ssid, finalPass, context)
                                    delay(8000) 
                                }
                                isConnecting = false
                                Toast.makeText(context, "Ritual de conexión finalizado", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "Marca primero las víctimas (redes)", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = DemonicRed) },
                    colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
                )
                NavigationDrawerItem(
                    label = { Text("CONFIGURACIÓN", color = Color.White) },
                    selected = false,
                    onClick = { 
                        scope.launch { 
                            drawerState.close()
                            Toast.makeText(context, "Ajustes del averno próximamente...", Toast.LENGTH_SHORT).show()
                        } 
                    },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null, tint = Color.Gray) },
                    colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
                )
                Spacer(modifier = Modifier.weight(1f))
                
                // Créditos abajo
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    HorizontalDivider(color = Color.DarkGray, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "BY JAYLIZ & DEMONI-TEAM",
                        color = Color.Gray,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "v1.0.666-STABLE",
                        color = DemonicRed,
                        fontSize = 9.sp
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize().imePadding(),
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = "📡 DEMONI-WIFI 📡",
                            color = DemonicRed,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu", tint = DemonicRed)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = EvilBlack,
                        scrolledContainerColor = Color.Unspecified,
                        navigationIconContentColor = Color.Unspecified,
                        titleContentColor = DemonicRed,
                        actionIconContentColor = Color.Unspecified
                    )
                )
            },
            containerColor = EvilBlack,
            bottomBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1A0000))
                        .padding(16.dp)
                        .navigationBarsPadding()
                        .border(1.dp, DemonicRed)
                        .padding(8.dp)
                ) {
                    Text(
                        text = if (selectedSsids.isNotEmpty()) "SELECCIONADAS: ${selectedSsids.size}" else "SELECCIONA LAS REDES OBJETIVO",
                        color = PentagramGold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (selectedSsids.isNotEmpty()) {
                        Text(
                            text = selectedSsids.joinToString(", ").take(50) + if(selectedSsids.joinToString(", ").length > 50) "..." else "",
                            color = Color.Gray,
                            fontSize = 10.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        label = { Text("CLAVE DEL ABISMO (GENERAL)", color = Color.Gray) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = DemonicRed,
                            unfocusedBorderColor = Color.DarkGray,
                            focusedLabelColor = DemonicRed,
                            cursorColor = DemonicRed
                        ),
                        singleLine = true,
                        enabled = !isConnecting
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            if (selectedSsids.isNotEmpty() && !isConnecting) {
                                isConnecting = true
                                scope.launch {
                                    for (ssid in selectedSsids) {
                                        Toast.makeText(context, "Atacando red: $ssid...", Toast.LENGTH_SHORT).show()
                                        
                                        val finalPass = passwordInput.ifEmpty {
                                            wifiHelper.getWifiPasswordRoot(ssid)
                                        }

                                        wifiHelper.connectToWifi(ssid, finalPass, context)
                                        delay(8000) 
                                    }
                                    isConnecting = false
                                    Toast.makeText(context, "Ritual de conexión finalizado", Toast.LENGTH_LONG).show()
                                }
                            } else if (!isConnecting) {
                                Toast.makeText(context, "Marca primero las víctimas (redes)", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = if(isConnecting) Color.Gray else DemonicRed),
                        shape = RoundedCornerShape(0.dp),
                        enabled = !isConnecting
                    ) {
                        Text(if(isConnecting) "PROCESANDO ALMAS..." else "INVOCAR ATAQUE SECUENCIAL", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(top = innerPadding.calculateTopPadding(), start = 8.dp, end = 8.dp, bottom = 8.dp)
                    .fillMaxSize()
            ) {
                Button(
                    onClick = {
                        if (hasPermission) {
                            wifiHelper.startScan()
                            Toast.makeText(context, "Rastreando espectros...", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF440000)),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(0.dp),
                    contentPadding = PaddingValues(4.dp)
                ) {
                    Text("REFRESCAR DIMENSIÓN WIFI", color = Color.White, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF330000))
                        .border(1.dp, DemonicRed)
                        .padding(4.dp)
                ) {
                    Text("SEL.", Modifier.weight(0.7f), color = PentagramGold, fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.Center)
                    Text("SSID", Modifier.weight(3f), color = PentagramGold, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text("RSSI", Modifier.weight(0.8f), color = PentagramGold, fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.Center)
                    Text("SEGUR.", Modifier.weight(1.2f), color = PentagramGold, fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.Center)
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(networks) { network ->
                        WifiRow(
                            network = network,
                            isSelected = selectedSsids.contains(network.ssid),
                            onSelect = {
                                selectedSsids = if (selectedSsids.contains(network.ssid)) {
                                    selectedSsids - network.ssid
                                } else {
                                    selectedSsids + network.ssid
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WifiRow(network: WifiNetwork, isSelected: Boolean, onSelect: () -> Unit) {
    val security = when {
        network.capabilities.contains("WPA3") -> "WPA3"
        network.capabilities.contains("WPA2") -> "WPA2"
        network.capabilities.contains("WPA") -> "WPA"
        else -> "OPEN"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isSelected) Color(0xFF440000) else Color.Transparent)
            .border(0.5.dp, DemonicRed)
            .clickable { onSelect() }
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
        
        Text(network.ssid, Modifier.weight(3f), color = Color.White, fontSize = 12.sp)
        Text("${network.signalLevel}", Modifier.weight(0.8f), color = DemonicRed, fontSize = 12.sp, textAlign = TextAlign.Center)
        Text(security, Modifier.weight(1.2f), color = Color.Gray, fontSize = 10.sp, textAlign = TextAlign.Center)
    }
}
