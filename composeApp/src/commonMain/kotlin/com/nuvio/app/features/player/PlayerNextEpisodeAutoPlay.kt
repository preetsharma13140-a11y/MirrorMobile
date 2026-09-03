package com.mirror.app.features.player

import com.mirror.app.features.addons.AddonRepository
import com.mirror.app.features.addons.enabledAddons
import com.mirror.app.features.debrid.DebridSettingsRepository
import com.mirror.app.features.details.MetaVideo
import com.mirror.app.features.downloads.DownloadItem
import com.mirror.app.features.downloads.DownloadsRepository
import com.mirror.app.features.player.skip.NextEpisodeInfo
import com.mirror.app.features.streams.StreamAutoPlayMode
import com.mirror.app.features.streams.StreamAutoPlaySelector
import com.mirror.app.features.streams.StreamAutoPlaySource
import com.mirror.app.features.streams.StreamItem
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

internal fun CoroutineScope.launchPlayerNextEpisodeAutoPlay(
    previousJob: Job?,
    nextEpisodeInfo: NextEpisodeInfo?,
    allEpisodes: List<MetaVideo>,
    parentMetaId: String,
    parentMetaType: String,
    contentType: String?,
    settings: PlayerSettingsUiState,
    currentStreamBingeGroup: String?,
    currentAddonName: String? = null,
    currentStreamLabel: String? = null,
    onDownloadedEpisodeSelected: (DownloadItem, MetaVideo) -> Unit,
    onEpisodeStreamSelected: (StreamItem, MetaVideo) -> Unit,
    onManualSelectionRequired: (MetaVideo) -> Unit,
    onSearchingChanged: (Boolean) -> Unit,
    onSourceNameChanged: (String?) -> Unit,
    onCountdownChanged: (Int?) -> Unit,
    onNextEpisodeCardVisibleChanged: (Boolean) -> Unit,
): Job? {
    val nextVideoId = nextEpisodeInfo?.videoId ?: return null
    val nextVideo = allEpisodes.firstOrNull { video -> video.id == nextVideoId } ?: return null
    if (nextEpisodeInfo.hasAired != true) return null

    val downloadedNextEpisode = DownloadsRepository.findPlayableDownload(
        parentMetaId = parentMetaId,
        seasonNumber = nextVideo.season,
        episodeNumber = nextVideo.episode,
        videoId = nextVideo.id,
    )
    if (downloadedNextEpisode != null) {
        onDownloadedEpisodeSelected(downloadedNextEpisode, nextVideo)
        return null
    }

    previousJob?.cancel()
    onSearchingChanged(true)
    onSourceNameChanged(null)
    onCountdownChanged(null)

    val type = contentType ?: parentMetaType
    val shouldAutoSelectInManualMode =
        settings.streamAutoPlayMode == StreamAutoPlayMode.MANUAL &&
            (
                settings.streamAutoPlayNextEpisodeEnabled ||
                    settings.streamAutoPlayPreferBingeGroup
                )

    val bingeGroupOnlyManualMode =
        shouldAutoSelectInManualMode &&
            !settings.streamAutoPlayNextEpisodeEnabled &&
            settings.streamAutoPlayPreferBingeGroup

    val effectiveMode = if (shouldAutoSelectInManualMode) {
        StreamAutoPlayMode.FIRST_STREAM
    } else {
        settings.streamAutoPlayMode
    }
    val effectiveSource = if (shouldAutoSelectInManualMode) {
        StreamAutoPlaySource.ALL_SOURCES
    } else {
        settings.streamAutoPlaySource
    }
    val effectiveSelectedAddons = if (shouldAutoSelectInManualMode) {
        emptySet()
    } else {
        settings.streamAutoPlaySelectedAddons
    }
    val effectiveSelectedPlugins = if (shouldAutoSelectInManualMode) {
        emptySet()
    } else {
        settings.streamAutoPlaySelectedPlugins
    }
    val effectiveRegex = if (shouldAutoSelectInManualMode) {
        ""
    } else {
        settings.streamAutoPlayRegex
    }
    val preferredBingeGroup = if (settings.streamAutoPlayPreferBingeGroup) {
        currentStreamBingeGroup
    } else {
        null
    }

    return launch {
        PlayerStreamsRepository.loadEpisodeStreams(
            type = type,
            videoId = nextVideo.id,
            season = nextVideo.season,
            episode = nextVideo.episode,
        )

        val installedAddonNames = AddonRepository.uiState.value.addons
            .enabledAddons()
            .map { it.displayTitle }
            .toSet()
        val debridSettings = DebridSettingsRepository.snapshot()

        val timeoutSeconds = settings.streamAutoPlayTimeoutSeconds
        var autoSelectTriggered = false
        var timeoutElapsed = false
        var selectedStream: StreamItem? = null
        val autoSelectSettled = CompletableDeferred<Unit>()

        fun settleAutoSelect() {
            if (!autoSelectSettled.isCompleted) {
                autoSelectSettled.complete(Unit)
            }
        }

        fun selectStream(stream: StreamItem) {
            autoSelectTriggered = true
            selectedStream = stream
            settleAutoSelect()
        }

        fun finishWithoutSelection() {
            autoSelectTriggered = true
            settleAutoSelect()
        }

        fun tryFallbackByAddon(streams: List<StreamItem>): StreamItem? {
            val addonName = currentAddonName?.trim()?.takeIf { it.isNotEmpty() } ?: return null
            val addonStreams = streams.filter { it.addonName == addonName }
            if (addonStreams.isEmpty()) return null

            val prevLabel = currentStreamLabel?.trim()?.takeIf { it.isNotEmpty() }
            if (prevLabel != null) {
                val normalizedPreviousLabel = prevLabel.replace(
                    Regex("web[-\\s]?dl", RegexOption.IGNORE_CASE),
                    "webdl",
                )
                val qualityTokens = normalizedPreviousLabel
                    .split(Regex("[\\s|/\\-_]+"))
                    .filter { token ->
                        token.isNotBlank() &&
                        token.length >= 2 &&
                        (token.contains(Regex("\\d{3,4}p", RegexOption.IGNORE_CASE)) ||
                         token.equals("4k", ignoreCase = true) ||
                         token.equals("hdr", ignoreCase = true) ||
                         token.equals("dolby", ignoreCase = true) ||
                         token.equals("hevc", ignoreCase = true) ||
                         token.equals("h265", ignoreCase = true) ||
                         token.equals("h264", ignoreCase = true) ||
                         token.equals("avc", ignoreCase = true) ||
                         token.equals("bluray", ignoreCase = true) ||
                         token.equals("webdl", ignoreCase = true) ||
                         token.equals("webrip", ignoreCase = true))
                    }
                if (qualityTokens.isNotEmpty()) {
                    val labelMatch = addonStreams.firstOrNull { stream ->
                        val label = stream.streamLabel.replace(
                            Regex("web[-\\s]?dl", RegexOption.IGNORE_CASE),
                            "webdl",
                        )
                        qualityTokens.all { token -> label.contains(token, ignoreCase = true) }
                    }
                    if (labelMatch != null) return labelMatch
                }
            }
            return addonStreams.first()
        }

        fun trySelectStream(streams: List<StreamItem>): StreamItem? {
            val autoSelected = StreamAutoPlaySelector.selectAutoPlayStream(
                streams = streams,
                mode = effectiveMode,
                regexPattern = effectiveRegex,
                source = effectiveSource,
                installedAddonNames = installedAddonNames,
                selectedAddons = effectiveSelectedAddons,
                selectedPlugins = effectiveSelectedPlugins,
                preferredBingeGroup = preferredBingeGroup,
                preferBingeGroupInSelection = settings.streamAutoPlayPreferBingeGroup,
                bingeGroupOnly = bingeGroupOnlyManualMode,
                debridEnabled = debridSettings.canResolvePlayableLinks,
                activeResolverProviderId = debridSettings.activeResolverProviderId,
            )
            if (autoSelected != null &&
                preferredBingeGroup != null &&
                autoSelected.behaviorHints.bingeGroup != preferredBingeGroup
            ) {
                val addonFallback = tryFallbackByAddon(streams)
                if (addonFallback != null) return addonFallback
            }
            return autoSelected ?: tryFallbackByAddon(streams)
        }

        fun tryBingeGroupOnly(streams: List<StreamItem>): StreamItem? {
            if (preferredBingeGroup == null || !settings.streamAutoPlayPreferBingeGroup) return null
            return StreamAutoPlaySelector.selectAutoPlayStream(
                streams = streams,
                mode = effectiveMode,
                regexPattern = effectiveRegex,
                source = effectiveSource,
                installedAddonNames = installedAddonNames,
                selectedAddons = effectiveSelectedAddons,
                selectedPlugins = effectiveSelectedPlugins,
                preferredBingeGroup = preferredBingeGroup,
                preferBingeGroupInSelection = true,
                bingeGroupOnly = true,
                debridEnabled = debridSettings.canResolvePlayableLinks,
                activeResolverProviderId = debridSettings.activeResolverProviderId,
            )
        }

        val innerJob = launch {
            PlayerStreamsRepository.episodeStreamsState.collectLatest { state ->
                if (state.groups.isEmpty() && state.isAnyLoading) return@collectLatest

                val allStreams = state.groups.flatMap { it.streams }

                if (autoSelectTriggered) {
                    // Already resolved.
                } else if (timeoutElapsed) {
                    if (allStreams.isNotEmpty()) {
                        val candidate = trySelectStream(allStreams)
                        if (candidate != null) {
                            selectStream(candidate)
                        }
                    }
                } else if (allStreams.isNotEmpty()) {
                    val earlyMatch = tryBingeGroupOnly(allStreams)
                    if (earlyMatch != null) {
                        selectStream(earlyMatch)
                    }
                }

                if (!autoSelectTriggered && !state.isAnyLoading) {
                    if (allStreams.isNotEmpty()) {
                        val candidate = trySelectStream(allStreams)
                        if (candidate != null) {
                            selectStream(candidate)
                        }
                    }
                    if (!autoSelectTriggered) {
                        finishWithoutSelection()
                    }
                    return@collectLatest
                }

                if (autoSelectTriggered) return@collectLatest
            }
        }

        val timeoutMs = timeoutSeconds * 1_000L
        val isBoundedTimeout = timeoutSeconds in 1..30

        if (isBoundedTimeout) {
            delay(timeoutMs)
            timeoutElapsed = true
            if (!autoSelectTriggered) {
                val allStreams = PlayerStreamsRepository.episodeStreamsState.value.groups.flatMap { it.streams }
                if (allStreams.isNotEmpty()) {
                    val candidate = trySelectStream(allStreams)
                    if (candidate != null) {
                        selectStream(candidate)
                    }
                }
            }
            if (selectedStream != null) {
                innerJob.cancel()
            } else if (PlayerStreamsRepository.episodeStreamsState.value.groups.flatMap { it.streams }.isNotEmpty()) {
                innerJob.cancel()
                finishWithoutSelection()
            } else {
                val completed = withTimeoutOrNull(timeoutMs) { autoSelectSettled.await() }
                innerJob.cancel()
                if (completed == null && !autoSelectTriggered) {
                    val allStreams = PlayerStreamsRepository.episodeStreamsState.value.groups.flatMap { it.streams }
                    if (allStreams.isNotEmpty()) {
                        selectedStream = trySelectStream(allStreams)
                    }
                    finishWithoutSelection()
                }
            }
        } else {
            timeoutElapsed = true
            if (!autoSelectTriggered) {
                val allStreams = PlayerStreamsRepository.episodeStreamsState.value.groups.flatMap { it.streams }
                if (allStreams.isNotEmpty()) {
                    trySelectStream(allStreams)?.let(::selectStream)
                }
            }
            val completed = withTimeoutOrNull(NEXT_EPISODE_HARD_TIMEOUT_MS) { autoSelectSettled.await() }
            innerJob.cancel()
            if (completed == null && !autoSelectTriggered) {
                val allStreams = PlayerStreamsRepository.episodeStreamsState.value.groups.flatMap { it.streams }
                if (allStreams.isNotEmpty()) {
                    selectedStream = trySelectStream(allStreams)
                }
                finishWithoutSelection()
            }
        }

        onSearchingChanged(false)
        val selected = selectedStream
        if (selected != null) {
            onSourceNameChanged(selected.addonName)
            for (i in 3 downTo 1) {
                onCountdownChanged(i)
                delay(1000)
            }
            onEpisodeStreamSelected(selected, nextVideo)
            onNextEpisodeCardVisibleChanged(false)
            onCountdownChanged(null)
            onSourceNameChanged(null)
        } else {
            onManualSelectionRequired(nextVideo)
            onNextEpisodeCardVisibleChanged(false)
        }
    }
}
