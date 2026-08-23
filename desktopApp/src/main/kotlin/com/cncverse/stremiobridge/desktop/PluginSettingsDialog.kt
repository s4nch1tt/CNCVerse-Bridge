package com.cncverse.stremiobridge.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.cncverse.stremiobridge.settings.PluginSettingSchema
import com.cncverse.stremiobridge.settings.PluginSettingsSchemaRegistry
import com.cncverse.stremiobridge.ui.AmoledCard
import com.cncverse.stremiobridge.ui.AmoledCard2
import com.cncverse.stremiobridge.ui.AmoledSurface
import com.cncverse.stremiobridge.ui.CardBorder
import com.cncverse.stremiobridge.ui.DividerColor
import com.cncverse.stremiobridge.ui.TextMuted
import com.cncverse.stremiobridge.ui.TextPrimary
import com.cncverse.stremiobridge.ui.TextSecondary
import com.cncverse.stremiobridge.ui.Violet200
import com.cncverse.stremiobridge.ui.Violet400
import com.cncverse.stremiobridge.ui.Violet500
import com.cncverse.stremiobridge.ui.VioletGlow
import com.lagradost.cloudstream3.CloudStreamApp

/**
 * Desktop plugin settings dialog (the gear button). Plugins declare settings in
 * code and read them through CloudStreamApp/DataStore; every key they touch is
 * registered in [PluginSettingsSchemaRegistry], which this dialog renders as a
 * categorized UI (mirrors the cs3-desktop-client gear dialog): toggles for
 * booleans/provider switches, checkbox grids for provider lists, text/number
 * fields for the rest. Writes go back through CloudStreamApp.setKey using the
 * schema's storageKey so the plugin reads them.
 */
