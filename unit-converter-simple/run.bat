@echo off
setlocal

echo ========================================
echo       Unit Converter - Java Server
echo ========================================
echo.

if not exist out mkdir out

echo Compiling Java...
javac -d out src\UnitConverterServer.java

if errorlevel 1 (
    echo.
    echo Compilation failed.
    pause
    exit /b 1
)

echo.
echo Starting server...
echo Open http://localhost:8080 in your browser.
echo Press Ctrl+C to stop the server.
echo.

java -cp out UnitConverterServer

pause
