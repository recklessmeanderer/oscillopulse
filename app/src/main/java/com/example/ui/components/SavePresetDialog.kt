package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SavePresetDialog(
    carrierFreq: Float,
    carrierWaveName: String,
    modFreq: Float,
    modWaveName: String,
    onDismiss: () -> Unit,
    onSave: (name: String, description: String) -> Unit
) {
    var name by remember {
        mutableStateOf("Custom ${carrierFreq.toInt()}Hz + ${modFreq.toInt()}Hz")
    }
    var description by remember {
        mutableStateOf("Carrier: ${carrierFreq.toInt()}Hz ($carrierWaveName), Mod: ${modFreq.toInt()}Hz ($modWaveName)")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Save Custom Preset",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Preset Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description / Notes") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onSave(name.trim(), description.trim()) },
                enabled = name.isNotBlank()
            ) {
                Text("Save Preset")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
