@echo off
:: Disk Analyzer v2.0 - Windows One-Click Launcher
:: Copyright (c) 2026 Disk Analyzer

title Disk Analyzer v2.0 - Starting...

:: Set console colors
color 0A

:: Clear screen
cls

echo.
echo ===============================================
echo     Disk Analyzer v2.0 is starting...
echo ===============================================
echo.

:: Check Java environment
echo [1/4] Checking Java environment...
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Java not detected!
    echo.
    echo Please install Java 8 or higher:
    echo - Visit: https://www.java.com/en/download/
    echo - Download and install Java Runtime Environment
    echo.
    pause
    exit /b 1
)
echo SUCCESS: Java environment detected

:: Check project files
echo.
echo [2/4] Checking project files...

if not exist "target\classes" (
    echo WARNING: Compiled classes not found!
    echo Attempting to compile...
    echo.
    
    :: Try to compile
    echo Compiling source files...
    javac -cp "src/main/java" src/main/java/com/diskanalyzer/FinalDiskAnalyzer.java -d target/classes
    
    if %errorlevel% neq 0 (
        echo ERROR: Compilation failed!
        echo Please check your Java installation and source files
        pause
        exit /b 1
    )
    echo SUCCESS: Compilation completed
) else (
    echo SUCCESS: Project files check passed
)

:: Set memory parameters
echo.
echo [3/4] Setting runtime parameters...
set JAVA_OPTS=-Xmx2g -Xms512m -XX:+UseG1GC -XX:MaxGCPauseMillis=200
set CLASSPATH=target/classes
echo SUCCESS: Runtime parameters configured

:: Check admin privileges
echo.
echo [4/4] Checking permissions...
net session >nul 2>&1
if %errorlevel% equ 0 (
    echo SUCCESS: Administrator privileges detected
    echo You can scan system directories
) else (
    echo INFO: Running with standard privileges
    echo Some system directories may not be accessible
    echo For full access, right-click and select "Run as administrator"
)

:: Launch program
echo.
echo ===============================================
echo     Launching Disk Analyzer v2.0...
echo ===============================================
echo.
echo Features:
echo    * Direct scanning - no pre-scanning wait
echo    * Tree map visualization - intuitive space distribution
echo    * Real-time progress - scan speed at a glance
echo    * Smart memory management - supports large disks
echo    * System file protection - prevents accidental deletion
echo.
echo Usage Tips:
echo    * Click file blocks to view details
echo    * Drag and drop directories to scan
echo    * Right-click files for delete operations
echo    * Cancel scan anytime during process
echo.
echo ===============================================
echo.

:: Start Java program
java %JAVA_OPTS% -cp "%CLASSPATH%" com.diskanalyzer.FinalDiskAnalyzer

:: Program end message
echo.
echo ===============================================
echo     Disk Analyzer has closed
echo ===============================================
echo.
echo Thank you for using Disk Analyzer v2.0!
echo For issues, please check README.md
echo.
pause