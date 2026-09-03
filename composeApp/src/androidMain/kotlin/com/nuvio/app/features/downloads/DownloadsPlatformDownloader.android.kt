package com.mirror.app.features.downloads

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.Settings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import mirror.composeapp.generated.resources.*
import org.jetbrains.compose.resources.getString
import java.io.File
import java.io.FileOutputStream
import java.net.URI
import java.util.concurrent.TimeUnit

private val downloadHttpClient = OkHttpClient.Builder()
    .connectTimeout(60, TimeUnit.SECONDS)
    .readTimeout(60, TimeUnit.SECONDS)
    .writeTimeout(60, TimeUnit.SECONDS)
    .followRedirects(true)
    .followSslRedirects(true)
    .build()

internal actual object DownloadsPlatformDownloader {
    private var appContext: Context? = null

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    actual fun start(
        request: DownloadPlatformRequest,
        onProgress: (downloadedBytes: Long, totalBytes: Long?) -> Unit,
        onSuccess: (localFileUri: String, totalBytes: Long?) -> Unit,
        onFailure: (message: String) -> Unit,
    ): DownloadsTaskHandle {
        val job = SupervisorJob()
        val scope = CoroutineScope(job + Dispatchers.IO)
        var call: Call? = null

        scope.launch {
            val context = appContext
            if (context == null) {
                onFailure(runBlocking { getString(Res.string.downloads_error_not_initialized) })
                return@launch
            }

            val downloadsDir = File(context.filesDir, "downloads").apply { mkdirs() }
            val destination = File(downloadsDir, request.destinationFileName)
            val tempFile = File(downloadsDir, "${request.destinationFileName}.part")

            try {
                var resumeFromBytes = tempFile.takeIf { it.exists() }?.length()?.coerceAtLeast(0L) ?: 0L

                fun buildRequest(rangeStart: Long?): Request {
                    val requestBuilder = Request.Builder().url(request.sourceUrl)
                    request.sourceHeaders.forEach { (key, value) ->
                        requestBuilder.header(key, value)
                    }
                    if (rangeStart != null && rangeStart > 0L) {
                        requestBuilder.header("Range", "bytes=$rangeStart-")
                    }
                    return requestBuilder.get().build()
                }

                var attemptedRangeRequest = resumeFromBytes > 0L
                var httpRequest = buildRequest(if (attemptedRangeRequest) resumeFromBytes else null)
                call = downloadHttpClient.newCall(httpRequest)
                var response = call?.execute() ?: error(
                    runBlocking { getString(Res.string.downloads_error_request_failed) },
                )

                if (attemptedRangeRequest && response.code == 416) {
                    response.close()
                    tempFile.delete()
                    resumeFromBytes = 0L
                    attemptedRangeRequest = false
                    httpRequest = buildRequest(null)
                    call = downloadHttpClient.newCall(httpRequest)
                    response = call?.execute() ?: error(
                        runBlocking { getString(Res.string.downloads_error_request_failed) },
                    )
                }

                response.use { response ->
                    if (!response.isSuccessful) {
                        error(
                            runBlocking {
                                getString(Res.string.downloads_error_http_failed, response.code)
                            },
                        )
                    }

                    val isPartialResume = attemptedRangeRequest && response.code == 206 && resumeFromBytes > 0L
                    val appendToTemp = isPartialResume
                    val startingBytes = if (appendToTemp) resumeFromBytes else 0L

                    if (!appendToTemp && tempFile.exists()) {
                        tempFile.delete()
                    }

                    val body = response.body ?: error(
                        runBlocking { getString(Res.string.downloads_error_empty_body) },
                    )
                    val totalBytes = resolveTotalBytes(
                        startingBytes = startingBytes,
                        isPartialResume = isPartialResume,
                        contentRangeHeader = response.header("Content-Range"),
                        contentLength = body.contentLength().takeIf { it > 0L },
                    )
                    var downloadedBytes = startingBytes
                    onProgress(downloadedBytes, totalBytes)

                    body.byteStream().use { input ->
                        FileOutputStream(tempFile, appendToTemp).use { output ->
                            val buffer = ByteArray(16 * 1024)
                            while (true) {
                                ensureActive()
                                val read = input.read(buffer)
                                if (read <= 0) break
                                output.write(buffer, 0, read)
                                downloadedBytes += read.toLong()
                                onProgress(downloadedBytes, totalBytes)
                            }
                            output.flush()
                        }
                    }

                    if (destination.exists()) {
                        destination.delete()
                    }
                    if (!tempFile.renameTo(destination)) {
                        tempFile.copyTo(destination, overwrite = true)
                        tempFile.delete()
                    }

                    val finalSize = destination.length()
                    // Use file:// URI — works for both ExoPlayer and MPV native code.
                    // content:// FileProvider URIs break libmpv since native C can't open them.
                    val finalUri = destination.toURI().toString()

                    // Register with MediaStore so file appears in system Files app
                    registerWithMediaStore(context, destination)

                    onSuccess(finalUri, totalBytes ?: finalSize)
                }
            } catch (error: Throwable) {
                onFailure(error.message ?: runBlocking { getString(Res.string.download_failed) })
            }
        }

        job.invokeOnCompletion {
            call?.cancel()
        }

        return AndroidDownloadsTaskHandle(job)
    }

    actual fun removeFile(localFileUri: String?): Boolean {
        if (localFileUri.isNullOrBlank()) return false

        // Try direct file:// path first (old-format URIs stored before the content:// fix)
        val directFile = localFileUri.toLocalFileOrNull()
        if (directFile != null) {
            return runCatching { directFile.delete() }.getOrDefault(false)
        }

        // content:// URIs can't resolve to a File directly; the caller should use
        // removeCompletedFile(fileName) for content-URI backed downloads.
        return false
    }

    /**
     * Deletes a completed download file by its stored [destinationFileName].
     * Use this instead of [removeFile] when only a content:// URI is available.
     */
    actual fun removeCompletedFile(destinationFileName: String): Boolean {
        val context = appContext ?: return false
        val fileName = destinationFileName.trim().takeIf { it.isNotBlank() } ?: return false
        val file = File(File(context.filesDir, "downloads"), fileName)
        if (!file.exists()) return true
        return runCatching { file.delete() }.getOrDefault(false)
    }

    actual fun removePartialFile(destinationFileName: String): Boolean {
        val context = appContext ?: return false
        val downloadsDir = File(context.filesDir, "downloads")
        val tempFile = File(downloadsDir, "$destinationFileName.part")
        if (!tempFile.exists()) return true
        return runCatching { tempFile.delete() }.getOrDefault(false)
    }

    actual fun resolveLocalFileUri(localFileUri: String?, destinationFileName: String): String? {
        val context = appContext ?: return null

        // Resolve the backing File, preferring the stored URI then falling back to fileName lookup
        val resolvedFile = localFileUri
            ?.toLocalFileOrNull()
            ?.takeIf { it.exists() }
            ?: run {
                val fileName = destinationFileName.trim().takeIf { it.isNotBlank() }
                    ?: localFileUri?.toLocalFileOrNull()?.name?.takeIf { it.isNotBlank() }
                    ?: return null
                File(File(context.filesDir, "downloads"), fileName).takeIf { it.exists() }
            }
            ?: return null

        // Return a file:// URI pointing to the actual path. This works for both ExoPlayer
        // (via DefaultDataSource) and MPV (native code can open file:// paths directly).
        // content:// FileProvider URIs break MPV because native libmpv cannot open them.
        return resolvedFile.toURI().toString()
    }

    actual fun openDownloadsDirectory(): Boolean {
        val context = appContext ?: return false
        // Private filesDir is not accessible from external file managers.
        // Open the system Downloads app/folder instead as a useful alternative.
        val intents = listOf(
            // Android 10+ Downloads content URI
            Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("content://downloads/public_downloads")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
            // Generic Downloads manager
            Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
            // Fallback: open the system file manager
            Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_APP_FILES)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
        )
        return intents.any { intent ->
            runCatching { context.startActivity(intent); true }.getOrDefault(false)
        }
    }
}

