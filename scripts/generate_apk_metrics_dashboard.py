#!/usr/bin/env python3
"""
Generate interactive APK metrics dashboard from Diffuse slim reports.

This script:
1. Parses all slim diffuse reports from docs/apk-diffs/*-slim.txt
2. Extracts key metrics (APK sizes, DEX counts, ARSC data)
3. Generates a static HTML dashboard with Chart.js visualizations
4. Can be re-run to update the dashboard with new release data

Usage:
    python3 scripts/generate_apk_metrics_dashboard.py

Output:
    - docs/apk-metrics-dashboard.html - Interactive dashboard with charts
"""

import json
import re
import sys
from pathlib import Path
from datetime import datetime
from typing import List, Dict, Optional


class Colors:
    """ANSI color codes for terminal output."""
    HEADER = '\033[95m'
    OKBLUE = '\033[94m'
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


def log_error(message: str):
    """Print error message."""
    print(f"{Colors.FAIL}✗ {message}{Colors.ENDC}")


def parse_size_value(size_str: str) -> Optional[float]:
    """
    Parse size string to MiB value.
    
    Examples:
        "3.2 MiB" -> 3.2
        "469 KiB" -> 0.458
        "912 B" -> 0.000868
    """
    if not size_str:
        return None
    
    match = re.search(r'([\d.]+)\s*(MiB|KiB|B)', size_str.strip())
    if not match:
        return None
    
    value = float(match.group(1))
    unit = match.group(2)
    
    if unit == 'MiB':
        return value
    elif unit == 'KiB':
        return value / 1024
    elif unit == 'B':
        return value / (1024 * 1024)
    
    return None


def parse_number_value(num_str: str) -> Optional[int]:
    """
    Parse number string to integer, handling commas and +/- prefixes.
    
    Examples:
        "21,481" -> 21481
        "+7 (+123 -116)" -> 7
        "  0  " -> 0
    """
    if not num_str:
        return None
    
    # Extract the main number (first number in the string)
    match = re.search(r'([+-]?\d+(?:,\d+)*)', num_str.strip())
    if not match:
        return None
    
    return int(match.group(1).replace(',', ''))


