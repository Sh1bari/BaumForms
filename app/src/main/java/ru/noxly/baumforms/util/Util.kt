package ru.noxly.baumforms.util

import java.net.Inet4Address
import java.net.NetworkInterface

fun getLocalIpAddress(): String? {
    try {
        val interfaces = NetworkInterface.getNetworkInterfaces()
        for (intf in interfaces) {
            if (!intf.isUp || intf.isLoopback) continue

            // Проверка, чтобы приоритет был у Wi-Fi/Hotspot интерфейсов
            if (intf.name.startsWith("wlan") || intf.name.startsWith("ap")) {
                val addresses = intf.inetAddresses
                for (addr in addresses) {
                    if (addr is Inet4Address && !addr.isLoopbackAddress) {
                        return addr.hostAddress
                    }
                }
            }
        }

        // Fallback: если не нашли wlan/ap — ищем любой подходящий интерфейс
        val fallbackInterfaces = NetworkInterface.getNetworkInterfaces()
        for (intf in fallbackInterfaces) {
            if (!intf.isUp || intf.isLoopback) continue
            val addresses = intf.inetAddresses
            for (addr in addresses) {
                if (addr is Inet4Address && !addr.isLoopbackAddress) {
                    return addr.hostAddress
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return null
}