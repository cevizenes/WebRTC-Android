package com.example.webrtc_android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.webrtc_android.utils.ChatItems

@Composable
fun ChatSection(chatItems: List<ChatItems>) {
    val listState = rememberLazyListState()

    LaunchedEffect(chatItems.size) {
        if (chatItems.isNotEmpty()){
            listState.scrollToItem(chatItems.size - 1)
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(chatItems) { chatItem ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = if (chatItem.isMine) Arrangement.Start else Arrangement.End
            ) {
                // Each message inside a Card
                Card(
                    modifier = Modifier.widthIn(0.dp, 320.dp), // Max width of 80% of screen width
                    shape = MaterialTheme.shapes.medium,
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Surface(modifier = Modifier) {
                        Text(
                            modifier = Modifier
                                .background(
                                    if (chatItem.isMine) Color(0xFFA4BAD1) else Color(0xFFC294A4)
                                )
                                .padding(12.dp),
                            text = chatItem.text,
                            style = TextStyle(fontWeight = FontWeight.Normal),
                            color = Color.Black
                        )
                    }
                }
            }
        }
    }
}