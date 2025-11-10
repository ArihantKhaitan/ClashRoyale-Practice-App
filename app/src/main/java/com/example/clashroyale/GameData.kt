package com.example.clashroyale

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color

// --- GAME STATE ---

// **FIX: Added Difficulty and GamePhase enums**
enum class Difficulty { EASY, MEDIUM, HARD }
enum class GamePhase { MENU, PLAYING, GAME_OVER }

data class GameState(
    val player1: Player,
    val player2: Player,
    val troops: List<Troop> = emptyList(),
    val towers: List<Tower>,
    val buildings: List<Building> = emptyList(),
    val effects: List<GameEffect> = emptyList(),
    val emotes: List<Emote> = emptyList(),
    var gameTimeSeconds: Int = 180, // 3-minute game
    var isPaused: Boolean = false,
    var isOvertime: Boolean = false,
    val phase: GamePhase = GamePhase.MENU, // **FIX: Replaced 'winner' with 'phase'**
    val winnerName: String? = null, // **FIX: To store winner's name**
    val difficulty: Difficulty = Difficulty.MEDIUM // **FIX: To store difficulty**
)

// --- PLAYER & DECK ---

data class Player(
    val name: String,
    var elixir: Double = 5.0, // Use Double for smoother generation
    val fullDeck: List<Card>,
    var hand: MutableList<Card>,
    var upcoming: MutableList<Card>, // Cards not in hand
    var crowns: Int = 0
) {
    /**
     * Cycles a card from the hand to the back of the upcoming queue
     * and brings the next upcoming card into the hand.
     */
    fun cycleCard(playedCard: Card) {
        val handIndex = hand.indexOf(playedCard)
        if (handIndex == -1) return // Card not in hand

        // Check if upcoming is empty (can happen with small decks in testing)
        if (upcoming.isEmpty()) {
            upcoming.add(playedCard)
            hand.removeAt(handIndex)
            return
        }

        val nextCard = upcoming.removeAt(0)
        hand[handIndex] = nextCard
        upcoming.add(playedCard)
    }
}

/**
 * Holds the master list of all cards.
 */
class Deck {
    val cards: List<Card> = listOf(
        // --- Original Cards (Updated) ---
        Card(name = "Knight", cost = 3, hp = 600, damage = 75, range = 10f, attackSpeed = 1.2f, movementSpeed = 2.0f, targetPriority = TargetPriority.ALL, emoji = "🤺", entityType = CardType.TROOP),
        Card(name = "Archers", cost = 3, hp = 120, damage = 40, range = 100f, attackSpeed = 1.2f, movementSpeed = 2.0f, targetPriority = TargetPriority.ALL, emoji = "🏹", entityType = CardType.TROOP),
        Card(name = "Giant", cost = 5, hp = 2000, damage = 100, range = 10f, attackSpeed = 1.5f, movementSpeed = 1.5f, targetPriority = TargetPriority.BUILDINGS_ONLY, emoji = "🦍", entityType = CardType.TROOP),
        Card(name = "Hog Rider", cost = 4, hp = 800, damage = 150, range = 10f, attackSpeed = 1.6f, movementSpeed = 3.0f, targetPriority = TargetPriority.BUILDINGS_ONLY, emoji = "🐖", entityType = CardType.TROOP),
        Card(name = "Fireball", cost = 4, entityType = CardType.SPELL, damage = 250, radius = 60f, emoji = "🔥"),
        Card(name = "Musketeer", cost = 4, hp = 350, damage = 100, range = 120f, attackSpeed = 1.1f, movementSpeed = 2.0f, targetPriority = TargetPriority.ALL, emoji = "💂", entityType = CardType.TROOP),
        Card(name = "Valkyrie", cost = 4, hp = 900, damage = 100, range = 10f, attackSpeed = 1.5f, movementSpeed = 2.0f, targetPriority = TargetPriority.ALL, emoji = "👩‍🦰", entityType = CardType.TROOP),
        Card(name = "Arrows", cost = 3, entityType = CardType.SPELL, damage = 100, radius = 80f, emoji = "🎯"),

        // --- New Cards ---
        Card(name = "Skeletons", cost = 1, hp = 30, damage = 30, range = 10f, attackSpeed = 1.0f, movementSpeed = 3.0f, targetPriority = TargetPriority.ALL, emoji = "💀", entityType = CardType.TROOP, spawnCount = 3),
        Card(name = "P.E.K.K.A", cost = 7, hp = 1500, damage = 300, range = 10f, attackSpeed = 1.8f, movementSpeed = 1.0f, targetPriority = TargetPriority.ALL, emoji = "🤖", entityType = CardType.TROOP),
        Card(name = "Inferno", cost = 5, hp = 800, damage = 50, range = 120f, attackSpeed = 0.4f, lifetimeSeconds = 30, emoji = "🔥", entityType = CardType.BUILDING, targetPriority = TargetPriority.ALL)

    ).shuffled()
}

