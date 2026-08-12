@echo off
setlocal
cd /d "%~dp0"
if not exist build\classes mkdir build\classes
javac -d build\classes src\adt\*.java src\boundary\*.java src\control\*.java src\dao\*.java src\entity\*.java src\utility\*.java
if errorlevel 1 (
  echo.
  echo Build failed. Install a JDK and ensure javac is available on PATH.
  pause
  exit /b 1
)
java -cp build\classes control.TarumtResortsSystem
pause
