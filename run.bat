@echo off
cd /d "%~dp0"
title Mobile Showroom Inventory Management System
echo ====================================================================
echo    Mobile Showroom Inventory Management System - MobileZone Pro
echo ====================================================================
echo Starting server...
echo URL: http://localhost:8080
echo Admin: admin / admin123
echo ====================================================================
java -jar target\inventory-0.0.1-SNAPSHOT.jar
pause
