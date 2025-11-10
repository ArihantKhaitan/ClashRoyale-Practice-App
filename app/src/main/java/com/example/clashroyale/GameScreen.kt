package com.example.clashroyale

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex

// --- COLORS ---
val ArenaColor = Color(0xFFEED8AF)
val RiverColor = Color(0xFF67A1F3)
val BridgeColor = Color(0xFFFADA5E) // Yellow
val Player1Color = Color(0xFF3B82F6) // Blue
val Player2Color = Color(0xFFEF4444) // Red
val ElixirPurple = Color(0xFF8A2BE2)
val ValidPlacementColor = Color(0x8000FF00) // Translucent Green
val InvalidPlacementColor = Color(0x80FF0000) // Translucent Red

@Composable
fun GameScreen(
    gameState: GameState,
    onCardPlayed: (Card, Offset) -> Unit,
    onTogglePause: () -> Unit,
    onShowEmote: (String, Player) -> Unit,
    onStartGame: (Difficulty) -> Unit,
    onGoToMenu: () -> Unit,
    onPlayAgain: () -> Unit
) {
    var selectedCard by remember { mutableStateOf<Card?>(null) }
    var showEmoteMenu by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {

        // Show game UI if playing or game over
        if (gameState.phase == GamePhase.PLAYING || gameState.phase == GamePhase.GAME_OVER) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                GameHeader(gameState = gameState, onTogglePause = onTogglePause)

                Battlefield(
                    modifier = Modifier.weight(1f),
                    gameState = gameState,
                    selectedCard = selectedCard,
                    onPlayCard = { card, offset ->
                        onCardPlayed(card, offset)
                        selectedCard = null
                    }
                )

                BottomBar(
                    player = gameState.player1,
                    selectedCard = selectedCard,
                    onCardSelected = { card ->
                        selectedCard = if (selectedCard == card) null else card
                    },
                    onEmoteClick = { showEmoteMenu = !showEmoteMenu }
                )
            }

            if (showEmoteMenu) {
                EmoteMenu(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(y = (-110).dp, x = (-10).dp),
                    onEmoteSelected = { emoji ->
                        onShowEmote(emoji, gameState.player1)
                        showEmoteMenu = false
                    }
                )
            }
        }

        // **FIX: Show Pause Menu**
        if (gameState.isPaused && gameState.phase == GamePhase.PLAYING) {
            PauseMenuOverlay(
                onResume = onTogglePause,
                onGoToMenu = onGoToMenu
            )
        }

        // Show Game Over screen
        if (gameState.phase == GamePhase.GAME_OVER) {
            WinLossOverlay(
                winnerName = gameState.winnerName,
                player1Name = gameState.player1.name,
                player1Crowns = gameState.player1.crowns, // **FIX: Pass Int**
                player2Crowns = gameState.player2.crowns, // **FIX: Pass Int**
                onPlayAgain = onPlayAgain,
                onGoToMenu = onGoToMenu
            )
        }

        // Show Difficulty Menu screen
        if (gameState.phase == GamePhase.MENU) {
            DifficultySelectionOverlay(onStartGame = onStartGame)
        }
    }
}

// --- UI COMPONENTS ---

@Composable
fun GameHeader(gameState: GameState, onTogglePause: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.DarkGray)
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CrownCounter(player = gameState.player2)

        val timerColor = if (gameState.isOvertime) Color.Yellow else Color.White
        Text(
            text = "${gameState.gameTimeSeconds / 60}:${(gameState.gameTimeSeconds % 60).toString().padStart(2, '0')}",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = timerColor
        )

        // **FIX: Pause button now just calls onTogglePause, which will show the menu**
        Button(onClick = onTogglePause) {
            Text(text = "||")
        }
    }
}

@Composable
fun BottomBar(
    player: Player,
    selectedCard: Card?,
    onCardSelected: (Card) -> Unit,
    onEmoteClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF333333))
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ElixirBar(player = player)
        Spacer(modifier = Modifier.height(8.dp))
        PlayerHand(
            player = player,
            selectedCard = selectedCard,
            onCardSelected = onCardSelected,
            onEmoteClick = onEmoteClick
        )
    }
}

