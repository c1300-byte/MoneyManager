@echo off
rem ------------------------------------------------------------
rem  Money Manager - build the Windows app (dist\MoneyManager)
rem  Double-click this file, or run it from a terminal.
rem  Needs: JDK 17+ (for jpackage). Maven is optional: if it's
rem  missing, the ready-made jar in target\ is used instead.
rem  Add /nopause to skip the "Press any key" at the end.
rem ------------------------------------------------------------
setlocal
cd /d "%~dp0"

set "VERSION=2.0.0"
set "JAR=money-manager-%VERSION%.jar"

echo.
echo Money Manager %VERSION%
echo.

where mvn >nul 2>nul
if errorlevel 1 goto :no_maven
echo Building with Maven...
call mvn -q clean package
if errorlevel 1 goto :fail
goto :jar_check

:no_maven
echo Maven not found, using the prebuilt target\%JAR%

:jar_check
if not exist "target\%JAR%" (
    echo Could not find target\%JAR%
    goto :fail
)

set "JPACKAGE=jpackage"
where jpackage >nul 2>nul
if not errorlevel 1 goto :package
if not defined JAVA_HOME goto :no_jpackage
set "JPACKAGE=%JAVA_HOME%\bin\jpackage.exe"
if not exist "%JPACKAGE%" goto :no_jpackage

:package
if exist packaging-input rmdir /s /q packaging-input
mkdir packaging-input
copy /y "target\%JAR%" packaging-input\ >nul

if exist dist\MoneyManager rmdir /s /q dist\MoneyManager
if exist dist\MoneyManager (
    echo Close Money Manager first, then run this again.
    goto :fail
)

echo Packaging with jpackage...
"%JPACKAGE%" --type app-image --name MoneyManager --app-version %VERSION% ^
    --input packaging-input --main-jar %JAR% ^
    --main-class com.carsen.moneymanager.MoneyManagerApp ^
    --icon packaging\MoneyManager.ico --dest dist
if errorlevel 1 goto :fail

echo.
echo Done: dist\MoneyManager\MoneyManager.exe
echo Your data in %USERPROFILE%\.money-manager is not touched by this build.
echo.
if /i not "%~1"=="/nopause" pause
exit /b 0

:no_jpackage
echo jpackage wasn't found. Install a JDK 17 or newer and add its bin folder
echo to PATH, or set JAVA_HOME to the JDK folder.
goto :fail

:fail
echo.
echo Build failed.
if /i not "%~1"=="/nopause" pause
exit /b 1
