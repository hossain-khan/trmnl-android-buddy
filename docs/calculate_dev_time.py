#!/usr/bin/env python3
"""
Development Time Analysis Script

Analyzes git commit history to estimate development time using a configurable gap threshold.

Usage:
    python3 calculate_dev_time.py [gap_hours]

Arguments:
    gap_hours: Session gap threshold in hours (default: 1.0)
               If time between commits > gap_hours, a new session starts

Example:
    python3 calculate_dev_time.py        # Uses 1-hour threshold
    python3 calculate_dev_time.py 3.0    # Uses 3-hour threshold
"""

from datetime import datetime
import subprocess
import sys


def get_git_timestamps():
    """Fetch all commit timestamps from git repository."""
    try:
        result = subprocess.run(
            ['git', 'log', '--pretty=format:%ai', '--all'],
            capture_output=True,
            text=True,
            check=True
        )
        
        timestamps = []
        for line in result.stdout.strip().split('\n'):
            if line:
                dt = datetime.strptime(line.strip(), '%Y-%m-%d %H:%M:%S %z')
                timestamps.append(dt)
        
        return sorted(timestamps)
    except subprocess.CalledProcessError as e:
        print(f"Error running git command: {e}", file=sys.stderr)
        sys.exit(1)
    except Exception as e:
        print(f"Error parsing git log: {e}", file=sys.stderr)
        sys.exit(1)


def calculate_sessions(timestamps, gap_threshold_hours=1.0):
    """
    Calculate work sessions from commit timestamps.
    
    Args:
        timestamps: Sorted list of datetime objects
        gap_threshold_hours: Maximum gap between commits in same session (hours)
    
    Returns:
        List of session dictionaries with start, end, duration, and commits
    """
    if not timestamps:
        return []
    
    sessions = []
    current_session_start = timestamps[0]
    current_session_end = timestamps[0]
    current_session_commits = 1
    
    for i in range(1, len(timestamps)):
        gap = (timestamps[i] - timestamps[i-1]).total_seconds() / 3600  # gap in hours
        
        if gap <= gap_threshold_hours:
            # Continue current session
            current_session_end = timestamps[i]
            current_session_commits += 1
        else:
            # End current session, start new one
            duration = (current_session_end - current_session_start).total_seconds() / 3600
            sessions.append({
                'start': current_session_start,
                'end': current_session_end,
                'duration': duration,
                'commits': current_session_commits
            })
            current_session_start = timestamps[i]
            current_session_end = timestamps[i]
            current_session_commits = 1
    
    # Add final session
    duration = (current_session_end - current_session_start).total_seconds() / 3600
    sessions.append({
        'start': current_session_start,
        'end': current_session_end,
        'duration': duration,
        'commits': current_session_commits
    })
    
    return sessions


def print_analysis(sessions, gap_threshold_hours, total_commits):
    """Print formatted analysis results."""
    total_hours = sum(s['duration'] for s in sessions)
    sessions_sorted = sorted(sessions, key=lambda x: x['duration'], reverse=True)
    
    print("=" * 80)
    print(f"DEVELOPMENT TIME ANALYSIS (Gap Threshold: {gap_threshold_hours} hour{'s' if gap_threshold_hours != 1 else ''})")
    print("=" * 80)
    print(f"\nTotal Commits: {total_commits}")
    print(f"Total Sessions: {len(sessions)}")
    print(f"Total Development Time: {total_hours:.2f} hours")
    print(f"Average Session Duration: {total_hours/len(sessions):.2f} hours")
    
    print(f"\nTop 10 Longest Sessions:")
    print("-" * 80)
    print(f"{'Rank':<6}{'Date':<15}{'Duration':<12}{'Time Range':<25}{'Commits':<10}")
    print("-" * 80)
    
    for i, session in enumerate(sessions_sorted[:10], 1):
        date_str = session['start'].strftime('%b %d, %Y')
        duration_str = f"{session['duration']:.2f} hrs"
        start_time = session['start'].strftime('%H:%M')
        end_time = session['end'].strftime('%H:%M')
        time_range = f"{start_time} - {end_time}"
        commits = session['commits']
        
        print(f"{i:<6}{date_str:<15}{duration_str:<12}{time_range:<25}{commits:<10}")
    
    print("\n" + "=" * 80)
    print("METHODOLOGY:")
    print(f"  - Session Gap Threshold: {gap_threshold_hours} hour{'s' if gap_threshold_hours != 1 else ''}")
    print(f"  - If gap between commits ≤ {gap_threshold_hours}h → same session")
    print(f"  - If gap between commits > {gap_threshold_hours}h → new session")
    print("  - Session duration = time from first to last commit")
    print("  - Conservative estimate (excludes research, planning, testing without commits)")
    print("=" * 80)


def main():
    """Main function to run the analysis."""
    # Parse command line arguments
    gap_threshold_hours = 1.0  # Default: 1 hour
    if len(sys.argv) > 1:
        try:
            gap_threshold_hours = float(sys.argv[1])
            if gap_threshold_hours <= 0:
                print("Error: Gap threshold must be positive", file=sys.stderr)
                sys.exit(1)
        except ValueError:
            print(f"Error: Invalid gap threshold '{sys.argv[1]}'. Must be a number.", file=sys.stderr)
            sys.exit(1)
    
    # Fetch git timestamps
    print("Fetching git commit history...", file=sys.stderr)
    timestamps = get_git_timestamps()
    
    if not timestamps:
        print("No commits found in repository", file=sys.stderr)
        sys.exit(1)
    
    print(f"Found {len(timestamps)} commits", file=sys.stderr)
    print(f"Analyzing with {gap_threshold_hours}-hour gap threshold...\n", file=sys.stderr)
    
    # Calculate sessions
    sessions = calculate_sessions(timestamps, gap_threshold_hours)
    
    # Print results
    print_analysis(sessions, gap_threshold_hours, len(timestamps))


if __name__ == '__main__':
    main()
