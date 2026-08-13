package com.cncverse.stremiobridge.tunnel

import android.os.Build
import com.cncverse.stremiobridge.repo.AndroidContextHolder
import java.io.File

actual fun getCloudflaredBinaryPath(): String {
    val context = AndroidContextHolder.appContext
    val nativeLibDir = context.applicationInfo.nativeLibraryDir
    return File(nativeLibDir, "libcloudflared.so").absolutePath
}

actual fun setFileExecutable(filePath: String) {
    val file = File(filePath)
    if (file.exists()) {
        file.setReadable(true, false)
        file.setWritable(true, true)
        file.setExecutable(true, false)
        try {
            Runtime.getRuntime().exec(arrayOf("chmod", "755", filePath)).waitFor()
        } catch (_: Exception) {}
        try {
            Runtime.getRuntime().exec(arrayOf("chmod", "+x", filePath)).waitFor()
        } catch (_: Exception) {}
    }
}

actual fun getPlatformCloudflaredDownloadUrl(): String {
    val abis = Build.SUPPORTED_ABIS ?: emptyArray()
    val primaryAbi = abis.firstOrNull()?.lowercase() ?: ""
    
    return when {
        primaryAbi.contains("arm64") || primaryAbi.contains("aarch64") -> {
            "https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-linux-arm64"
        }
        primaryAbi.contains("arm") || primaryAbi.contains("v7") -> {
            "https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-linux-arm"
        }
        primaryAbi.contains("x86_64") || primaryAbi.contains("amd64") -> {
            "https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-linux-amd64"
        }
        primaryAbi.contains("x86") -> {
            "https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-linux-386"
        }
        else -> {
            "https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-linux-arm64"
        }
    }
}

actual fun resolveCloudflareEdgeIps(): List<String> {
    val edgeIps = mutableListOf<String>()
    try {
        val hosts = listOf("region1.v2.argotunnel.com", "region2.v2.argotunnel.com")
        for (host in hosts) {
            java.net.InetAddress.getAllByName(host)
                .filter { it is java.net.Inet4Address }
                .forEach { edgeIps.add("${it.hostAddress}:7844") }
        }
    } catch (e: Exception) {
        // Fallback or ignore if DNS fails entirely, but it shouldn't if device has internet
    }
    return edgeIps
}
