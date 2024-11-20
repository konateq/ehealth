#!/bin/bash

# Initialization Script

# Define required versions
MIN_JAVA_VERSION="11"
MIN_MAVEN_VERSION="3.9.9"

# Function to compare versions
version_greater_equal() {
  printf '%s\n%s\n' "$2" "$1" | sort -V -C
}

# Check Java version
echo "Checking Java version..."
if command -v java >/dev/null 2>&1; then
  JAVA_VERSION=$(java -version 2>&1 | awk -F[\".] '/version/ {print $2"."$3}')
  echo "Java version found: $JAVA_VERSION"
  if version_greater_equal "$JAVA_VERSION" "$MIN_JAVA_VERSION"; then
    echo "Java version is sufficient."
  else
    echo "Java version is below the required minimum ($MIN_JAVA_VERSION). Please update."
    exit 1
  fi
else
  echo "Java is not installed or not in PATH."
  exit 1
fi

# Check Maven version
echo "Checking Maven version..."
if command -v mvn >/dev/null 2>&1; then
  MAVEN_VERSION=$(mvn -v 2>&1 | awk -F' ' '/Apache Maven/ {print $3}')
  echo "Maven version found: $MAVEN_VERSION"
  if version_greater_equal "$MAVEN_VERSION" "$MIN_MAVEN_VERSION"; then
    echo "Maven version is sufficient."
  else
    echo "Maven version is below the required minimum ($MIN_MAVEN_VERSION). Please update."
    exit 1
  fi
else
  echo "Maven is not installed or not in PATH."
  exit 1
fi

echo "Initialization complete."
