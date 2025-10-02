#!/bin/bash

# Android Environment Setup Script
# This script sets up the Android SDK and environment variables for Gradle builds

set -e  # Exit on any error

echo "🚀 Setting up Android development environment..."

# Configuration
ANDROID_SDK_ROOT="/opt/android-sdk"
CMDLINE_TOOLS_URL="https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip"
CMDLINE_TOOLS_ZIP="commandlinetools-linux-latest.zip"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${GREEN}✓${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}⚠${NC} $1"
}

print_error() {
    echo -e "${RED}✗${NC} $1"
}

# Check if Android SDK is already installed
if [ -d "$ANDROID_SDK_ROOT" ] && [ -d "$ANDROID_SDK_ROOT/cmdline-tools" ]; then
    print_warning "Android SDK appears to be already installed at $ANDROID_SDK_ROOT"
    echo "Checking if licenses are accepted..."
    
    # Check if we can run sdkmanager
    if [ -x "$ANDROID_SDK_ROOT/cmdline-tools/latest/bin/sdkmanager" ]; then
        print_status "Android SDK command line tools found"
    else
        print_error "Android SDK command line tools not found or not executable"
        exit 1
    fi
else
    echo "📦 Installing Android SDK..."
    
    # Create Android SDK directory
    sudo mkdir -p "$ANDROID_SDK_ROOT"
    cd /tmp
    
    # Download Android SDK command line tools
    print_status "Downloading Android SDK command line tools..."
    wget -q "$CMDLINE_TOOLS_URL" -O "$CMDLINE_TOOLS_ZIP"
    
    # Extract command line tools
    print_status "Extracting command line tools..."
    sudo unzip -q "$CMDLINE_TOOLS_ZIP"
    
    # Move to proper location
    sudo mkdir -p "$ANDROID_SDK_ROOT/cmdline-tools"
    sudo mv cmdline-tools "$ANDROID_SDK_ROOT/cmdline-tools/latest"
    
    # Set proper ownership
    sudo chown -R $(whoami):$(whoami) "$ANDROID_SDK_ROOT"
    
    # Clean up
    rm "$CMDLINE_TOOLS_ZIP"
    
    print_status "Android SDK command line tools installed"
fi

# Set up environment variables
print_status "Setting up environment variables..."
export ANDROID_HOME="$ANDROID_SDK_ROOT"
export ANDROID_SDK_ROOT="$ANDROID_SDK_ROOT"
export PATH="$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools"

# Accept all licenses
print_status "Accepting Android SDK licenses..."
yes | "$ANDROID_SDK_ROOT/cmdline-tools/latest/bin/sdkmanager" --licenses > /dev/null 2>&1

# Install required SDK packages
print_status "Installing required Android SDK packages..."
"$ANDROID_SDK_ROOT/cmdline-tools/latest/bin/sdkmanager" \
    "platform-tools" \
    "build-tools;35.0.0" \
    "platforms;android-35" \
    "platforms;android-36" > /dev/null 2>&1

# Create or update local.properties file
print_status "Creating local.properties file..."
echo "sdk.dir=$ANDROID_SDK_ROOT" > local.properties

# Add environment variables to shell profile for persistence
SHELL_PROFILE=""
if [ -f "$HOME/.bashrc" ]; then
    SHELL_PROFILE="$HOME/.bashrc"
elif [ -f "$HOME/.zshrc" ]; then
    SHELL_PROFILE="$HOME/.zshrc"
fi

if [ -n "$SHELL_PROFILE" ]; then
    # Check if Android environment variables are already in the profile
    if ! grep -q "ANDROID_HOME" "$SHELL_PROFILE"; then
        print_status "Adding Android environment variables to $SHELL_PROFILE..."
        cat >> "$SHELL_PROFILE" << EOF

# Android SDK Environment Variables
export ANDROID_HOME="$ANDROID_SDK_ROOT"
export ANDROID_SDK_ROOT="$ANDROID_SDK_ROOT"
export PATH="\$PATH:\$ANDROID_HOME/cmdline-tools/latest/bin:\$ANDROID_HOME/platform-tools"
EOF
    else
        print_warning "Android environment variables already exist in $SHELL_PROFILE"
    fi
fi

# Verify installation
print_status "Verifying Android SDK installation..."
if [ -x "$ANDROID_SDK_ROOT/cmdline-tools/latest/bin/sdkmanager" ]; then
    SDK_VERSION=$("$ANDROID_SDK_ROOT/cmdline-tools/latest/bin/sdkmanager" --version 2>/dev/null | head -n1 || echo "Unknown")
    print_status "Android SDK Manager version: $SDK_VERSION"
else
    print_error "Failed to verify Android SDK installation"
    exit 1
fi

# Test Gradle build
echo ""
echo "🧪 Testing Gradle build..."
if ./gradlew tasks > /dev/null 2>&1; then
    print_status "Gradle can successfully access Android SDK"
else
    print_error "Gradle build test failed"
    exit 1
fi

echo ""
print_status "Android development environment setup completed successfully!"
echo ""
echo "📋 Summary:"
echo "   • Android SDK installed at: $ANDROID_SDK_ROOT"
echo "   • local.properties created with SDK path"
echo "   • All SDK licenses accepted"
echo "   • Required SDK packages installed"
echo "   • Environment variables configured"
echo ""
echo "💡 You can now run './gradlew build' to build your Android project"
echo ""

# Display current Java version
echo "☕ Current Java version:"
java -version 2>&1 | head -n1

echo ""
echo "🎉 Setup complete! Happy coding! 🚀"