// --- ENTITIES ---

/**
 * Common interface for Towers, Troops, and Buildings
 */
interface GameEntity {
    val id: String
    val owner: Player
    var hp: Int
    val position: Offset
}

enum class TargetPriority {
    ALL,
    BUILDINGS_ONLY
}

enum class CardType {
    TROOP,
    BUILDING,
    SPELL
}

data class Card(
    val id: String = java.util.UUID.randomUUID().toString(), // Unique ID for hand management
    val name: String,
    val cost: Int,
    val hp: Int? = null,
    val damage: Int? = null,
    val range: Float? = null,
    val attackSpeed: Float? = null, // Attacks per second
    val movementSpeed: Float? = null, // Units per update
    val targetPriority: TargetPriority = TargetPriority.ALL,
    val emoji: String,
    val entityType: CardType = CardType.TROOP,
    val radius: Float? = null,
    val spawnCount: Int = 1,
    val lifetimeSeconds: Int? = null
)

data class Troop(
    override val id: String = java.util.UUID.randomUUID().toString(),
    val card: Card,
    override val owner: Player,
    val maxHp: Int,
    override var hp: Int,
    override var position: Offset,
    var lastAttackTime: Long = 0
) : GameEntity {
    fun canAttack(currentTime: Long): Boolean {
        val attackSpeedMs = (1000 / (card.attackSpeed ?: 1.0f)).toLong()
        return currentTime - lastAttackTime >= attackSpeedMs
    }
}

data class Tower(
    override val id: String = java.util.UUID.randomUUID().toString(),
    override val owner: Player,
    val type: TowerType,
    val maxHp: Int,
    override var hp: Int,
    override val position: Offset,
    val range: Float,
    val damage: Int,
    val attackSpeed: Float, // Attacks per second
    var isActive: Boolean = true,
    var lastAttackTime: Long = 0
) : GameEntity {
    fun canAttack(currentTime: Long): Boolean {
        val attackSpeedMs = (1000 / attackSpeed).toLong()
        return currentTime - lastAttackTime >= attackSpeedMs
    }
}

data class Building(
    override val id: String = java.util.UUID.randomUUID().toString(),
    val card: Card,
    override val owner: Player,
    val maxHp: Int,
    override var hp: Int,
    override val position: Offset,
    var lastAttackTime: Long = 0,
    var lifetimeRemainingMs: Long
) : GameEntity {
    fun canAttack(currentTime: Long): Boolean {
        val attackSpeedMs = (1000 / (card.attackSpeed ?: 1.0f)).toLong()
        return currentTime - lastAttackTime >= attackSpeedMs
    }
}

enum class TowerType { KING, PRINCESS }

// --- EFFECTS ---

sealed class GameEffect(
    val id: String = java.util.UUID.randomUUID().toString(),
    var duration: Long,
    val position: Offset
)

class SpellEffect(
    position: Offset,
    val radius: Float,
    val emoji: String
) : GameEffect(duration = 500L, position = position)

class Projectile(
    position: Offset, // Starting position
    val targetPosition: Offset,
    val color: Color
) : GameEffect(duration = 100L, position = position)

class Emote(
    val emoji: String,
    val towerId: String, // Which tower to appear above
    var duration: Long = 2000L
)