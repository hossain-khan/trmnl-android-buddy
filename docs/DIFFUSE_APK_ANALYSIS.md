# APK Size Analysis with Diffuse

## Overview

This project uses [Diffuse](https://github.com/JakeWharton/diffuse) by Jake Wharton to automatically track APK size, method count, and code complexity changes in pull requests.

## What is Diffuse?

Diffuse is a tool for analyzing Android build artifacts (APKs, AABs, AARs, JARs) that provides:

- **Size tracking**: Compressed and uncompressed size of DEX, resources, assets, and manifest
- **Method count tracking**: DEX method, class, string, and type counts
- **Detailed breakdowns**: File-by-file size differences
- **Code-level changes**: Added/removed strings, types, and methods with full signatures

## How It Works

### Automated PR Analysis

When you create a pull request, the Diffuse workflow automatically:

1. **Downloads base APK**:
   - Fetches the APK from the latest GitHub release
   - Uses the official release APK as the comparison baseline
   - Ensures consistent comparison against production builds

2. **Builds PR APK**:
   - Compiles your feature branch code
   - Generates a release APK for comparison

3. **Runs Diffuse comparison**:
   - Uses [diffuse-action](https://github.com/usefulness/diffuse-action) for simplified workflow
   - Compares latest release APK vs your PR APK
   - Generates formatted diff report

4. **Posts results as PR comment**:
   - Summary table with size/method count changes
   - Detailed file-by-file breakdown
   - Automatic updates on subsequent pushes (avoids spam)

### Example Output

```
OLD: base.apk (signature: V2)
NEW: app-release.apk (signature: V2)

          │            compressed             │           uncompressed            
          ├───────────┬───────────┬───────────┼───────────┬───────────┬───────────
 APK      │ old       │ new       │ diff      │ old       │ new       │ diff      
──────────┼───────────┼───────────┼───────────┼───────────┼───────────┼───────────
      dex │   4.5 MiB │   4.5 MiB │ +21.3 KiB │   4.5 MiB │   4.5 MiB │ +21.3 KiB 
     arsc │ 900.6 KiB │ 900.6 KiB │       0 B │ 900.5 KiB │ 900.5 KiB │       0 B 
 manifest │     3 KiB │     3 KiB │       0 B │  12.5 KiB │  12.5 KiB │       0 B 
      res │ 334.8 KiB │ 334.8 KiB │       0 B │ 462.2 KiB │ 462.2 KiB │       0 B 
   native │  60.1 KiB │  60.1 KiB │       0 B │  58.9 KiB │  58.9 KiB │       0 B 
    asset │  50.2 KiB │  50.2 KiB │     -23 B │ 137.9 KiB │ 137.9 KiB │     -23 B 
    other │  51.4 KiB │  51.4 KiB │       0 B │ 112.7 KiB │ 112.7 KiB │       0 B 
──────────┼───────────┼───────────┼───────────┼───────────┼───────────┼───────────
    total │   5.9 MiB │   5.9 MiB │ +21.3 KiB │   6.1 MiB │   6.2 MiB │ +21.3 KiB 

 DEX     │ old   │ new   │ diff              
─────────┼───────┼───────┼───────────────────
   files │     1 │     1 │   0               
 strings │ 21167 │ 21259 │ +92 (+120 -28)    
   types │  6942 │  6958 │ +16 (+41 -25)     
 classes │  5722 │  5734 │ +12 (+20 -8)      
 methods │ 29532 │ 29599 │ +67 (+2642 -2575) 
  fields │ 19073 │ 19148 │ +75 (+2562 -2487) 

 ARSC    │ old  │ new  │ diff 
─────────┼──────┼──────┼──────
 configs │  133 │  133 │  0   
 entries │ 1574 │ 1574 │  0
```

## Interpreting Results

### APK Size Changes

- **Compressed**: Actual APK file size (what users download)
- **Uncompressed**: Size after extraction on device
- **Key metrics**:
  - `dex`: Compiled Kotlin/Java code
  - `arsc`: Android resources (strings, colors, etc.)
  - `res`: Drawable files, layouts, etc.

### DEX Changes

- **Methods**: Android has a 64K method limit per DEX file (multidex required beyond that)
- **Classes**: Number of classes in the app
- **Strings**: String constants in code
- **Types**: Kotlin/Java types (classes, interfaces, enums)

### What to Watch For

🟢 **Good signs**:
- Zero or negative size changes
- Method count decreases (code optimization)
- Resource size reductions

🟡 **Review needed**:
- Size increases >100KB (investigate why)
- Method count increases >50 (check for unnecessary dependencies)
- New classes from dependencies (verify they're needed)

🔴 **Action required**:
- Size increases >500KB (requires justification)
- Method count approaching 65K (multidex risk)
- Unexpected file additions

## Running Diffuse Locally

### Installation

**macOS**:
```bash
brew install JakeWharton/repo/diffuse
```

**Linux/Windows**:
```bash
# Download latest release
curl -L https://github.com/JakeWharton/diffuse/releases/download/0.3.0/diffuse-0.3.0-binary.jar -o diffuse.jar
```

### Usage

1. **Download base APK from latest release**:
   ```bash
   # Get latest release APK URL
   curl -s https://api.github.com/repos/hossain-khan/trmnl-android-buddy/releases/latest | \
     jq -r '.assets[] | select(.name | endswith(".apk")) | .browser_download_url'
   
   # Download it
   curl -L -o base.apk "https://github.com/hossain-khan/trmnl-android-buddy/releases/download/2.6.0/trmnl-android-buddy-v2.6.0.apk"
   ```

2. **Build your PR APK**:
   ```bash
   ./gradlew assembleRelease
   cp app/build/outputs/apk/release/app-release.apk pr.apk
   ```

3. **Run Diffuse**:
   ```bash
   # Using Homebrew installation
   diffuse diff base.apk pr.apk
   
   # Using JAR
   java -jar diffuse.jar diff base.apk pr.apk
   ```

3. **View single APK info**:
   ```bash
   diffuse info app-release.apk
   ```

4. **List methods/fields**:
   ```bash
   diffuse members app-release.apk
   diffuse members --methods app-release.apk
   diffuse members --fields app-release.apk
   ```

## Workflow Configuration

The Diffuse workflow is defined in `.github/workflows/diffuse.yml`.

### Workflow Features

- ✅ Runs on all PRs targeting `main`
- ✅ Downloads base APK from latest GitHub release (production baseline)
- ✅ Builds PR APK automatically
- ✅ Uses [diffuse-action](https://github.com/usefulness/diffuse-action) for simplified integration
- ✅ Posts results as PR comment with formatted output
- ✅ Updates existing comment on new pushes (no spam)
- ✅ Uploads both APKs as artifacts for manual inspection (7-day retention)

### Key Dependencies

- **diffuse-action**: GitHub Action wrapper that simplifies Diffuse integration
- **peter-evans/find-comment**: Finds existing Diffuse comment on PR
- **peter-evans/create-or-update-comment**: Creates or updates PR comment

### Permissions

The workflow requires:
- `contents: read` - To checkout code and access releases
- `pull-requests: write` - To post comments on PRs

## Best Practices

### For Contributors

1. **Review Diffuse output**: Check the PR comment after CI completes
2. **Investigate large increases**: If APK grows significantly, understand why
3. **Optimize when possible**: Look for opportunities to reduce size
4. **Document intentional increases**: Explain in PR description if size increase is expected

### For Reviewers

1. **Check Diffuse report**: Review size/method count changes before approving
2. **Question large increases**: Ask contributors to justify significant growth
3. **Suggest optimizations**: Recommend ProGuard/R8 rules, image compression, etc.
4. **Track trends**: Watch for gradual size creep across multiple PRs

## Optimization Tips

If Diffuse reveals size issues, consider:

1. **Dependency optimization**:
   - Remove unused dependencies
   - Use smaller alternatives (e.g., Moshi instead of Gson)
   - Exclude transitive dependencies not needed

2. **Code optimization**:
   - Enable R8 full mode
   - Add ProGuard/R8 rules for better optimization
   - Remove dead code

3. **Resource optimization**:
   - Use WebP instead of PNG/JPG
   - Enable resource shrinking
   - Use vector drawables instead of rasterized images
   - Remove unused resources

4. **Configuration**:
   - Enable code shrinking: `minifyEnabled true`
   - Enable resource shrinking: `shrinkResources true`
   - Optimize PNG files: `cruncherEnabled true`

## Troubleshooting

### Workflow fails to build APKs

**Problem**: Gradle build fails in CI

**Solutions**:
- Ensure `assembleRelease` works locally
- Check if signing configuration is correct (workflow uses unsigned APKs)
- Verify all dependencies are available

### No base APK found in latest release

**Problem**: Latest release doesn't contain an APK asset

**Solutions**:
- Verify that release workflow uploads APK (check `.github/workflows/android-release.yml`)
- Ensure at least one release exists with an APK asset
- Check release asset naming (must end with `.apk`)

### Diffuse output is empty

**Problem**: Diffuse action produces no output

**Solutions**:
- Check if both APK files were created successfully
- Verify APK paths are correct
- Check diffuse-action logs for errors

### Comment not posted on PR

**Problem**: Workflow runs but no comment appears

**Solutions**:
- Verify `pull-requests: write` permission is granted
- Check workflow logs for GitHub API errors
- Ensure `GITHUB_TOKEN` has required permissions
- Verify peter-evans actions are running correctly

## References

- [APK Size Trend Report](apk-size-trend.md) - Historical APK size analysis across all releases
- [Diffuse Repository](https://github.com/JakeWharton/diffuse)
- [diffuse-action Repository](https://github.com/usefulness/diffuse-action)
- [Diffuse Releases](https://github.com/JakeWharton/diffuse/releases/latest)
- [Android App Size Optimization](https://developer.android.com/topic/performance/reduce-apk-size)
- [R8 Optimization](https://developer.android.com/build/shrink-code)

## Maintenance

### Historical APK Size Trend

For a comprehensive view of APK size evolution across all releases, see the [APK Size Trend Report](apk-size-trend.md). This report shows:
- Size changes for all 22 releases from 1.0.0 to current
- Method and class count evolution
- Detailed comparisons between consecutive releases
- Links to individual Diffuse reports for each release pair

To regenerate the trend report with updated releases:

```bash
# Update scripts/releases.json with new release information
# Then run:
python3 scripts/generate_apk_trend_report.py
```

### Updating Diffuse Version

The `diffuse-action` automatically uses the latest version of Diffuse. To pin a specific version:

```yaml
- uses: usefulness/diffuse-action@v1
  with:
    old-file-path: base.apk
    new-file-path: pr.apk
    lib-version: 0.3.0  # Pin to specific version
```

### Customizing the Workflow

You can modify `.github/workflows/diffuse.yml` to:
- Add size threshold checks (fail CI if APK grows too much)
- Compare AAB files instead of APK
- Generate additional reports (AAR, JAR analysis)
- Store historical metrics
- Integrate with other tools (Slack notifications, etc.)

Example - Add size threshold check:
```yaml
- name: Check APK size threshold
  run: |
    # Extract size diff from output and fail if > 500KB
    SIZE_DIFF=$(echo "${{ steps.diffuse.outputs.diff-raw }}" | grep -oP 'total.*\K[+-]\d+')
    if [ "$SIZE_DIFF" -gt 512000 ]; then
      echo "APK size increased by more than 500KB!"
      exit 1
    fi
```

---

**Related Issues**: #367
**Workflow File**: `.github/workflows/diffuse.yml`
