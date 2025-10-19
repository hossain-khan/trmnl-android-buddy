## Android Signing Keystore

This directory contains keystores for signing Android builds.

### Debug Keystore

For local development, a debug keystore (`debug.keystore`) is used with standard Android debug credentials:
- Store Password: `android`
- Key Alias: `androiddebugkey`
- Key Password: `android`

See https://developer.android.com/studio/publish/app-signing#debug-mode

### Release Keystore (Production)

For production releases, a separate release keystore is required. The release keystore should:
- Use strong, unique passwords
- Be securely stored (never commit to version control)
- Be backed up in a secure location
- Have a unique key alias

#### Local Development

For local release builds, the app will fall back to using the debug keystore if no production keystore is configured.

#### GitHub Actions / CI/CD

For automated builds via GitHub Actions, the release keystore is configured using repository secrets:

1. **KEYSTORE_BASE64**: Base64-encoded production keystore file
2. **KEYSTORE_PASSWORD**: Keystore password
3. **KEY_ALIAS**: Key alias within the keystore

The build system reads these environment variables and uses them to sign release builds.

### Creating a Production Release Keystore

To create a new production keystore:

```bash
keytool -genkey -v \
  -keystore trmnl-android-buddy-release.keystore \
  -alias trmnl-android-buddy \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -storetype PKCS12
```

**Important**: 
- Use the **same password** for both the keystore and the key
- Keep the keystore file secure and backed up
- Never commit the keystore to version control

### Converting Keystore to Base64 (for GitHub Secrets)

To prepare the keystore for GitHub Actions:

```bash
base64 -i trmnl-android-buddy-release.keystore | pbcopy  # macOS
base64 -i trmnl-android-buddy-release.keystore | xclip   # Linux
```

Then paste the output into the `KEYSTORE_BASE64` GitHub repository secret.

### Workflows

Three GitHub Actions workflows are available for testing and building releases:

1. **android-release.yml**: Builds release APK on main branch pushes and GitHub releases
2. **test-keystore-apk-signing.yml**: Validates keystore configuration and APK signing
3. **test-keystore.yml**: Comprehensive keystore diagnostics and troubleshooting

See [Setup Instructions](#setup-instructions) for configuration steps.