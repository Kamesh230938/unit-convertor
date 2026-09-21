#!/bin/sh

set -e

echo "========================================"
echo "      Unit Converter - Java Server"
echo "========================================"
echo

mkdir -p out

echo "Compiling Java..."
javac -d out src/UnitConverterServer.java

echo
echo "Starting server..."
echo "Open http://localhost:8080 in your browser."
echo "Press Ctrl+C to stop the server."
echo

java -cp out UnitConverterServer
