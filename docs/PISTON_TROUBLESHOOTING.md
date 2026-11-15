# Piston and Machine Troubleshooting

This guide records the behaviors and fixes established during piston and simulation-distance testing. For repeatable test layouts, see [Piston Test Scenarios](PISTON_TEST_SCENARIOS.md).

## Start With the Right Log

- A running client or dedicated server writes to its own `logs/latest.log`. For the development client, use `run/logs/latest.log`.
- GameTests run from `runGameTestServer` use `run-gametest/logs/latest.log`. That server has a separate world and does not test or modify the open client world.
- Check timestamps and coordinates before interpreting a message. Old entries can make a new test look like it just ran.
- `[MACHINE TICK]` means the machine ticker ran at that time. If those entries stop after leaving, the machine chunk is not being ticked. The mod does not add a chunk loader.
- `[IO MODULE] tickInputs` or `tickOutputs` means the IO face was evaluated. It does not mean a transfer happened.

## IO Does Not Transfer at a Distance

The machine must be ticking for its modules to run. In addition, the adjacent inventory chunk must be ticking before IO looks up its item-handler capability. A chunk can be loaded without being in the active block-ticking area.

For output, look for:

```text
[IO PUSH DIAGNOSTIC] ... targetChunkLoaded=..., targetChunkTicking=...
[IO PUSH] ... chunkTicking=false — skipping capability lookup
```

For input, look for:

```text
[IO PULL] ... chunkTicking=false — skipping capability lookup
```

When `chunkTicking=false`, no capability lookup or transfer should occur. Item counts should stay unchanged. When the player returns and the target starts ticking, IO can resume.

`/execute if loaded ...` and `/execute unless loaded ...` only report whether the chunk is loaded. They do not report whether it is ticking. Use the periodic push diagnostic or the skip/transfer log entries to establish IO ticking behavior. Make sure the IO module has an adjacent Inventory Module in the machine grid and that the face is configured for the intended direction.

## A Piston Does Not Move a Machine Into an Unloaded Destination

When the move list includes a machine, the piston code checks required destination chunks before changing blocks or detaching the machine's block entity data. If a destination or a block in the destruction list is unavailable, expect a `moveBlocks ABORT` entry. The move should leave the machine and its data in place.

Walking just beyond simulation distance is not a reliable way to unload an adjacent chunk. The player ticket can keep neighboring chunks loaded. The `/execute ... if loaded` commands are useful for checking loaded state, but the controlled unavailable-destination and destruction cases are covered by GameTests using a development fixture.

## Machine Contents Appear Lost After a Sticky Retraction

This was a real bug found during testing. The piston mixin initially resolved the pull before accounting for the piston head state used by vanilla. The custom path could fail to see a side-attached machine, after which vanilla moved the structure without the mod's data-preservation path.

The fix resolves retraction against the head-free state, captures every machine block entity included in the resolved structure, and restores its saved data when the moving piston finishes. If the resolver rejects a sticky network attached to a machine, the mixin prevents an unprotected fallback move.

For a valid side-branch pull, the log should show:

1. `Sticky retract ... resolverPositions=[...]` containing the machine position.
2. `Captured MachineFrameworkBlockEntity` with module and inventory slots occupied.
3. `moveBlocks COMPLETE`.
4. `RESTORED machine` at the pulled-to position with module and inventory slots occupied.

The GameTest suite covers a machine attached to a two-block slime chain and one attached to honey. Manual tests also covered two machines branching from one slime chain, repeated piston moves, and a mixed slime/honey arrangement. Open the machine in-game to verify the actual stacks; the log reports occupied slot counts, not item names or counts.

## A Slime or Honey Piston Does Not Move the Expected Structure

The piston resolver counts **all moved blocks**, including sticky blocks and blocks attached to their sides. The maximum is 12 blocks total, not 12 blocks of distance. A 13th non-destroyable block causes the move to be rejected. Destroyable blocks may be destroyed according to vanilla piston rules; see Test 16 for that separate case.

Slime and honey use vanilla adhesion rules. They can pull or push eligible side branches, but slime does not adhere to honey. A block in the main push line can still be pushed forward even when two adjacent sticky blocks do not adhere to each other. On retraction, only the sticky group attached to the piston may come back, so a mixed line can separate.

Keep sticky structures clear of the floor and nearby blocks while counting. Slime can attach to eligible blocks on its sides, including floor blocks. Those blocks may increase the resolved count or make the structure invalid. If the move fails, check the resolver's positions and count the whole structure, including side branches.

## A Quasi-Connected Piston Looks Unpowered

Java pistons can detect power as if the position directly above the piston base were another piston. A redstone source two blocks above the base can therefore activate it, even without a direct redstone connection beside the piston. Sometimes the power change does not notify the piston immediately. Place and break a temporary neighboring block to trigger an update.

This is redstone behavior, not chunk loading. A sticky piston can use the same behavior for both extension and retraction. Check machine data after both movements.

## Useful Piston Log Patterns

| Log pattern | Meaning |
|---|---|
| `Resolver result: resolved=true` | Vanilla accepted a move list. Check `blocksToPush` and the following positions to see what is included. |
| `moveBlocks COMPLETE` | The custom move finished. Wait for the moving-block animation and restoration to finish before checking the machine. |
| `RESTORED machine` | Saved machine data was applied to the block entity at the destination. Verify the actual items in-game too. |
| `moveBlocks ABORT: Destination chunk is NOT loaded` | The custom preflight rejected an unavailable destination before mutating the move. |
| `moveBlocks ABORT: Destruction chunk is NOT loaded` | The custom preflight rejected a move whose destroy-list position was unavailable. |
| `FAILED: Could not restore` | A data restoration failure. Save the surrounding log lines and coordinates for investigation. |
| `moveBlocks VANILLA` | The resolved move list contains no machine, so vanilla handles the move. |

## Test Results Recorded on 2026-11-12

- IO skips capability lookup when an adjacent inventory chunk is not ticking. The machine's own ticker stops when its chunk stops ticking.
- All five required GameTests passed: unavailable extension destination, unavailable destruction position, unavailable sticky-retraction destination, slime-chain side branch, and honey side branch.
- Manual tests passed for multiple machines on a slime chain, the 12-block slime limit, a mixed slime/honey line, repeated piston cycling or advancement, and quasi-connectivity power. Machine contents were checked in-game after movement.
- The original side-branch retraction test exposed data loss. After the resolver fix, the player's log showed the machine in the retraction move list and its module and inventory slots occupied before capture and after restoration.

These tests cover the interactions exercised so far. They do not prove every possible combination of redstone wiring, block layout, or other mods.
