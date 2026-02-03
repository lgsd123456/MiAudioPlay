package com.miaudioplay.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log

/**
 * 网络工具类
 */
object NetworkUtils {
    private const val TAG = "NetworkUtils"
    
    /**
     * 检查网络是否连接
     */
    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            
            val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            val hasValidated = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            
            Log.d(TAG, "Network available: hasInternet=$hasInternet, hasValidated=$hasValidated")
            hasInternet && hasValidated
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            val isConnected = networkInfo?.isConnected == true
            Log.d(TAG, "Network available (legacy): $isConnected")
            isConnected
        }
    }
    
    /**
     * 检查是否连接到WiFi
     */
    fun isWifiConnected(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            networkInfo?.type == ConnectivityManager.TYPE_WIFI
        }
    }
    
    /**
     * 获取网络状态描述
     */
    fun getNetworkStatusDescription(context: Context): String {
        if (!isNetworkAvailable(context)) {
            return "无网络连接"
        }
        
        return if (isWifiConnected(context)) {
            "WiFi已连接"
        } else {
            "移动网络已连接"
        }
    }
}
