package com.example.clashroyale

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

// --- ARENA CONSTANTS ---
object Arena {
    const val WIDTH = 1000f
    const val HEIGHT = 2000f
    const val RIVER_Y = HEIGHT / 2f
    const val RIVER_HEIGHT = 100f // This is the total height
    val BRIDGES = listOf(
        Offset(WIDTH * 0.25f, RIVER_Y),
        Offset(WIDTH * 0.75f, RIVER_Y)
    )
}

class GameViewModel : ViewModel() {

    private val _gameState = MutableStateFlow(createInitialGameState())
    val gameState = _gameState.asStateFlow()

    private var gameLoopJob: Job? = null

    init {
        // Wait for user to select difficulty
    }

    private fun createInitialGameState(): GameState {
        val player1Deck = Deck().cards
        val player2Deck = Deck().cards

        val player1 = Player(
            name = "Player 1",
            fullDeck = player1Deck,
            hand = player1Deck.take(4).toMutableList(),
            upcoming = player1Deck.drop(4).toMutableList()
        )
        val player2 = Player(
            name = "Player 2",
            fullDeck = player2Deck,
            hand = player2Deck.take(4).toMutableList(),
            upcoming = player2Deck.drop(4).toMutableList()
        )

        return GameState(
            player1 = player1,
            player2 = player2,
            towers = createInitialTowers(player1, player2),
            phase = GamePhase.MENU // Start in menu
        )
    }

    private fun createInitialTowers(player1: Player, player2: Player): List<Tower> {
        // Uses the Arena constants
        return listOf(
            // Player 1 (Bottom)
            Tower(owner = player1, type = TowerType.PRINCESS, maxHp = 1400, hp = 1400, position = Offset(Arena.WIDTH * 0.2f, Arena.HEIGHT * 0.8f), range = 150f, damage = 50, attackSpeed = 1.2f),
            Tower(owner = player1, type = TowerType.PRINCESS, maxHp = 1400, hp = 1400, position = Offset(Arena.WIDTH * 0.8f, Arena.HEIGHT * 0.8f), range = 150f, damage = 50, attackSpeed = 1.2f),
            Tower(owner = player1, type = TowerType.KING, maxHp = 2400, hp = 2400, position = Offset(Arena.WIDTH * 0.5f, Arena.HEIGHT * 0.9f), range = 130f, damage = 60, attackSpeed = 1.5f, isActive = false),
            // Player 2 (Top)
            Tower(owner = player2, type = TowerType.PRINCESS, maxHp = 1400, hp = 1400, position = Offset(Arena.WIDTH * 0.2f, Arena.HEIGHT * 0.2f), range = 150f, damage = 50, attackSpeed = 1.2f),
            Tower(owner = player2, type = TowerType.PRINCESS, maxHp = 1400, hp = 1400, position = Offset(Arena.WIDTH * 0.8f, Arena.HEIGHT * 0.2f), range = 150f, damage = 50, attackSpeed = 1.2f),
            Tower(owner = player2, type = TowerType.KING, maxHp = 2400, hp = 2400, position = Offset(Arena.WIDTH * 0.5f, Arena.HEIGHT * 0.1f), range = 130f, damage = 60, attackSpeed = 1.5f, isActive = false)
        )
    }

    fun togglePause() {
        if (_gameState.value.phase != GamePhase.PLAYING) return
        _gameState.value = _gameState.value.copy(isPaused = !_gameState.value.isPaused)
    }

    fun startGame(difficulty: Difficulty) {
        val newState = createInitialGameState() // Get a clean state
        _gameState.value = newState.copy(
            difficulty = difficulty,
            phase = GamePhase.PLAYING,
            gameTimeSeconds = 180,
            isOvertime = false,
            isPaused = false,
            winnerName = null
        )
        startGameLoop()
    }

    /**
     * **FIX: New function to restart with the SAME difficulty**
     */
    fun playAgain() {
        val currentDifficulty = _gameState.value.difficulty // Keep old difficulty
        gameLoopJob?.cancel()
        startGame(currentDifficulty) // Start new game
    }

