#!/usr/bin/env bash
# Run this from /Users/erwanjouan/Development/java
# It will create the ddl-comparator project in that directory.

set -e

TARGET="/Users/erwanjouan/Development/java/ddl-comparator"
echo "Creating project at $TARGET ..."

mkdir -p "$TARGET/src/main/java/com/ddlcomparator/"{model,extractor,comparator,report}
mkdir -p "$TARGET/src/test/java/com/ddlcomparator"
mkdir -p "$TARGET/src/main/resources"

echo "✓ Directory structure created"
echo ""
echo "Now copy the source files from the downloaded archive into $TARGET"
echo "Then run:"
echo "  cd $TARGET && mvn clean package && mvn test"
