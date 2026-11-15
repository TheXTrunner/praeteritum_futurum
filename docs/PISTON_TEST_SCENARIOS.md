# Piston & Simulation Distance Test Scenarios

This document outlines the edge cases to test for piston mechanics and simulation distance interactions with MachineFrameworkBlock. All tests should be run on the **server side** (singleplayer or dedicated server). Check the server log (`logs/latest.log`) for `[PISTON DEBUG]`, `[IO PULL]`, `[IO PUSH]`, and `[MACHINE TICK]` messages.

For symptoms, log interpretation, and the fixes found during testing, see the [Piston Troubleshooting Guide](PISTON_TROUBLESHOOTING.md).

## What These Tests Verify

- A machine's normal block-entity ticker does work only while Minecraft ticks the machine's chunk. This mod does not add a machine chunk loader.
- IO checks that an adjacent chunk is ticking before querying its inventory capability. A chunk being loaded is not enough; IO skips inventories outside the active block-ticking area.
- A piston move that includes a MachineFrameworkBlock aborts before changing blocks if any required destination or destruction chunk is not loaded.

`hasChunkAt` means **loaded**, not **inside simulation distance**. Player simulation-distance loading can keep a one-chunk border around ticking chunks loaded. Thus moving one chunk past the simulation-distance edge does not guarantee an adjacent chest or piston destination is unloaded. These commands report loaded status only: `/execute if loaded <x> <y> <z> run say TARGET_LOADED` and `/execute unless loaded <x> <y> <z> run say TARGET_UNLOADED`. For IO ticking status, use `[IO PUSH DIAGNOSTIC]` and the skip/transfer messages described in the troubleshooting guide.

### Shared Remote Test Area

Use this area for Tests 9–11; these coordinates are far from the usual world spawn:

- Machine: `(1007, -60, 1000)` — chunk `(62, 62)`.
- Adjacent chest / piston destination: `(1008, -60, 1000)` — chunk `(63, 62)`.
- Player boundary position: about `(920, -59, 1000)` — chunk `(57, 62)`.
- Simulation distance: `5` chunks.

Use `/tp @s 1006 -59 1000` to travel to the setup area and `/tp @s 920 -59 1000` to reach the boundary position (or fly to the same coordinates). Before testing, set `/gamerule spawnChunkRadius 0` if supported, run `/forceload query`, and remove any forced chunks or third-party chunk-loader tickets covering the test area. Do not use a chunk loader during these tests. Note the item counts before moving away. Wait at least 10 seconds after moving so chunk tickets can settle.

---

## How to Read the Logs

| Log Prefix | Meaning |
|---|---|
| `[PISTON DEBUG] ===== moveBlocks START` | Piston activation detected, about to process |
| `[PISTON DEBUG] ===== moveBlocks COMPLETE` | Piston successfully moved blocks |
| `[PISTON DEBUG] ===== moveBlocks ABORT` | Machine piston move rejected; read the abort reason |
| `[PISTON DEBUG] ===== moveBlocks VANILLA` | No machine in push list; vanilla handles it |
| `[PISTON DEBUG] ===== RESTORED machine` | Machine data successfully restored after animation |
| `[PISTON DEBUG] ===== FAILED: Could not restore` | **BUG**: Machine data could not be restored |
| `[PISTON DEBUG] ===== moveBlocks ABORT: Destination chunk is NOT loaded` | Preflight canceled the move before changing blocks |
| `[IO PULL]` / `[IO PUSH]` with `transferred N items` | IO transfer completed |
| `[IO PULL]` / `[IO PUSH]` with `skipping capability lookup` | Neighbor chunk was unloaded; IO did not query its capability |
| `[MACHINE TICK]` | Machine ticker ran; its chunk was ticking at that logged time |
| `[IO MODULE] tickInputs` / `tickOutputs` | Configured IO face was evaluated; this alone does not mean an item moved |