def parse_diffuse_slim_report(file_path: Path) -> Optional[Dict]:
    """
    Parse a slim diffuse report file and extract metrics.
    
    Returns dict with:
        - old_version: str
        - new_version: str
        - old_version_code: int
        - new_version_code: int
        - apk_sizes: dict with compressed/uncompressed sizes for components
        - dex_metrics: dict with DEX file statistics
        - arsc_metrics: dict with resource statistics
    """
    try:
        content = file_path.read_text()
        
        # Extract version information from header
        # Format: "Diffuse Comparison: 1.0.0 → 1.0.1"
        version_match = re.search(r'Diffuse Comparison:\s+([\d.]+)\s+→\s+([\d.]+)', content)
        if not version_match:
            log_error(f"Could not find version info in {file_path.name}")
            return None
        
        old_version = version_match.group(1)
        new_version = version_match.group(2)
        
        # Extract version codes from MANIFEST section
        # Format: " version code │ 1       │ 2       "
        version_code_match = re.search(r'version code\s+│\s+(\d+)\s+│\s+(\d+)', content)
        old_version_code = int(version_code_match.group(1)) if version_code_match else None
        new_version_code = int(version_code_match.group(2)) if version_code_match else None
        
        # Parse APK size table (compressed and uncompressed)
        # The table has format:
        # APK      │ old       │ new       │ diff     │ old       │ new       │ diff
        # dex      │   3.2 MiB │   3.2 MiB │   +912 B │   3.2 MiB │   3.2 MiB │    +912 B
        
        apk_table_match = re.search(
            r'APK\s+│.*?compressed.*?│.*?uncompressed.*?\n.*?───.*?\n(.*?)\n.*?───',
            content,
            re.DOTALL
        )
        
        apk_sizes = {
            'compressed': {},
            'uncompressed': {}
        }
        
        if apk_table_match:
            table_content = apk_table_match.group(1)
            for line in table_content.strip().split('\n'):
                if '│' not in line:
                    continue
                
                parts = [p.strip() for p in line.split('│')]
                if len(parts) < 7:
                    continue
                
                component = parts[0].strip()
                if not component or component == 'total':
                    if component == 'total':
                        # Parse total separately
                        new_compressed = parse_size_value(parts[2])
                        new_uncompressed = parse_size_value(parts[5])
                        if new_compressed:
                            apk_sizes['compressed']['total'] = new_compressed
                        if new_uncompressed:
                            apk_sizes['uncompressed']['total'] = new_uncompressed
                    continue
                
                # Parse NEW values (column index 2 for compressed, 5 for uncompressed)
                new_compressed = parse_size_value(parts[2])
                new_uncompressed = parse_size_value(parts[5])
                
                if new_compressed:
                    apk_sizes['compressed'][component] = new_compressed
                if new_uncompressed:
                    apk_sizes['uncompressed'][component] = new_uncompressed
        
        # Parse DEX metrics table
        # Format:
        # DEX     │ old   │ new   │ diff
        # files   │     1 │     1 │   0
        # strings │ 15957 │ 15958 │ +1 (+4 -3)
        
        dex_metrics = {}
        dex_table_match = re.search(
            r'DEX\s+│.*?\n.*?───.*?\n(.*?)(?:\n\s*\n|\n.*?ARSC)',
            content,
            re.DOTALL
        )
        
        if dex_table_match:
            table_content = dex_table_match.group(1)
            for line in table_content.strip().split('\n'):
                if '│' not in line:
                    continue
                
                parts = [p.strip() for p in line.split('│')]
                if len(parts) < 3:
                    continue
                
                metric = parts[0].strip()
                if metric in ['files', 'strings', 'types', 'classes', 'methods', 'fields']:
                    new_value = parse_number_value(parts[2])
                    if new_value is not None:
                        dex_metrics[metric] = new_value
        
        # Parse ARSC metrics table
        # Format:
        # ARSC    │ old │ new │ diff
        # configs │ 108 │ 108 │   0
        # entries │ 333 │ 347 │ +14 (+14 -0)
        
        arsc_metrics = {}
        arsc_table_match = re.search(
            r'ARSC\s+│.*?\n.*?───.*?\n(.*?)(?:\n\s*\n|$)',
            content,
            re.DOTALL
        )
        
        if arsc_table_match:
            table_content = arsc_table_match.group(1)
            for line in table_content.strip().split('\n'):
                if '│' not in line:
                    continue
                
                parts = [p.strip() for p in line.split('│')]
                if len(parts) < 3:
                    continue
                
                metric = parts[0].strip()
                if metric in ['configs', 'entries']:
                    new_value = parse_number_value(parts[2])
                    if new_value is not None:
                        arsc_metrics[metric] = new_value
        
        return {
            'old_version': old_version,
            'new_version': new_version,
            'old_version_code': old_version_code,
            'new_version_code': new_version_code,
            'apk_sizes': apk_sizes,
            'dex_metrics': dex_metrics,
            'arsc_metrics': arsc_metrics
        }
    
    except Exception as e:
        log_error(f"Failed to parse {file_path.name}: {e}")
        return None


