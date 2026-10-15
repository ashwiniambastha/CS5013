@echo off
"%~dp0runtime\bin\java.exe" --enable-native-access=ALL-UNNAMED -jar "%~dp0app\tracker.jar" %*
exit /b %errorlevel%