## Test 1: Basic Piston Push (Sanity Check)

**Setup:** Place a piston facing a single MachineFrameworkBlock with at least one module installed.

**Action:** Activate the piston with redstone.

**Expected Log Output:**
```
[PISTON DEBUG] ===== moveBlocks START: pos=..., dir=..., extending=true, isSticky=false
[PISTON DEBUG] Resolver result: resolved=true, blocksToPush=1
[PISTON DEBUG] Block #0: pos=..., state=..., chunkLoaded=true
[PISTON DEBUG] MachineFrameworkBlock detected, taking over moveBlocks
[PISTON DEBUG] Captured MachineFrameworkBlockEntity at index 0 pos ...
[PISTON DEBUG]   - Owner: ..., Public: ...
[PISTON DEBUG]   - Modules: 1 slots occupied
[PISTON DEBUG]   - Inventory: 0 slots occupied
[PISTON DEBUG]   - NBT keys: [Modules, Inventory, ModuleData, OwnerUUID, IsPublic]
[PISTON DEBUG] Movement direction: moveDir=... (extending=true, pistonDir=...)
[PISTON DEBUG] Moving block #0: ... -> ... (state=...)
[PISTON DEBUG]   - Destination chunk loaded: true
[PISTON DEBUG] Attached held block entity data to PistonMovingBlockEntity at ...
[PISTON DEBUG] ===== moveBlocks COMPLETE: Successfully moved 1 blocks
[PISTON DEBUG] ===== restoreHeldData START at ...
[PISTON DEBUG] ===== RESTORED machine at ...
[PISTON DEBUG]   - Modules: 1 slots occupied
```

**Expected In-Game:** Piston extends, machine moves forward one block, piston head appears, machine retains its modules and inventory.

**Pass Criteria:** Machine moves correctly, data is restored, no warnings or errors in logs.

---

## Test 2: Sticky Piston Retraction

**Setup:** Place a sticky piston. Push a MachineFrameworkBlock one block away. Then deactivate the piston.

**Action:** Deactivate the sticky piston to retract and pull the machine back.

**Expected Log Output:**
```
[PISTON DEBUG] ===== moveBlocks START: pos=..., dir=..., extending=false, isSticky=true
[PISTON DEBUG] Resolver result: resolved=true, blocksToPush=...
[PISTON DEBUG] Sticky retract at ...: candidate=..., candidateState=..., resolverPositions=[...], finalPositions=[...]
[PISTON DEBUG] MachineFrameworkBlock detected, taking over moveBlocks
[PISTON DEBUG] Captured MachineFrameworkBlockEntity at index ... pos ...
[PISTON DEBUG] Movement direction: moveDir=... (extending=false, pistonDir=...)
[PISTON DEBUG] ===== moveBlocks COMPLETE: Successfully moved 1 blocks
[PISTON DEBUG] ===== RESTORED machine at ...
```

**Expected In-Game:** Piston retracts, pulling the machine back to its original position. Machine retains all data.

**Pass Criteria:** Machine returns to original position with all modules and inventory intact.

---

## Test 3: Multi-Block Machine Push

**Setup:** Place a piston facing a line of blocks: `[Piston] [Machine] [Cobblestone] [Cobblestone]`

**Action:** Activate the piston.

**Expected Log Output:**
```
[PISTON DEBUG] Resolver result: resolved=true, blocksToPush=4
[PISTON DEBUG] Block #0: pos=..., state=machine_framework, chunkLoaded=true
[PISTON DEBUG] Block #1: pos=..., state=cobblestone, chunkLoaded=true
[PISTON DEBUG] Block #2: pos=..., state=cobblestone, chunkLoaded=true
[PISTON DEBUG] Block #3: pos=..., state=cobblestone, chunkLoaded=true
[PISTON DEBUG] Moving block #3: ... -> ...
[PISTON DEBUG] Moving block #2: ... -> ...
[PISTON DEBUG] Moving block #1: ... -> ...
[PISTON DEBUG] Moving block #0: ... -> ...
[PISTON DEBUG] ===== RESTORED machine at ...
```

