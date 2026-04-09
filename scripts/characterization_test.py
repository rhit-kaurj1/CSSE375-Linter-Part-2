#!/usr/bin/env python3
"""
Simple characterization test runner for the Java linter project.

What this script does:
1) Builds the project so test .class files and jar exist.
2) Runs the linter against a small set of representative inputs.
3) Creates baseline output files on first run.
4) On later runs, compares current output against baselines.
5) Prints PASS/FAIL per test and exits non-zero on failures.
"""

from __future__ import annotations

import argparse
import difflib
import os
import shutil
import subprocess
import sys
from pathlib import Path
from typing import List


PROJECT_ROOT = Path(__file__).resolve().parent.parent
BASELINE_DIR = PROJECT_ROOT / "characterization_tests" / "baselines"
CURRENT_DIR = PROJECT_ROOT / "characterization_tests" / "current"
JAR_PATH = PROJECT_ROOT / "target" / "LinterProject-1.0-rc3-jar-with-dependencies.jar"


# These are intentionally simple starter cases:
# - clean class
# - messy class
# - near-threshold class
# - minimal/empty input
TEST_CASES = {
    "clean_class": ["target/classes/test/GoodCohesionExample.class"],
    "messy_class": ["target/classes/test/SRPViolationExample.class"],
    "near_threshold": ["target/classes/test/MultipleSRPViolationExample.class"],
    "empty_or_minimal_input": ["characterization_tests/inputs/empty_input.txt"],
}


def find_maven_command() -> List[str]:
    """Find a Maven command that works on this machine (Windows/Linux/macOS)."""
    # 1) Prefer project wrapper if present (most portable for beginners).
    mvnw_cmd = PROJECT_ROOT / "mvnw.cmd"
    mvnw_sh = PROJECT_ROOT / "mvnw"
    if mvnw_cmd.exists():
        return [str(mvnw_cmd)]
    if mvnw_sh.exists():
        return [str(mvnw_sh)]

    # 2) Try common PATH entries.
    for candidate in ["mvn", "mvn.cmd", "mvn.bat"]:
        resolved = shutil.which(candidate)
        if resolved:
            return [resolved]

    # 3) Try Maven home env vars if PATH is missing.
    # Keep env var logic explicit and beginner-readable.
    for env_var in ["MAVEN_HOME", "M2_HOME"]:
        value = os.environ.get(env_var)
        if not value:
            continue
        mvn_cmd = Path(value) / "bin" / "mvn.cmd"
        mvn_bat = Path(value) / "bin" / "mvn.bat"
        mvn_sh_candidate = Path(value) / "bin" / "mvn"
        if mvn_cmd.exists():
            return [str(mvn_cmd)]
        if mvn_bat.exists():
            return [str(mvn_bat)]
        if mvn_sh_candidate.exists():
            return [str(mvn_sh_candidate)]

    # 4) Last Windows fallback: common Apache Maven install folders.
    program_files_dirs = [
        Path("C:/Program Files"),
        Path("C:/Program Files (x86)"),
    ]
    for base_dir in program_files_dirs:
        if not base_dir.exists():
            continue
        for maven_dir in sorted(base_dir.glob("apache-maven*"), reverse=True):
            mvn_cmd = maven_dir / "bin" / "mvn.cmd"
            if mvn_cmd.exists():
                return [str(mvn_cmd)]

    print("FAIL: Could not find Maven executable.")
    print("Please install Maven or add it to PATH, then try again.")
    print("Tip on Windows: ensure mvn.cmd is available from PowerShell.")
    sys.exit(2)


def run_command(command: List[str], input_text: str | None = None) -> subprocess.CompletedProcess:
    """Run a command from project root and return completed process."""
    return subprocess.run(
        command,
        cwd=PROJECT_ROOT,
        text=True,
        input=input_text,
        capture_output=True,
        check=False,
    )