    /**
     * This function returns to the main menu.
     */
    fun goToMenu() {
        gameLoopJob?.cancel()
        _gameState.value = createInitialGameState()
    }

    private fun endGame(winner: Player?) {
        gameLoopJob?.cancel()
        _gameState.value = _gameState.value.copy(
            phase = GamePhase.GAME_OVER,
            winnerName = winner?.name,
            isPaused = true
        )
    }

    fun showEmote(emoji: String, player: Player) {
        val tower = _gameState.value.towers.find { it.owner.name == player.name && it.type == TowerType.KING }
        if (tower != null) {
            val newEmotes = _gameState.value.emotes.toMutableList()
            newEmotes.add(Emote(emoji, tower.id))
            _gameState.value = _gameState.value.copy(emotes = newEmotes)
        }
    }

    private fun checkCrownWinner(): Player? {
        val p1Crowns = _gameState.value.player1.crowns
        val p2Crowns = _gameState.value.player2.crowns
        return when {
            p1Crowns > p2Crowns -> _gameState.value.player1
            p2Crowns > p1Crowns -> _gameState.value.player2
            else -> null
        }
    }

    private fun startGameLoop() {
        gameLoopJob = viewModelScope.launch {
            // Game Timer
            launch {
                while (_gameState.value.phase == GamePhase.PLAYING) {
                    delay(1000)
                    if (_gameState.value.isPaused) continue

                    val currentState = _gameState.value
                    val newTime = currentState.gameTimeSeconds - 1

                    if (newTime <= 0) {
                        if (currentState.isOvertime) {
                            endGame(checkCrownWinner())
                        } else if (currentState.player1.crowns == currentState.player2.crowns) {
                            _gameState.value = currentState.copy(isOvertime = true, gameTimeSeconds = 60)
                        } else {
                            endGame(checkCrownWinner())
                        }
                    } else {
                        _gameState.value = currentState.copy(gameTimeSeconds = newTime)
                    }
                }
            }

            // Elixir Generation
            launch {
                while (_gameState.value.phase == GamePhase.PLAYING) {
                    delay(100L)
                    if (_gameState.value.isPaused) continue

                    val currentState = _gameState.value
                    val elixirTick = if (currentState.isOvertime) 0.1 else 0.05
                    val p2ElixirMultiplier = when (currentState.difficulty) {
                        Difficulty.EASY -> 0.8
                        Difficulty.MEDIUM -> 1.0
                        Difficulty.HARD -> 1.2
                    }
                    val p1NewElixir = (currentState.player1.elixir + elixirTick).coerceAtMost(10.0)
                    val p2NewElixir = (currentState.player2.elixir + (elixirTick * p2ElixirMultiplier)).coerceAtMost(10.0)

                    _gameState.value = currentState.copy(
                        player1 = currentState.player1.copy(elixir = p1NewElixir),
                        player2 = currentState.player2.copy(elixir = p2NewElixir)
                    )
                }
            }

            // Main Game Loop
            launch {
                var lastTime = System.currentTimeMillis()
                while (_gameState.value.phase == GamePhase.PLAYING) {
                    val currentTime = System.currentTimeMillis()
                    val deltaTime = (currentTime - lastTime).coerceAtLeast(0)
                    lastTime = currentTime

                    if (!_gameState.value.isPaused) {
                        updateGame(deltaTime)
                    }
                    delay(100) // ~10 FPS game tick
                }
            }

            // Enemy AI
            launch {
                while (_gameState.value.phase == GamePhase.PLAYING) {
                    val reactionDelay = when (_gameState.value.difficulty) {
                        Difficulty.EASY -> 4000L
                        Difficulty.MEDIUM -> 3000L
                        Difficulty.HARD -> 1500L
                    }
                    delay(reactionDelay)

                    if (_gameState.value.isPaused) continue
                    playEnemyCard()

                    if (Random.nextFloat() < 0.2f) {
                        showEmote(listOf("😂", "👍", "😡", "😢").random(), _gameState.value.player2)
                    }
                }
            }
        }
    }