def build_cumulative_data(parsed_reports: List[Dict]) -> List[Dict]:
    """
    Build cumulative version data from parsed reports.
    
    Each report shows old -> new, so we need to:
    1. Start with the first version (old from first report)
    2. Add each new version from subsequent reports
    """
    if not parsed_reports:
        return []
    
    # Sort reports by version code to ensure correct order
    sorted_reports = sorted(parsed_reports, key=lambda r: r['new_version_code'])
    
    cumulative_data = []
    
    # Add the first version (old from first report)
    first_report = sorted_reports[0]
    cumulative_data.append({
        'version': first_report['old_version'],
        'versionCode': first_report['old_version_code'],
        'compressed': first_report['apk_sizes']['compressed'],
        'uncompressed': first_report['apk_sizes']['uncompressed'],
        'dex': first_report['dex_metrics'],
        'arsc': first_report['arsc_metrics']
    })
    
    # Add all subsequent versions (new from each report)
    for report in sorted_reports:
        cumulative_data.append({
            'version': report['new_version'],
            'versionCode': report['new_version_code'],
            'compressed': report['apk_sizes']['compressed'],
            'uncompressed': report['apk_sizes']['uncompressed'],
            'dex': report['dex_metrics'],
            'arsc': report['arsc_metrics']
        })
    
    return cumulative_data


