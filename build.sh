#!/bin/env bash

set -e

TARGET=target
LIB=lib

JUNIT_JAR="$LIB/junit-platform-console-standalone.jar"
JUNIT_URL="https://repo1.maven.org/maven2/org/junit/platform/junit-platform-console-standalone/1.10.2/junit-platform-console-standalone-1.10.2.jar"

MAIN_CLASS="jkvs.Main"

ensure_junit() {
  if [ ! -f "$JUNIT_JAR" ]; then
    echo "JUnit not found. Downloading..."

    mkdir -p $LIB

    curl -L "$JUNIT_URL" -o "$JUNIT_JAR"

    echo "Downloaded JUnit ✔"
  fi
}

compile() {
  echo "Compiling source..."
  rm -rf $TARGET
  mkdir -p $TARGET

  javac -d $TARGET jkvs/Main.java jkvs/lib/JKVStore.java jkvs/Std.java
}

compile_tests() {
  echo "Compiling tests..."
  javac -cp "$TARGET:$LIB/*" -d $TARGET tests/jkvs/Main.java
}

run_tests() {
  ensure_junit

  echo "Running tests..."
  java -jar "$JUNIT_JAR" \
    --class-path "$TARGET" \
    --scan-classpath
}

run_app() {
  shift
  compile
  java -cp "$TARGET" $MAIN_CLASS "$@"
}

case "$1" in

  test)
    compile
    compile_tests
    run_tests
    ;;

  run)
    run_app "$@"
    ;;

  *)
    echo "Usage:"
    echo "  ./build.sh test"
    echo "  ./build.sh run arg1 arg2"
    exit 1
    ;;

esac
# set -e
#
# rm -rf target
# mkdir -p target
#
# javac -d target jkvs/Main.java
# java -cp target jkvs.Main "$@"
