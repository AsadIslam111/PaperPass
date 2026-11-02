@echo off
title PaperPass Autonomous Test Agent
cd /d "%~dp0"
echo =======================================================
echo Checking ADB connection to physical phone...
echo =======================================================
adb devices
echo.
echo Starting Autonomous Test Agent...
python autonomous_test_agent.py
pause

