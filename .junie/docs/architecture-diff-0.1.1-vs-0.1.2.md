# Architecture Differences: 0.1.1 (GitHub Release) vs 0.1.2 (Local)

> **Status:** Internal document — not for public release.
> **Purpose:** Tracks all architectural, code quality, and behavioral differences between the released 0.1.1 version on GitHub and the current local 0.1.2 work-in-progress.

---

## 1. New Features

### Grindstone Module (`GrindstoneModuleItem.java`)
- **Entirely new file** — not present in 0.1.1.
- Extends `ProcessingModuleItem` for disenchanting items or repairing equipment.
- Supports up to 3 input slots (`getMaxInputs() = 3`) for combining items in repair mode.
- Instant processing (no progress bar), matching the Stonecutter module's behavior.
- Removes all non-curse enchantments via `DataComponents.ENCHANTMENTS` and `EnchantmentTags.CURSE`.
- Awards XP on disenchant and repair, matching vanilla grindstone behavior.
- Output slot is checked before processing to prevent XP farming exploits.

### "Configured" Tooltip (`ProcessingModuleItem.java`)
- New `appendHoverText()` override shows a gray "Configured" tooltip when any direction has a non-DISABLED mode.
- Helps players understand why configured modules don't stack.
- Translation key `tooltip.praeteritum_futurum.configured` added to `ModLanguageProvider.java` for datagen.

### Filter Module
- **Present in 0.1.1 release** — confirmed by user. Previously listed in 0.1.2 changelog by mistake.

---

## 2. Code Quality Improvements

### Named Constants (`MachineFrameworkBlockEntity.java`)
- **Before (0.1.1):** Magic numbers `27`, `4`, and `9` hardcoded throughout the class.
- **After (0.1.2):** Extracted into named constants:
  - `GRID_SIZE = 27` (total module slots)
  - `NUM_CHANNELS = 4` (status channels per slot)
  - `GRID_WIDTH = 9` (columns in the grid)
  - `GRID_ROWS = 3` (rows in the grid)
- These constants are referenced by `IoModuleItem.java` and `BaseModuleItem.java` via `MachineFrameworkBlockEntity.GRID_WIDTH` and `MachineFrameworkBlockEntity.GRID_ROWS`.

### Import Statements (`MachineFrameworkBlockEntity.java`, `IoModuleItem.java`)
- **Before (0.1.1):** Fully qualified class names used inline (e.g., `com.thextrunner.praeteritum_futurum.item.custom.ProcessingModuleItem`).
- **After (0.1.2):** Replaced with proper import statements for consistency.

### Deprecated API Usage (`YoolInteractHandler.java`)
- **Before (0.1.1):** `state.rotate(Rotation.COUNTERCLOCKWISE_90)` called without suppression.
- **After (0.1.2):** Added `@SuppressWarnings("deprecation")` at method level with a comment explaining that `BlockState.rotate()` is the correct public API despite the deprecation warning, as the protected `BlockBehaviour.rotate()` overload is not accessible.

### Deprecated Block Methods (`MachineFrameworkBlock.java`)
- **After (0.1.2):** Added `@SuppressWarnings("deprecation")` to `rotate()` and `mirror()` overrides with a comment explaining that overriding these deprecated methods is still required for block rotation and mirroring to function correctly.

### Comment Hygiene (`MachineFrameworkBlockEntity.java`)
- **Before (0.1.1):** Inline developer comment `//IT IS USED just that IntelliJ IDEA doesn't know where` at line 172.
- **After (0.1.2):** Removed the inline comment.

### `@SuppressWarnings("unused")` Cleanup
- **Before (0.1.1):** Overused in `ICustomScreenProvider.java`, `BaseModuleItem.java` (2 methods), `YoolInteractHandler.java`.
- **After (0.1.2):** Reviewed and cleaned up where unnecessary; kept where methods are intentionally empty API hooks.

---

## 3. Security & Network Changes

### Packet Validation (All 6 Serverbound Packets)
- **Before (0.1.1):** No validation — packets trusted all client input.
- **After (0.1.2):** All 6 serverbound packets now validate player reach distance before applying changes:
  - `ServerboundToggleIoFacePacket.java`
  - `ServerboundOpenModuleConfigPacket.java`
  - `ServerboundReturnToMachineMenuPacket.java`
  - `ServerboundSetModuleConfigItemPacket.java`
  - `ServerboundToggleFilterModePacket.java`
  - `ServerboundToggleModuleDirectionPacket.java`
