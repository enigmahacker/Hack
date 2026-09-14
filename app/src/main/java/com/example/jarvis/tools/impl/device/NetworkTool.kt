package com.example.jarvis.tools.impl.device

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.jarvis.tools.Tool
import com.example.jarvis.tools.models.RiskLevel
import com.example.jarvis.tools.models.ToolResult

class NetworkTool : Tool {
    override val name: String = "get_network_status"
    override val description: String = "Checks current internet connectivity, WiFi, and cellular network status."
    override val riskLevel: RiskLevel = RiskLevel.SAFE
    override val parametersJsonSchema: String = """{"type":"object","properties":{}}"""

    override suspend fun execute(context: Context, params: Map<String, Any?>): ToolResult {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val activeNetwork = cm?.activeNetwork
            val capabilities = cm?.getNetworkCapabilities(activeNetwork)

            if (capabilities == null) {
                return ToolResult(isSuccess = true, output = "Device is currently offline with no active network connection.")
            }

            val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            val isWifi = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
            val isCellular = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)

            val transport = when {
                isWifi -> "Wi-Fi"
                isCellular -> "Cellular data"
                else -> "Active network"
            }

            val status = if (hasInternet) "online and connected via $transport" else "connected to $transport, but internet is unreachable"
            ToolResult(
                isSuccess = true,
                output = "System is $status.",
                data = mapOf("connected" to hasInternet, "transport" to transport)
            )
        } catch (e: Exception) {
            ToolResult(isSuccess = false, output = "Network check failed: ${e.message}")
        }
    }
}
