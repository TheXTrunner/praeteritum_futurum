# Contributing to Praeteritum Futurum

Thank you for your interest in contributing! This mod is developed with AI assistance (Google's Gemini and JetBrains' Junie), and contributions from the community are welcome.

For the project overview and player-facing features, start with the [README](https://github.com/TheXTrunner/praeteritum_futurum/blob/main/README.md). Use the [documentation index](https://github.com/TheXTrunner/praeteritum_futurum/blob/main/docs/README.md) to find focused guides; the [architecture overview](https://github.com/TheXTrunner/praeteritum_futurum/blob/main/docs/ARCHITECTURE.md) maps the major code areas and runtime flow.

## Development Setup

### Prerequisites
- **Java 21** (Temurin distribution recommended)
- **Git** for version control
- **IDE**: IntelliJ IDEA (recommended) or Eclipse

### Getting Started
1. Clone the repository:
   ```bash
   git clone https://github.com/TheXTrunner/praeteritum_futurum.git
   cd praeteritum_futurum
   ```

2. Open the project in IntelliJ IDEA:
   - In IntelliJ IDEA, choose File > Open and select the project root directory
   - Wait for Gradle sync to complete

3. Run the development client. On Windows:
   ```bash
   .\gradlew.bat runClient
   ```
   On macOS or Linux, use `./gradlew runClient`. You can also use the run configuration in your IDE.

### Building the Mod
On Windows, run `.\gradlew.bat build`; on macOS or Linux, run `./gradlew build`.
The compiled JAR will be in `build/libs/`.

### Running GameTests

Run the registered server-side GameTests with:

On Windows, run `.\gradlew.bat runGameTestServer`; on macOS or Linux, run `./gradlew runGameTestServer`.

The GameTest server uses the separate `run-gametest/` directory. Its worlds and logs are generated files and are ignored by Git. Test source and structure templates live under `src/main/java/.../gametest/` and `src/main/resources/data/praeteritum_futurum/structure/`.

### Data Generation
If you add new blocks, items, or recipes, run the data generators:
On Windows, run `.\gradlew.bat runData`; on macOS or Linux, run `./gradlew runData`.
Commit the generated files in `src/generated/resources/`.

## Code Style
- Follow the existing code style in the project
- Use meaningful Javadoc comments for public APIs
- Keep magic numbers as named constants
- Use proper imports instead of fully qualified class names

## Module API
The mod uses a modular API for extensibility:
- `IConfigurableModule`: for modules with configuration screens
- `IModuleMenuProvider`: for modules that provide container menus
- `ICustomScreenProvider`: for modules with custom GUI screens

See the [Module API Guide](https://github.com/TheXTrunner/praeteritum_futurum/blob/main/docs/MODULE-API-GUIDE.md) and existing implementations (`IoModuleItem`, `ProcessingModuleItem`, `FilterModuleItem`) for examples.

## Reporting Issues
- Use the [GitHub Issues](https://github.com/TheXTrunner/praeteritum_futurum/issues) tracker for bugs and feature requests
- Include your Minecraft version, NeoForge version, and mod version
- Provide steps to reproduce the issue
- **Security vulnerabilities:** Please report privately via our [Security Policy](https://github.com/TheXTrunner/praeteritum_futurum/blob/main/.github/SECURITY.md) instead of opening a public issue

## Pull Requests
1. Fork the repository
2. Create a feature branch
3. Make your changes and update relevant documentation when behavior or setup instructions change
4. Test thoroughly
5. Submit a pull request with a clear description of changes
