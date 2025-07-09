package com.example.webrtc_android.ui.screens

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.webrtc_android.ui.components.SurfaceViewRendererComposable
import com.example.webrtc_android.ui.viewmodel.MainViewModel
import com.example.webrtc_android.utils.MatchState

@Composable
fun CameraScreen() {
    val viewModel: MainViewModel = hiltViewModel()
    val matchState by viewModel.matchState.collectAsState()
    val context = LocalContext.current

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        if (perms.values.all { it }) viewModel.permissionsGranted() else
            Toast.makeText(context, "Camera & Mic permissions are required", Toast.LENGTH_LONG).show()
    }
    LaunchedEffect(Unit) {
        permLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
    }

    Column(
        Modifier.fillMaxSize().background(Color.Black)
    ) {
        Box(Modifier.weight(1f)) {
            SurfaceViewRendererComposable(
                modifier = Modifier.fillMaxSize(),
                onSurfaceReady = viewModel::initRemoteSurfaceView,
                message = when (matchState) {
                    is MatchState.LookingForMatchState -> "Looking For Match …"
                    is MatchState.IDLE -> "Idle"
                    else -> null
                }
            )
        }
        Row(Modifier.weight(1f)) {
            Box(Modifier.weight(1f).padding(4.dp)) {
                SurfaceViewRendererComposable(
                    modifier = Modifier.fillMaxSize(),
                    onSurfaceReady = viewModel::startLocalStream
                )
            }
        }
    }
}

