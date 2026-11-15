# Contributing to Praeteritum Futurum

Thank you for your interest in contributing! This mod is developed with AI assistance (Google Gemini), and contributions from the community are welcome.

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
   - File → Open → Select the project root directory
   - Wait for Gradle sync to complete

3. Run the development client:
   ```bash
   ./gradlew runClient
   ```
   Or use the run configuration in your IDE.

### Building the Mod
```bash
./gradlew build
```
The compiled JAR will be in `build/libs/`.

### Data Generation
If you add new blocks, items, or recipes, run the data generators:
```bash
./gradlew runData
```
Commit the generated files in `src/generated/resources/`.

## Code Style
- Follow the existing code style in the project
- Use meaningful Javadoc comments for public APIs
- Keep magic numbers as named constants
- Use proper imports instead of fully qualified class names

## Module API
The mod uses a modular API for extensibility:
- `IConfigurableModule` — for modules with configuration screens
- `IModuleMenuProvider` — for modules that provide container menus
- `ICustomScreenProvider` — for modules with custom GUI screens

See the existing module implementations (`IoModuleItem`, `ProcessingModuleItem`, `FilterModuleItem`) for examples.

## Reporting Issues
- Use the [GitHub Issues](https://github.com/TheXTrunner/praeteritum_futurum/issues) tracker for bugs and feature requests
- Include your Minecraft version, NeoForge version, and mod version
- Provide steps to reproduce the issue

## Pull Requests
1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Test thoroughly
5. Submit a pull request with a clear description of changes