**Expected In-Game:** All 4 blocks move forward together. The machine is at the front (closest to piston head) and retains its data.

**Pass Criteria:** Entire column moves, machine data preserved, no blocks lost.

---

## Test 4: Push Limit (12 Blocks)

**Setup:** Place a piston facing a line of 12 blocks, with the MachineFrameworkBlock somewhere in the line.

**Action:** Activate the piston.

**Expected:** All 12 blocks move. The machine's data is preserved.

**Pass Criteria:** 12 blocks move, machine data intact.

---

## Test 5: Push Limit Exceeded (13 Blocks)

**Setup:** Place a piston facing a line of 13 blocks, with the MachineFrameworkBlock at position 12 or 13.

**Action:** Activate the piston.

**Expected:** A destroyable block (for example, a torch) in position 13 is destroyed and the first 12 blocks move. The machine block has normal piston push reaction, so if it is in position 13 the resolver should reject the whole move rather than destroy the machine. See Test 16 for exact steps and this machine-at-position-13 check.

**Pass Criteria:** Push limit correctly enforced, machine data preserved if not destroyed.

---

## Test 6: Machine Data Preservation Check

**Setup:** 
1. Place a MachineFrameworkBlock
2. Install an IO module with face configurations
3. Install an inventory module adjacent to the IO module
4. Place items in the inventory module's slot
5. Set the machine to private mode (if applicable)
6. Place a piston to push it

**Action:** Push the machine one block and verify all data.

**Expected Log Output:**
```
[PISTON DEBUG]   - Owner: <your-uuid>, Public: false
[PISTON DEBUG]   - Modules: 2 slots occupied
[PISTON DEBUG]   - Inventory: 1 slots occupied
[PISTON DEBUG]   - NBT keys: [Modules, Inventory, ModuleData, OwnerUUID, IsPublic]
[PISTON DEBUG] ===== RESTORED machine at ...
[PISTON DEBUG]   - Modules: 2 slots occupied
[PISTON DEBUG]   - Inventory: 1 slots occupied
[PISTON DEBUG]   - Owner: <your-uuid>, Public: false
```

**Expected In-Game:** Open the machine after it's pushed. All modules, face configurations, inventory items, owner, and public/private setting should be intact.

**Pass Criteria:** All data fields match before and after the push.

---

## Test 7: Piston at Chunk Boundary

**Setup:** 
1. Find a chunk boundary (use F3 to see chunk coordinates — stand where X or Z is a multiple of 16)
2. Place a piston on one side of the boundary, facing across it
3. Place a MachineFrameworkBlock so it will be pushed across the chunk boundary

**Action:** Activate the piston.

**Expected Log Output:**
```
[PISTON DEBUG] Block #0: pos=..., state=..., chunkLoaded=true
[PISTON DEBUG] Moving block #0: ... -> ... (state=...)
[PISTON DEBUG]   - Destination chunk loaded: true
[PISTON DEBUG] ===== RESTORED machine at ...
```

**Expected In-Game:** Machine moves across the chunk boundary. Data is preserved.

**Pass Criteria:** Machine crosses chunk boundary, data intact, no warnings about unloaded chunks.

---

## Test 8: Rapid Piston Toggle

**Setup:** Place a piston clock (rapid redstone toggler) connected to a piston facing a MachineFrameworkBlock.

**Action:** Let the clock run for ~10 seconds (many extend/retract cycles).

**Expected Log Output:** Multiple cycles of:
```
[PISTON DEBUG] ===== moveBlocks START: ... extending=true ...
[PISTON DEBUG] ===== moveBlocks COMPLETE: Successfully moved 1 blocks
[PISTON DEBUG] ===== RESTORED machine at ...
[PISTON DEBUG] ===== moveBlocks START: ... extending=false ...
[PISTON DEBUG] ===== moveBlocks COMPLETE: Successfully moved 1 blocks
[PISTON DEBUG] ===== RESTORED machine at ...
```

