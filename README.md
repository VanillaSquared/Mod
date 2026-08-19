<h1 align="center">
  <br>
  Vanilla²
  <br>
</h1>

<h4 align="center">This Mod overhauls many of Minecrafts outdated system, like the combat system and enchanting system. On top of that the mod also adds a few other features.</h4>

<p align="center">
  <img alt="Minecraft" src="https://img.shields.io/badge/Minecraft-26.2-brightgreen?style=flat-square">
  <img alt="Fabric Loader" src="https://img.shields.io/badge/Fabric%20Loader-%3E%3D0.19.3-orange?style=flat-square">
  <img alt="Fabric API" src="https://img.shields.io/badge/Fabric%20API-0.154.2%2B26.2-blue?style=flat-square">
  <img alt="Java" src="https://img.shields.io/badge/Java-%3E%3D25-red?style=flat-square">
</p>

### [Our official website](https://vanillasquared.up.railway.app)
### [Official Bluesky](https://bsky.app/profile/painterflow11.bsky.social)
### [Join our Discord](https://discord.gg/NrPZQbwc8Y)
## Overview

Vanilla² focuses on adding more Vanilla-style content to the game. Currently vsq overhauls the enchanting system and adds new enchantments. There are also a few other minor features like Sulfur Goo and a rebalance of most weapons, tools and armor.

## Features

- Rebalances sword, axe, spear, trident, mace, shield, fishing rod, armor, tool, and weapon behavior.
- Expands armor, protection, magic protection, mace protection, spear protection, and dripstone protection handling beyond vanilla limits.
- Updates combat damage calculations so higher armor and absorb values continue to scale correctly.
- Adds new attributes.
- Changes item durability and combat stats by material for tools, weapons, armor, and fishing rods.
- Changes potion stack sizes.
- Lets fishing rods work as combat tools with hook damage and enchantment integration.
- Lets sword targeting pass through configured blocks such as grass and flowers.
- Improves offhand interaction priority for fishing rod combat cases.

## Enchanting overhaul

Vanilla² replaces much of vanilla enchanting with a recipe-based enchantment table and an enchantment recipe book.

- Adds enchanting recipes for vanilla and Vanilla² enchantments.
- Adds enchantment recipe discovery through loot, fishing, piglin bartering, villager librarians, and structure chests.
- Adds an enchanting recipe book UI and server/client synchronization for known recipes and selected recipes.
- Adds enchantment slot categories such as Special, Damage, Secondary, Defense, Utility, and Curse.
- Supports enchantment profiles so an enchantment can have different behavior depending on the selected slot/profile.
- Rebalances almost all vanilla enchantments(+channeling got a rework).

## Enchantments

Vanilla² changes many vanilla enchantments and adds new ones.

### New enchantments

- **Dash** - burst forward and dash through enemies in your path.
- **Ruthless** - greatly increases attack damage but with each hit you also take damage.
- **Swirling** - spin with your weapon and repeatedly strike nearby enemies (AOE effect).
- **Void Strike** - applies the Voided effect to targets which multiplies the damage they get over time.

## Requirements

- Minecraft 26.2.
- Fabric Loader 0.19.3 or newer.
- Fabric API 0.154.2+26.2 or compatible.
- Java 25 or newer.

## Installation

1. Download the latest Vanilla² jar from the releases page.
2. Install Fabric Loader and Fabric API for the supported Minecraft version.
3. Place the Vanilla² `.jar` file in your `mods` folder.
4. Restart/Launch Minecraft.

## Compatibility notes

- Known to be incompatible with ViaFabricPlus.
- Probably some other mods, I did not yet focus mod compatibility.

## Development

Build the project with the included Gradle wrapper.

```sh
./gradlew build
```

Run a development client with the mod loaded.

```sh
./gradlew runClient
```

Build artifacts are written to `build/libs/`.

## Notes
[pxlarified](https://github.com/pxlarified) is not an active developer of this project. While I occasionally contribute to the website and mod, most of the work and ideas come from PainterFlow. He is responsible for the majority of the concepts and development associated with the project, and I do not want to misrepresent that by claiming ownership or implying that most of the work is mine.
