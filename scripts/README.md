# Scripts

This directory contains utility scripts for the TRMNL Android Buddy project.

## APK Size Trend Report Generator

**Script**: `generate_apk_trend_report.py`

Generates a comprehensive historical analysis of APK size evolution across all releases using [Diffuse](https://github.com/JakeWharton/diffuse).

### Features

- **Automated Diffuse Build**: Clones and builds Diffuse from source (no binary download required)
- **Release Analysis**: Analyzes 22 releases from 1.0.0 to 2.6.0
- **Trend Visualization**: Shows size evolution, method counts, and percentage changes
- **Detailed Reports**: Generates individual comparison reports for each consecutive release pair
- **Easy Updates**: Uses `releases.json` for release metadata

### Usage

```bash
# Run from repository root
python3 scripts/generate_apk_trend_report.py
```

### Prerequisites

- Python 3.7+
- Java 17+ (for building and running Diffuse)
- curl (for downloading APKs)
- git (for cloning Diffuse)

### Output Files

- **`docs/apk-size-trend.md`** - Main trend report with size evolution table and summary
- **`docs/apk-diffs/*.txt`** - Detailed Diffuse comparison reports (21 files, one per release pair)
- **`build/apk-trend-analysis/`** - Temporary build artifacts (git-ignored)
  - APKs downloaded from releases
  - Built Diffuse binary

### Updating for New Releases

When a new release is published:

1. Update `scripts/releases.json` with the new release information:
   ```json
   {
     "tag": "2.7.0",
     "published_at": "2025-11-20T12:00:00Z",
     "apk_size": 6234567,
     "apk_name": "trmnl-android-buddy-v2.7.0.apk"
   }
   ```

2. Run the script to regenerate the report:
   ```bash
   python3 scripts/generate_apk_trend_report.py
   ```

3. Commit the updated files:
   ```bash
   git add scripts/releases.json docs/apk-size-trend.md docs/apk-diffs/
   git commit -m "Update APK size trend report for v2.7.0"
   ```

### How It Works

1. **Diffuse Setup**: Clones Diffuse repository and builds it using Gradle
2. **Release Loading**: Reads release metadata from `releases.json`
3. **APK Download**: Downloads all release APKs from GitHub using curl
4. **Comparison**: Runs Diffuse on each consecutive release pair (1.0.0→1.0.1, 1.0.1→1.0.2, etc.)
5. **Report Generation**: Creates markdown report with:
   - Release history table with dates and sizes
   - Size changes between each release with metrics
   - Links to detailed Diffuse reports

### Performance

- First run: ~5-10 minutes (builds Diffuse, downloads all APKs, runs comparisons)
- Subsequent runs: ~2-3 minutes (reuses built Diffuse and cached APKs)

### Troubleshooting

**Build fails**: Ensure Java 17+ is installed and `JAVA_HOME` is set correctly

**Download fails**: Check internet connection and GitHub access

**Parse errors**: Verify `releases.json` is valid JSON format

## Release Metadata

**File**: `releases.json`

Contains metadata for all releases with APK assets. Used by the trend report generator.

### Format

```json
[
  {
    "tag": "1.0.0",
    "published_at": "2025-10-03T15:13:03Z",
    "apk_size": 4226252,
    "apk_name": "trmnl-buddy.apk"
  }
]
```

### Fields

- **tag**: Release version tag (e.g., "1.0.0", "2.6.0")
- **published_at**: ISO 8601 timestamp of release publication
- **apk_size**: APK file size in bytes
- **apk_name**: Name of the APK asset in the release

---

For more information about Diffuse and APK analysis, see [`docs/DIFFUSE_APK_ANALYSIS.md`](../docs/DIFFUSE_APK_ANALYSIS.md).
