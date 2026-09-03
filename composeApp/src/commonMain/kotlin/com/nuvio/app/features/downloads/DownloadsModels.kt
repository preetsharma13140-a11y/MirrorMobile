package com.mirror.app.features.downloads

import kotlinx.serialization.Serializable
import mirror.composeapp.generated.resources.Res
import mirror.composeapp.generated.resources.downloads_enqueue_missing_url
import mirror.composeapp.generated.resources.downloads_enqueue_replaced
import mirror.composeapp.generated.resources.downloads_enqueue_started
import mirror.composeapp.generated.resources.downloads_enqueue_unsupported_format
import org.jetbrains.compose.resources.getString

@Serializable
enum class DownloadStatus {
    Downloading,
    Paused,
    Completed,
    Failed,
}

@Serializable
data class DownloadItem(
    val id: String,
    val contentType: String,
    val parentMetaId: String,
    val parentMetaType: String,
    val videoId: String,
    val title: String,
    val logo: String? = null,
    val poster: String? = null,
    val background: String? = null,
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null,
    val episodeTitle: String? = null,
    val episodeThumbnail: String? = null,
    val streamTitle: String,
    val streamSubtitle: String? = null,
    val providerName: String,
    val providerAddonId: String? = null,
    val sourceUrl: String,
    val sourceHeaders: Map<String, String> = emptyMap(),
    val sourceResponseHeaders: Map<String, String> = emptyMap(),
    val localFileUri: String? = null,
    val fileName: String,
    val status: DownloadStatus,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long? = null,
    val errorMessage: String? = null,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
) {
    val isEpisode: Boolean
        get() = seasonNumber != null && episodeNumber != null

    val isPlayable: Boolean
        get() = status == DownloadStatus.Completed && !localFileUri.isNullOrBlank()

    val displaySubtitle: String
        get() = episodeTitle.orEmpty()

    val progressFraction: Float
        get() {
            val total = totalBytes?.takeIf { it > 0L } ?: return 0f
            return (downloadedBytes.toDouble() / total.toDouble())
                .toFloat()
                .coerceIn(0f, 1f)
        }

    val logicalContentKey: String
        get() = if (isEpisode) {
            "${parentMetaId.trim()}|${seasonNumber ?: -1}|${episodeNumber ?: -1}"
        } else {
            "${parentMetaId.trim()}|movie"
        }
}

data class DownloadsUiState(
    val items: List<DownloadItem> = emptyList(),
) {
    val activeItems: List<DownloadItem>
        get() = items.filter { it.status != DownloadStatus.Completed }

    val completedItems: List<DownloadItem>
        get() = items.filter { it.status == DownloadStatus.Completed }
}

enum class DownloadEnqueueResult {
    Started,
    Replaced,
    MissingUrl,
    UnsupportedFormat;

    // Non-suspend: strings are resolved on the caller's coroutine scope via the
    // overload below. Keep this as a regular function so it can be called from
    // both coroutine and non-coroutine lambdas.
    fun toastMessage(
        startedText: String,
        replacedText: String,
        missingUrlText: String,
        unsupportedFormatText: String,
    ): String = when (this) {
        Started -> startedText
        Replaced -> replacedText
        MissingUrl -> missingUrlText
        UnsupportedFormat -> unsupportedFormatText
    }

    /** Suspend overload — resolves string resources without runBlocking. */
    suspend fun toastMessage(): String = toastMessage(
        startedText = getString(Res.string.downloads_enqueue_started),
        replacedText = getString(Res.string.downloads_enqueue_replaced),
        missingUrlText = getString(Res.string.downloads_enqueue_missing_url),
        unsupportedFormatText = getString(Res.string.downloads_enqueue_unsupported_format),
    )
}

internal fun List<DownloadItem>.sortedForSeriesDownloads(): List<DownloadItem> =
    sortedWith(downloadSeriesEpisodeComparator)

internal val downloadSeriesEpisodeComparator: Comparator<DownloadItem> =
    compareBy<DownloadItem> { it.seasonNumber ?: Int.MAX_VALUE }
        .thenBy { it.episodeNumber ?: Int.MAX_VALUE }
        .thenBy { it.episodeTitle?.trim().orEmpty().lowercase() }
        .thenBy { it.title.trim().lowercase() }
        .thenBy { it.id }
