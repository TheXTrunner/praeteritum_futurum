/**
 * Public extension contracts for modules integrated with the Machine Framework.
 * <p>
 * Types in this package describe supported module configuration, UI, item-handler, and mode
 * contracts. Built-in implementation details and examples are maintained in
 * {@code docs/MODULE-API-GUIDE.md}.
 * </p>
 *
 * <h2>Extension points</h2>
 * <ul>
 *   <li>{@link IConfigurableModule}, {@link IModuleMenuProvider}, and {@link ICustomScreenProvider}
 *       define module configuration entry points and screens.</li>
 *   <li>{@link ITextInputModule} provides the contract for modules with inline text configuration.</li>
 *   <li>{@link IItemHandlerModule} exposes a module's item handler to compatible automation.</li>
 *   <li>{@link CyclingMode} defines forward and reverse mode cycling for reusable mode types.</li>
 *   <li>{@link ProcessingMode} supplies display metadata and shared slot roles for processing modes
 *       defined by each module.</li>
 * </ul>
 */
package com.thextrunner.praeteritum_futurum.api.module;
