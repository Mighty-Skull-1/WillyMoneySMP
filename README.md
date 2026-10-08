# 🏆 Willy Money SMP — Official Tournament Guide & Rulebook

Welcome to **Willy Money SMP**, a high-stakes, economy-driven Minecraft tournament engine built for **Paper 1.21+** (compatible with modern Paper / Purpur servers).

---

## 📌 Table of Contents
1. [Tournament Overview](#-tournament-overview)
2. [The Two Currencies](#-the-two-currencies)
3. [Tournament Phases](#-tournament-phases)
4. [Player Tiers & Drafting](#-player-tiers--drafting)
5. [Teams, Elimination & Buyback](#-teams-elimination--buyback)
6. [The In-Game Credit Shop](#-the-in-game-credit-shop)
7. [Banned Items & Mobility Rules](#-banned-items--mobility-rules)
8. [Live Tournament Events](#-live-tournament-events)
9. [Team Tracker Compass](#-team-tracker-compass)
10. [Admin Control Panel & System Reset](#-admin-control-panel--system-reset)
11. [Complete Commands Reference](#-complete-commands-reference)

---

## 🎮 Tournament Overview
In **Willy Money SMP**, players compete in teams led by designated Captains. The tournament progresses through four structured phases:
- **Drafting**: Captains spend Draft Credits in live 30-second auctions to build their rosters.
- **Resource Gathering**: A Grace Period where PvP is strictly disabled so teams can mine and gear up.
- **Combat & Eliminations**: Full PvP enabled. Deaths trigger permadeath into Spectator mode with lightning strikes and global sound effects.
- **Finale**: The ultimate endgame showdown where surviving teams fight for the championship.

---

## 🪙 The Two Currencies

| Currency | Purpose | How to Earn / Obtain |
| :--- | :--- | :--- |
| **Draft Credits** | Used **strictly by Team Captains** during the Draft Phase auction to bid on players. | Set at the start (`400` default) or granted by Admins via `/credits give <player> <amt> draft`. |
| **Game Credits** | Used in the `/shop` to purchase combat gear, potions, and to **revive eliminated teammates** (`/team revive`). | Earned from **Player Kills**, winning **King of the Hill (KOTH)**, opening **Airdrops**, and completing **Bingo Lines**. |

---

## ⏳ Tournament Phases

The global game phase is controlled by server admins via `/smp phase <phase>` or via the interactive GUI (**`/smp`**).

### 1. Draft Phase (`/smp phase draft`)
- **PvP Status**: Disabled.
- **Gameplay**: Captains participate in live 30-second player auctions. Admins nominate players, and captains bid using Draft Credits.

### 2. Grace Period (`/smp phase grace`)
- **PvP Status**: Strict block on all PvP damage and friendly fire.
- **Gameplay**: Teams explore the world, mine ores, gather food, enchant armor, craft tools, and establish bases.

### 3. PvP Enabled (`/smp phase pvp`)
- **PvP Status**: Full player combat enabled. Friendly fire remains blocked between teammates.
- **Permadeath & Elimination**:
  - Dying strikes lightning on the death location, plays a global Wither chime, and moves the player into **Spectator Mode**.
  - Elimination kill rewards:
    - **Captain Elimination**: `+25 Game Credits`
    - **Tier A Elimination**: `+15 Game Credits`
    - **Tier B Elimination**: `+10 Game Credits`
    - **Tier C Elimination**: `+10 Game Credits`

### 4. Finale Showdown (`/smp phase finale`)
- **PvP Status**: High-stakes endgame combat.
- **Gameplay**: Surviving teams converge with full shop gear, potions, and active teammates for the final battle.

---

## 🎖️ Player Tiers & Drafting

Players are classified into four skill tiers:
- **Captain**: Team leaders who hold draft funds and execute revives.
- **Tier A**: Elite PvP and tactical fighters.
- **Tier B**: Balanced combat and resource gatherers.
- **Tier C**: Support players, miners, and builders.

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

- **Friendly Fire Block**: Players on the same team cannot damage each other.
- **Team Chat**: Send private team-only messages using `/tc <message>` (or `/teamchat`).
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

Any player can type `/shop` at any time to open the interactive 27-slot chest GUI.

| Item | Cost | Details |
| :--- | :--- | :--- |
| **Golden Apple (x2)** | 15 Credits | Instant absorption and regeneration. |
| **Enchanted Golden Apple** | 75 Credits | Maximum defense, absorption, and resistance. |
| **Totem of Undying** | 85 Credits | Second-chance death prevention. |
| **Wind Charge (x4)** | 20 Credits | **High-mobility tactical burst** (Replaces banned Ender Pearls). |
| **Team Tracker Compass** | 25 Credits | Radar compass to track allies or enemy captains. |
| **Splash Potion of Healing II** | 25 Credits | Instant splash health recovery. |
| **Potion of Swiftness II** | 20 Credits | High-speed maneuverability. |
| **Potion of Strength** | 35 Credits | Increased melee damage boost. |
| **Arrows (x32)** | 10 Credits | Standard bow and crossbow ammunition. |
| **Diamonds (x4)** | 30 Credits | Diamond resource bundle. |
| **Firework Rockets (x16)** | 15 Credits | Elytra flight fuel. |

---

## 🚫 Banned Items & Mobility Rules

To ensure competitive tournament balance and eliminate instant-escape teleportation:

### 1. Ender Pearls Are Banned
- **Full Usage Lock**: Right-clicking, throwing, placing, crafting, or consuming Ender Pearls is completely blocked.
- **Interception**: Any launched pearls are canceled immediately and removed to prevent teleportation exploits.
- **Player Feedback**: Blocked actions trigger an alert sound (`ENTITY_VILLAGER_NO`), a chat notice, and an **Action Bar warning**:
  `✖ Ender Pearl is BANNED! Use Wind Charges`.

### 2. Approved Mobility: Wind Charges
- **Wind Charges** replace Ender Pearls across the entire tournament.
- Available in the **Credit Shop** (`/shop`), **Rare Airdrops**, and **Legendary Airdrops**.
- Allows skilled vertical rocket-jumping, knockback, and tactical repositioning.

### 3. Interactive Banned Items GUI (`/banneditems`)
- Open the 54-slot menu via **`/banneditems`** (or click **Slot 14** in `/smp`).
- Displays all restricted items, the reasons for their restriction, and recommended legal alternatives.
- **Admin Management**:
  - Click any banned item in the GUI to unban it.
  - Click Slot 48 (`Ban Item In Hand`) while holding an item to ban it instantly.
  - File persistence saved in `plugins/WillyMoneySMP/banned-items.yml`.

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
  - **Common Airdrop**: Random survival gear (iron, bread, arrows, golden apples, shields) + filler loot (**0 Game Credits** / Loot only).
  - **Rare Airdrop**: Diamonds, golden apples, **wind charges**, obsidian + **`+5 Game Credits`**.
  - **Legendary Airdrop**: Totem of Undying, netherite scraps, enchanted apples, diamonds, **wind charges** + **`+15 Game Credits`**.

### 3. Lockout Bingo (5x5 Board)
- **Start**: `/bingo start` (or click Map in `/smp` GUI).
- **View Board**: `/bingo` opens the centered 54-slot GUI.
- **Visual Alignment & Theme**:
  - The 5x5 grid is centered on Columns 2–6 (slots 11–15, 20–24, 29–33, 38–42, 47–51).
  - Symmetrical dark glass border framing (`BLACK_STAINED_GLASS_PANE` & `GRAY_STAINED_GLASS_PANE`).
  - **Live Widgets**:
    - **Slot 4 (Header)**: Race status and event rules.
    - **Slot 17 (Progress Book)**: Viewing team's claim tally (e.g., `4 / 25 Tasks Claimed`).
    - **Slot 26 (Standings Map)**: Real-time leaderboard ranking all teams by claimed tasks.
    - **Slot 35 (Line Rewards)**: Gold Ingot explaining the `+150 Credits` captain payout.
    - **Slot 53 (Close Button)**: Barrier button to exit the menu.
- **Task Tracking**:
  - Tasks like *"Use Wind Charge"* and *"Mine 64 Coal"* auto-complete in real-time.
- **Lockout Rule**: The first team to complete a task claims it permanently in their team color—other teams are locked out.
- **Reward**: Completing any 5-in-a-row line (row, column, diagonal) awards **`+150 Game Credits`** to the Captain.

---

## 🧭 Team Tracker Compass (`/tracker`)

- **Right-Click**: Switches tracking modes in your hand:
  - **Teammate Tracking (Green)**: Compass needle points toward the nearest living teammate.
  - **Enemy Captain Tracking (Red)**: Compass needle points toward the nearest enemy team Captain.

---

## 🛠️ Admin Control Panel & System Reset

Type **`/smp`** to open the interactive Admin Control Panel:

```
[Row 1 - Tournament Phases, Banned Items & Stats]
  Slot 10: [Gold Ingot]       Draft Phase (Glows when active)
  Slot 11: [Shield]           Grace Period (Glows when active)
  Slot 12: [Diamond Sword]    PvP Phase (Glows when active)
  Slot 13: [Netherite Sword]  Finale Showdown (Glows when active)
  Slot 14: [Barrier]          Banned Items Menu (Opens /banneditems GUI)
  Slot 16: [Nether Star]      Tournament Live Stats Overview

[Row 2 - Events & System Controls]
  Slot 18: [Beacon]           KOTH Toggle (Left: Start, Right: Stop)
  Slot 19: [Chest]            Summon Common Airdrop
  Slot 20: [Ender Chest]      Summon Rare Airdrop (With Wind Charges)
  Slot 21: [Trapped Chest]    Summon Legendary Airdrop (With Wind Charges)
  Slot 22: [Filled Map]       Lockout Bingo Toggle
  Slot 23: [Ender Eye]        Save Tournament Data to data.yml
  Slot 24: [Redstone Torch]   Reload Configurations
  Slot 25: [TNT]              RESET TOURNAMENT (Shift-Click to confirm)
  Slot 26: [Barrier]          Close Menu
```

### Full Tournament Reset
After running tests, you can completely reset everything to the beginning:
- **Command**: `/smp reset confirm`
- **GUI**: **Shift + Click** the TNT (Slot 25) in `/smp`.
- **What gets reset**:
  - All players restored to Survival mode.
  - Health and hunger filled to maximum, potion effects cleared, XP reset to 0.
  - Inventories and ender chests emptied.
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
| Command | Aliases | Description |
| :--- | :--- | :--- |
| `/shop` | — | Open the Game Credits store. |
| `/banneditems` | `/banned`, `/banlist`, `/bannedlist` | Open the Banned Items GUI and view restrictions. |
| `/team list` | — | View all active teams and rosters. |
| `/team info [team]` | — | Inspect a team's members, captain, and elimination status. |
| `/team revive <player>` | — | *(Captains only)* Revive an eliminated teammate for 150 credits. |
| `/tc <message>` | `/teamchat` | Send a private message to your teammates. |
| `/tracker` | — | Receive a Team Tracker compass. |
| `/bingo` | — | Open the interactive centered 5x5 Lockout Bingo GUI. |
| `/leaderboard` | `/top` | View top teams ranked by credit balances. |

### Admin Commands
| Command | Usage | Description |
| :--- | :--- | :--- |
| `/smp` | — | Open the Admin Control Panel GUI. |
| `/smp phase <draft\|grace\|pvp\|finale>` | — | Switch the active tournament phase. |
| `/smp reset confirm` | — | Perform a complete tournament wipe & reset. |
| `/smp reload` | — | Reload configuration files and airdrop tables. |
| `/smp save` | — | Force an immediate write of all data to `data.yml`. |
| `/banneditems add <material> [reason]` | — | Ban an item material from the tournament. |
| `/banneditems remove <material>` | `/banneditems unban` | Remove an item from the ban list. |
| `/banneditems hand [reason]` | — | Ban the item currently held in your main hand. |
| `/banneditems list` | — | List all banned items and reasons in chat. |
| `/tier set <player> <A\|B\|C\|CAPTAIN>` | — | Assign a player's tier. |
| `/tier remove <player>` | — | Remove a player's assigned tier. |
| `/team create <name> <captain>` | — | Create a team with a captain. |
| `/team remove <player>` | — | Remove a player from their team. |
| `/team disband <team>` | — | Disband an entire team. |
| `/bid nominate <player>` | — | Put a player up for a 30s draft auction. |
| `/bid cancel` | — | Cancel the active auction. |
| `/credits give <player> <amt> [game\|draft]` | — | Give credits to a player. |
| `/credits take <player> <amt> [game\|draft]` | — | Deduct credits from a player. |
| `/credits set <player> <amt> [game\|draft]` | — | Set a player's credit balance. |
| `/credits balance <player>` | — | Check credit balances of a player. |
| `/koth start` / `/koth stop` | — | Start or stop King of the Hill at your position. |
| `/airdrop <common\|rare\|legendary>` | — | Spawn an airdrop at your position. |
| `/airdrop random <tier>` | — | Spawn an airdrop 50–130m near an active player. |
| `/bingo start` / `/bingo stop` | — | Start or stop the Lockout Bingo event. |
| `/announce <message>` | `/broadcast`, `/alert` | Global server announcement with sound. |
| `/titleannounce <title>` | — | Fullscreen screen title alert for all players. |
