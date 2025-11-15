#!/bin/bash

set -e

echo "🚀 Setting up Android development environment..."

# Accept Android SDK licenses
yes | sdkmanager --licenses || true

# Install required Android SDK components
echo "📦 Installing Android SDK components..."
sdkmanager "platform-tools" "platforms;android-35" "build-tools;35.0.0" || true

# Update SDK components
echo "🔄 Updating SDK components..."
sdkmanager --update || true

# Set proper permissions for Gradle wrapper
echo "🔧 Setting Gradle wrapper permissions..."
chmod +x ./gradlew

# Install Gradle dependencies (helps with IDE indexing)
echo "📚 Downloading Gradle dependencies..."
./gradlew --version

echo "✅ Android development environment setup complete!"
echo "📱 You can now build the project with: ./gradlew build"
echo "🧪 Run tests with: ./gradlew test"
echo "📊 Generate coverage report with: ./gradlew koverHtmlReport"