def generate_dashboard_html(cumulative_data: List[Dict], output_path: Path):
    """Generate the static HTML dashboard with embedded data."""
    
    # Convert data to JavaScript format
    js_data = json.dumps(cumulative_data, indent=12)
    
    generated_date = datetime.now().strftime('%B %d, %Y')
    
    html_content = f'''<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>TRMNL Android Buddy - APK Metrics Dashboard</title>
    <script src="https://cdn.jsdelivr.net/npm/chart.js@4.4.0/dist/chart.umd.min.js"></script>
    <style>
        * {{
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }}
        
        body {{
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: #333;
            padding: 20px;
            min-height: 100vh;
        }}
        
        .container {{
            max-width: 1400px;
            margin: 0 auto;
            background: white;
            border-radius: 16px;
            padding: 30px;
            box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
        }}
        
        header {{
            text-align: center;
            margin-bottom: 40px;
            padding-bottom: 20px;
            border-bottom: 3px solid #667eea;
        }}
        
        h1 {{
            font-size: 2.5em;
            color: #667eea;
            margin-bottom: 10px;
        }}
        
        .subtitle {{
            color: #666;
            font-size: 1.1em;
        }}
        
        .stats-grid {{
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
            gap: 20px;
            margin-bottom: 40px;
        }}
        
        .stat-card {{
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            padding: 20px;
            border-radius: 12px;
            box-shadow: 0 4px 15px rgba(102, 126, 234, 0.3);
        }}
        
        .stat-label {{
            font-size: 0.9em;
            opacity: 0.9;
            margin-bottom: 8px;
        }}
        
        .stat-value {{
            font-size: 2em;
            font-weight: bold;
        }}
        
        .chart-container {{
            margin-bottom: 50px;
            background: #f8f9fa;
            padding: 25px;
            border-radius: 12px;
            box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
        }}
        
        .chart-title {{
            font-size: 1.5em;
            color: #667eea;
            margin-bottom: 20px;
            font-weight: 600;
        }}
        
        .chart-wrapper {{
            position: relative;
            height: 400px;
        }}
        
        .chart-wrapper.tall {{
            height: 500px;
        }}
        
        footer {{
            text-align: center;
            margin-top: 40px;
            padding-top: 20px;
            border-top: 2px solid #e0e0e0;
            color: #666;
        }}
        
        .source-info {{
            background: #e3f2fd;
            padding: 15px;
            border-radius: 8px;
            margin-bottom: 30px;
            border-left: 4px solid #2196f3;
        }}
        
        footer a {{
            color: #667eea;
            text-decoration: none;
        }}
        
        footer a:hover {{
            text-decoration: underline;
        }}
    </style>
</head>
<body>
    <div class="container">
        <header>
            <h1>📊 TRMNL Android Buddy</h1>
            <p class="subtitle">APK Metrics Dashboard - Build History Analysis</p>
        </header>

        <div class="source-info">
            <strong>📝 Data Source:</strong> Diffuse reports from <code>docs/apk-diffs/*-slim.txt</code> 
            | <strong>Versions:</strong> <span id="version-range"></span>
            | <strong>Generated:</strong> {generated_date}
        </div>

        <div class="stats-grid" id="summary-stats">
            <!-- Stats will be populated by JavaScript -->
        </div>

        <div class="chart-container">
            <h2 class="chart-title">Total APK Size Over Time</h2>
            <div class="chart-wrapper">
                <canvas id="apkSizeChart"></canvas>
            </div>
        </div>

        <div class="chart-container">
            <h2 class="chart-title">APK Size Breakdown by Component</h2>
            <div class="chart-wrapper tall">
                <canvas id="componentBreakdownChart"></canvas>
            </div>
        </div>

        <div class="chart-container">
            <h2 class="chart-title">Size Changes Between Versions</h2>
            <div class="chart-wrapper">
                <canvas id="sizeChangesChart"></canvas>
            </div>
        </div>

        <div class="chart-container">
            <h2 class="chart-title">DEX Metrics Evolution</h2>
            <div class="chart-wrapper tall">
                <canvas id="dexMetricsChart"></canvas>
            </div>
        </div>

        <div class="chart-container">
            <h2 class="chart-title">Methods and Fields Count</h2>
            <div class="chart-wrapper">
                <canvas id="methodsFieldsChart"></canvas>
            </div>
        </div>

        <div class="chart-container">
            <h2 class="chart-title">Resource Entries Over Time</h2>
            <div class="chart-wrapper">
                <canvas id="resourcesChart"></canvas>
            </div>
        </div>

        <footer>
            <p>Generated using <a href="https://github.com/JakeWharton/diffuse" target="_blank">Diffuse</a> by Jake Wharton</p>
            <p>Regenerate: <code>python3 scripts/generate_apk_metrics_dashboard.py</code></p>
            <p>TRMNL Android Buddy © 2025</p>
        </footer>
    </div>

    <script>
        // Data extracted from diffuse slim reports
        const diffuseData = {js_data};

        // Extract versions
        const versions = diffuseData.map(d => d.version);
        
        // Update version range
        if (versions.length > 0) {{
            document.getElementById('version-range').textContent = 
                `${{versions[0]}} → ${{versions[versions.length - 1]}} (${{versions.length}} releases)`;
        }}

        // Calculate summary statistics
        const firstVersion = diffuseData[0];
        const lastVersion = diffuseData[diffuseData.length - 1];
        const totalSizeGrowth = (lastVersion.compressed.total - firstVersion.compressed.total) * 1024 * 1024;
        const methodsGrowth = lastVersion.dex.methods - firstVersion.dex.methods;
        const classesGrowth = lastVersion.dex.classes - firstVersion.dex.classes;

        // Format bytes to human readable
        function formatBytes(bytes) {{
            if (bytes >= 1024 * 1024) return (bytes / (1024 * 1024)).toFixed(2) + ' MiB';
            if (bytes >= 1024) return (bytes / 1024).toFixed(2) + ' KiB';
            return bytes.toFixed(0) + ' B';
        }}

        // Populate summary stats
        const summaryStats = document.getElementById('summary-stats');
        summaryStats.innerHTML = `
            <div class="stat-card">
                <div class="stat-label">Total Releases</div>
                <div class="stat-value">${{diffuseData.length}}</div>
            </div>
            <div class="stat-card">
                <div class="stat-label">Current APK Size</div>
                <div class="stat-value">${{lastVersion.compressed.total.toFixed(2)}} MiB</div>
            </div>
            <div class="stat-card">
                <div class="stat-label">Size Growth</div>
                <div class="stat-value">${{totalSizeGrowth > 0 ? '+' : ''}}${{formatBytes(totalSizeGrowth)}}</div>
            </div>
            <div class="stat-card">
                <div class="stat-label">Method Count</div>
                <div class="stat-value">${{lastVersion.dex.methods.toLocaleString()}}</div>
            </div>
            <div class="stat-card">
                <div class="stat-label">Methods Added</div>
                <div class="stat-value">+${{methodsGrowth.toLocaleString()}}</div>
            </div>
            <div class="stat-card">
                <div class="stat-label">Classes Added</div>
                <div class="stat-value">+${{classesGrowth.toLocaleString()}}</div>
            </div>
        `;

        // Chart.js default configuration
        Chart.defaults.font.family = '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif';
        Chart.defaults.color = '#666';

        // 1. Total APK Size Over Time
        new Chart(document.getElementById('apkSizeChart'), {{
            type: 'line',
            data: {{
                labels: versions,
                datasets: [{{
                    label: 'Compressed APK Size',
                    data: diffuseData.map(d => d.compressed.total),
                    borderColor: '#667eea',
                    backgroundColor: 'rgba(102, 126, 234, 0.1)',
                    borderWidth: 3,
                    fill: true,
                    tension: 0.3,
                    pointRadius: 4,
                    pointHoverRadius: 6
                }}]
            }},
            options: {{
                responsive: true,
                maintainAspectRatio: false,
                plugins: {{
                    legend: {{ display: true }},
                    tooltip: {{
                        callbacks: {{
                            label: (context) => `${{context.dataset.label}}: ${{context.parsed.y.toFixed(2)}} MiB`
                        }}
                    }}
                }},
                scales: {{
                    y: {{
                        beginAtZero: false,
                        title: {{ display: true, text: 'Size (MiB)' }}
                    }},
                    x: {{
                        title: {{ display: true, text: 'Version' }}
                    }}
                }}
            }}
        }});

        // 2. APK Size Breakdown by Component (Stacked Area)
        new Chart(document.getElementById('componentBreakdownChart'), {{
            type: 'line',
            data: {{
                labels: versions,
                datasets: [
                    {{
                        label: 'DEX',
                        data: diffuseData.map(d => d.compressed.dex || 0),
                        borderColor: '#667eea',
                        backgroundColor: 'rgba(102, 126, 234, 0.7)',
                        fill: true
                    }},
                    {{
                        label: 'Resources (ARSC)',
                        data: diffuseData.map(d => d.compressed.arsc || 0),
                        borderColor: '#764ba2',
                        backgroundColor: 'rgba(118, 75, 162, 0.7)',
                        fill: true
                    }},
                    {{
                        label: 'Res Files',
                        data: diffuseData.map(d => d.compressed.res || 0),
                        borderColor: '#f093fb',
                        backgroundColor: 'rgba(240, 147, 251, 0.7)',
                        fill: true
                    }},
                    {{
                        label: 'Native Libs',
                        data: diffuseData.map(d => d.compressed.native || 0),
                        borderColor: '#4facfe',
                        backgroundColor: 'rgba(79, 172, 254, 0.7)',
                        fill: true
                    }},
                    {{
                        label: 'Assets',
                        data: diffuseData.map(d => d.compressed.asset || 0),
                        borderColor: '#43e97b',
                        backgroundColor: 'rgba(67, 233, 123, 0.7)',
                        fill: true
                    }},
                    {{
                        label: 'Other',
                        data: diffuseData.map(d => d.compressed.other || 0),
                        borderColor: '#fa709a',
                        backgroundColor: 'rgba(250, 112, 154, 0.7)',
                        fill: true
                    }}
                ]
            }},
            options: {{
                responsive: true,
                maintainAspectRatio: false,
                plugins: {{
                    legend: {{ display: true, position: 'bottom' }},
                    tooltip: {{
                        mode: 'index',
                        callbacks: {{
                            label: (context) => `${{context.dataset.label}}: ${{context.parsed.y.toFixed(2)}} MiB`
                        }}
                    }}
                }},
                scales: {{
                    y: {{
                        stacked: true,
                        title: {{ display: true, text: 'Size (MiB)' }}
                    }},
                    x: {{
                        stacked: true,
                        title: {{ display: true, text: 'Version' }}
                    }}
                }}
            }}
        }});

        // 3. Size Changes Between Versions
        const sizeChanges = diffuseData.slice(1).map((d, i) => 
            (d.compressed.total - diffuseData[i].compressed.total) * 1024
        );
        const versionTransitions = diffuseData.slice(1).map((d, i) => 
            `${{diffuseData[i].version}} → ${{d.version}}`
        );

        new Chart(document.getElementById('sizeChangesChart'), {{
            type: 'bar',
            data: {{
                labels: versionTransitions,
                datasets: [{{
                    label: 'Size Change',
                    data: sizeChanges,
                    backgroundColor: sizeChanges.map(v => v >= 0 ? 'rgba(244, 67, 54, 0.7)' : 'rgba(76, 175, 80, 0.7)'),
                    borderColor: sizeChanges.map(v => v >= 0 ? '#f44336' : '#4caf50'),
                    borderWidth: 1
                }}]
            }},
            options: {{
                responsive: true,
                maintainAspectRatio: false,
                plugins: {{
                    legend: {{ display: false }},
                    tooltip: {{
                        callbacks: {{
                            label: (context) => {{
                                const val = context.parsed.y;
                                return `${{val >= 0 ? '+' : ''}}${{val.toFixed(2)}} KiB`;
                            }}
                        }}
                    }}
                }},
                scales: {{
                    y: {{
                        title: {{ display: true, text: 'Size Change (KiB)' }},
                        grid: {{ color: (context) => context.tick.value === 0 ? '#333' : 'rgba(0, 0, 0, 0.1)' }}
                    }},
                    x: {{
                        title: {{ display: true, text: 'Version Transition' }},
                        ticks: {{ maxRotation: 90, minRotation: 45 }}
                    }}
                }}
            }}
        }});

        // 4. DEX Metrics Evolution
        new Chart(document.getElementById('dexMetricsChart'), {{
            type: 'line',
            data: {{
                labels: versions,
                datasets: [
                    {{
                        label: 'Strings',
                        data: diffuseData.map(d => d.dex.strings || 0),
                        borderColor: '#667eea',
                        backgroundColor: 'rgba(102, 126, 234, 0.1)',
                        borderWidth: 2,
                        tension: 0.3,
                        yAxisID: 'y'
                    }},
                    {{
                        label: 'Types',
                        data: diffuseData.map(d => d.dex.types || 0),
                        borderColor: '#764ba2',
                        backgroundColor: 'rgba(118, 75, 162, 0.1)',
                        borderWidth: 2,
                        tension: 0.3,
                        yAxisID: 'y'
                    }},
                    {{
                        label: 'Classes',
                        data: diffuseData.map(d => d.dex.classes || 0),
                        borderColor: '#f093fb',
                        backgroundColor: 'rgba(240, 147, 251, 0.1)',
                        borderWidth: 2,
                        tension: 0.3,
                        yAxisID: 'y'
                    }}
                ]
            }},
            options: {{
                responsive: true,
                maintainAspectRatio: false,
                plugins: {{
                    legend: {{ display: true, position: 'bottom' }}
                }},
                scales: {{
                    y: {{
                        type: 'linear',
                        display: true,
                        position: 'left',
                        title: {{ display: true, text: 'Count' }}
                    }},
                    x: {{
                        title: {{ display: true, text: 'Version' }}
                    }}
                }}
            }}
        }});

        // 5. Methods and Fields Count
        new Chart(document.getElementById('methodsFieldsChart'), {{
            type: 'line',
            data: {{
                labels: versions,
                datasets: [
                    {{
                        label: 'Methods',
                        data: diffuseData.map(d => d.dex.methods || 0),
                        borderColor: '#667eea',
                        backgroundColor: 'rgba(102, 126, 234, 0.2)',
                        borderWidth: 3,
                        fill: true,
                        tension: 0.3,
                        pointRadius: 4
                    }},
                    {{
                        label: 'Fields',
                        data: diffuseData.map(d => d.dex.fields || 0),
                        borderColor: '#43e97b',
                        backgroundColor: 'rgba(67, 233, 123, 0.2)',
                        borderWidth: 3,
                        fill: true,
                        tension: 0.3,
                        pointRadius: 4
                    }}
                ]
            }},
            options: {{
                responsive: true,
                maintainAspectRatio: false,
                plugins: {{
                    legend: {{ display: true, position: 'top' }},
                    tooltip: {{
                        callbacks: {{
                            label: (context) => `${{context.dataset.label}}: ${{context.parsed.y.toLocaleString()}}`
                        }}
                    }}
                }},
                scales: {{
                    y: {{
                        title: {{ display: true, text: 'Count' }}
                    }},
                    x: {{
                        title: {{ display: true, text: 'Version' }}
                    }}
                }}
            }}
        }});

        // 6. Resource Entries Over Time
        new Chart(document.getElementById('resourcesChart'), {{
            type: 'bar',
            data: {{
                labels: versions,
                datasets: [{{
                    label: 'ARSC Entries',
                    data: diffuseData.map(d => d.arsc.entries || 0),
                    backgroundColor: 'rgba(102, 126, 234, 0.7)',
                    borderColor: '#667eea',
                    borderWidth: 1
                }}]
            }},
            options: {{
                responsive: true,
                maintainAspectRatio: false,
                plugins: {{
                    legend: {{ display: true }}
                }},
                scales: {{
                    y: {{
                        beginAtZero: true,
                        title: {{ display: true, text: 'Number of Entries' }}
                    }},
                    x: {{
                        title: {{ display: true, text: 'Version' }}
                    }}
                }}
            }}
        }});
    </script>
</body>
</html>'''
    
    output_path.write_text(html_content)
    log_success(f"Dashboard generated: {output_path}")


