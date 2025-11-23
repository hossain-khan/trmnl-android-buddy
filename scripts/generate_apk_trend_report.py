#!/usr/bin/env python3
"""
Generate APK size trend report using Diffuse.

This script:
1. Clones and builds Diffuse from GitHub source
2. Loads release metadata from releases.json
3. Downloads APKs for each release
4. Runs Diffuse comparisons between consecutive releases
5. Generates an aggregated trend report with visualizations

Usage:
    python3 scripts/generate_apk_trend_report.py

Output:
    - docs/apk-size-trend.md - Markdown report with trend data
    - docs/apk-diffs/ - Directory with individual comparison reports
"""

import json
import os
import re
import subprocess
import sys
import urllib.request
from pathlib import Path
from typing import List, Dict, Tuple
from datetime import datetime

# Configuration
GITHUB_REPO = "hossain-khan/trmnl-android-buddy"
DIFFUSE_REPO = "https://github.com/JakeWharton/diffuse.git"
WORK_DIR = Path("build/apk-trend-analysis")
DIFFUSE_BUILD_DIR = WORK_DIR / "diffuse"
OUTPUT_DIR = Path("docs/apk-diffs")
REPORT_PATH = Path("docs/apk-size-trend.md")


class Colors:
    """ANSI color codes for terminal output."""
    HEADER = '\033[95m'
    OKBLUE = '\033[94m'
    OKCYAN = '\033[96m'
    OKGREEN = '\033[92m'
    WARNING = '\033[93m'
    FAIL = '\033[91m'
    ENDC = '\033[0m'
    BOLD = '\033[1m'


def log_info(message: str):
    """Print info message."""
    print(f"{Colors.OKBLUE}ℹ {message}{Colors.ENDC}")


def log_success(message: str):
    """Print success message."""
    print(f"{Colors.OKGREEN}✓ {message}{Colors.ENDC}")


def log_warning(message: str):
    """Print warning message."""
    print(f"{Colors.WARNING}⚠ {message}{Colors.ENDC}")


def log_error(message: str):
    """Print error message."""
    print(f"{Colors.FAIL}✗ {message}{Colors.ENDC}")


def download_file(url: str, output_path: Path) -> bool:
    """Download a file from URL to output path."""
    try:
        log_info(f"Downloading {url}")
        urllib.request.urlretrieve(url, output_path)
        log_success(f"Downloaded to {output_path}")
        return True
    except Exception as e:
        log_error(f"Failed to download {url}: {e}")
        return False


def get_releases() -> List[Dict]:
    """Load releases from the releases.json file."""
    log_info("Loading releases from releases.json...")
    
    try:
        releases_file = Path("scripts/releases.json")
        with open(releases_file, 'r') as f:
            releases = json.load(f)
        
        # Add apk_url for downloading
        for release in releases:
            release['apk_url'] = f"https://github.com/{GITHUB_REPO}/releases/download/{release['tag']}/{release['apk_name']}"
            release['name'] = f"Release {release['tag']}"
        
        log_success(f"Loaded {len(releases)} releases")
        return releases
    
    except Exception as e:
        log_error(f"Failed to load releases: {e}")
        return []


def build_diffuse(work_dir: Path) -> Path:
    """Build Diffuse from source."""
    diffuse_bin = DIFFUSE_BUILD_DIR / "diffuse" / "build" / "install" / "diffuse" / "bin" / "diffuse"
    
    if diffuse_bin.exists():
        log_info(f"Diffuse already built: {diffuse_bin}")
        return diffuse_bin
    
    # Check if directory exists and clean it if needed
    if DIFFUSE_BUILD_DIR.exists():
        log_info("Removing existing Diffuse build directory...")
        import shutil
        shutil.rmtree(DIFFUSE_BUILD_DIR)
    
    log_info("Cloning Diffuse repository...")
    try:
        # Clone the Diffuse repository
        subprocess.run(
            ['git', 'clone', '--depth', '1', DIFFUSE_REPO, str(DIFFUSE_BUILD_DIR)],
            check=True,
            capture_output=True
        )
        log_success("Diffuse repository cloned")
    except subprocess.CalledProcessError as e:
        log_error(f"Failed to clone Diffuse: {e}")
        raise Exception("Failed to clone Diffuse")
    
    log_info("Building Diffuse...")
    try:
        # Build Diffuse
        subprocess.run(
            ['./gradlew', 'installDist'],
            cwd=str(DIFFUSE_BUILD_DIR),
            check=True,
            capture_output=True
        )
        log_success("Diffuse built successfully")
        return diffuse_bin
    except subprocess.CalledProcessError as e:
        log_error(f"Failed to build Diffuse: {e}")
        raise Exception("Failed to build Diffuse")


