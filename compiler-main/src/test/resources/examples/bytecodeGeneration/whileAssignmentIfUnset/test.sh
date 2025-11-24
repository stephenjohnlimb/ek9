#!/usr/bin/env bash
set -e
cd "$(dirname "$0")"
EK9_BINARY="../../../../../../../ek9-wrapper/target/bin/ek9"
EK9_HOME="../../../../../../target"
if [ ! -f "$EK9_BINARY" ]; then echo "ERROR: ek9 binary not found"; exit 1; fi
if [ ! -f "$EK9_HOME/ek9c-jar-with-dependencies.jar" ]; then echo "ERROR: Compiler JAR not found"; exit 1; fi
rm -rf .ek9 actual_*.txt
export EK9_HOME
$EK9_BINARY -C *.ek9
$EK9_BINARY whileAssignmentIfUnset.ek9 5 > actual_case_5.txt 2>&1
if ! diff -u expected_case_5.txt actual_case_5.txt; then echo "FAIL: Case 5"; exit 1; fi
$EK9_BINARY whileAssignmentIfUnset.ek9 0 > actual_case_0.txt 2>&1
if ! diff -u expected_case_0.txt actual_case_0.txt; then echo "FAIL: Case 0"; exit 1; fi
echo "PASS: whileAssignmentIfUnset"