    private fun updateGame(deltaTime: Long) {
        if (_gameState.value.phase != GamePhase.PLAYING) return

        val currentTime = System.currentTimeMillis()
        val currentState = _gameState.value

        var newTowers = currentState.towers.toMutableList()
        var newTroops = currentState.troops.toMutableList()
        var newBuildings = currentState.buildings.toMutableList()
        var newEffects = currentState.effects.toMutableList()
        var newEmotes = currentState.emotes.toMutableList()

        // Update Towers
        newTowers.forEachIndexed { i, tower ->
            val updatedTower = updateTower(tower, newTroops, newEffects, currentTime)
            newTowers[i] = updatedTower
        }

        // Update Buildings
        newBuildings = newBuildings.map { building ->
            updateBuilding(building, newTroops, newEffects, currentTime, deltaTime)
        }.toMutableList()

        // Update Troops
        newTroops = newTroops.map { troop ->
            updateTroop(troop, newTowers, newBuildings, newTroops, currentTime, deltaTime)
        }.toMutableList()

        // Update effects & emotes
        newEffects.removeAll { it.duration -= deltaTime; it.duration <= 0 }
        newEmotes.removeAll { it.duration -= deltaTime; it.duration <= 0 }

        // Check for deaths
        newTroops.removeAll { it.hp <= 0 }
        newBuildings.removeAll { it.hp <= 0 || it.lifetimeRemainingMs <= 0 }

        val destroyedTowers = newTowers.filter { it.hp <= 0 && it.hp > -9999 }
        var winner: Player? = null
        var p1Crowns = currentState.player1.crowns
        var p2Crowns = currentState.player2.crowns

        // Check for King Tower kill first
        val p1KingDestroyed = destroyedTowers.any { it.owner.name == currentState.player1.name && it.type == TowerType.KING }
        val p2KingDestroyed = destroyedTowers.any { it.owner.name == currentState.player2.name && it.type == TowerType.KING }

        if (p1KingDestroyed) {
            winner = currentState.player2
            p2Crowns = 3
        } else if (p2KingDestroyed) {
            winner = currentState.player1
            p1Crowns = 3
        } else if (destroyedTowers.isNotEmpty()) {
            destroyedTowers.forEach { destroyed ->
                newTowers = newTowers.map { tower ->
                    if (tower.id == destroyed.id) tower.copy(hp = -10000) else tower
                }.toMutableList()

                val ownerKing = newTowers.find { it.owner.name == destroyed.owner.name && it.type == TowerType.KING }
                if (ownerKing != null) {
                    newTowers = newTowers.map { tower ->
                        if (tower.id == ownerKing.id) tower.copy(isActive = true) else tower
                    }.toMutableList()
                }

                if (destroyed.owner.name == currentState.player1.name) {
                    p2Crowns++
                    if (currentState.isOvertime) winner = currentState.player2
                } else {
                    p1Crowns++
                    if (currentState.isOvertime) winner = currentState.player1
                }
            }
        }

        if (winner != null) {
            _gameState.value = currentState.copy(
                player1 = currentState.player1.copy(crowns = p1Crowns),
                player2 = currentState.player2.copy(crowns = p2Crowns),
                troops = newTroops,
                towers = newTowers,
                buildings = newBuildings,
                effects = newEffects,
                emotes = newEmotes
            )
            endGame(winner)
            return
        }

        _gameState.value = currentState.copy(
            player1 = currentState.player1.copy(crowns = p1Crowns),
            player2 = currentState.player2.copy(crowns = p2Crowns),
            troops = newTroops,
            towers = newTowers,
            buildings = newBuildings,
            effects = newEffects,
            emotes = newEmotes
        )
    }