@Composable
fun ElixirBar(player: Player) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Row(
            modifier = Modifier
                .height(20.dp)
                .background(Color.Black, RoundedCornerShape(4.dp))
                .border(2.dp, ElixirPurple, RoundedCornerShape(4.dp))
                .padding(2.dp),
            horizontalArrangement = Arrangement.Start
        ) {
            val elixirFloat = player.elixir.toFloat()
            for (i in 1..10) {
                val filled = elixirFloat >= i
                val partial = elixirFloat > (i - 1) && elixirFloat < i
                val fillPercent = if (filled) 1f else if (partial) elixirFloat - (i - 1) else 0f

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(Color.Gray.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fillPercent)
                        .background(ElixirPurple)
                    )
                    Text(
                        text = "$i",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (i < 10) {
                    Spacer(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color.Black))
                }
            }
        }
    }
}

@Composable
fun PlayerHand(
    player: Player,
    selectedCard: Card?,
    onCardSelected: (Card) -> Unit,
    onEmoteClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Player Hand
        player.hand.forEach { card ->
            Card(
                modifier = Modifier
                    .size(width = 70.dp, height = 90.dp)
                    .padding(horizontal = 2.dp)
                    .clickable { onCardSelected(card) }
                    .border(
                        if (card.id == selectedCard?.id) 3.dp else 0.dp,
                        Color.Yellow,
                        shape = RoundedCornerShape(8.dp)
                    ),
                shape = RoundedCornerShape(8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                CardContent(card = card)
            }
        }

        Spacer(modifier = Modifier.width(4.dp))

        // Next Card & Emote
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val nextCard = player.upcoming.firstOrNull()
            if (nextCard != null) {
                Card(
                    modifier = Modifier
                        .size(width = 50.dp, height = 65.dp)
                        .border(1.dp, Color.Gray, RoundedCornerShape(4.dp)),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().background(Color.DarkGray)) {
                        Text(text = nextCard.emoji, fontSize = 24.sp)
                        Text(
                            text = "Next",
                            fontSize = 8.sp,
                            color = Color.White,
                            modifier = Modifier.align(Alignment.TopCenter).padding(top = 2.dp)
                        )
                    }
                }
            }
            // Emote Button
            Box(
                modifier = Modifier
                    .size(50.dp, 25.dp)
                    .padding(top = 4.dp)
                    .background(Color.Gray, RoundedCornerShape(4.dp))
                    .clickable { onEmoteClick() }
                    .border(1.dp, Color.White, RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "💬", fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun CardContent(card: Card) {
    Column(
        modifier = Modifier.fillMaxSize().background(Color.DarkGray).padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = card.name, fontSize = 12.sp, color = Color.White, maxLines = 1)
        Text(text = card.emoji, fontSize = 28.sp)
        Text(text = "${card.cost}", fontSize = 16.sp, color = ElixirPurple, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
fun EmoteMenu(modifier: Modifier, onEmoteSelected: (String) -> Unit) {
    Row(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
            .padding(8.dp)
            .zIndex(100f),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf("😂", "👍", "😡", "😢").forEach { emoji ->
            Text(
                text = emoji,
                fontSize = 32.sp,
                modifier = Modifier.clickable { onEmoteSelected(emoji) }
            )
        }
    }
}

@Composable
fun CrownCounter(player: Player) {
    val color = if (player.name == "Player 1") Player1Color else Player2Color
    Text(
        text = "👑 ${player.crowns}",
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White,
        modifier = Modifier
            .background(color, RoundedCornerShape(8.dp))
            .border(2.dp, color.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

/**
 * **FIX: New overlay for pause menu**
 */
@Composable
fun PauseMenuOverlay(onResume: () -> Unit, onGoToMenu: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .zIndex(100f),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "Paused", color = Color.White, fontSize = 68.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onResume) {
                Text(text = "Resume", fontSize = 20.sp)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onGoToMenu) {
                Text(text = "Menu", fontSize = 20.sp)
            }
        }
    }
}

/**
 * **FIX: Updated to look better and add a title**
 */
@Composable
fun DifficultySelectionOverlay(onStartGame: (Difficulty) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient( // **FIX: Added gradient background**
                    colors = listOf(Player1Color, Color(0xFF000033))
                )
            )
            .padding(16.dp)
            .zIndex(100f),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.DarkGray.copy(alpha = 0.9f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(32.dp)
            ) {
                Text(
                    text = "👑 Clash Royale 👑", // **FIX: Title added**
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Practice Mode", // **FIX: Title added**
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(32.dp))
                Button(onClick = { onStartGame(Difficulty.EASY) }, modifier = Modifier.fillMaxWidth()) {
                    Text(text = "Easy 🥱", fontSize = 20.sp) // **FIX: Emoji added**
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { onStartGame(Difficulty.MEDIUM) }, modifier = Modifier.fillMaxWidth()) {
                    Text(text = "Medium 😐", fontSize = 20.sp) // **FIX: Emoji added**
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { onStartGame(Difficulty.HARD) }, modifier = Modifier.fillMaxWidth()) {
                    Text(text = "Hard 🔥", fontSize = 20.sp) // **FIX: Emoji added**
                }
            }
        }
    }
}

/**
 * **FIX: Updated to show crown emojis**
 */
@Composable
fun WinLossOverlay(
    winnerName: String?,
    player1Name: String,
    player1Crowns: Int,
    player2Crowns: Int,
    onPlayAgain: () -> Unit,
    onGoToMenu: () -> Unit
) {
    val message = when {
        winnerName == null -> "Draw!"
        winnerName == player1Name -> "You Win!"
        else -> "You Lose!"
    }

    // **FIX: Helper function to generate crown emojis**
    fun getCrownEmojis(count: Int): String {
        return "👑".repeat(count).ifEmpty { "🚫" }
    }

    val p1CrownsText = getCrownEmojis(player1Crowns)
    val p2CrownsText = getCrownEmojis(player2Crowns)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .zIndex(100f),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = message, color = Color.White, fontSize = 68.sp, fontWeight = FontWeight.ExtraBold)
            Text(
                text = "$p1CrownsText - $p2CrownsText", // **FIX: Show emojis**
                color = Color.White,
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onPlayAgain) {
                Text(text = "Play Again", fontSize = 18.sp)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onGoToMenu) {
                Text(text = "Menu", fontSize = 18.sp)
            }
        }
    }
}

