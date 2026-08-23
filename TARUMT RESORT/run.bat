@echo off
setlocal
cd /d "%~dp0"
chcp 65001 >nul

rem ---- Locate a JDK (prefers newest available) -------------------------------
set "JAVA_HOME="

if defined JAVA_HOME if exist "%JAVA_HOME%\bin\javac.exe" goto :jdkfound

for %%V in (26 25 21) do (
  if not defined JAVA_HOME for /d %%D in ("%USERPROFILE%\.jdks\*jdk%%~V*") do if exist "%%~fD\bin\javac.exe" set "JAVA_HOME=%%~fD"
  if not defined JAVA_HOME for /d %%D in ("C:\Program Files\Java\*jdk%%~V*") do if exist "%%~fD\bin\javac.exe" set "JAVA_HOME=%%~fD"
  if not defined JAVA_HOME for /d %%D in ("C:\Program Files\Eclipse Adoptium\*jdk%%~V*") do if exist "%%~fD\bin\javac.exe" set "JAVA_HOME=%%~fD"
  if not defined JAVA_HOME for /d %%D in ("%USERPROFILE%\Downloads\*jdk%%~V*\*jdk%%~V*") do if exist "%%~fD\bin\javac.exe" set "JAVA_HOME=%%~fD"
)

:jdkfound
if not defined JAVA_HOME set "JAVA_HOME=C:\Users\User\.jdks\oracleJdk-26"

set "JAVAC=%JAVA_HOME%\bin\javac.exe"
set "JAVA=%JAVA_HOME%\bin\java.exe"

if not exist "%JAVAC%" (
  echo.
  echo [ERROR] No JDK found. Install a JDK or edit JAVA_HOME in this batch file.
  echo         Searched: %%JAVA_HOME%%, %%USERPROFILE%%\.jdks, C:\Program Files\Java,
  echo                   C:\Program Files\Eclipse Adoptium
  echo.
  pause
  exit /b 1
)

if not exist build\classes mkdir build\classes
if not exist lib\pdfbox-app-3.0.3.jar (
  echo.
  echo [ERROR] Missing lib\pdfbox-app-3.0.3.jar - PDF reports will not work.
  echo         Download it from https://pdfbox.apache.org/ and place in the lib\ folder.
  echo.
)
set PDFBOX_JAR=lib\pdfbox-app-3.0.3.jar

echo Using Java:
"%JAVA%" -version
echo.

"%JAVAC%" --release 8 -encoding UTF-8 -cp "%PDFBOX_JAR%" -d build\classes src\adt\*.java src\boundary\*.java src\control\*.java src\dao\*.java src\entity\*.java src\utility\*.java
if errorlevel 1 (
  echo.
  echo Build failed. Check the compiler errors above.
  pause
  exit /b 1
)

"%JAVA%" -Dfile.encoding=UTF-8 -cp "build\classes;%PDFBOX_JAR%" control.TarumtResortsSystem
pause