private class AndroidDownloadsTaskHandle(
    private val job: Job,
) : DownloadsTaskHandle {
    override fun cancel() {
        job.cancel()
    }
}

/**
 * Registers a completed download with MediaStore.Downloads so it appears
 * in the system Files app and Downloads folder. Safe to call multiple times.
 */
private fun registerWithMediaStore(context: Context, file: File) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        // Pre-Android 10: scan the file into MediaStore via broadcast
        runCatching {
            context.sendBroadcast(
                Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE).apply {
                    data = Uri.fromFile(file)
                }
            )
        }
        return
    }
    // Android 10+: insert into MediaStore.Downloads
    runCatching {
        val resolver = context.contentResolver
        val mimeType = guessMimeType(file.name)
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, file.name)
            put(MediaStore.Downloads.MIME_TYPE, mimeType)
            put(MediaStore.Downloads.SIZE, file.length())
            put(MediaStore.Downloads.IS_PENDING, 0)
        }
        val collection = MediaStore.Downloads.getContentUri(
            MediaStore.VOLUME_EXTERNAL_PRIMARY
        )
        // Check if already registered to avoid duplicates
        resolver.query(
            collection,
            arrayOf(MediaStore.Downloads._ID),
            "${MediaStore.Downloads.DISPLAY_NAME} = ?",
            arrayOf(file.name),
            null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) return // already registered
        }
        resolver.insert(collection, values)
    }
}

private fun guessMimeType(fileName: String): String {
    val ext = fileName.substringAfterLast('.', "").lowercase()
    return when (ext) {
        "mp4" -> "video/mp4"
        "mkv" -> "video/x-matroska"
        "avi" -> "video/x-msvideo"
        "mov" -> "video/quicktime"
        "webm" -> "video/webm"
        "mp3" -> "audio/mpeg"
        "m4a" -> "audio/mp4"
        "aac" -> "audio/aac"
        "flac" -> "audio/flac"
        else -> "video/mp4"
    }
}

private fun String.toLocalFileOrNull(): File? {
    return runCatching {
        if (startsWith("file:")) {
            File(URI(this))
        } else {
            File(this)
        }
    }.getOrNull()
}

private fun resolveTotalBytes(
    startingBytes: Long,
    isPartialResume: Boolean,
    contentRangeHeader: String?,
    contentLength: Long?,
): Long? {
    parseContentRangeTotal(contentRangeHeader)?.let { return it }
    val normalizedLength = contentLength?.takeIf { it > 0L } ?: return null
    return if (isPartialResume && startingBytes > 0L) {
        startingBytes + normalizedLength
    } else {
        normalizedLength
    }
}

private fun parseContentRangeTotal(headerValue: String?): Long? {
    val value = headerValue?.trim().orEmpty()
    if (value.isBlank()) return null
    val slashIndex = value.lastIndexOf('/')
    if (slashIndex == -1 || slashIndex == value.lastIndex) return null
    val totalPart = value.substring(slashIndex + 1).trim()
    if (totalPart == "*") return null
    return totalPart.toLongOrNull()?.takeIf { it > 0L }
}
