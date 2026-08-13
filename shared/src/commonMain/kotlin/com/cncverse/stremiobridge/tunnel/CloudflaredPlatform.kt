package com.cncverse.stremiobridge.tunnel

expect fun getCloudflaredBinaryPath(): String
expect fun setFileExecutable(filePath: String)
expect fun getPlatformCloudflaredDownloadUrl(): String
expect fun resolveCloudflareEdgeIps(): List<String>