    private fun updateTower(tower: Tower, troops: List<Troop>, effects: MutableList<GameEffect>, currentTime: Long): Tower {
        if (!tower.isActive || tower.hp <= 0) return tower

        val targets = troops.filter { it.owner.name != tower.owner.name && distance(it.position, tower.position) <= tower.range }
        if (targets.isEmpty()) return tower

        val target = targets.minByOrNull { distance(it.position, tower.position) } ?: return tower

        if (tower.canAttack(currentTime)) {
            target.hp -= tower.damage
            effects.add(Projectile(tower.position, target.position, if (tower.owner.name == _gameState.value.player1.name) Color.Blue else Color.Red))
            return tower.copy(lastAttackTime = currentTime)
        }
        return tower
    }

    private fun updateBuilding(building: Building, troops: List<Troop>, effects: MutableList<GameEffect>, currentTime: Long, deltaTime: Long): Building {
        var updatedBuilding = building.copy(lifetimeRemainingMs = building.lifetimeRemainingMs - deltaTime)
        if (updatedBuilding.lifetimeRemainingMs <= 0 || updatedBuilding.hp <= 0) {
            return updatedBuilding.copy(hp = 0) // Mark for removal
        }

        val range = building.card.range ?: 100f
        val damage = building.card.damage ?: 0

        val targets = troops.filter { it.owner.name != updatedBuilding.owner.name && distance(it.position, updatedBuilding.position) <= range }
        if (targets.isEmpty()) return updatedBuilding

        val target = targets.minByOrNull { distance(it.position, updatedBuilding.position) } ?: return updatedBuilding

        if (updatedBuilding.canAttack(currentTime)) {
            target.hp -= damage
            effects.add(Projectile(updatedBuilding.position, target.position, if (updatedBuilding.owner.name == _gameState.value.player1.name) Color.Blue else Color.Red))
            return updatedBuilding.copy(lastAttackTime = currentTime)
        }
        return updatedBuilding
    }

    private fun updateTroop(troop: Troop, towers: MutableList<Tower>, buildings: MutableList<Building>, troops: MutableList<Troop>, currentTime: Long, deltaTime: Long): Troop {
        val enemies: List<GameEntity> = (towers.filter { it.owner.name != troop.owner.name && it.hp > 0 } +
                buildings.filter { it.owner.name != troop.owner.name && it.hp > 0 } +
                troops.filter { it.owner.name != troop.owner.name && it.hp > 0 })

        val potentialTargets = when (troop.card.targetPriority) {
            TargetPriority.BUILDINGS_ONLY -> enemies.filter { it is Tower || it is Building }
            TargetPriority.ALL -> enemies
        }

        val target = potentialTargets.minByOrNull { distance(troop.position, it.position) }

        // Determine the navigation destination
        val destination: Offset? = if (target != null) {
            val isPlayer1 = troop.owner.name == _gameState.value.player1.name
            val riverTop = Arena.RIVER_Y - (Arena.RIVER_HEIGHT / 2f)
            val riverBottom = Arena.RIVER_Y + (Arena.RIVER_HEIGHT / 2f)
            val onHomeSide = if (isPlayer1) troop.position.y > riverBottom else troop.position.y < riverTop

            if (onHomeSide) {
                Arena.BRIDGES.minByOrNull { distance(target.position, it) }
            } else {
                target.position
            }
        } else {
            getPathDestination(troop)
        }

        // Now, move or attack
        if (target != null) {
            val distanceToTarget = distance(troop.position, target.position)
            val troopRange = troop.card.range ?: 10f

            if (distanceToTarget <= troopRange) {
                // Attack
                if (troop.canAttack(currentTime)) {
                    target.hp -= troop.card.damage ?: 0
                    if (target is Tower && target.type == TowerType.KING) {
                        target.isActive = true
                    }
                    return troop.copy(lastAttackTime = currentTime)
                }
                return troop
            }
        }

        // Move
        if (destination != null) {
            val moveSpeed = (troop.card.movementSpeed ?: 1f) * (deltaTime / 100f)
            val angle = atan2((destination.y - troop.position.y).toDouble(), (destination.x - troop.position.x).toDouble())
            val newX = troop.position.x + (moveSpeed * cos(angle)).toFloat()
            val newY = troop.position.y + (moveSpeed * sin(angle)).toFloat()
            return troop.copy(position = Offset(newX, newY))
        }

        return troop // No change
    }