def main():
    """Main execution function."""
    print(f"\n{Colors.BOLD}{Colors.HEADER}APK Metrics Dashboard Generator{Colors.ENDC}\n")
    
    # Find all slim diffuse reports
    slim_reports_dir = Path("docs/apk-diffs")
    if not slim_reports_dir.exists():
        log_error(f"Directory not found: {slim_reports_dir}")
        return 1
    
    slim_files = sorted(slim_reports_dir.glob("*-slim.txt"))
    if not slim_files:
        log_error(f"No slim diffuse reports found in {slim_reports_dir}")
        return 1
    
    log_info(f"Found {len(slim_files)} slim diffuse reports")
    
    # Parse all reports
    parsed_reports = []
    for slim_file in slim_files:
        log_info(f"Parsing {slim_file.name}...")
        parsed = parse_diffuse_slim_report(slim_file)
        if parsed:
            parsed_reports.append(parsed)
            log_success(f"  ✓ {parsed['old_version']} → {parsed['new_version']}")
    
    if not parsed_reports:
        log_error("No reports could be parsed successfully")
        return 1
    
    log_success(f"Successfully parsed {len(parsed_reports)} reports")
    
    # Build cumulative data
    log_info("Building cumulative version data...")
    cumulative_data = build_cumulative_data(parsed_reports)
    log_success(f"Generated data for {len(cumulative_data)} versions")
    
    # Generate dashboard
    output_path = Path("docs/apk-metrics-dashboard.html")
    log_info("Generating dashboard HTML...")
    generate_dashboard_html(cumulative_data, output_path)
    
    print(f"\n{Colors.OKGREEN}{Colors.BOLD}✓ Dashboard generation complete!{Colors.ENDC}\n")
    log_info(f"Dashboard: {output_path}")
    log_info(f"Open with: open {output_path}")
    
    return 0


if __name__ == "__main__":
    sys.exit(main())
