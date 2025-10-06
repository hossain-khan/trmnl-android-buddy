# Contributing to TRMNL Android Buddy

Thank you for your interest in contributing to TRMNL Android Buddy! This document provides guidelines and instructions for contributing to the project.

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [Development Workflow](#development-workflow)
- [Code Style and Conventions](#code-style-and-conventions)
- [Pull Request Process](#pull-request-process)
- [Reporting Bugs](#reporting-bugs)
- [Suggesting Features](#suggesting-features)
- [Questions](#questions)

## Code of Conduct

This project follows a standard code of conduct. Be respectful and considerate in all interactions with other contributors.

## Getting Started

### Prerequisites

- JDK 17 or higher
- Android Studio Ladybug (2025.1.1) or newer
- Git
- Basic knowledge of Kotlin and Android development

### Setting Up Your Development Environment

1. **Fork the repository** on GitHub

2. **Clone your fork**:
   ```bash
   git clone https://github.com/YOUR_USERNAME/trmnl-android-buddy.git
   cd trmnl-android-buddy
   ```

3. **Add upstream remote**:
   ```bash
   git remote add upstream https://github.com/hossain-khan/trmnl-android-buddy.git
   ```

4. **Open in Android Studio**:
   - Open Android Studio
   - Select "Open an Existing Project"
   - Navigate to the cloned directory
   - Wait for Gradle sync to complete

5. **Build the project**:
   ```bash
   ./gradlew assembleDebug
   ```

6. **Run tests to ensure everything works**:
   ```bash
   ./gradlew test
   ```

### Technical Documentation

For detailed technical documentation, architecture, and design patterns, see [DEVGUIDE.md](DEVGUIDE.md).

## Development Workflow

### Creating a Feature Branch

Always create a new branch for your work:

```bash
# Update your local main branch
git checkout main
git pull upstream main

# Create a feature branch
git checkout -b feature/your-feature-name
# or for bug fixes
git checkout -b fix/bug-description
```

### Making Changes

1. **Make your changes** following the code style guidelines below

2. **Format your code** (REQUIRED before committing):
   ```bash
   ./gradlew formatKotlin
   ```

3. **Run tests** to ensure nothing is broken:
   ```bash
   ./gradlew test
   ```

4. **Update CHANGELOG.md** following [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) format:
   ```markdown
   ## [Unreleased]
   
   ### Added
   - Description of your new feature
   
   ### Fixed
   - Description of your bug fix
   ```

### Committing Changes

1. **Stage your changes**:
   ```bash
   git add .
   ```

2. **Commit with a descriptive message**:
   ```bash
   git commit -m "feat: Add user profile screen
   
   - Implemented profile screen with Circuit
   - Added user repository for API calls
   - Updated CHANGELOG.md"
   ```

   **Commit Message Format**:
   - Use imperative mood ("Add feature" not "Added feature")
   - Start with a prefix: `feat:`, `fix:`, `docs:`, `refactor:`, `test:`, `chore:`
   - Keep first line under 50 characters
   - Add detailed description if needed

3. **Push to your fork**:
   ```bash
   git push origin feature/your-feature-name
   ```

## Code Style and Conventions

### Kotlin Style Guide

This project follows the [official Kotlin style guide](https://kotlinlang.org/docs/coding-conventions.html) enforced by ktlint.

**Key Points**:
- **Classes**: PascalCase (e.g., `UserAccountScreen`)
- **Functions/Properties**: camelCase (e.g., `getUserData()`)
- **Constants**: SCREAMING_SNAKE_CASE (e.g., `MAX_RETRY_COUNT`)
- **Composables**: PascalCase like classes (e.g., `UserProfileCard()`)

### Material Design 3 Guidelines

**IMPORTANT**: All UI components must be Material You compatible.

✅ **DO**:
```kotlin
Card(
    colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    )
) {
    Text(
        text = "Hello",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onPrimaryContainer
    )
}
```

❌ **DON'T**:
```kotlin
// Never hardcode colors
Card(colors = CardDefaults.cardColors(containerColor = Color.Blue)) {
    Text(text = "Hello", color = Color.White)
}
```

### Architecture Patterns

1. **Circuit UDF**:
   - Screens split into Presenter (logic) and UI (rendering)
   - State flows down, events flow up
   - Use `@CircuitInject` for dependency injection

2. **Metro DI**:
   - Constructor injection preferred
   - Use `@Inject` for classes
   - Define bindings in `AppBindings.kt` or `AppGraph.kt`

3. **Repository Pattern**:
   - Abstract data sources (API, DataStore)
   - Return `Flow<T>` for reactive data or suspend functions
   - Use EitherNet's `ApiResult<T, E>` for API calls

### Testing Guidelines

1. **Use AssertK** for all assertions:
   ```kotlin
   import assertk.assertThat
   import assertk.assertions.*
   
   assertThat(result).isEqualTo("expected")
   ```

2. **Prefer Fakes over Mocks**:
   - Fakes: Lightweight implementations (e.g., `FakeNavigator`)
   - Mocks: Only when necessary (require frameworks)

3. **Write tests for**:
   - New API endpoints
   - Repository methods
   - Business logic in Presenters
   - Utility functions

4. **Example test**:
   ```kotlin
   @Test
   fun `getDevices returns success when API responds 200`() = runTest {
       // Arrange
       mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(jsonResponse))
       
       // Act
       val result = apiService.getDevices("Bearer token")
       
       // Assert
       assertThat(result).isInstanceOf<ApiResult.Success<*>>()
   }
   ```

### Code Formatting

**REQUIRED before every commit**:

```bash
# Auto-format all Kotlin code
./gradlew formatKotlin

# Check formatting without modifying files
./gradlew lintKotlin
```

## Pull Request Process

### Before Submitting

1. ✅ Code is formatted with `./gradlew formatKotlin`
2. ✅ All tests pass with `./gradlew test`
3. ✅ CHANGELOG.md is updated under `[Unreleased]` section
4. ✅ Commits have descriptive messages
5. ✅ No unnecessary files (build artifacts, IDE files, etc.)

### Submitting a Pull Request

1. **Push your branch** to your fork:
   ```bash
   git push origin feature/your-feature-name
   ```

2. **Create a pull request** on GitHub:
   - Go to the original repository
   - Click "New Pull Request"
   - Select your fork and branch
   - Fill in the PR template

3. **PR Title Format**:
   ```
   feat: Add user profile screen
   fix: Resolve crash on device list refresh
   docs: Update API documentation
   ```

4. **PR Description** should include:
   - What changes were made
   - Why these changes are needed
   - Any related issues (e.g., "Fixes #123")
   - Screenshots (for UI changes)
   - Testing performed

### PR Review Process

1. **Automated checks** will run:
   - CI/CD build and tests
   - Code formatting verification
   - Lint checks

2. **Code review** by maintainers:
   - Address review comments
   - Make requested changes
   - Push updates to the same branch

3. **Merge**:
   - Once approved, your PR will be merged
   - Delete your feature branch after merge

### Protected Main Branch

The `main` branch is protected. All changes must go through pull requests. Direct pushes are not allowed.

## Reporting Bugs

### Before Reporting

1. **Search existing issues** to avoid duplicates
2. **Ensure it's a bug** and not expected behavior
3. **Reproduce the bug** consistently

### Bug Report Template

Create a new issue with:

**Title**: Brief, descriptive summary

**Description**:
```markdown
**Describe the bug**
A clear description of what the bug is.

**Steps to reproduce**
1. Go to '...'
2. Click on '...'
3. Scroll down to '...'
4. See error

**Expected behavior**
What you expected to happen.

**Actual behavior**
What actually happened.

**Screenshots**
If applicable, add screenshots.

**Device Information**
- Device: [e.g., Pixel 7]
- Android Version: [e.g., Android 14]
- App Version: [e.g., 1.0.3]

**Logs**
Any relevant logcat output.
```

## Suggesting Features

### Feature Request Template

Create a new issue with:

**Title**: Brief feature description

**Description**:
```markdown
**Is your feature request related to a problem?**
A clear description of the problem.

**Proposed solution**
How would you like this to work?

**Alternatives considered**
Any alternative solutions you've thought about.

**Additional context**
Any other context, mockups, or examples.
```

## Questions

If you have questions about contributing:

1. **Check the documentation**:
   - [README.md](README.md) - Project overview
   - [DEVGUIDE.md](DEVGUIDE.md) - Technical documentation
   - [api/README.md](api/README.md) - API module docs
   - [app/src/main/java/ink/trmnl/android/buddy/ui/README.md](app/src/main/java/ink/trmnl/android/buddy/ui/README.md) - UI screens docs

2. **Search existing issues** for similar questions

3. **Create a new issue** with the "question" label

## Resources

- **Circuit Documentation**: https://slackhq.github.io/circuit/
- **Metro Documentation**: https://zacsweers.github.io/metro/
- **Material 3 Guidelines**: https://m3.material.io/
- **Jetpack Compose**: https://developer.android.com/compose
- **Kotlin Style Guide**: https://kotlinlang.org/docs/coding-conventions.html
- **Keep a Changelog**: https://keepachangelog.com/en/1.1.0/
- **Semantic Versioning**: https://semver.org/

## License

By contributing, you agree that your contributions will be licensed under the same license as the project (see [LICENSE](LICENSE)).

---

Thank you for contributing to TRMNL Android Buddy! 🎉