**Expected In-Game:** Machine oscillates back and forth. After stopping, machine should be in a valid state with all data intact.

**Pass Criteria:** No data corruption after many cycles, no FAILED restore messages.

**Manual result (2026-11-12):** Repeated piston cycling with a machine payload completed with contents intact.

---

## Test 9: IO at Simulation Distance Border

This test separates “outside simulation distance” from “actually unloaded.”

**Setup:**
1. Use the Shared Remote Test Area above. Put the machine at `(1007, -60, 1000)` and chest at `(1008, -60, 1000)`.
2. Install an IO module and inventory module. Configure the face toward the chest as `OUTPUT`.
3. Put 16 items in the machine inventory module and leave space in the chest.
4. Stand beside the setup. Run `/execute if loaded 1008 -60 1000 run say TARGET_LOADED` and `/execute unless loaded 1008 -60 1000 run say TARGET_UNLOADED`. Confirm the first reports loaded. Confirm a few items transfer and record both inventories.
5. Move to about `(920, -59, 1000)`, chunk `(57, 62)`, keeping simulation distance at 5. Wait 10 seconds.

**Action:** Check F3 for the player chunk, run both loaded/unloaded commands above, and inspect the newest log entries and inventory counts.

**Expected when target chunk is loaded but not ticking:** `[MACHINE TICK]` continues for the machine, while IO logs `chunkTicking=false — skipping capability lookup`; item counts remain unchanged. A loaded border chunk is not sufficient for IO.

**Expected when target chunk is ticking:** transfers may proceed. When the player returns and the target starts ticking, IO should resume.

**Pass Criteria:** No capability lookup or transfer occurs while the adjacent chunk is not ticking. Record the `[IO PUSH DIAGNOSTIC]` values and skip message; the loaded/unloaded command alone does not establish ticking status.

**Pull variant:** Repeat with the chest on an `INPUT` face, items in the chest, and empty space in the machine inventory. When the target is not ticking, expect `[IO PULL] ... chunkTicking=false — skipping capability lookup` and unchanged chest contents.

## Test 10: Machine Unload/Reload

**Setup:**
1. Use a machine in the Shared Remote Test Area, away from spawn and chunk loaders.
2. Note its modules, inventory, settings, and chest count.

**Action:** Move at least 10 chunks away from the machine, wait 10 seconds, then return. Use `[MACHINE TICK]` timestamps to confirm machine ticks stop while away and resume after returning. In this version, that message is emitted from inside the ticker, so it proves that the machine ticker ran at the logged time.

**Expected:** No processing or IO runs while the machine ticker is not called. On return, modules, inventory, owner, and settings are unchanged, and processing/IO resumes.

**Pass Criteria:** No data loss after chunk unload/reload cycle.

## Test 11: Piston Pushing Machine Into Unloaded Chunk (Edge Case)

This test has a normal chunk-boundary case and a controlled unloaded-destination case.

**Normal boundary case:**
1. At the Shared Remote Test Area, temporarily remove the chest. Place an east-facing piston at `(1006, -60, 1000)` and the machine at `(1007, -60, 1000)`; leave `(1008, -60, 1000)` clear.
2. Put a module and items in the machine. Activate the piston while nearby, then move to about `(920, -59, 1000)` and wait 10 seconds.
3. Run `/execute if loaded 1008 -60 1000 run say DESTINATION_LOADED` and `/execute unless loaded 1008 -60 1000 run say DESTINATION_UNLOADED`; inspect the piston log.

**Expected:** If the destination is loaded, the piston may move the machine across the boundary. Expect destination loaded, `moveBlocks COMPLETE`, and successful data restoration. Verify the machine and contents at the destination.

