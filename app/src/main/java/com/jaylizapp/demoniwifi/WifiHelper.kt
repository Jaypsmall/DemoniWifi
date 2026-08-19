package com.jaylizapp.demoniwifi

import android.content.Context
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSpecifier
import android.net.wifi.WifiNetworkSuggestion
import android.net.wifi.WifiConfiguration
import android.net.ConnectivityManager
import android.net.NetworkRequest
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

data class WifiNetwork(
    val ssid: String,
    val bssid: String,
    val signalLevel: Int,
    val capabilities: String,
    val frequency: Int
)

@Suppress("DEPRECATION")
class WifiHelper(context: Context) {
    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    fun getScanResults(): List<WifiNetwork> {
        return try {
            wifiManager.scanResults.map {
                WifiNetwork(
                    ssid = it.SSID,
                    bssid = it.BSSID,
                    signalLevel = it.level,
                    capabilities = it.capabilities,
                    frequency = it.frequency
                )
            }.sortedByDescending { it.signalLevel }
        } catch (e: SecurityException) {
            emptyList()
        }
    }

    @Suppress("DEPRECATION")
    fun startScan() {
        wifiManager.startScan()
    }

    suspend fun getWifiPasswordRoot(targetSsid: String): String = withContext(Dispatchers.IO) {
        try {
            val process = Runtime.getRuntime().exec("su")
            val os = process.outputStream
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            
            val paths = arrayOf(
                "/data/misc/wifi/WifiConfigStore.xml",
                "/data/misc/apexdata/com.android.wifi/WifiConfigStore.xml",
                "/data/misc/wifi/wpa_supplicant.conf"
            )
            
            var foundPassword = ""
            
            for (path in paths) {
                val command = "cat $path | grep -A 20 '\"$targetSsid\"' | grep -E 'PreSharedKey|psk=' | cut -d'>' -f2 | cut -d'<' -f1 | cut -d'=' -f2 | tr -d '\"'\n"
                os.write(command.toByteArray())
                os.flush()
                
                val output = reader.readLine()
                if (!output.isNullOrBlank() && !output.contains("No such file")) {
                    foundPassword = output.trim()
                    break
                }
            }
            
            os.write("exit\n".toByteArray())
            os.flush()
            process.waitFor()
            
            foundPassword
        } catch (e: Exception) {
            ""
        }
    }

    fun connectToWifi(ssid: String, password: String, context: Context) {
        Log.d("WifiHelper", "Intentando conectar a $ssid")
        
        // Determinar seguridad (esto es simplificado, en producción se usaría el ScanResult original)
        val isWpa3 = false // Podríamos pasar esto desde la UI
        val isOpen = password.isEmpty()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val suggestionBuilder = WifiNetworkSuggestion.Builder()
                .setSsid(ssid)
            
            val specifierBuilder = WifiNetworkSpecifier.Builder()
                .setSsid(ssid)

            if (!isOpen) {
                if (isWpa3) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        suggestionBuilder.setWpa3Passphrase(password)
                        specifierBuilder.setWpa3Passphrase(password)
                    } else {
                        suggestionBuilder.setWpa2Passphrase(password)
                        specifierBuilder.setWpa2Passphrase(password)
                    }
                } else {
                    suggestionBuilder.setWpa2Passphrase(password)
                    specifierBuilder.setWpa2Passphrase(password)
                }
            }

            // 1. Añadir sugerencia
            val status = wifiManager.addNetworkSuggestions(listOf(suggestionBuilder.build()))
            Log.d("WifiHelper", "Status sugerencia: $status")

            // 2. Pedir red activamente (esto lanza el diálogo del sistema)
            val request = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .setNetworkSpecifier(specifierBuilder.build())
                .build()

            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            connectivityManager.requestNetwork(request, object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: android.net.Network) {
                    super.onAvailable(network)
                    connectivityManager.bindProcessToNetwork(network)
                    Log.d("WifiHelper", "Red conectada y vinculada")
                }

                override fun onUnavailable() {
                    super.onUnavailable()
                    Log.d("WifiHelper", "El usuario canceló o la red no está disponible")
                }
            })
            
            Toast.makeText(context, "Lanzando diálogo de sistema para $ssid...", Toast.LENGTH_LONG).show()
        } else {
            // Android 9-
            val wifiConfig = WifiConfiguration().apply {
                SSID = "\"$ssid\""
                if (isOpen) {
                    allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE)
                } else {
                    preSharedKey = "\"$password\""
                }
            }
            val netId = wifiManager.addNetwork(wifiConfig)
            wifiManager.disconnect()
            wifiManager.enableNetwork(netId, true)
            wifiManager.reconnect()
            Toast.makeText(context, "Conectando al estilo clásico...", Toast.LENGTH_SHORT).show()
        }
    }
}
