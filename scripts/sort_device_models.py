#!/usr/bin/env python3
"""
Sort device models in device-models.json by device name.

This script reads the device-models.json file, sorts the devices alphabetically
by their 'name' field, and writes the sorted data back to the file.

Usage:
    python scripts/sort_device_models.py
"""

import json
from pathlib import Path


def sort_device_models():
    """Sort device models in device-models.json by name."""
    # Get the project root directory
    script_dir = Path(__file__).parent
    project_root = script_dir.parent
    device_models_path = project_root / "api" / "resources" / "device-models.json"

    # Check if file exists
    if not device_models_path.exists():
        print(f"❌ Error: File not found at {device_models_path}")
        return 1

    print(f"📖 Reading {device_models_path.relative_to(project_root)}")

    # Read the JSON file
    with open(device_models_path, "r", encoding="utf-8") as f:
        data = json.load(f)

    # Get original count and first/last devices
    original_count = len(data["data"])
    original_first = data["data"][0]["name"] if data["data"] else None
    original_last = data["data"][-1]["name"] if data["data"] else None

    print(f"📊 Found {original_count} device models")
    print(f"   Current order: {original_first} ... {original_last}")

    # Sort the data array by 'name' field
    data["data"].sort(key=lambda device: device["name"])

    # Get new first/last devices after sorting
    sorted_first = data["data"][0]["name"] if data["data"] else None
    sorted_last = data["data"][-1]["name"] if data["data"] else None

    print(f"   Sorted order:  {sorted_first} ... {sorted_last}")

    # Write the sorted data back to the file
    with open(device_models_path, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2, ensure_ascii=False)
        f.write("\n")  # Add trailing newline

    print(f"✅ Successfully sorted and saved {device_models_path.relative_to(project_root)}")
    return 0


if __name__ == "__main__":
    exit(sort_device_models())
