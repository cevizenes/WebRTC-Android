
package com.example.webrtc_android.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.webrtc_android.ui.components.ChatSection
import com.example.webrtc_android.ui.viewmodel.MainViewModel
import com.example.webrtc_android.utils.ChatItems


@Composable
fun ChatScreen() {
    val viewModel: MainViewModel = hiltViewModel()

    val matchState by viewModel.matchState.collectAsState()
    val chatState by viewModel.chatList.collectAsState()
    val chatText = remember { mutableStateOf("") }
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        if (perms.values.all { it }) {
            viewModel.permissionsGranted()
        } else {
            Toast.makeText(context, "Camera & Microphone permissions are required", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(
            arrayOf(android.Manifest.permission.RECORD_AUDIO, android.Manifest.permission.CAMERA)
        )
    }


    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFFEAEAEA))
            .padding(8.dp)
    ) {

        Box(Modifier.weight(1f)) {
            ChatSection(chatItems = chatState)
        }

        Spacer(Modifier.height(4.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
        ) {
            OutlinedTextField(
                value = chatText.value,
                onValueChange = { chatText.value = it },
                label = { Text("Type your message") },
                modifier = Modifier.weight(7f)
            )
            Spacer(Modifier.width(4.dp))
            IconButton(onClick = {
                if (chatText.value.isNotBlank()) {
                    viewModel.sendChatItem(ChatItems(chatText.value, isMine = true))
                    chatText.value = ""
                }
            }) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color(0xFFA4BAD1))
            }
        }

    }
}