package com.example.webrtc_android.ui.components

import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import org.webrtc.SurfaceViewRenderer

@Composable
fun SurfaceViewRendererComposable(
    modifier: Modifier = Modifier,
    onSurfaceReady: (SurfaceViewRenderer) -> Unit,
    message: String? = null
) {
    if (!message.isNullOrEmpty()) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = message,
                color = Color.White,
                style = TextStyle(fontSize = 18.sp)
            )
        }
    } else {
        Column(
            modifier = modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AndroidView(modifier = Modifier
                .fillMaxWidth()
                .weight(1f), factory = { ctx ->
                FrameLayout(ctx).apply {
                    addView(SurfaceViewRenderer(ctx).also {
                        onSurfaceReady.invoke(it)
                    })
                }
            })
        }
    }
}