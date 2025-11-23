# Diffuse Binary

This directory contains the pre-built [Diffuse](https://github.com/JakeWharton/diffuse) binary (v0.3.0) used by the APK analysis scripts.

## Contents

- `bin/diffuse` - Unix/Linux/macOS executable
- `bin/diffuse.bat` - Windows batch script
- `lib/` - JAR dependencies required by Diffuse

## Version

**Diffuse v0.3.0** (released Feb 21, 2024)

## Why Committed to Repository?

The pre-built binary is committed to avoid:
- Cloning and building Diffuse from source on every run (~2-3 minutes)
- Network dependency for downloading on every execution
- CI/CD build time overhead

## Size

Total: ~33 MB (compressed in git)

## Updating

To update to a newer version of Diffuse:

1. Download the latest release:
   ```bash
   cd scripts/diffuse
   VERSION=0.4.0  # Update to desired version
   curl -L -o diffuse.zip "https://github.com/JakeWharton/diffuse/releases/download/${VERSION}/diffuse-${VERSION}.zip"
   ```

2. Extract and replace:
   ```bash
   rm -rf bin/ lib/
   unzip diffuse.zip
   mv diffuse-${VERSION}/* .
   rmdir diffuse-${VERSION}
   rm diffuse.zip
   ```

3. Test the binary:
   ```bash
   ./bin/diffuse --version
   ```

4. Update the version in this README and commit the changes.

## Usage

The binary is automatically used by `generate_apk_trend_report.py`:

```bash
python3 scripts/generate_apk_trend_report.py
```

No manual setup required!
