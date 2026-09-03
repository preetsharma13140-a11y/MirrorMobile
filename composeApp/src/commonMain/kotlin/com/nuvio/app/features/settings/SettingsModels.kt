package com.mirror.app.features.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.ui.graphics.vector.ImageVector
import mirror.composeapp.generated.resources.Res
import mirror.composeapp.generated.resources.compose_settings_category_about
import mirror.composeapp.generated.resources.compose_settings_category_general
import mirror.composeapp.generated.resources.compose_settings_page_account
import mirror.composeapp.generated.resources.compose_settings_page_addons
import mirror.composeapp.generated.resources.compose_settings_page_advanced
import mirror.composeapp.generated.resources.compose_settings_page_appearance
import mirror.composeapp.generated.resources.compose_settings_page_content_discovery
import mirror.composeapp.generated.resources.compose_settings_page_debrid
import mirror.composeapp.generated.resources.compose_settings_page_continue_watching
import mirror.composeapp.generated.resources.compose_settings_page_homescreen
import mirror.composeapp.generated.resources.compose_settings_page_integrations
import mirror.composeapp.generated.resources.compose_settings_page_licenses_attributions
import mirror.composeapp.generated.resources.compose_settings_page_mdblist_ratings
import mirror.composeapp.generated.resources.compose_settings_page_meta_screen
import mirror.composeapp.generated.resources.compose_settings_page_notifications
import mirror.composeapp.generated.resources.compose_settings_page_playback
import mirror.composeapp.generated.resources.compose_settings_page_plugins
import mirror.composeapp.generated.resources.compose_settings_page_poster_customization
import mirror.composeapp.generated.resources.compose_settings_page_root
import mirror.composeapp.generated.resources.compose_settings_page_streams
import mirror.composeapp.generated.resources.compose_settings_page_supporters_contributors
import mirror.composeapp.generated.resources.compose_settings_page_tmdb_enrichment
import mirror.composeapp.generated.resources.compose_settings_page_trakt
import mirror.composeapp.generated.resources.settings_account
import org.jetbrains.compose.resources.StringResource

internal enum class SettingsCategory(
    val labelRes: StringResource,
    val icon: ImageVector,
) {
    Account(Res.string.settings_account, Icons.Rounded.AccountCircle),
    General(Res.string.compose_settings_category_general, Icons.Rounded.Settings),
    About(Res.string.compose_settings_category_about, Icons.Rounded.Info),
    Advanced(Res.string.compose_settings_page_advanced, Icons.Rounded.Tune),
}

internal enum class SettingsPage(
    val titleRes: StringResource,
    val category: SettingsCategory,
    val parentPage: SettingsPage?,
) {
    Root(
        titleRes = Res.string.compose_settings_page_root,
        category = SettingsCategory.General,
        parentPage = null,
    ),
    Account(
        titleRes = Res.string.compose_settings_page_account,
        category = SettingsCategory.Account,
        parentPage = Root,
    ),
    SupportersContributors(
        titleRes = Res.string.compose_settings_page_supporters_contributors,
        category = SettingsCategory.About,
        parentPage = Root,
    ),
    LicensesAttributions(
        titleRes = Res.string.compose_settings_page_licenses_attributions,
        category = SettingsCategory.About,
        parentPage = Root,
    ),
    Playback(
        titleRes = Res.string.compose_settings_page_playback,
        category = SettingsCategory.General,
        parentPage = Root,
    ),
    Appearance(
        titleRes = Res.string.compose_settings_page_appearance,
        category = SettingsCategory.General,
        parentPage = Root,
    ),
    Streams(
        titleRes = Res.string.compose_settings_page_streams,
        category = SettingsCategory.General,
        parentPage = Appearance,
    ),
    Advanced(
        titleRes = Res.string.compose_settings_page_advanced,
        category = SettingsCategory.Advanced,
        parentPage = Root,
    ),
    Notifications(
        titleRes = Res.string.compose_settings_page_notifications,
        category = SettingsCategory.General,
        parentPage = Root,
    ),
    ContinueWatching(
        titleRes = Res.string.compose_settings_page_continue_watching,
        category = SettingsCategory.General,
        parentPage = Appearance,
    ),
    PosterCustomization(
        titleRes = Res.string.compose_settings_page_poster_customization,
        category = SettingsCategory.General,
        parentPage = Appearance,
    ),
    ContentDiscovery(
        titleRes = Res.string.compose_settings_page_content_discovery,
        category = SettingsCategory.General,
        parentPage = Root,
    ),
    Addons(
        titleRes = Res.string.compose_settings_page_addons,
        category = SettingsCategory.General,
        parentPage = ContentDiscovery,
    ),
    Plugins(
        titleRes = Res.string.compose_settings_page_plugins,
        category = SettingsCategory.General,
        parentPage = ContentDiscovery,
    ),
    Homescreen(
        titleRes = Res.string.compose_settings_page_homescreen,
        category = SettingsCategory.General,
        parentPage = Appearance,
    ),
    MetaScreen(
        titleRes = Res.string.compose_settings_page_meta_screen,
        category = SettingsCategory.General,
        parentPage = Appearance,
    ),
    Integrations(
        titleRes = Res.string.compose_settings_page_integrations,
        category = SettingsCategory.General,
        parentPage = Root,
    ),
    TmdbEnrichment(
        titleRes = Res.string.compose_settings_page_tmdb_enrichment,
        category = SettingsCategory.General,
        parentPage = Integrations,
    ),
    MdbListRatings(
        titleRes = Res.string.compose_settings_page_mdblist_ratings,
        category = SettingsCategory.General,
        parentPage = Integrations,
    ),
    Debrid(
        titleRes = Res.string.compose_settings_page_debrid,
        category = SettingsCategory.General,
        parentPage = Integrations,
    ),
    TraktAuthentication(
        titleRes = Res.string.compose_settings_page_trakt,
        category = SettingsCategory.Account,
        parentPage = Root,
    ),
}

internal val SettingsPage.opensInlineOnTablet: Boolean
    get() = parentPage != null

internal fun SettingsPage.previousPage(): SettingsPage? = parentPage
