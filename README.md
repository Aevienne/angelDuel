# AngelDuel

A Paper 1.21.1 Minecraft plugin implementing the **Duelist's Code** — a formal, honor-based PvP duel system with zone lockdown, third-party interference blocking, honor token trophies, and a persistent leaderboard.

---

## Requirements

| Requirement | Version |
|---|---|
| Minecraft Server | Paper 1.21.1 |
| Java | 21+ |
| Build Tool | Gradle 8.8 |
| API Version | 1.21 |

---

## Building

```bash
./gradlew jar
```

Output: `build/libs/AngelDuel-1.0.0.jar`

Drop the jar into your server's `plugins/` folder and restart.

---

## Commands

| Command | Aliases | Description | Permission |
|---|---|---|---|
| `/challenge <player>` | `/duel` | Challenge another player to a duel | `angelduel.challenge` |
| `/duelaccept` | — | Accept a pending duel challenge | `angelduel.challenge` |
| `/dueldecline` | — | Decline a pending duel challenge | `angelduel.challenge` |
| `/duelleaderboard` | `/duellb`, `/honorboard` | View the top 10 honor leaderboard | `angelduel.leaderboard` |

All permissions default to `true` (all players).

---

## How It Works

### 1. Challenge Flow

```
/challenge <player>
    └── Target has 30s to /duelaccept or /dueldecline
            └── On accept: 5s countdown → DUEL ACTIVE
```

### 2. Duel Zone

On accept, a **15-block radius zone** is established around the midpoint between the two players. During an active duel:

- Both players are **locked inside** the zone — fleeing causes an automatic forfeit.
- **Third parties cannot** deal damage to, or interact with, either duelist.
- Environmental damage is blocked during the countdown phase.

### 3. Winning & Honor Tokens

The duel ends when:
- One player dies → opponent wins.
- One player disconnects → opponent wins.
- One player exits the duel zone → that player forfeits.

The **winner receives an Honor Token** (Gold Nugget) with custom lore identifying the defeated player. This token serves as a permanent trophy.

```
§6§lHonor Token
§7Taken from <loser>
§7A trophy of victory.
§8[AngelDuel Honor Trophy]
```

### 4. State Restoration

After the duel ends, **both players** have their pre-duel state fully restored:
- Inventory and armor contents
- Health and hunger
- Active potion effects
- Teleport back to their original location

### 5. Leaderboard

Win/loss records are saved to `plugins/AngelDuel/leaderboard.yml` and persist across restarts. `/duelleaderboard` displays the top 10 players ranked by wins.

---

## Configuration

**`plugins/AngelDuel/config.yml`**

```yaml
duel:
  zone-radius: 15          # Duel zone radius in blocks
  challenge-timeout: 30    # Seconds before a challenge expires
  countdown: 5             # Countdown seconds before duel starts
  restore-state: true      # Restore inventory/health/effects after duel
  drop-token: true         # Drop token if winner's inventory is full
  token-drop-location: LOSER
```

All messages in the `messages:` section support `&` color codes.

---

## Project Structure

```
src/main/java/me/angelique/angelDuel/
├── AngelDuel.java                        # Main plugin class
├── commands/
│   ├── ChallengeCommand.java             # /challenge
│   ├── DuelAcceptCommand.java            # /duelaccept
│   ├── DuelDeclineCommand.java           # /dueldecline
│   └── DuelLeaderboardCommand.java       # /duelleaderboard
├── listeners/
│   ├── DuelCombatListener.java           # Death, damage, disconnect handling
│   └── DuelZoneListener.java             # Zone enforcement, interaction blocking
├── managers/
│   ├── DuelManager.java                  # Core duel lifecycle logic
│   └── LeaderboardManager.java           # YAML-backed win/loss persistence
└── models/
    ├── DuelSession.java                  # Per-duel state and player snapshots
    └── DuelState.java                    # PENDING / COUNTDOWN / ACTIVE / ENDED
```

---

## Data Files

| File | Purpose |
|---|---|
| `config.yml` | Tunable settings and all messages |
| `leaderboard.yml` | Auto-saved win/loss records per UUID |

---

## Main Class

```
me.angelique.angelDuel.AngelDuel
```
