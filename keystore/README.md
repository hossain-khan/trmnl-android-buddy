## Android Signing Keystore

This directory contains keystores for signing Android builds.

### Debug Keystore

For local development, a debug keystore (`debug.keystore`) is used with standard Android debug credentials:
- Store Password: `android`
- Key Alias: `androiddebugkey`
- Key Password: `android`

See https://developer.android.com/studio/publish/app-signing#debug-mode

### Release Keystore (Production)
Release builds are signed by workflows using securely stored keystore files and credentials.

### Workflows

Three GitHub Actions workflows are available for testing and building releases:

1. **android-release.yml**: Builds release APK on main branch pushes and GitHub releases
2. **test-keystore-apk-signing.yml**: Validates keystore configuration and APK signing
3. **test-keystore.yml**: Comprehensive keystore diagnostics and troubleshooting

See [release setup](../docs/RELEASE_SETUP.md) for details on how the workflows are setup with secrets.
