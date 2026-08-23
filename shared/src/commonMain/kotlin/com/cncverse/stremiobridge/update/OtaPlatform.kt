package com.cncverse.stremiobridge.update

expect fun getOtaDownloadDir(): String
expect fun saveOtaFile(data: ByteArray, fileName: String): String
expect fun installOtaUpdate(filePath: String)

/** True on desktop JVM, false on Android. */
expect val isDesktopPlatform: Boolean

/** File extension to look for in release assets. */
val otaAssetExtension: String
    get() = if (isDesktopPlatform) ".msi" else ".apk"