@Composable
fun PluginSettingsDialog(
    pluginInternalName: String,
    pluginDisplayName: String,
    onDismiss: () -> Unit,
) {
    val schemaUpdates by PluginSettingsSchemaRegistry.schemaUpdates.collectAsState()

    val settings = remember(pluginInternalName, pluginDisplayName, schemaUpdates) {
        PluginSettingsSchemaRegistry.getSettingsForPlugin(pluginInternalName, pluginDisplayName)
            .sortedWith(
                compareBy<PluginSettingSchema> { getCategoryPriority(it.key) }
                    .thenBy { getFriendlyName(it.key) },
            )
    }

    // Current raw values from the shared settings store (everything is stringified)
    val currentValues = remember(pluginInternalName, schemaUpdates) {
        mutableStateMapOf<String, String?>().also { map ->
            settings.forEach { schema ->
                map[schema.storageKey] = CloudStreamApp.getKey<String>(schema.storageKey)
            }
        }
    }

    var hasChanged by remember { mutableStateOf(false) }

    fun writeValue(schema: PluginSettingSchema, raw: String?) {
        currentValues[schema.storageKey] = raw
        hasChanged = true
        CloudStreamApp.setKey(schema.storageKey, raw)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = AmoledCard,
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
            modifier = Modifier.width(550.dp).fillMaxHeight(0.85f),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AmoledSurface)
                        .padding(horizontal = 24.dp, vertical = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Filled.Settings,
                        contentDescription = null,
                        tint = Violet400,
                        modifier = Modifier.size(26.dp),
                    )
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "$pluginDisplayName Settings",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                        )
                        Text(
                            if (settings.isEmpty()) "No settings discovered yet"
                            else "Configure sub-providers, accounts, and scraper channels",
                            color = TextMuted,
                            fontSize = 12.sp,
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.clip(RoundedCornerShape(50)).background(AmoledCard2),
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                HorizontalDivider(color = DividerColor)

                // Content
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
                        contentPadding = PaddingValues(vertical = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        if (hasChanged) {
                            item {
                                Surface(
                                    color = VioletGlow,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(
                                        "✓ Changes saved. Close this settings box to apply the new " +
                                            "provider configuration in real-time.",
                                        color = Violet200,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(14.dp),
                                    )
                                }
                            }
                        }

                        if (settings.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        "This plugin has not exposed any configurable options yet. " +
                                            "Settings appear here automatically once the plugin reads them.",
                                        color = TextSecondary,
                                        fontSize = 13.sp,
                                    )
                                }
                            }
                        } else {
                            val grouped = settings.groupBy { getCategory(it.key) }
                            val sortedCategories = grouped.keys.sortedBy { category ->
                                categoryPriorities[category] ?: 4
                            }

                            sortedCategories.forEach { category ->
                                item {
                                    Column(modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)) {
                                        Text(
                                            category.uppercase(),
                                            color = Violet400,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp,
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        HorizontalDivider(
                                            color = Violet500.copy(alpha = 0.15f),
                                            thickness = 2.dp,
                                        )
                                    }
                                }

                                items(grouped[category]!!, key = { it.key }) { schema ->
                                    SettingCard(
                                        schema = schema,
                                        rawValue = currentValues[schema.storageKey],
                                        onChanged = { raw -> writeValue(schema, raw) },
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = DividerColor)

                // Footer
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AmoledSurface)
                        .padding(horizontal = 24.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Violet500,
                            contentColor = TextPrimary,
                        ),
                    ) {
                        Text("Apply & Close", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingCard(
    schema: PluginSettingSchema,
    rawValue: String?,
    onChanged: (String?) -> Unit,
) {
    val friendly = getFriendlyName(schema.key)
    val desc = getDescription(schema.key)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AmoledCard2)
            .border(1.dp, CardBorder.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(16.dp),
    ) {
        when {
            schema.options != null -> ListSetting(schema, friendly, desc, rawValue, onChanged)
            schema.type == "StringSet" -> StringSetSetting(schema, friendly, desc, rawValue, onChanged)
            isBooleanLike(schema, rawValue) -> BooleanSetting(friendly, desc, schema, rawValue, onChanged)
            else -> TextSetting(schema, friendly, desc, rawValue, onChanged)
        }
    }
}

@Composable
private fun ListSetting(
    schema: PluginSettingSchema,
    friendly: String,
    desc: String,
    rawValue: String?,
    onChanged: (String?) -> Unit,
) {
    val options = schema.options ?: emptyMap()
    val currentValue = rawValue ?: schema.defaultValue?.toString() ?: options.values.firstOrNull() ?: ""

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(friendly, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        if (desc.isNotEmpty()) {
            Text(desc, color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
        }
        Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
            options.forEach { (label, value) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = (value == currentValue),
                        onClick = { onChanged(value) },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = Violet500,
                            unselectedColor = TextSecondary
                        )
                    )
                    Text(
                        text = label,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun BooleanSetting(
    friendly: String,
    desc: String,
    schema: PluginSettingSchema,
    rawValue: String?,
    onChanged: (String?) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Text(friendly, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            if (desc.isNotEmpty()) {
                Text(desc, color = TextSecondary, fontSize = 12.sp)
            }
        }
        Switch(
            checked = rawValue == "true" ||
                (rawValue == null && (schema.defaultValue == true || schema.defaultValue == "true")),
            onCheckedChange = { checked -> onChanged(if (checked) "true" else "false") },
        )
    }
}

@Composable
private fun TextSetting(
    schema: PluginSettingSchema,
    friendly: String,
    desc: String,
    rawValue: String?,
    onChanged: (String?) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(friendly, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        if (desc.isNotEmpty()) {
            Text(
                desc,
                color = TextSecondary,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 10.dp),
            )
        }

        val isNumeric = schema.type in setOf("Int", "Long", "Float")
        OutlinedTextField(
            value = rawValue ?: schema.defaultValue?.toString() ?: "",
            onValueChange = { newValue ->
                val valid = !isNumeric || newValue.isEmpty() ||
                    (if (schema.type == "Float") newValue.toFloatOrNull() != null else newValue.toLongOrNull() != null)
                if (valid) onChanged(newValue.ifEmpty { null })
            },
            singleLine = true,
            textStyle = TextStyle(color = TextPrimary, fontSize = 14.sp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Violet400,
                unfocusedBorderColor = CardBorder,
                cursorColor = Violet400,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * Provider/source lists render as a checkbox grid. The plugin's default set
 * (usually the full provider list) provides the options; "disabled" keys store
 * the OFF providers, everything else stores the ON ones. Stored newline-joined
 * to round-trip with the SharedPreferences stub's getStringSet.
 */
@Composable
private fun StringSetSetting(
    schema: PluginSettingSchema,
    friendly: String,
    desc: String,
    rawValue: String?,
    onChanged: (String?) -> Unit,
) {
    val isDisabledKey = schema.key.lowercase().contains("disabled")

    val currentSet = parseStoredSet(rawValue)
    val options = ((schema.defaultValue as? Set<*>)?.map { it.toString() }?.toSet() ?: emptySet())
        .plus(currentSet)
        .sorted()

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(friendly, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        if (desc.isNotEmpty()) {
            Text(
                desc,
                color = TextSecondary,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 12.dp),
            )
        }

        fun writeSet(next: Set<String>) {
            onChanged(next.joinToString("\n").ifEmpty { null })
        }

        if (options.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = {
                        writeSet(if (isDisabledKey) emptySet() else options.toSet())
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(if (isDisabledKey) "Enable All" else "Select All", fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick = {
                        writeSet(if (isDisabledKey) options.toSet() else emptySet())
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(if (isDisabledKey) "Disable All" else "Deselect All", fontSize = 12.sp)
                }
            }

            Spacer(Modifier.height(8.dp))

            options.chunked(2).forEach { rowSources ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    rowSources.forEach { source ->
                        val isChecked = if (isDisabledKey) !currentSet.contains(source) else currentSet.contains(source)
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    val next = if (isDisabledKey) {
                                        if (checked) currentSet - source else currentSet + source
                                    } else {
                                        if (checked) currentSet + source else currentSet - source
                                    }
                                    writeSet(next)
                                },
                            )
                            Text(
                                source.replace("API", "").replace("Api", ""),
                                color = TextPrimary,
                                fontSize = 13.sp,
                            )
                        }
                    }
                    if (rowSources.size < 2) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        } else {
            // No known provider list yet — fall back to comma-separated editing
            OutlinedTextField(
                value = currentSet.joinToString(", "),
                onValueChange = { newValue ->
                    onChanged(
                        newValue.split(",")
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }
                            .joinToString("\n")
                            .ifEmpty { null },
                    )
                },
                singleLine = true,
                textStyle = TextStyle(color = TextPrimary, fontSize = 14.sp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Violet400,
                    unfocusedBorderColor = CardBorder,
                    cursorColor = Violet400,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** Parses a stored set value: newline-joined (native) or legacy "[a, b]" toStrings. */
private fun parseStoredSet(raw: String?): Set<String> {
    if (raw.isNullOrBlank()) return emptySet()
    val trimmed = raw.trim()
    val items = if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
        trimmed.removeSurrounding("[", "]").split(",")
    } else {
        trimmed.split("\n")
    }
    return items.map { it.trim() }.filter { it.isNotEmpty() }.toSet()
}

private fun isBooleanLike(schema: PluginSettingSchema, rawValue: String?): Boolean {
    if (schema.type == "Boolean" || schema.defaultValue is Boolean) return true
    if (rawValue == "true" || rawValue == "false") return true
    // Plugins toggle individual providers through keys named ProviderXxx / xxxEnable
    if (schema.key.startsWith("Provider") || schema.key.endsWith("Enable")) return true
    return false
}

// ── Logical categorization and visual polish (mirrors cs3-desktop-client) ────

private val categoryPriorities = mapOf(
    "General Configurations" to 0,
    "Accounts & API Integrations" to 1,
    "Stremio & External Catalogs" to 2,
    "Scrapers & Engines" to 3,
)

private fun getCategory(key: String): String {
    val lower = key.lowercase()
    return when {
        lower.contains("stremio") || lower.contains("addon") || lower.contains("catalog") -> "Stremio & External Catalogs"
        lower.contains("key") || lower.contains("token") || lower.contains("auth") || lower.contains("api") ||
            lower.contains("password") || lower.contains("username") -> "Accounts & API Integrations"
        lower.contains("provider") || lower.contains("source") || lower.contains("channel") ||
            lower.contains("extractor") || lower.endsWith("enable") || lower.contains("concurrency") ||
            lower.startsWith("scrape") -> "Scrapers & Engines"
        else -> "General Configurations"
    }
}

private fun getCategoryPriority(key: String): Int = categoryPriorities[getCategory(key)] ?: 4


/** Explicit friendly name overrides for known plugin settings. */
private val friendlyNameOverrides = mapOf(
    "ProviderCineStream" to "CineStream Catalog",
    "ProviderSimkl" to "Simkl Catalog",
    "ProviderTmdb" to "TMDB Catalog",
    "stremio_addons" to "Stremio Addon URLs",
    "token" to "FebBox Token",
    "provider_concurrency" to "Scrape Concurrency",
    "enabled_plugins_saved" to "Enabled Providers",
    "moviebox_host" to "MovieBox Server",
    "ScrapeConcurrency" to "Scrape Concurrency",
    "DownloadEnable" to "Download Only Links",
    "showbox_ui_token" to "ShowBox Token",
    "wyzie_subs_api_key" to "Wyzie Subtitles API Key",
    "gramcinema_bearer_token" to "GramCinema Token",
    "new_provider_default_on" to "Auto-Enable New Providers",
    "cloudflare_webview_bypass_enabled" to "Cloudflare Bypass",
)

/** Explicit description overrides for known plugin settings. */
private val descriptionOverrides = mapOf(
    "ProviderCineStream" to "Enable the Cinemeta-backed catalog for browsing movies & shows.",
    "ProviderSimkl" to "Enable the Simkl-backed catalog for anime & watchlist integration.",
    "ProviderTmdb" to "Enable the TMDB-backed catalog for trending & popular content.",
    "stremio_addons" to "Comma-separated Stremio addon manifest URLs for external catalogs.",
    "token" to "Paste your FebBox authentication token for premium source access.",
    "provider_concurrency" to "Max parallel scraping threads (-1 = unlimited).",
    "enabled_plugins_saved" to "Select which scraping engines are active for this plugin.",
    "moviebox_host" to "Select which MovieBox API server to use.",
    "ScrapeConcurrency" to "Max parallel scraping threads (default: 10).",
    "DownloadEnable" to "Only fetch direct download links (not for streaming).",
    "showbox_ui_token" to "Paste your ShowBox/FebBox UI token.",
    "wyzie_subs_api_key" to "API key for Wyzie subtitle service.",
    "gramcinema_bearer_token" to "Bearer token for GramCinema source.",
    "new_provider_default_on" to "Automatically enable newly added providers on update.",
    "cloudflare_webview_bypass_enabled" to "Use WebView to bypass Cloudflare protection on supported sites.",
)

private fun getFriendlyName(key: String): String {
    friendlyNameOverrides[key]?.let { return it }

    var clean = key.substringAfterLast('/')
    if (clean.startsWith("Provider")) {
        clean = clean.removePrefix("Provider")
    }

    val friendly = clean.replace("_", " ")
        .replace(Regex("([a-z])([A-Z])"), "$1 $2")
        .trim()
        .split(" ")
        .joinToString(" ") { it.replaceFirstChar { c -> c.uppercaseChar() } }

    return friendly
        .replace(" Saved Links", " Links Cache")
        .replace(" Concurrency", " Simultaneous Connections")
}

private fun getDescription(key: String): String {
    descriptionOverrides[key]?.let { return it }
    val friendly = getFriendlyName(key)
    return when {
        key.startsWith("Provider") -> "Enable or disable the $friendly search scraper channel."
        key.lowercase().contains("concurrency") -> "Set maximum simultaneous connection threads to speed up retrieval."
        key.lowercase().contains("token") || key.lowercase().contains("key") -> "Configure authentication credentials/API key for $friendly."
        key.lowercase().contains("stremio") -> "Configure external streaming catalog source links."
        key.lowercase().contains("disabled") -> "Toggle individual sub-scrapers and data sources for this plugin."
        key.lowercase().contains("enabled") -> "Select which sub-engines are active."
        else -> ""
    }
}

