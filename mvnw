#!/bin/bash
# Simple Maven wrapper that downloads Maven if needed

MAVEN_VERSION="3.9.6"
MAVEN_HOME="$HOME/.m2/apache-maven-$MAVEN_VERSION"
MAVEN_BIN="$MAVEN_HOME/bin/mvn"

if [ ! -f "$MAVEN_BIN" ]; then
    echo "Downloading Maven $MAVEN_VERSION..."
    mkdir -p "$HOME/.m2"
    cd "$HOME/.m2"
    curl -fsSL "https://dlcdn.apache.org/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.tar.gz" | tar xz
    cd -
fi

export PATH="$MAVEN_HOME/bin:$PATH"
exec mvn "$@"