// --- BATTLEFIELD & ENTITY DRAWING ---

fun scaleLogicOffset(logicOffset: Offset, canvasSize: IntSize): Offset {
    val scaleX = canvasSize.width.toFloat() / Arena.WIDTH
    val scaleY = canvasSize.height.toFloat() / Arena.HEIGHT
    return Offset(logicOffset.x * scaleX, logicOffset.y * scaleY)
}

@Composable
fun Battlefield(
    modifier: Modifier = Modifier,
    gameState: GameState,
    selectedCard: Card?,
    onPlayCard: (Card, Offset) -> Unit
) {
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .onSizeChanged { canvasSize = it }
            .pointerInput(selectedCard) {
                detectTapGestures(
                    onTap = { screenOffset ->
                        val scaleX = Arena.WIDTH / canvasSize.width.toFloat()
                        val scaleY = Arena.HEIGHT / canvasSize.height.toFloat()
                        val logicOffset = Offset(screenOffset.x * scaleX, screenOffset.y * scaleY)

                        selectedCard?.let {
                            onPlayCard(it, logicOffset)
                        }
                    }
                )
            }
    ) {
        // 1. Arena Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            drawRect(color = ArenaColor)

            val riverY = canvasHeight / 2f
            val riverHeightPx = Arena.RIVER_HEIGHT * (canvasHeight / Arena.HEIGHT)
            drawRect(
                color = RiverColor,
                topLeft = Offset(0f, riverY - riverHeightPx / 2f),
                size = Size(canvasWidth, riverHeightPx)
            )

            val bridgeWidthLogic = 150f
            val bridgeWidthPx = bridgeWidthLogic * (canvasWidth / Arena.WIDTH)
            val bridgeHeightPx = riverHeightPx * 1.1f
            drawRect(
                color = BridgeColor,
                topLeft = Offset(canvasWidth * 0.25f - bridgeWidthPx / 2f, riverY - bridgeHeightPx / 2f),
                size = Size(bridgeWidthPx, bridgeHeightPx)
            )
            drawRect(
                color = BridgeColor,
                topLeft = Offset(canvasWidth * 0.75f - bridgeWidthPx / 2f, riverY - bridgeHeightPx / 2f),
                size = Size(bridgeWidthPx, bridgeHeightPx)
            )

            // --- Placement Zones ---
            if (selectedCard != null) {
                val riverTopPx = (Arena.RIVER_Y - Arena.RIVER_HEIGHT / 2f) * (canvasHeight / Arena.HEIGHT)
                val riverBottomPx = (Arena.RIVER_Y + Arena.RIVER_HEIGHT / 2f) * (canvasHeight / Arena.HEIGHT)

                if (selectedCard.entityType == CardType.TROOP) {
                    drawRect(color = ValidPlacementColor, topLeft = Offset(0f, riverBottomPx), size = Size(canvasWidth, canvasHeight - riverBottomPx))
                    val enemyTowerLeftDown = gameState.towers.none { it.owner.name == gameState.player2.name && it.type == TowerType.PRINCESS && it.position.x < Arena.WIDTH / 2f && it.hp > 0 }
                    val enemyTowerRightDown = gameState.towers.none { it.owner.name == gameState.player2.name && it.type == TowerType.PRINCESS && it.position.x > Arena.WIDTH / 2f && it.hp > 0 }
                    drawRect(color = if (enemyTowerLeftDown) ValidPlacementColor else InvalidPlacementColor, topLeft = Offset(0f, 0f), size = Size(canvasWidth / 2f, riverTopPx))
                    drawRect(color = if (enemyTowerRightDown) ValidPlacementColor else InvalidPlacementColor, topLeft = Offset(canvasWidth / 2f, 0f), size = Size(canvasWidth / 2f, riverTopPx))
                } else if (selectedCard.entityType == CardType.BUILDING) {
                    drawRect(color = ValidPlacementColor, topLeft = Offset(0f, riverBottomPx), size = Size(canvasWidth, canvasHeight - riverBottomPx))
                    drawRect(color = InvalidPlacementColor, topLeft = Offset(0f, 0f), size = Size(canvasWidth, riverBottomPx))
                }
            }

            // Draw Spell/Projectile Effects
            gameState.effects.forEach { effect ->
                val startPos = scaleLogicOffset(effect.position, canvasSize)
                when(effect) {
                    is Projectile -> {
                        val endPos = scaleLogicOffset(effect.targetPosition, canvasSize)
                        drawLine(color = effect.color, start = startPos, end = endPos, strokeWidth = 4f, cap = StrokeCap.Round)
                    }
                    is SpellEffect -> {
                        val alpha = (effect.duration / 500f)
                        drawCircle(color = Color.Yellow, radius = effect.radius * (canvasSize.width / Arena.WIDTH), center = startPos, style = Stroke(width = 4f), alpha = alpha)
                    }
                }
            }
        }

        // 2. Towers
        gameState.towers.filter { it.hp > 0 }.forEach { tower ->
            TowerView(tower = tower, canvasSize = canvasSize)
        }

        // 3. Buildings
        gameState.buildings.filter { it.hp > 0 }.forEach { building ->
            BuildingView(building = building, canvasSize = canvasSize)
        }

        // 4. Troops
        gameState.troops.forEach { troop ->
            TroopView(troop = troop, canvasSize = canvasSize)
        }

        // 5. Emotes
        gameState.emotes.forEach { emote ->
            val tower = gameState.towers.find { it.id == emote.towerId }
            if (tower != null) {
                EmoteView(emote = emote, towerPosition = tower.position, canvasSize = canvasSize)
            }
        }

        // 6. Player 1 Crowns
        Box(modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)) {
            CrownCounter(player = gameState.player1)
        }
    }
}

