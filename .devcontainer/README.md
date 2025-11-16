# Dev Container Configuration

This directory contains the development container configuration for TRMNL Android Buddy.

## Features

- **Base Image**: Java 21 (Bookworm)
- **Android SDK**: Automatically installed with latest version
- **Android NDK**: Latest version
- **Command Line Tools**: Latest version
- **VS Code Extensions**: Kotlin, Gradle, Java, and GitHub Copilot support

## Environment Variables

- `ANDROID_HOME`: `/usr/local/lib/android/sdk`
- `ANDROID_SDK_ROOT`: `/usr/local/lib/android/sdk`

## Post-Create Setup

The `post-create.sh` script automatically:
1. Accepts Android SDK licenses
2. Installs Android SDK Platform 35 and Build Tools 35.0.0
3. Updates SDK components
4. Sets Gradle wrapper permissions
5. Pre-downloads Gradle dependencies

## Usage

### First Time Setup

1. Open the project in VS Code
2. When prompted, click "Reopen in Container"
3. Wait for the container to build and post-create script to complete

### Building the Project

```bash
./gradlew build
```

### Running Tests

```bash
./gradlew test
```

### Generating Code Coverage Report

```bash
./gradlew koverHtmlReport
```

The coverage report will be available at:
- Merged report: `build/reports/kover/html/index.html`
- Module reports: `{module}/build/reports/kover/html/index.html`

### Formatting Code

```bash
./gradlew formatKotlin
```

### Linting Code

```bash
./gradlew lintKotlin
```

## Connecting Android Devices

The container is configured with `--privileged` mode to allow ADB access to connected devices. Your local `.android` directory is mounted to preserve ADB keys and settings.

## Troubleshooting

### SDK License Issues

If you encounter SDK license errors, run:
```bash
yes | sdkmanager --licenses
```

### Gradle Issues

If Gradle fails to sync, try:
```bash
./gradlew clean
./gradlew --refresh-dependencies
```

### Android SDK Not Found

Verify the environment variables:
```bash
echo $ANDROID_HOME
echo $ANDROID_SDK_ROOT
```

Both should point to `/usr/local/lib/android/sdk`.