    private fun getPathDestination(troop: Troop): Offset? {
        val isPlayer1 = troop.owner.name == _gameState.value.player1.name
        val riverTop = Arena.RIVER_Y - (Arena.RIVER_HEIGHT / 2f)
        val riverBottom = Arena.RIVER_Y + (Arena.RIVER_HEIGHT / 2f)
        val onHomeSide = if (isPlayer1) troop.position.y > riverBottom else troop.position.y < riverTop

        val enemyTargets = (_gameState.value.towers.filter { it.owner.name != troop.owner.name && it.hp > 0 } +
                _gameState.value.buildings.filter { it.owner.name != troop.owner.name && it.hp > 0 })
        if (enemyTargets.isEmpty()) return null

        val ultimateTarget = enemyTargets.minByOrNull { distance(troop.position, it.position) } ?: return null

        return if (onHomeSide) {
            Arena.BRIDGES.minByOrNull { distance(ultimateTarget.position, it) }
        } else {
            ultimateTarget.position
        }
    }

    private fun getPlacementValidity(position: Offset, card: Card): Boolean {
        val riverTop = Arena.RIVER_Y - (Arena.RIVER_HEIGHT / 2f)
        val riverBottom = Arena.RIVER_Y + (Arena.RIVER_HEIGHT / 2f)

        if (card.entityType == CardType.SPELL) return true // Spells anywhere
        if (position.y > riverTop && position.y < riverBottom) return false
        if (card.entityType == CardType.BUILDING) return position.y >= riverBottom

        // --- Troop Placement ---
        val isPlayer1Side = position.y >= riverBottom
        val isPlayer2Side = position.y <= riverTop

        val enemyTowerLeftDown = _gameState.value.towers.none { it.owner.name == _gameState.value.player2.name && it.type == TowerType.PRINCESS && it.position.x < Arena.WIDTH / 2 && it.hp > 0 }
        val enemyTowerRightDown = _gameState.value.towers.none { it.owner.name == _gameState.value.player2.name && it.type == TowerType.PRINCESS && it.position.x > Arena.WIDTH / 2 && it.hp > 0 }

        if (isPlayer1Side) return true
        if (isPlayer2Side) {
            if (position.x < Arena.WIDTH / 2 && enemyTowerLeftDown) return true
            if (position.x > Arena.WIDTH / 2 && enemyTowerRightDown) return true
        }
        return false
    }

    fun onCardPlayed(card: Card, position: Offset) {
        if (_gameState.value.phase != GamePhase.PLAYING || _gameState.value.isPaused) return
        if (_gameState.value.player1.elixir < card.cost) return
        if (!getPlacementValidity(position, card)) return

        _gameState.value = _gameState.value.copy(
            player1 = _gameState.value.player1.copy(elixir = _gameState.value.player1.elixir - card.cost)
        )

        val currentState = _gameState.value
        val newTroops = currentState.troops.toMutableList()
        val newBuildings = currentState.buildings.toMutableList()
        val newEffects = currentState.effects.toMutableList()

        when (card.entityType) {
            CardType.SPELL -> {
                val targets: List<GameEntity> = (currentState.troops + currentState.towers + currentState.buildings)
                    .filter { it.owner.name != currentState.player1.name && it.hp > 0 }
                targets.forEach { target ->
                    if (distance(position, target.position) <= (card.radius ?: 0f)) {
                        target.hp -= card.damage ?: 0
                        if (target is Tower && target.type == TowerType.KING) {
                            target.isActive = true
                        }
                    }
                }
                newEffects.add(SpellEffect(position, card.radius ?: 0f, card.emoji))
            }
            CardType.TROOP -> {
                for (i in 1..card.spawnCount) {
                    val spawnPos = position.copy(
                        x = position.x + (Random.nextInt(-20, 20)),
                        y = position.y + (Random.nextInt(-20, 20))
                    )
                    val newTroop = Troop(card = card, owner = currentState.player1, maxHp = card.hp ?: 0, hp = card.hp ?: 0, position = spawnPos)
                    newTroops.add(newTroop)
                }
            }
            CardType.BUILDING -> {
                val newBuilding = Building(card = card, owner = currentState.player1, maxHp = card.hp ?: 0, hp = card.hp ?: 0, position = position, lifetimeRemainingMs = (card.lifetimeSeconds ?: 30) * 1000L)
                newBuildings.add(newBuilding)
            }
        }

        _gameState.value = currentState.copy(
            troops = newTroops,
            buildings = newBuildings,
            effects = newEffects
        )

        _gameState.value.player1.cycleCard(card)
    }