@Composable
fun TowerView(tower: Tower, canvasSize: IntSize) {
    val towerColor = if (tower.owner.name == "Player 1") Player1Color else Player2Color
    val towerSize = if (tower.type == TowerType.KING) 60.dp else 45.dp

    val scaledPos = scaleLogicOffset(tower.position, canvasSize)
    val density = LocalDensity.current
    val posX = with(density) { scaledPos.x.toDp() - towerSize / 2 }
    val posY = with(density) { scaledPos.y.toDp() - towerSize / 2 }

    val (towerText, towerTextSize) = when {
        !tower.isActive -> "Zzz" to 24.sp
        tower.type == TowerType.KING -> "👑" to 28.sp
        else -> "🏹" to 26.sp
    }

    Column(
        modifier = Modifier
            .offset(x = posX, y = posY)
            .width(towerSize),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (tower.type == TowerType.KING) {
            Text(text = tower.owner.name, color = towerColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Box(
            modifier = Modifier
                .size(width = towerSize, height = towerSize * 0.8f)
                .background(towerColor.copy(alpha = 0.8f), shape = RoundedCornerShape(8.dp))
                .border(2.dp, Color.Black, shape = RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = towerText, color = Color.White, fontWeight = FontWeight.Bold, fontSize = towerTextSize)
        }
        HpBar(currentHp = tower.hp, maxHp = tower.maxHp, ownerColor = towerColor, width = towerSize)
    }
}

@Composable
fun BuildingView(building: Building, canvasSize: IntSize) {
    val buildingColor = if (building.owner.name == "Player 1") Player1Color else Player2Color
    val buildingSize = 45.dp

    val scaledPos = scaleLogicOffset(building.position, canvasSize)
    val density = LocalDensity.current
    val posX = with(density) { scaledPos.x.toDp() - buildingSize / 2 }
    val posY = with(density) { scaledPos.y.toDp() - buildingSize / 2 }

    Column(
        modifier = Modifier
            .offset(x = posX, y = posY)
            .width(buildingSize),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(buildingSize)
                .background(buildingColor.copy(alpha = 0.8f), shape = RoundedCornerShape(8.dp))
                .border(2.dp, Color.Black, shape = RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = building.card.emoji, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 28.sp)
        }
        HpBar(currentHp = building.hp, maxHp = building.maxHp, ownerColor = buildingColor, width = buildingSize)
        LifetimeBar(
            currentMs = building.lifetimeRemainingMs,
            maxMs = (building.card.lifetimeSeconds ?: 30) * 1000L,
            width = buildingSize
        )
    }
}

@Composable
fun TroopView(troop: Troop, canvasSize: IntSize) {
    val troopColor = if (troop.owner.name == "Player 1") Player1Color else Player2Color
    val troopSize = 40.dp

    val scaledPos = scaleLogicOffset(troop.position, canvasSize)
    val density = LocalDensity.current
    val posX = with(density) { scaledPos.x.toDp() - troopSize / 2 }
    val posY = with(density) { scaledPos.y.toDp() - troopSize / 2 }

    Column(
        modifier = Modifier
            .offset(x = posX, y = posY)
            .size(troopSize),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(troopSize * 0.7f)
                .background(troopColor, shape = RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = troop.card.emoji, color = Color.White, fontSize = 20.sp, textAlign = TextAlign.Center)
        }
        HpBar(currentHp = troop.hp, maxHp = troop.maxHp, ownerColor = troopColor, width = troopSize * 0.9f)
    }
}

