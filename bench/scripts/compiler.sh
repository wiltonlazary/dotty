#!/usr/bin/env bash
find compiler/src/ -type f \( -name "*.scala" -or -name "*.java" \) -exec echo "scala3-bench-bootstrapped/jmh:run 5 10" {} + | sbt