def build_project() -> None:
    """Build project so jar + class files are present before characterization runs."""
    print("Building project with Maven...")
    maven_cmd = find_maven_command()
    result = run_command(maven_cmd + ["-q", "-DskipTests", "package"])
    if result.returncode != 0:
        print("FAIL: Maven build failed.")
        print(result.stdout)
        print(result.stderr)
        sys.exit(2)

    if not JAR_PATH.exists():
        print(f"FAIL: Expected jar not found: {JAR_PATH}")
        sys.exit(2)


def run_linter_for_paths(paths: List[str]) -> str:
    """Run CLI linter once, feeding comma-separated paths through stdin."""
    abs_paths = [str((PROJECT_ROOT / rel_path).resolve()) for rel_path in paths]
    linter_input = ",".join(abs_paths) + "\n"

    # Use classpath + main class so we can run terminal mode (non-GUI) reliably.
    result = run_command(
        ["java", "-cp", str(JAR_PATH), "presentation.LinterMain"],
        input_text=linter_input,
    )

    # Keep stderr in output to preserve current behavior for characterization.
    return (result.stdout or "") + (result.stderr or "")


def write_text(path: Path, text: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text, encoding="utf-8")


def read_text(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def normalize_for_compare(text: str) -> str:
    """Normalize non-semantic formatting so comparisons focus on behavior output."""
    normalized = text.replace("\r\n", "\n").replace("\r", "\n")
    normalized = normalized.lstrip("\ufeff")

    lines = normalized.split("\n")
    while lines and lines[-1] == "":
        lines.pop()

    return "\n".join(lines)


def compare_output(test_name: str, current: str, baseline: str) -> bool:
    """Compare two outputs and print a compact diff when they differ."""
    current_normalized = normalize_for_compare(current)
    baseline_normalized = normalize_for_compare(baseline)

    if current_normalized == baseline_normalized:
        print(f"PASS: {test_name}")
        return True

    print(f"FAIL: {test_name}")
    diff_lines = list(
        difflib.unified_diff(
            baseline_normalized.splitlines(),
            current_normalized.splitlines(),
            fromfile=f"baseline/{test_name}.txt",
            tofile=f"current/{test_name}.txt",
            lineterm="",
        )
    )

    print("--- Difference (first 60 lines) ---")
    for line in diff_lines[:60]:
        print(line)
    if len(diff_lines) > 60:
        print("... (diff truncated)")

    return False


def main() -> int:
    parser = argparse.ArgumentParser(description="Run beginner-friendly characterization tests for linter output.")
    parser.add_argument(
        "--reset-baseline",
        action="store_true",
        help="Overwrite existing baselines with current output.",
    )
    args = parser.parse_args()

    BASELINE_DIR.mkdir(parents=True, exist_ok=True)
    CURRENT_DIR.mkdir(parents=True, exist_ok=True)

    build_project()

    passed = 0
    failed = 0
    created = 0

    print("\nRunning characterization tests...\n")

    for test_name, paths in TEST_CASES.items():
        current_output = run_linter_for_paths(paths)

        current_file = CURRENT_DIR / f"{test_name}.txt"
        baseline_file = BASELINE_DIR / f"{test_name}.txt"

        write_text(current_file, current_output)

        if args.reset_baseline or not baseline_file.exists():
            write_text(baseline_file, current_output)
            created += 1
            if args.reset_baseline:
                print(f"BASELINE RESET: {test_name}")
            else:
                print(f"BASELINE CREATED: {test_name}")
            continue

        if compare_output(test_name, current_output, read_text(baseline_file)):
            passed += 1
        else:
            failed += 1

    print("\nSummary")
    print(f"Baselines created/reset: {created}")
    print(f"Passed: {passed}")
    print(f"Failed: {failed}")

    # If we only created baselines, that is considered success for setup.
    if failed == 0:
        print("\nRESULT: PASS")
        return 0

    print("\nRESULT: FAIL")
    return 1


if __name__ == "__main__":
    raise SystemExit(main())