def download_apk(release: Dict, work_dir: Path) -> Path:
    """Download APK for a release using curl."""
    apk_path = work_dir / f"{release['tag']}.apk"
    
    if apk_path.exists():
        log_info(f"APK already downloaded: {apk_path}")
        return apk_path
    
    log_info(f"Downloading APK for {release['tag']}...")
    try:
        # Use curl to download the APK
        subprocess.run(
            ['curl', '-L', '-o', str(apk_path), release['apk_url']],
            check=True,
            capture_output=True
        )
        
        if apk_path.exists():
            log_success(f"Downloaded: {apk_path}")
            return apk_path
        else:
            raise Exception(f"APK not found after download: {apk_path}")
    except subprocess.CalledProcessError as e:
        log_error(f"Failed to download APK: {e}")
        raise Exception(f"Failed to download APK for {release['tag']}")


def run_diffuse_comparison(diffuse_bin: Path, old_apk: Path, new_apk: Path) -> str:
    """Run Diffuse comparison between two APKs."""
    log_info(f"Running Diffuse: {old_apk.name} → {new_apk.name}")
    
    try:
        result = subprocess.run(
            [str(diffuse_bin), 'diff', str(old_apk), str(new_apk)],
            capture_output=True,
            text=True,
            check=True
        )
        log_success("Diffuse comparison completed")
        return result.stdout
    except subprocess.CalledProcessError as e:
        log_error(f"Diffuse failed: {e}")
        return f"ERROR: Diffuse comparison failed\n{e.stderr}"


def parse_diffuse_output(output: str) -> Dict:
    """Parse Diffuse output to extract key metrics."""
    metrics = {
        'apk_size_compressed_diff': None,
        'apk_size_uncompressed_diff': None,
        'dex_size_compressed_diff': None,
        'method_count_diff': None,
        'class_count_diff': None
    }
    
    # Regex pattern to match the total size difference in the APK table
    # Format: "total │ <size> │ <size> │ <diff> │ ..."
    # Example: "total │ 5.9 MiB │ 6.0 MiB │ +21.3 KiB │ ..."
    total_match = re.search(r'total\s+│[^│]+│[^│]+│\s*([+-]?[\d.]+\s*[KMGT]?i?B)', output)
    if total_match:
        metrics['apk_size_compressed_diff'] = total_match.group(1).strip()
    
    # Regex pattern to match method count changes
    # Format: "methods │ <old> │ <new> │ <diff>"
    method_match = re.search(r'methods\s+│\s*\d+\s+│\s*\d+\s+│\s*([+-]?\d+)', output)
    if method_match:
        metrics['method_count_diff'] = method_match.group(1).strip()
    
    # Regex pattern to match class count changes
    # Format: "classes │ <old> │ <new> │ <diff>"
    class_match = re.search(r'classes\s+│\s*\d+\s+│\s*\d+\s+│\s*([+-]?\d+)', output)
    if class_match:
        metrics['class_count_diff'] = class_match.group(1).strip()
    
    return metrics


def generate_slim_report(full_report_content: str) -> str:
    """
    Generate slim version of diffuse report by removing DEX section.
    
    Keeps everything before the DEX section, which includes:
    - APK size summary table
    - ARSC metrics
    - Manifest changes
    """
    lines = full_report_content.split('\n')
    slim_lines = []
    
    for line in lines:
        # Stop when we hit the DEX section header
        if line.strip().startswith('====   DEX   ===='):
            break
        slim_lines.append(line)
    
    return '\n'.join(slim_lines)


