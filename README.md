# 🏆 Willy Money SMP — Official Tournament Guide & Rulebook

Welcome to **Willy Money SMP**, a high-stakes, economy-driven Minecraft tournament engine built for **Paper 1.21+** (compatible with modern Paper / Purpur servers).

---

## 📌 Table of Contents
1. [Tournament Overview](#-tournament-overview)
2. [The Two Currencies](#-the-two-currencies)
3. [Tournament Phases](#-tournament-phases)
4. [Player Tiers & Drafting](#-player-tiers--drafting)
5. [Teams, Elimination & Buyback](#-teams-elimination--buyback)
6. [Banned Items & Movement Rules](#-banned-items--movement-rules)
7. [Live Tournament Events](#-live-tournament-events)
8. [Team Tracker Compass](#-team-tracker-compass)
9. [Admin Control Panel & System Reset](#-admin-control-panel--system-reset)
10. [Complete Commands Reference](#-complete-commands-reference)

---

## 🎮 Tournament Overview
In **Willy Money SMP**, players compete in teams led by designated Captains. The tournament progresses through four structured phases:
- **Drafting**: Captains spend Draft Credits in live 30-second auctions (bids go up 5 at a time; highest bid when the clock runs out wins) to build their rosters.
- **Resource Gathering**: A Grace Period where PvP is strictly disabled so teams can mine and gear up.
- **Combat & Eliminations**: Full PvP enabled. Deaths trigger permadeath into Spectator mode with lightning strikes and global sound effects.
- **Finale**: The ultimate endgame showdown where surviving teams fight for the championship.

---

## 🪙 The Two Currencies

| Currency | Purpose | How to Earn / Obtain |
| :--- | :--- | :--- |
| **Draft Credits** | Used **strictly by Team Captains** during the Draft Phase auction to bid on players. | Set at the start (`300` default) or granted by Admins via `/credits give <player> <amt> draft`. |
| **Game Credits** | Used to rank teams on the leaderboard (`/leaderboard`). | Earned from **Player Kills**, winning **King of the Hill (KOTH)**, opening **Airdrops**, and placing in **Bingo**. |

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
- **Respawn Mechanics**: Normal respawning is active (no permadeath). Players keep playing in survival upon death.
- **Kill Credit Rewards**:
  - **Captain Kill**: `+25 Game Credits`
  - **Tier A Kill**: `+15 Game Credits`
  - **Tier B Kill**: `+10 Game Credits`
  - **Tier C Kill**: `+10 Game Credits`

### 4. Finale Showdown: Free Fall (`/freefall start`)
- **PvP Status**: High-stakes endgame combat.
- **Free Fall**: The world border shrinks in (default 1000 → 50 blocks over 20 minutes), forcing every team into the middle. Being outside the border hurts. **Permadeath:** die and you are out for good (spectator). A team stays in while any player is alive; when its last player is knocked out it locks in its place (first out = last place). The last team standing wins. `/freefall standings` shows the live placings, `/freefall out <player>` eliminates an inactive player, and `/freefall end` ends it early (teams still standing are ranked by credits).
- **Admin**: `/freefall center` (stand where the middle should be), `/freefall start [minutes] [finalSize]`, `/freefall status`, `/freefall stop`, `/freefall standings`, `/freefall out <player>`, `/freefall end` (ends early), `/freefall reset`.

---

## 🎖️ Player Tiers & Drafting

Players are classified into four skill tiers:
- **Captain**: Team leaders who hold draft funds and lead their squad.
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
   /bid
   ```
   Every bid is exactly 5 more than the last (5, 10, 15, 20...), so nobody can blow their whole purse
   on one player. The 30-second clock counts down without resetting; the top bid at 0 wins. Settings: `draft:` in `config.yml`.
4. When the timer expires, the highest bidder automatically wins the player, adds them to their team roster, and deducts the Draft Credits.

---

## 🛡️ Teams & Combat

- **Friendly Fire Block**: Players on the same team cannot damage each other.
- **Team Chat**: Send private team-only messages using `/tc <message>` (or `/teamchat`).
- **Respawn System**: No permadeath. When players die in PvP, they respawn normally at their bed or world spawn.

---

## 🚫 Banned Items & Movement Rules

To ensure competitive tournament balance and eliminate instant-escape teleportation:

### 1. Restricted Equipment
- **Ender Pearls**: Teleportation projectile is disabled. Throwing or right-clicking is canceled and pearl is removed immediately.
- **Wind Charges**: Burst jumping is disabled. Throwing or launching is canceled and projectiles are removed.
- **Player Feedback**: Blocked actions trigger an alert sound (`ENTITY_VILLAGER_NO`) and a red **Action Bar alert**:
  `✖ <Item> is BANNED in tournament!`.

### 2. Fair Movement Standards
- Combat mobility is grounded in sprint mechanics, terrain control, and PvP skill.
- Teleportation and burst launchers are strictly prohibited.

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
- **Duration**: 5 minutes (`koth.duration-seconds` in config.yml).
- **KOTH Sword**: every team player gets a Knockback II wooden sword that breaks after about 5 hits.
- **Mechanism**: A 5-block particle flame ring spawns with a floating holographic countdown timer.
- **Capturing**: Teams standing inside the ring capture the hill. If multiple teams are inside, it becomes `CONTESTED`.
- **Rewards** (most seconds holding the hill): **1st `+125`**, **2nd `+100`**, **3rd `+75`**, **4th `+50`**, **5th `+25` Game Credits**, paid to each team's Captain.

### 2. Proximity Airdrops
- **Summon**: Click Airdrop chest in `/smp` GUI or `/airdrop random <tier>`.
- **Landing**: Drops 50–130 blocks away from an active player. Descends from the sky and leaves a smoke flare signal upon landing.
- **Chests & Loot Pools**:
  - **Common Airdrop**: Random survival gear (iron, bread, arrows, golden apples, shields) + filler loot (**0 Game Credits** / Loot only).
  - **Rare Airdrop**: Diamonds, golden apples, obsidian, bows, enchanted books + **`+5 Game Credits`**.
  - **Legendary Airdrop**: Totem of Undying, netherite scraps, enchanted apples, diamonds + **`+15 Game Credits`**.

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
    - **Slot 35 (Placement Rewards)**: Gold Ingot displaying tiered payouts (1st: 150, 2nd: 120, 3rd: 100, 4th: 50, 5th: 25).
    - **Slot 53 (Close Button)**: Barrier button to exit the menu.
- **Task Tracking**:
  - Auto-tracking covers all 25 survival objectives (e.g., *"Obtain Heavy Core"*, *"75-Block Water Clutch"*, *"Mine 64 Coal"*).
- **Lockout Rule**: The first team to complete a task claims it permanently in their team color—other teams are locked out.
- **Rewards** (paid when an admin ends bingo with `/bingo stop`, ranked by **most squares claimed**):
  - **1st**: `+150` · **2nd**: `+120` · **3rd**: `+100` · **4th**: `+50` · **5th**: `+25` Game Credits to the Captain.
  - Completing a line is announced, but the rewards only go out at the end.

---

## 🧭 Team Tracker Compass (`/tracker`)

- **Tier A, B & C players**: Right-click to point the compass at your nearest teammate.
- **Captains only**: Right-click to switch between your nearest teammate and the nearest **enemy Captain**.

**Team colours**: Every player's name above their head is shown in their team's colour (e.g. TeamBlue = blue), so you can tell teams apart at a glance. The colour comes from the team name: red, blue, green, yellow, purple, gold/orange or aqua/cyan.

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
  Slot 20: [Ender Chest]      Summon Rare Airdrop
  Slot 21: [Trapped Chest]    Summon Legendary Airdrop
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
  - Credits reset to starting defaults (`game: 100`, `draft: 300`).
  - Tournament phase reset to `Draft`.
  - All ongoing events stopped.
  - `data.yml` wiped fresh.

---

## ⌨️ Complete Commands Reference

### Player Commands
| Command | Aliases | Description |
| :--- | :--- | :--- |
| `/banneditems` | `/banned`, `/banlist`, `/bannedlist` | Open the Banned Items GUI and view restrictions. |
| `/team list` | — | View all active teams and rosters. |
| `/team info [team]` | — | Inspect a team's members, captain, and elimination status. |
| `/tc <message>` | `/teamchat` | Send a private message to your teammates. |
| `/tracker` | — | Receive a Team Tracker compass. |
| `/bingo` | — | Open the interactive centered 5x5 Lockout Bingo GUI. |
| `/leaderboard` | `/top` | View top teams ranked by credit balances. |
| `/website` | `/site`, `/web` | Clickable link to the live standings website. |

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
| `/freefall start [minutes] [size]` | — | Finale: shrink the world border (also `center`, `status`, `stop`, `standings`, `out`, `end`, `reset`). |
| `/airdrop <common\|rare\|legendary>` | — | Spawn an airdrop at your position. |
| `/airdrop random <tier>` | — | Spawn an airdrop 50–130m near an active player. |
| `/bingo start` / `/bingo stop` | — | Start or stop the Lockout Bingo event. |
| `/announce <message>` | `/broadcast`, `/alert` | Global server announcement with sound. |
| `/titleannounce <title>` | — | Fullscreen screen title alert for all players. |

## Live website sync

The plugin pushes the standings, draft auction and draft pool to the website
(https://github.com/Mighty-Skull-1/WillyMoneySMPWeb) through the public ntfy.sh relay.

- Updates go out whenever something changes (checked every 2 seconds), plus a heartbeat every minute.
- Every update is signed with `plugins/WillyMoneySMP/web-signing-key.properties`. The website only accepts
  updates that match the public key in its `src/config.ts`, so nobody can post fake standings.
  **Never share or commit that file.**
- If the key file is missing the plugin creates a new one and prints its public key in the console;
  copy it into the website's `SIGNING_PUBLIC_KEY` (or upload the key file that matches the site).
- `/smp sync` pushes an update immediately, `/smp webkey` shows the public key.
- `/smp setip <address>` changes the server address shown on the website (saved as `server-address` in `config.yml`).
- Settings live under `web-sync:` in `config.yml` (`ntfy-topic` must match `NTFY_TOPIC` on the website).
