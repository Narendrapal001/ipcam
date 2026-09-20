package com.example.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Collections

object NetworkUtils {

    fun getLocalIpAddress(): String {
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            // Prioritize wlan0 and ap0 (hotspot) interfaces
            val sorted = interfaces.sortedWith(compareByDescending {
                when {
                    it.name.startsWith("wlan") -> 10
                    it.name.startsWith("ap") -> 9
                    it.name.startsWith("eth") -> 8
                    it.name.startsWith("rndis") -> 7 // USB tethering
                    else -> 1
                }
            })

            for (networkInterface in sorted) {
                if (!networkInterface.isUp || networkInterface.isLoopback) continue

                val addresses = Collections.list(networkInterface.inetAddresses)
                for (address in addresses) {
                    if (!address.isLoopbackAddress && address is Inet4Address) {
                        val hostAddress = address.hostAddress ?: continue
                        // Filter out link-local and invalid addresses
                        if (!hostAddress.startsWith("127.") && !hostAddress.startsWith("169.254.")) {
                            return hostAddress
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return "127.0.0.1"
    }

    fun getNetworkName(context: Context): String {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val network = cm?.activeNetwork
            val capabilities = cm?.getNetworkCapabilities(network)
            when {
                capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> "Wi-Fi Network"
                capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> "Cellular / Hotspot"
                capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true -> "Ethernet"
                else -> "Local Network"
            }
        } catch (e: Exception) {
            "Local Network"
        }
    }
}