def generate_trend_report(releases: List[Dict], comparisons: List[Tuple[Dict, Dict, str, Dict]]):
    """Generate the aggregated trend report."""
    log_info("Generating trend report...")
    
    # Create output directory
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    
    # Save individual comparison reports (full and slim versions)
    for old_release, new_release, diff_output, metrics in comparisons:
        # Save full report
        report_file = OUTPUT_DIR / f"{old_release['tag']}_to_{new_release['tag']}.txt"
        with open(report_file, 'w') as f:
            f.write(f"Diffuse Comparison: {old_release['tag']} → {new_release['tag']}\n")
            f.write(f"{'=' * 80}\n\n")
            f.write(diff_output)
        log_success(f"Saved comparison: {report_file.name}")
        
        # Save slim report (without DEX section)
        slim_report_file = OUTPUT_DIR / f"{old_release['tag']}_to_{new_release['tag']}-slim.txt"
        full_report = f"Diffuse Comparison: {old_release['tag']} → {new_release['tag']}\n{'=' * 80}\n\n{diff_output}"
        slim_content = generate_slim_report(full_report)
        with open(slim_report_file, 'w') as f:
            f.write(slim_content)
        log_success(f"Saved slim version: {slim_report_file.name}")
    
    # Generate markdown report
    with open(REPORT_PATH, 'w') as f:
        f.write("# APK Size Trend Report\n\n")
        f.write("This report shows the APK size evolution across releases using [Diffuse](https://github.com/JakeWharton/diffuse).\n\n")
        f.write(f"**Generated**: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n\n")
        f.write(f"**Repository**: `{GITHUB_REPO}`\n\n")
        f.write(f"**Releases Analyzed**: {len(releases)}\n\n")
        
        # Summary table
        f.write("## Release History\n\n")
        f.write("| Version | Release Date | APK Size |\n")
        f.write("|---------|--------------|----------|\n")
        for release in releases:
            size_mb = release['apk_size'] / (1024 * 1024)
            date = datetime.fromisoformat(release['published_at'].replace('Z', '+00:00')).strftime('%Y-%m-%d')
            f.write(f"| {release['tag']} | {date} | {size_mb:.2f} MB |\n")
        
        # Comparison details
        f.write("\n## Size Changes Between Releases\n\n")
        for old_release, new_release, diff_output, metrics in comparisons:
            f.write(f"### {old_release['tag']} → {new_release['tag']}\n\n")
            
            # Calculate size change percentage
            old_size_mb = old_release['apk_size'] / (1024 * 1024)
            new_size_mb = new_release['apk_size'] / (1024 * 1024)
            size_change_mb = new_size_mb - old_size_mb
            size_change_pct = (size_change_mb / old_size_mb) * 100 if old_size_mb > 0 else 0
            
            change_emoji = "📈" if size_change_mb > 0 else "📉" if size_change_mb < 0 else "➡️"
            
            f.write(f"**APK Size**: {old_size_mb:.2f} MB → {new_size_mb:.2f} MB ")
            f.write(f"({change_emoji} {size_change_mb:+.2f} MB, {size_change_pct:+.1f}%)\n\n")
            
            # Key metrics
            if metrics['method_count_diff']:
                f.write(f"**Method Count Change**: {metrics['method_count_diff']}\n\n")
            if metrics['class_count_diff']:
                f.write(f"**Class Count Change**: {metrics['class_count_diff']}\n\n")
            
            # Link to detailed report
            report_file = f"apk-diffs/{old_release['tag']}_to_{new_release['tag']}.txt"
            f.write(f"[View detailed Diffuse report]({report_file})\n\n")
            f.write("---\n\n")
        
        # Footer
        f.write("\n## Notes\n\n")
        f.write("- All comparisons use release APKs (signed, production builds)\n")
        f.write("- Size measurements are for the compressed APK file\n")
        f.write("- Method and class counts reflect DEX file contents\n")
        f.write("- Detailed Diffuse reports available in `docs/apk-diffs/`\n")
        f.write("\n## Regenerating This Report\n\n")
        f.write("```bash\n")
        f.write("python3 scripts/generate_apk_trend_report.py\n")
        f.write("```\n")
    
    log_success(f"Trend report saved to {REPORT_PATH}")


def main():
    """Main execution function."""
    print(f"\n{Colors.BOLD}{Colors.HEADER}APK Size Trend Report Generator{Colors.ENDC}\n")
    
    # Create work directory
    WORK_DIR.mkdir(parents=True, exist_ok=True)
    log_success(f"Work directory: {WORK_DIR}")
    
    # Build Diffuse
    try:
        diffuse_bin = build_diffuse(WORK_DIR)
    except Exception as e:
        log_error(f"Failed to setup Diffuse: {e}")
        return 1
    
    # Get releases
    releases = get_releases()
    if len(releases) < 2:
        log_error("Need at least 2 releases with APKs to generate trend report")
        return 1
    
    # Download all APKs
    apk_paths = {}
    for release in releases:
        try:
            apk_path = download_apk(release, WORK_DIR)
            apk_paths[release['tag']] = apk_path
        except Exception as e:
            log_error(f"Failed to download APK for {release['tag']}: {e}")
            return 1
    
    # Run comparisons between consecutive releases
    comparisons = []
    for i in range(len(releases) - 1):
        old_release = releases[i]
        new_release = releases[i + 1]
        
        old_apk = apk_paths[old_release['tag']]
        new_apk = apk_paths[new_release['tag']]
        
        diff_output = run_diffuse_comparison(diffuse_bin, old_apk, new_apk)
        metrics = parse_diffuse_output(diff_output)
        
        comparisons.append((old_release, new_release, diff_output, metrics))
    
    # Generate report
    generate_trend_report(releases, comparisons)
    
    print(f"\n{Colors.OKGREEN}{Colors.BOLD}✓ APK trend report generation complete!{Colors.ENDC}\n")
    log_info(f"Report location: {REPORT_PATH}")
    log_info(f"Detailed diffs: {OUTPUT_DIR}")
    
    return 0


if __name__ == "__main__":
    sys.exit(main())
