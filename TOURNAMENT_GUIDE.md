# 🏆 Willy Money SMP — Official Tournament Guide & Rulebook

Welcome to **Willy Money SMP**, a high-stakes, economy-driven Minecraft tournament engine built for Paper 1.20.4.

---

## 📌 Table of Contents
1. [Tournament Overview](#-tournament-overview)
2. [The Two Currencies](#-the-two-currencies)
3. [Tournament Phases](#-tournament-phases)
4. [Player Tiers & Drafting](#-player-tiers--drafting)
5. [Teams, Elimination & Buyback](#-teams-elimination--buyback)
6. [The In-Game Credit Shop](#-the-in-game-credit-shop)
7. [Live Tournament Events](#-live-tournament-events)
8. [Team Tracker Compass](#-team-tracker-compass)
9. [Admin Control Panel & System Reset](#-admin-control-panel--system-reset)
10. [Complete Commands Reference](#-complete-commands-reference)

---

## 🎮 Tournament Overview
In **Willy Money SMP**, players compete in teams led by designated Captains. The game flows through four distinct phases:
- **Drafting**: Captains spend Draft Credits in live auctions to buy players.
- **Resource Gathering**: A Grace Period where PvP is disabled to allow teams to prepare.
- **Combat & Eliminations**: PvP is activated. Death triggers permadeath into Spectator mode.
- **Finale**: The endgame showdown where surviving teams fight for the championship.

---

## 🪙 The Two Currencies

| Currency | Purpose | How to Earn / Obtain |
| :--- | :--- | :--- |
| **Draft Credits** | Used **strictly by Team Captains** during the Draft Phase auction to bid on players. | Set at the start (`400` default) or granted by Admins via `/credits give <player> <amt> draft`. |
| **Game Credits** | Used in the `/shop` to buy combat gear, potions, and to **revive eliminated teammates** (`/team revive`). | Earned from **Player Kills**, winning **King of the Hill (KOTH)**, claiming **Airdrops**, and completing **Bingo Lines**. |

---

## ⏳ Tournament Phases

The global game phase is controlled by server admins via `/smp phase <phase>` or via the interactive GUI (`/smp`).

### 1. Draft Phase (`/smp phase draft`)
- **PvP Status**: Disabled.
- **Gameplay**: Captains participate in live 30-second player auctions. Admins nominate players, and captains bid using Draft Credits.

### 2. Grace Period (`/smp phase grace`)
- **PvP Status**: Strict block on all PvP damage and friendly fire.
- **Gameplay**: Teams explore the world, mine ores, gather food, enchant armor, craft tools, and establish bases.

### 3. PvP Enabled (`/smp phase pvp`)
- **PvP Status**: Full player combat enabled. Friendly fire remains blocked between teammates.
- **Permadeath & Elimination**:
  - Dying during this phase strikes lightning on the player's death spot, sounds a global Wither chime, and forces the player into **Spectator Mode**.
  - Elimination kill rewards:
    - **Captain Elimination**: `+25 Game Credits`
    - **Tier A Elimination**: `+15 Game Credits`
    - **Tier B Elimination**: `+10 Game Credits`
    - **Tier C Elimination**: `+10 Game Credits`

### 4. Finale Showdown (`/smp phase finale`)
- **PvP Status**: High-stakes endgame combat.
- **Gameplay**: Teams converge with full gear, shop potions, and remaining members for the final victory.

---

## 🎖️ Player Tiers & Drafting

Players are classified into four skill tiers:
- **Captain**: Team leaders who hold draft funds and execute revives.
- **Tier A**: Elite PvP and tactical players.
- **Tier B**: Balanced combat and resource gatherers.
- **Tier C**: Support, miners, and builders.

### Running a Draft Auction:
1. Admin nominates an unassigned player:
   ```
   /bid nominate <player>
   ```
2. A 30-second live auction begins with action-bar countdowns.
3. Captains bid by typing:
   ```
   /bid <amount>
   ```
4. When the timer expires, the highest bidder automatically wins the player, adds them to their team roster, and deducts the Draft Credits.

---

## 🛡️ Teams, Elimination & Buyback

- **Friendly Fire**: Players on the same team cannot hurt each other.
- **Team Chat**: Send private team-only messages using `/tc <message>`.
- **Spectator Upon Death**: Eliminated players respawn as Spectators and can watch the tournament live.
- **Captain Revive / Buyback**:
  - Captains can bring eliminated teammates back into the game by typing:
    ```
    /team revive <player>
    ```
  - **Cost**: `150 Game Credits` (deducted from the Captain).
  - **Effect**: The revived player returns to **Survival mode**, heals, and teleports directly to their Captain's location!

---

## 🛍️ The In-Game Credit Shop (`/shop`)

Any player can type `/shop` at any time to open the 27-slot chest GUI.

| Item | Cost | Details |
| :--- | :--- | :--- |
| **Golden Apple (x2)** | 15 Credits | Instant golden apple absorption & regeneration. |
| **Enchanted Golden Apple** | 75 Credits | High-tier defense & resistance. |
| **Totem of Undying** | 85 Credits | Second-chance death prevention. |
| **Ender Pearl (x4)** | 20 Credits | Fast teleportation & mobility. |
| **Team Tracker Compass** | 25 Credits | Radar compass to track allies or enemy captains. |
| **Splash Potion of Healing II** | 25 Credits | Instant health splash potion. |
| **Potion of Swiftness II** | 20 Credits | High-speed maneuverability. |
| **Potion of Strength** | 35 Credits | Increased melee damage. |
| **Arrows (x32)** | 10 Credits | Standard bow/crossbow ammunition. |
| **Diamonds (x4)** | 30 Credits | Diamond resource bundle. |
| **Firework Rockets (x16)** | 15 Credits | Elytra flight fuel. |

---

## 🎯 Live Tournament Events

### 1. King of the Hill (KOTH)
- **Start**: `/koth start` (or click Beacon in `/smp` GUI).
- **Duration**: 180 seconds.
- **Mechanism**: A 5-block particle flame ring spawns with a floating holographic countdown timer.
- **Capturing**: Teams standing inside the ring capture the hill. If multiple teams are inside, it becomes `CONTESTED`.
- **Reward**: The team with the highest capture time wins **`+50 Game Credits`** awarded to the Captain.

### 2. Proximity Airdrops
- **Summon**: Click Airdrop chest in `/smp` GUI or `/airdrop random <tier>`.
- **Landing**: Drops 50–130 blocks away from an active player. Descends from the sky and leaves a smoke flare signal upon landing.
- **Chests & Loot Pools**:
  - **Common Airdrop**: Random survival gear (iron, bread, arrows, golden apples, shields) + filler loot + **`0 Game Credits` (Loot only)**.
  - **Rare Airdrop**: Nerfed diamonds, golden apples, pearls, obsidian + **`5 Game Credits`**.
  - **Legendary Airdrop**: Totem of Undying, netherite scraps, enchanted apples, diamonds + **`15 Game Credits`**.

### 3. Lockout Bingo (5x5 Board)
- **Start**: `/bingo start` (or click Map in `/smp` GUI).
- **View Board**: `/bingo` opens a 54-slot GUI showing the 25 tasks.
- **Lockout Rule**: The first team to complete a task claims it permanently in their team color—other teams are locked out from that task.
- **Reward**: Completing any 5-in-a-row line (horizontal, vertical, diagonal) awards **`+150 Game Credits`** to the team Captain.

---

## 🧭 Team Tracker Compass (`/tracker`)

- **Right-Click**: Switches tracking modes in your hand:
  - **Teammate Tracking (Green)**: Compass needle points toward the nearest living teammate.
  - **Enemy Captain Tracking (Red)**: Compass needle points toward the nearest enemy team Captain.

---

## 🛠️ Admin Control Panel & System Reset

Type **`/smp`** to open the interactive Admin Control Panel:

```
[Row 1 - Tournament Phases & Stats]
  Slot 10: [Gold Ingot]       Draft Phase (Glows when active)
  Slot 11: [Shield]           Grace Period (Glows when active)
  Slot 12: [Diamond Sword]    PvP Phase (Glows when active)
  Slot 13: [Netherite Sword]  Finale Showdown (Glows when active)
  Slot 16: [Nether Star]      Tournament Live Stats Overview

[Row 2 - Events & System Controls]
  Slot 18: [Beacon]           KOTH Toggle (Left: Start, Right: Stop)
  Slot 19: [Chest]            Summon Common Airdrop
  Slot 20: [Ender Chest]      Summon Rare Airdrop
  Slot 21: [Trapped Chest]    Summon Legendary Airdrop
  Slot 22: [Filled Map]       Lockout Bingo Toggle
  Slot 23: [Eye of Ender]     Save Data to data.yml
  Slot 24: [Redstone Torch]   Reload Configs
  Slot 25: [TNT]              RESET TOURNAMENT (Shift + Click to Confirm)
  Slot 26: [Barrier]          Close Menu
```

### Full Tournament Reset
After running tests, you can completely reset everything to the beginning:
- **Command**: `/smp reset confirm`
- **GUI**: **Shift + Click** the TNT (Slot 25) in `/smp`.
- **What gets reset**:
  - All players restored to Survival mode.
  - Health and hunger filled to maximum, potion effects cleared, XP reset to 0.
  - Test inventories and ender chests emptied.
  - All players teleported to world spawn.
  - Eliminations cleared and spectators revived.
  - All teams disbanded and player tiers cleared.
  - Credits reset to starting defaults (`game: 100`, `draft: 400`).
  - Tournament phase reset to `Draft`.
  - All ongoing events stopped.
  - `data.yml` wiped fresh.

---

## ⌨️ Complete Commands Reference

### Player Commands
| Command | Usage |
| :--- | :--- |
| `/shop` | Open the Game Credits store. |
| `/team list` | View all active teams and rosters. |
| `/team info [team]` | Inspect a team's members, captain, and elimination status. |
| `/team revive <player>` | *(Captains only)* Revive an eliminated teammate for 150 credits. |
| `/tc <message>` | Send a private message to your teammates. |
| `/tracker` | Receive a Team Tracker compass. |
| `/bingo` | Open the interactive 5x5 Lockout Bingo GUI. |
| `/leaderboard` | View top teams ranked by credit balances. |

### Admin Commands
| Command | Usage |
| :--- | :--- |
| `/smp` | Open the Admin Control Panel GUI. |
| `/smp phase <draft\|grace\|pvp\|finale>` | Set the tournament phase. |
| `/smp reset confirm` | Perform a complete tournament wipe & reset. |
| `/smp reload` | Reload configuration files and airdrop tables. |
| `/smp save` | Force an immediate write of all data to `data.yml`. |
| `/tier set <player> <A\|B\|C\|CAPTAIN>` | Assign a player's tier. |
| `/tier remove <player>` | Remove a player's assigned tier. |
| `/team create <name> <captain>` | Create a team with a captain. |
| `/team remove <player>` | Remove a player from their team. |
| `/team disband <team>` | Disband an entire team. |
| `/bid nominate <player>` | Put a player up for 30s draft auction. |
| `/bid cancel` | Cancel the active auction. |
| `/credits give <player> <amt> [game\|draft]` | Give credits to a player. |
| `/credits take <player> <amt> [game\|draft]` | Deduct credits from a player. |
| `/credits set <player> <amt> [game\|draft]` | Set a player's credit balance. |
| `/credits balance <player>` | Check credit balances of a player. |
| `/koth start` / `/koth stop` | Start or stop King of the Hill at your position. |
| `/airdrop <common\|rare\|legendary>` | Spawn an airdrop at your position. |
| `/airdrop random <tier>` | Spawn a random airdrop 50–130m near a player. |
| `/bingo start` / `/bingo stop` | Start or stop the Lockout Bingo event. |
| `/announce <message>` | Global server announcement with sound. |
| `/titleannounce <title>` | Fullscreen screen title alert for all players. |
