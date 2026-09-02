package com.cncverse.stremiobridge.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cncverse.stremiobridge.repo.loadExtensionSettings
import com.cncverse.stremiobridge.repo.saveExtensionSettings

@Composable
fun SettingsScreen() {
    var settings by remember { mutableStateOf(loadExtensionSettings()) }
    var febboxToken by remember { mutableStateOf(settings["token"] ?: "") }
    var showboxToken by remember { mutableStateOf(settings["showbox_ui_token"] ?: "") }
    var wyzieKey by remember { mutableStateOf(settings["wyzie_subs_api_key"] ?: "") }
    var concurrency by remember { mutableStateOf(settings["ScrapeConcurrency"] ?: "10") }
    var savedMessage by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AmoledBlack)
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Text("Settings", color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("Configure global scraper tokens and concurrency", color = TextMuted, fontSize = 13.sp)

        Spacer(Modifier.height(20.dp))

        if (savedMessage) {
            Surface(
                color = VioletGlow,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("✓ Settings saved successfully", color = Violet200, fontSize = 13.sp, modifier = Modifier.padding(12.dp))
            }
            Spacer(Modifier.height(16.dp))
        }

        AmoledCard(modifier = Modifier.fillMaxWidth()) {
            Text("FebBox Token", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(
                value = febboxToken,
                onValueChange = { febboxToken = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Paste FebBox token", color = TextMuted) },
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Violet400, unfocusedBorderColor = CardBorder),
            )

            Spacer(Modifier.height(16.dp))

            Text("ShowBox Token", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(
                value = showboxToken,
                onValueChange = { showboxToken = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Paste ShowBox token", color = TextMuted) },
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Violet400, unfocusedBorderColor = CardBorder),
            )

            Spacer(Modifier.height(16.dp))

            Text("Wyzie Subtitles API Key", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(
                value = wyzieKey,
                onValueChange = { wyzieKey = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Paste Wyzie Subtitles API Key", color = TextMuted) },
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Violet400, unfocusedBorderColor = CardBorder),
            )

            Spacer(Modifier.height(16.dp))

            Text("Scrape Concurrency", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(
                value = concurrency,
                onValueChange = { concurrency = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("10", color = TextMuted) },
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Violet400, unfocusedBorderColor = CardBorder),
            )

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = {
                    val updated = settings.toMutableMap()
                    if (febboxToken.isNotBlank()) updated["token"] = febboxToken.trim() else updated.remove("token")
                    if (showboxToken.isNotBlank()) updated["showbox_ui_token"] = showboxToken.trim() else updated.remove("showbox_ui_token")
                    if (wyzieKey.isNotBlank()) updated["wyzie_subs_api_key"] = wyzieKey.trim() else updated.remove("wyzie_subs_api_key")
                    if (concurrency.isNotBlank()) updated["ScrapeConcurrency"] = concurrency.trim() else updated.remove("ScrapeConcurrency")
                    saveExtensionSettings(updated)
                    settings = updated
                    savedMessage = true
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Violet500),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("Save Settings", fontWeight = FontWeight.Bold)
            }
        }
    }
}
