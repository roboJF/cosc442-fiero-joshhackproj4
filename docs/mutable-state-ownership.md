# Mutable-state ownership

JoshHack shares live game objects while keeping their owned collections and
coordinate data behind explicit boundaries.

## Owned data

| Owner | Reader contract | Supported changes |
| --- | --- | --- |
| `Inventory` | `getItems()` returns a stable array snapshot, including empty slots. The `Item` objects retain their identity. | Use `add()` and `remove()`. |
| `Item` | `writtenSpells()` returns an unmodifiable list snapshot. Adding a spell does not change earlier snapshots. | Use `addWrittenSpell()`. |
| `Creature` | `activeEffectCount()` and `hasActiveEffects()` inspect active effects without returning live `Effect` objects. | Casting and consumption start effects; creature updates advance and end them. |
| `Line` | `getPoints()` and iteration return unmodifiable sequences containing copied `Point` objects. Iterator removal is unsupported. | A line's coordinates are established by its constructor. |
| `Path` | `points()` returns an unmodifiable sequence containing copied `Point` objects. An unavailable path remains `null`; an already reached destination produces an empty list. | Construct a new path to calculate another route. |
| `World` | Construction copies every array level of the supplied tile map. `Tile` enum values are shared. | Use world operations such as `dig()`; later changes to the input map have no effect. |

`Point` remains mutable for callers, but changing a point obtained from a line or
path cannot change the owner's coordinates or another snapshot. Map fixtures
must finish configuring their tile arrays before constructing a world.

## Intentional sharing

These references represent live game relationships and are retained:

- Creatures, AI controllers, factories, and field-of-view objects share the same
  `World` so they observe movement, digging, items, and other world updates.
- Screens and AI controllers retain the actual player or creature they control.
  Copying those objects would disconnect actions from the running game.
- A creature's inventory, equipped weapon, and armor refer to the same game
  objects. Inventory snapshots protect the slots while preserving item identity.
- `PlayerAi` writes to the message list that `PlayScreen` displays and clears.
  This list is an intentional communication channel, not an owned input snapshot.
- Creature AI references, fungus factories, and selected items are shared because
  their current game state is needed by their users.

SpotBugs can flag these retained references as `EI_EXPOSE_REP` or
`EI_EXPOSE_REP2`. They remain visible in the reports: this change adds no broad
or narrow exclusions. Future callers should respect the documented ownership
contracts rather than suppressing warnings or copying live game objects simply
to lower the warning count.
