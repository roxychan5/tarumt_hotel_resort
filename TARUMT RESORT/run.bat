@echo off
setlocal
cd /d "%~dp0"
chcp 65001 >nul
if not exist build\classes mkdir build\classes
javac -encoding UTF-8 -d build\classes src\adt\*.java src\boundary\*.java src\control\*.java src\dao\*.java src\entity\*.java src\utility\*.java
if errorlevel 1 (
  echo.
  echo Build failed. Install a JDK and ensure javac is available on PATH.
  pause
  exit /b 1
)
java -Dfile.encoding=UTF-8 -cp build\classes control.TarumtResortsSystem
pause