**Controlled unloaded-destination case:** The abort branch is exercised only if the destination really reports unloaded while the piston/machine can still execute. Walking away does not reliably create this condition because adjacent border chunks may remain loaded. Use a dedicated GameTest or development-only chunk-status fixture that keeps the source machine chunk ticking while the destination is absent; do not force-load the destination. Trigger one extension and verify:

```
[PISTON DEBUG] ===== moveBlocks ABORT: Destination chunk is NOT loaded at ...
```

The piston must not move, no machine data should be detached, and no restore/failure log should follow. Also test an unloaded position in the `toDestroy` list if verifying push-limit behavior across a boundary. If no fixture is available, mark this branch **not exercised**; the normal boundary case is not equivalent.

The development GameTest suite covers an unavailable extension destination, an unavailable sticky-retraction destination, and the separate `toDestroy` preflight check. These tests simulate the relevant position as unavailable while leaving the GameTest chunk loaded. The destruction-position test confirms the machine, 12-block line, and destroyable block remain unchanged; the sticky-retraction test also checks machine data and piston/head consistency. Run them with `./gradlew runGameTestServer`. These verify the guard paths without physically unloading chunks; the manual normal-boundary case above remains a separate in-world check.

## Test 12: Two Pistons Pushing Same Machine

**Setup:** Place two pistons facing each other with a MachineFrameworkBlock between them: `[Piston A] [Machine] [Piston B]`

**Action:** Activate both pistons simultaneously.

**Expected:** Only one piston should succeed (the one that ticks first). The other should fail.

**Pass Criteria:** No crashes, no duplicate machines, no data corruption.

---

## Test 13: Upward/Downward Piston Push

**Setup:** 
- Upward: Place a piston on the ground facing up with a MachineFrameworkBlock on top
- Downward: Place a piston in the ceiling facing down with a MachineFrameworkBlock below

**Action:** Activate the piston.

**Expected In-Game:** Machine moves in the correct direction (up or down). Data is preserved.

**Pass Criteria:** Directional movement works correctly for all orientations.

---

## Test 14: Piston Pushing Machine Into an Immovable Block

**Setup:** Place a piston facing a MachineFrameworkBlock with an immovable block directly beyond the machine, for example `[Piston] [Machine] [Obsidian]`. A single cobblestone block is not an obstruction for this test because the piston can push it along with the machine.

**Action:** Activate the piston.

**Expected Log Output:**
```
[PISTON DEBUG] ===== moveBlocks ABORT: Resolver rejected movement
```

**Expected In-Game:** Piston does not extend. The machine and obsidian stay in place.

**Pass Criteria:** Piston correctly refuses to push when blocked.

---

## Test 15: Sticky Piston Retraction Preserves Machine Data

This exercises the pull/retraction path after a sticky piston has already pushed the machine outward. It is a loaded-chunk data-preservation check, not an unloaded-chunk test.

**Setup:** In a clear area, place an east-facing sticky piston at `(1030, -60, 1000)`, the machine at `(1031, -60, 1000)`, and leave `(1032, -60, 1000)` clear. Install an Inventory Module and put 3 diamonds in its inventory. Note the machine's module and item counts.

**Action:**
1. Power the piston with `/setblock 1030 -60 1001 minecraft:redstone_block`.
2. Wait for the extension and moving-block animation to finish. Confirm the machine is at `(1032, -60, 1000)` and its data is present.
3. Remove power with `/setblock 1030 -60 1001 minecraft:air` and wait for the sticky retraction and animation to finish.

**Expected Log Output:** A retraction entry with `extending=false, isSticky=true`, followed by machine capture and `RESTORED machine` at the pulled-to position. No `FAILED: Could not restore` message should occur.

**Expected In-Game:** The piston retracts, the machine returns to `(1031, -60, 1000)`, and the Inventory Module and all 3 diamonds remain. There is exactly one machine.

**Pass Criteria:** Machine data is intact after both extension and sticky retraction; no duplicate/lost machine and no restore failure.