    private fun playEnemyCard() {
        val ai = _gameState.value.player2
        val playableCards = ai.hand.filter { it.cost <= ai.elixir }
        if (playableCards.isEmpty()) return

        val card = playableCards.random()

        _gameState.value = _gameState.value.copy(
            player2 = ai.copy(elixir = ai.elixir - card.cost)
        )
        val aiAfterPlay = _gameState.value.player2

        val x = Random.nextFloat() * Arena.WIDTH
        val y = Random.nextFloat() * (Arena.RIVER_Y - (Arena.RIVER_HEIGHT / 2f) - 20) + 20
        val position = Offset(x, y)

        val buildingPosition = Offset(
            x = Random.nextFloat() * (Arena.WIDTH * 0.8f) + (Arena.WIDTH * 0.1f),
            y = Random.nextFloat() * (Arena.RIVER_Y - Arena.RIVER_HEIGHT) + (Arena.HEIGHT * 0.1f)
        )

        val currentState = _gameState.value
        val newTroops = currentState.troops.toMutableList()
        val newBuildings = currentState.buildings.toMutableList()
        val newEffects = currentState.effects.toMutableList()

        when (card.entityType) {
            CardType.SPELL -> {
                val playerTower = (currentState.towers + currentState.buildings)
                    .filter { it.owner.name == currentState.player1.name && it.hp > 0 }.randomOrNull()
                if (playerTower != null) {
                    playerTower.hp -= card.damage ?: 0
                    if (playerTower is Tower && playerTower.type == TowerType.KING) playerTower.isActive = true
                    newEffects.add(SpellEffect(playerTower.position, card.radius ?: 0f, card.emoji))
                }
            }
            CardType.TROOP -> {
                for (i in 1..card.spawnCount) {
                    val spawnPos = position.copy(
                        x = position.x + (Random.nextInt(-20, 20)),
                        y = position.y + (Random.nextInt(-20, 20))
                    )
                    val newTroop = Troop(card = card, owner = aiAfterPlay, maxHp = card.hp ?: 0, hp = card.hp ?: 0, position = spawnPos)
                    newTroops.add(newTroop)
                }
            }
            CardType.BUILDING -> {
                val newBuilding = Building(card = card, owner = aiAfterPlay, maxHp = card.hp ?: 0, hp = card.hp ?: 0, position = buildingPosition, lifetimeRemainingMs = (card.lifetimeSeconds ?: 30) * 1000L)
                newBuildings.add(newBuilding)
            }
        }

        _gameState.value = currentState.copy(
            troops = newTroops,
            buildings = newBuildings,
            effects = newEffects
        )
        aiAfterPlay.cycleCard(card)
    }

    private fun distance(p1: Offset, p2: Offset): Float {
        return sqrt((p1.x - p2.x).pow(2) + (p1.y - p2.y).pow(2))
    }
}