@Composable
fun EmoteView(emote: Emote, towerPosition: Offset, canvasSize: IntSize) {
    val emoteSize = 60.dp
    val animatedAlpha by animateFloatAsState(
        targetValue = if (emote.duration < 500L) 0f else 1f,
        animationSpec = tween(durationMillis = 500)
    )

    val scaledPos = scaleLogicOffset(towerPosition, canvasSize)
    val density = LocalDensity.current
    val posX = with(density) { scaledPos.x.toDp() - emoteSize / 2 }
    val posY = with(density) { scaledPos.y.toDp() - emoteSize - 30.dp }

    Box(
        modifier = Modifier
            .offset(x = posX, y = posY)
            .size(emoteSize)
            .background(Color.White.copy(alpha = animatedAlpha * 0.7f), RoundedCornerShape(30.dp))
            .border(2.dp, Color.Black.copy(alpha = animatedAlpha), RoundedCornerShape(30.dp))
            .zIndex(200f),
        contentAlignment = Alignment.Center
    ) {
        Text(text = emote.emoji, fontSize = 32.sp, modifier = Modifier.padding(4.dp))
    }
}

@Composable
fun HpBar(currentHp: Int, maxHp: Int, ownerColor: Color, width: Dp) {
    Box(
        modifier = Modifier
            .width(width)
            .height(10.dp)
            .background(Color.Red, RoundedCornerShape(4.dp))
            .border(1.dp, Color.Black, RoundedCornerShape(4.dp))
    ) {
        val healthPercent = (currentHp.toFloat() / maxHp.toFloat()).coerceIn(0f, 1f)
        Box(
            modifier = Modifier
                .fillMaxWidth(healthPercent)
                .fillMaxHeight()
                .background(ownerColor, RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp))
        )
        Text(
            text = "$currentHp",
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
fun LifetimeBar(currentMs: Long, maxMs: Long, width: Dp) {
    Box(
        modifier = Modifier
            .width(width)
            .height(6.dp)
            .background(Color.DarkGray, RoundedCornerShape(2.dp))
            .border(1.dp, Color.Black, RoundedCornerShape(2.dp))
    ) {
        val percent = (currentMs.toFloat() / maxMs.toFloat()).coerceIn(0f, 1f)
        Box(
            modifier = Modifier
                .fillMaxWidth(percent)
                .fillMaxHeight()
                .background(Color.LightGray, RoundedCornerShape(topStart = 2.dp, bottomStart = 2.dp))
        )
    }
}