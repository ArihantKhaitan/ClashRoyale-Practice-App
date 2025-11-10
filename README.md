# Clash Royale Clone

A simple clone of the popular mobile game Clash Royale, built with Jetpack Compose for Android.

## 🚀 Features

*   **Real-time Elixir generation:** Watch your elixir bar fill up, just like in the real game.
*   **Card Deck:** A deck of cards with different elixir costs.
*   **Drag-and-Drop Card Gameplay:** Select cards from your hand and place them on the battlefield.
*   **Player and Enemy Towers:** Defend your towers and try to take down your opponent's.
*   **Game Logic:** Basic game loop, win/loss conditions, and a timer.
*   **Emotes:** Express yourself with a selection of emotes.
*   **Multiple Game States:** Includes a main menu, difficulty selection, active gameplay, pause menu, and a game over screen.

## 🤔 How It Works

The project follows a modern, state-driven architecture using Jetpack Compose and a `ViewModel`.

### `MainActivity.kt`

This is the main entry point of the application. Its primary responsibilities are:

1.  **Hosting the UI:** It sets up the main `GameScreen` composable.
2.  **ViewModel Initialization:** It creates and holds the `GameViewModel` instance.
3.  **State Observation:** It observes the `gameState` `StateFlow` from the `GameViewModel`. Whenever the game state changes (e.g., elixir increases, a troop moves), the UI automatically recomposes to reflect the new state.
4.  **Event Handling:** It captures user input events (like playing a card or pausing the game) and forwards them to the `GameViewModel` for processing.

### `GameViewModel.kt`

This is the brain of the game. It contains all the game logic and manages the game's state.

*   **State Management:** It holds the entire game state in a `MutableStateFlow<GameState>`. This `GameState` is a data class containing everything about the current game: player info, troop positions, tower health, the game timer, etc.
*   **Game Loop:** The `startGameLoop()` function launches several coroutines that run concurrently:
    *   A **timer** that counts down the game clock.
    *   An **elixir generator** that periodically increases the elixir for both players.
    *   A **main game tick** loop (`updateGame()`) that updates troop positions, handles attacks, checks for deaths, and other core game mechanics.
    *   An **enemy AI** that decides when and where the opponent should play a card.
*   **Business Logic:** It contains all the functions that modify the game state, such as `onCardPlayed()`, `togglePause()`, `startGame()`, and the logic for checking win/loss conditions.

### `GameState.kt` and `GameData.kt`

These files define the data structures for the entire game. They contain the `data class` definitions for `GameState`, `Player`, `Card`, `Troop`, `Tower`, and other game entities. This ensures a single, immutable source of truth for the UI and game logic.

## 🛠️ How to Build and Run

1.  Clone the repository:
    ```bash
    git clone <your-repository-url>
    ```
2.  Open the project in Android Studio.
3.  Let Gradle sync and download the required dependencies.
4.  Run the app on an Android emulator or a physical device.

## 💻 Tech Stack

*   **[Kotlin](https://kotlinlang.org/):** The primary programming language for Android development.
*   **[Jetpack Compose](https://developer.android.com/jetpack/compose):** Android's modern toolkit for building native UI.
*   **[ViewModel](https://developer.android.com/topic/libraries/architecture/viewmodel):** To manage UI-related data in a lifecycle-conscious way.
*   **[StateFlow](https://developer.android.com/kotlin/flow/stateflow-and-sharedflow):** For managing and observing game state.
*   **[Coroutines](https://kotlinlang.org/docs/coroutines-overview.html):** For managing background threads and asynchronous operations like the game loop.