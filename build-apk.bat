@echo off
gradle :app:assembleDebug
if errorlevel 1 exit /b 1
copy /Y app\build\outputs\apk\debug\app-debug.apk Thrive11-v1.0-debug.apk
echo Created: Thrive11-v1.0-debug.apk