- Each packet checks `player.distanceToSqr(...) > 64.0` (6-block reach distance) and returns early with a warning log if the player is too far.
- Logger field name corrected from `PraeteritumFuturumMod.log` to `PraeteritumFuturumMod.LOGGER`.

### Protocol Versioning (`PraeteritumFuturumMod.java`)
- **After (0.1.2):** Added a comment documenting the protocol versioning strategy in the network registrar section.

---

## 4. Bug Fixes

### Bottom Row Crash (`MachineFrameworkBlockEntity.java`, `BaseModuleItem.java`, `IoModuleItem.java`)
- **Before (0.1.1):** `getAdjacentSlots()` used `row < GRID_WIDTH - 1` (row < 8) to check for DOWN neighbors, but the grid has only 3 rows. Modules in row 2 (slots 18-26) calculated adjacent slots 27-35 which don't exist, causing array index out of bounds crashes.
- **After (0.1.2):** Added `GRID_ROWS = 3` constant. Changed `row < GRID_WIDTH - 1` to `row < GRID_ROWS - 1` in all three affected files. Bottom row modules now correctly ignore the DOWN direction.

### Grindstone XP Farming Exploit (`GrindstoneModuleItem.java`)
- **Before (0.1.1):** N/A (grindstone didn't exist).
- **After (0.1.2):** Output slot is checked for emptiness before processing. If occupied, the method returns immediately without awarding XP or consuming inputs.

### Grindstone 3-Input Support (`GrindstoneModuleItem.java`)
- **After (0.1.2):** `getMaxInputs()` returns 3, and `processModule()` collects all 3 input items using `ArrayList`. The `repairItems()` method combines durability from all items with 5% bonus per repair pair.

---

## 5. Project Structure Changes

### New Files
- `LICENSE` — MIT license text in repository root.
- `CHANGELOG.md` — Version tracking document.
- `CONTRIBUTING.md` — Development setup instructions.
- `src/main/java/com/thextrunner/praeteritum_futurum/api/module/package-info.java` — Module API documentation.
- `src/main/java/com/thextrunner/praeteritum_futurum/item/custom/GrindstoneModuleItem.java` — New grindstone module.

### Modified Configuration Files
- `gradle.properties`: Changed `mod_license` from `All Rights Reserved` to `MIT` to match `neoforge.mods.toml`.
- `.gitignore`: Added `.venv/` to exclude Python virtual environment.

### CI/CD (`.github/workflows/build.yml`)
- **Before (0.1.1):** Only ran `./gradlew build`.
- **After (0.1.2):** Added `./gradlew jar` step, artifact upload for the JAR, and automated GitHub Releases on tags.

### Documentation (`README.md`)
- **Before (0.1.1):** Informal format with backlog/brainstorming sections.
- **After (0.1.2):** Professional format following mod readme conventions (similar to Create, Applied Energistics): clean overview, feature table, installation steps, contributing section, license reference, and roadmap pointing to GitHub Issues.

---

## 6. Data Generator Changes

### `ModLanguageProvider.java`
- **Before (0.1.1):** Missing `tooltip.praeteritum_futurum.configured` translation.
- **After (0.1.2):** Added the translation in a "Tooltips" section so datagen produces it automatically.

---

## Summary

| Category | 0.1.1 (GitHub) | 0.1.2 (Local) |
|----------|---------------|---------------|
| Grindstone Module | ❌ Not present | ✅ Full implementation |
| Named Constants | ❌ Magic numbers | ✅ GRID_SIZE, NUM_CHANNELS, GRID_WIDTH, GRID_ROWS |
| Packet Validation | ❌ None | ✅ 6-block reach check on all 6 packets |
| Bottom Row Crash | ❌ Crashes on slots 18-26 | ✅ Fixed with GRID_ROWS |
| "Configured" Tooltip | ❌ Not present | ✅ On all processing modules |
| Import Style | ❌ Fully qualified names inline | ✅ Proper imports |
| License | ❌ Inconsistent (ARR vs MIT) | ✅ MIT everywhere |
| CI/CD | ❌ Build only | ✅ Artifacts + GitHub Releases |
| Documentation | ❌ Informal README | ✅ Professional README, CHANGELOG, CONTRIBUTING |
| Deprecated API | ⚠️ Warnings | ✅ Suppressed with justification |