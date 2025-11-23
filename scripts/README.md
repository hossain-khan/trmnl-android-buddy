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

## APK Metrics Dashboard Generator

**Script**: `generate_apk_metrics_dashboard.py`

Generates an interactive HTML dashboard with Chart.js visualizations from slim diffuse reports.

### Features

- **Automatic Parsing**: Extracts metrics from all slim diffuse reports in `docs/apk-diffs/`
- **Interactive Charts**: 6 Chart.js visualizations (size trends, component breakdown, DEX metrics, etc.)
- **Summary Statistics**: Key metrics cards (total size, growth, method/class counts)
- **Reproducible**: Can be re-run anytime to update dashboard with latest data
- **Self-Contained**: Single HTML file with embedded data and Chart.js CDN

### Usage

```bash
# Run from repository root
python3 scripts/generate_apk_metrics_dashboard.py
```

### Prerequisites

- Python 3.7+ (standard library only, no pip packages needed)
- Slim diffuse reports must exist in `docs/apk-diffs/*-slim.txt`

### Output Files

- **`docs/apk-metrics-dashboard.html`** - Interactive dashboard (self-contained HTML)

### Charts Included

1. **Total APK Size Over Time** - Line chart showing size evolution
2. **APK Size Breakdown by Component** - Stacked area chart (DEX, ARSC, resources, etc.)
3. **Size Changes Between Versions** - Bar chart showing deltas (green=decrease, red=increase)
4. **DEX Metrics Evolution** - Multi-line chart (strings, types, classes)
5. **Methods and Fields Count** - Growth trends for method/field counts
6. **Resource Entries Over Time** - Bar chart of ARSC entries

### Updating for New Releases

When new diffuse reports are generated:

1. Create slim versions of new reports:
   ```bash
   cd docs/apk-diffs
   for file in *_to_NEW_VERSION.txt; do
     slim_file="${file%.txt}-slim.txt"
     awk '/^=================$/,/^====   DEX   ====$/ {
       if (/^====   DEX   ====$/) exit;
     }
     {print}' "$file" > "$slim_file"
   done
   ```

2. Regenerate the dashboard:
   ```bash
   python3 scripts/generate_apk_metrics_dashboard.py
   ```

3. Open the updated dashboard:
   ```bash
   open docs/apk-metrics-dashboard.html
   ```

### How It Works

1. **Discovery**: Finds all `*-slim.txt` files in `docs/apk-diffs/`
2. **Parsing**: Extracts metrics using regex patterns:
   - APK size table (compressed/uncompressed components)
   - DEX metrics table (strings, types, classes, methods, fields)
   - ARSC metrics table (configs, entries)
   - Version information from manifest
3. **Cumulative Data**: Builds version history starting from first release
4. **HTML Generation**: Creates static HTML with embedded JSON data and Chart.js visualizations

### Performance

- Runtime: ~1-2 seconds (parses 21 reports and generates HTML)
- Output size: ~100KB (self-contained HTML with data)

---

## Workflow: Complete APK Analysis Pipeline

### Initial Setup (One-Time)

```bash
# 1. Generate full diffuse reports from releases
python3 scripts/generate_apk_trend_report.py

# 2. Create slim versions (removes verbose DEX sections)
cd docs/apk-diffs
for file in *.txt; do
  if [[ ! "$file" =~ -slim\.txt$ ]]; then
    slim_file="${file%.txt}-slim.txt"
    awk '/^=================$/,/^====   DEX   ====$/ {
      if (/^====   DEX   ====$/) exit;
    }
    {print}' "$file" > "$slim_file"
  fi
done
cd ../..

# 3. Generate interactive dashboard
python3 scripts/generate_apk_metrics_dashboard.py

# 4. View the dashboard
open docs/apk-metrics-dashboard.html
```

### Regular Updates (After New Releases)

```bash
# 1. Update releases.json with new release info
# (edit scripts/releases.json)

# 2. Generate new diffuse reports
python3 scripts/generate_apk_trend_report.py

# 3. Create slim version for new report
cd docs/apk-diffs
# Create slim for the new report only
awk '/^=================$/,/^====   DEX   ====$/ {
  if (/^====   DEX   ====$/) exit;
}
{print}' "OLD_VERSION_to_NEW_VERSION.txt" > "OLD_VERSION_to_NEW_VERSION-slim.txt"
cd ../..

# 4. Regenerate dashboard with updated data
python3 scripts/generate_apk_metrics_dashboard.py

# 5. Commit all changes
git add scripts/releases.json docs/
git commit -m "Update APK analysis for vNEW_VERSION"
```

---

For more information about Diffuse and APK analysis, see [`docs/DIFFUSE_APK_ANALYSIS.md`](../docs/DIFFUSE_APK_ANALYSIS.md).
