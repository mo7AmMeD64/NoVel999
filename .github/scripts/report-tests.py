"""Publish Gradle JUnit results as a GitHub summary and accessible check annotations."""

import os
from pathlib import Path
import xml.etree.ElementTree as ET


def escape(value: str) -> str:
    return value.replace("%", "%25").replace("\r", "%0D").replace("\n", "%0A")


def property_escape(value: str) -> str:
    return escape(value).replace(":", "%3A").replace(",", "%2C")


reports = sorted(Path("app/build/test-results/testDebugUnitTest").glob("TEST-*.xml"))
if not reports:
    print("No unit test reports were produced (the build may have stopped before testing).")
    raise SystemExit(0)

counts = {"tests": 0, "failures": 0, "errors": 0, "skipped": 0}
for report in reports:
    suite = ET.parse(report).getroot()
    for key in counts:
        counts[key] += int(suite.get(key, "0"))
    for case in suite.findall("testcase"):
        for failure in list(case.findall("failure")) + list(case.findall("error")):
            title = f"{case.get('classname', '')}.{case.get('name', '')}"
            message = failure.text or failure.get("message", "Test failed")
            print(f"::error title={property_escape(title)}::{escape(message)}")

failed = counts["failures"] + counts["errors"]
passed = counts["tests"] - failed - counts["skipped"]
summary = f"{counts['tests']} tests: {passed} passed, {failed} failed, {counts['skipped']} skipped."
print(f"::notice title=Unit test results::{summary}")
if summary_file := os.environ.get("GITHUB_STEP_SUMMARY"):
    with open(summary_file, "a", encoding="utf-8") as output:
        output.write(f"\n## Unit test results\n\n{summary}\n")