---

## Test 16: Push-Limit Destruction Beside a Machine

This checks that a destroyable 13th block is removed while a machine at the end of the 12-block push list moves and keeps its data.

**Setup:** In a clear area, face an east-facing piston at `(1040, -60, 1000)`. In front of it, place 11 cobblestone blocks at `x=1041` through `x=1051`, the machine at `(1052, -60, 1000)`, and a torch at `(1053, -60, 1000)`. Put a support block beneath the torch at `(1053, -61, 1000)` if needed. The machine is block 12; the torch is the destroyable block 13. Install an Inventory Module and put 3 diamonds in the machine.

**Action:** Power the piston once with `/setblock 1040 -60 1001 minecraft:redstone_block`.

**Expected:** The resolver accepts 12 pushable blocks. The torch breaks and drops exactly one torch item; the first 12 blocks move one block east. The machine moves to `(1053, -60, 1000)` with its module and 3 diamonds intact. The log should show `moveBlocks COMPLETE: Successfully moved 12 blocks` and successful machine restoration, with no failure message. The current log does not print the `toDestroy` list, so verify the torch destruction in-game.

**Pass Criteria:** One torch is destroyed/dropped, the machine and the 11 cobblestone blocks move once, machine data is preserved, and there are no duplicate blocks or restore errors.

**Machine-at-position-13 variant:** Rebuild the line at `z=1002`: east-facing piston `(1040, -60, 1002)`, 12 cobblestone blocks at `x=1041` through `x=1052`, and the machine at `(1053, -60, 1002)`. Power with `/setblock 1040 -60 1003 minecraft:redstone_block`. Because the machine itself is not destroyable, expect `Resolver rejected movement`; the piston stays retracted, all 12 cobblestone blocks and the machine stay in place, and no machine item drops. This corrects the old expectation that a machine in position 13 would be destroyed.

Remove power after each run by replacing the powering block with air. For the first layout use `/setblock 1040 -60 1001 minecraft:air`; for the variant use `/setblock 1040 -60 1003 minecraft:air`.

---

## Log Analysis Checklist

After running each test, check the logs for:

- [ ] No `[PISTON DEBUG] ===== FAILED` messages
- [ ] Unloaded IO targets log `skipping capability lookup`; item counts remain unchanged
- [ ] Test 9 records loaded status with `/execute if loaded` and ticking status from `[IO PUSH DIAGNOSTIC]` or the skip/transfer logs
- [ ] Piston preflight aborts before block/data mutation when a required chunk is unloaded
- [ ] Transfers into loaded border chunks are recorded as loaded-border behavior, not unloaded-target failures
- [ ] Module and inventory counts match before and after each piston operation
- [ ] Owner UUID and public/private settings are preserved
- [ ] No crashes or stack traces

## How to Run Tests Efficiently

1. Start the game in singleplayer or on a dedicated server with the current mod build.
2. Set simulation distance to 5 and use the Shared Remote Test Area, far from spawn. Disable spawn chunks if `/gamerule spawnChunkRadius 0` is available. Check `/forceload query` and any third-party chunk-loader mod; no test-area chunk should be forced.
3. Set up the machine, chest, and piston at the exact coordinates in Tests 9 and 11. Record inventory counts before each run.
4. Use F3 to confirm the player chunk. Check an adjacent target with `/execute if loaded x y z run say TARGET_LOADED` and `/execute unless loaded x y z run say TARGET_UNLOADED`.
5. Keep `run/logs/latest.log` open with auto-refresh, or filter for `[PISTON DEBUG]`, `[IO PULL]`, `[IO PUSH]`, `[IO MODULE]`, and `[MACHINE TICK]`.
6. After moving, wait 10 seconds with the world unpaused. For singleplayer, make sure the integrated server is running.
7. Run Test 9's push and pull variants, Test 10, then Test 11's normal boundary case. The controlled unloaded-destination piston case requires a GameTest/development fixture; player travel alone may not produce it. Mark it **not exercised** if no such fixture is available.
8. Run Test 15 to verify sticky retraction restores machine data, then Test 16 to verify both the destroyable-block push limit and the non-destroyable machine-at-position-13 rejection.

