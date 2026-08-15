@echo off
setlocal
cd /d "%~dp0"
chcp 65001 >nul
if not exist build\classes mkdir build\classes
if not exist lib\pdfbox-app-3.0.3.jar (
  echo.
  echo [ERROR] Missing lib\pdfbox-app-3.0.3.jar - PDF reports will not work.
  echo         Download it from https://pdfbox.apache.org/ and place in the lib\ folder.
  echo.
)
set PDFBOX_JAR=lib\pdfbox-app-3.0.3.jar
javac -encoding UTF-8 -cp "%PDFBOX_JAR%" -d build\classes src\adt\*.java src\boundary\*.java src\control\*.java src\dao\*.java src\entity\*.java src\utility\*.java
if errorlevel 1 (
  echo.
  echo Build failed. Install a JDK and ensure javac is available on PATH.
  pause
  exit /b 1
)
java -Dfile.encoding=UTF-8 -cp "build\classes;%PDFBOX_JAR%" control.TarumtResortsSystem
pause
