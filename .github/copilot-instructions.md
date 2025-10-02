# Project Overview

This is a template for an Android app using Jetpack Compose, designed to help you quickly set up a new project with best practices in mind. It includes features like dependency injection, Circuit UDF (Unidirectional Data Flow) architecture, and optional WorkManager integration.

The template is pre-configured with Circuit, a Compose-driven architecture for Kotlin and Android applications that provides a clean, unidirectional data flow pattern for building robust Android apps.

## Folder Structure

- `app/`: Contains the main application code.
- `build.gradle.kts`: Contains the Gradle build scripts (Kotlin DSL).
- `app/build.gradle.kts`: Contains the app-specific Gradle build scripts (Kotlin DSL).

## Libraries and Frameworks

- [Circuit](https://github.com/slackhq/circuit) for the app's UDF (Unidirectional Data Flow) architecture - a Compose-driven architecture for building robust Android applications.
- [Metro](https://zacsweers.github.io/metro/) for dependency injection

## Coding Standards

- Always use the `./gradlew formatKotlin` command to format your Kotlin code before committing.
- Run `./gradlew assembleDebug` to ensure your code compiles without errors.

## UI guidelines

- Use material 3 design components for compose
- Use material design best practices for UX and UI
