@echo off
title PaperPass Virtual Printer Server
cd /d "%~dp0"
echo Starting PaperPass PC Virtual Printer...
python virtual_printer.py
pause