---

## Test 17: Sticky Side Branch With Multiple Machines

This manual test checks more than one machine in the same sticky structure. Build the setup several blocks above the floor so slime does not attach to the ground.

**Setup:** Place an east-facing sticky piston, followed by two slime blocks in a straight line. Attach one machine to the side of each slime block. Keep all destinations and nearby side spaces clear. Put a different module and item stack in each machine.

**Action:** Extend the piston, wait for movement to finish, then retract it. Check both machines after each movement.

**Expected:** Both machines move with their attached slime structure and return on retraction. Each machine retains its own modules and inventory. There is exactly one of each machine.

**Manual result (2026-11-12):** Both side-attached machines moved and retained their contents.

---

## Test 18: Slime Chain and Honey Side-Branch Retraction

**Setup:** Keep the structure clear of the floor. Place a sticky piston facing a slime chain and attach a machine to the side of one slime block. Repeat with a honey block in place of the slime chain. Put a module and distinctive item stack in each machine.

**Action:** Extend, inspect the moved machine, retract, then inspect it again. For the honey case, verify that the honey block and machine follow their own honey group.

**Expected:** The resolver includes side-attached machines in valid sticky moves. The piston preserves machine data through both directions. Honey and slime do not adhere to each other, so a mixed line may separate on sticky retraction even though both blocks were pushed forward while in the same line.

**Automated coverage:** GameTests cover a machine attached to a two-block slime chain and a machine attached to a honey block. Both passed in the development GameTest server.

**Manual result (2026-11-12):** The mixed slime/honey setup separated as expected on retraction, and machine contents remained intact.

---

## Test 19: Sticky Structure Push Limit

**Setup:** Build an elevated slime structure with a machine in the main line or attached as a side branch. Count every block in the resolver's moving structure, including slime blocks, machines, and side branches.

**Action:** Test with 12 movable blocks, then add one movable block for a 13-block attempt. Keep all destinations clear.

**Expected:** A valid 12-block structure moves. A 13th non-destroyable block makes the resolver reject the move. Side-attached blocks count toward the 12-block cap; this is a block count, not distance from the piston. A destroyable block in the push list may be destroyed under vanilla piston rules, as described in Test 16.

**Manual result (2026-11-12):** The slime structure moved through the 12-block limit and stopped when the 13th block was added.

---

## Test 20: Repeated Machine Moves (Feed Tape)

**Setup:** Place a machine with a module and a distinctive item stack in front of a piston. Leave a clear path. Use repeated piston moves, either by cycling one piston or placing pistons behind the machine as it advances.

**Action:** Move the machine several positions. Wait for each animation to finish and check the machine at each new position.

**Expected:** One machine advances at a time. Modules and inventory remain intact after every move. No duplicate machine or failed restore appears in the log.

**Manual result (2026-11-12):** Repeated pushes preserved the machine contents.

---

## Test 21: Quasi-Connectivity Power

Quasi-connectivity is Java piston redstone behavior. A piston can detect power as if the block position directly above its base were another piston, so a source two blocks above the base can activate it. The update may require a neighboring block change.

**Setup:** Place a piston facing a machine, with one air block between the piston base and a redstone block directly above that air space. Use a sticky piston if testing both push and pull. Put a module and item stack in the machine.

**Action:** Place and remove the upper redstone block. If the piston does not react, place and break a temporary block beside the piston to trigger a neighbor update. Verify extension and retraction and inspect the machine data.

**Expected:** The piston responds to quasi-connected power and preserves the machine data through movement.

**Manual result (2026-11-12):** The piston pushed and pulled the machine through quasi-connectivity; contents were preserved.
