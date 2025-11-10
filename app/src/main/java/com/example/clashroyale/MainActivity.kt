package com.example.clashroyale

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val gameViewModel: GameViewModel by viewModels()

        try {
            setContent {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    val gameState by gameViewModel.gameState.collectAsState()
                    GameScreen(
                        gameState = gameState,
                        onCardPlayed = { card, position -> gameViewModel.onCardPlayed(card, position) },
                        onTogglePause = { gameViewModel.togglePause() },
                        onShowEmote = { emoji, player -> gameViewModel.showEmote(emoji, player) },
                        onStartGame = { difficulty -> gameViewModel.startGame(difficulty) },
                        onGoToMenu = { gameViewModel.goToMenu() },
                        onPlayAgain = { gameViewModel.playAgain() } // **FIX: Added this line**
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error while setting content", e)
            setContent {
                ErrorFallback(message = e.localizedMessage ?: "Unknown error")
            }
        }
    }
}

@Composable
fun ErrorFallback(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Red),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "Startup error: $message", color = Color.White, fontSize = 16.sp)
    }
}