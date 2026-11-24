@echo off
rem ------------------------------------------------------------
rem  Money Manager - build and install to C:\Program Files\MoneyManager
rem
rem  Double-click this file. Windows asks for permission once,
rem  because Program Files needs admin rights.
rem
rem   1. Builds the latest version with build-windows.bat
rem   2. Replaces everything in C:\Program Files\MoneyManager
rem   3. Adds Start menu and desktop shortcuts that use the app logo
rem
rem  Your money data lives in %USERPROFILE%\.money-manager and is
rem  never touched by this script.
rem ------------------------------------------------------------
setlocal
cd /d "%~dp0"

if /i "%~1"=="/elevated" goto :install

echo Step 1 of 2: building the latest version...
echo.
call "%~dp0build-windows.bat" /nopause
if errorlevel 1 goto :fail_build

echo.
echo Step 2 of 2: installing. Click Yes when Windows asks for permission.
powershell -NoProfile -ExecutionPolicy Bypass -Command "try { $p = Start-Process -FilePath '%~f0' -ArgumentList '/elevated' -Verb RunAs -Wait -PassThru; exit $p.ExitCode } catch { exit 1 }"
if errorlevel 1 goto :fail_elevate
exit /b 0


:install
title Installing Money Manager
set "SRC=%~dp0dist\MoneyManager"
set "DEST=%ProgramFiles%\MoneyManager"

if not exist "%SRC%\MoneyManager.exe" (
    echo Couldn't find the new build at %SRC%
    goto :fail_install
)

:wait_close
tasklist /fi "imagename eq MoneyManager.exe" 2>nul | find /i "MoneyManager.exe" >nul
if errorlevel 1 goto :copy
echo.
echo Money Manager is still open. Close it, then press any key to continue.
pause >nul
goto :wait_close

:copy
echo Replacing everything in %DEST% ...
robocopy "%SRC%" "%DEST%" /MIR /R:2 /W:1 /NFL /NDL /NJH /NJS /NP >nul
if %errorlevel% geq 8 (
    echo Copying failed with robocopy error %errorlevel%.
    goto :fail_install
)

rem Keep a copy of the logo next to the app for the shortcuts.
copy /y "%~dp0packaging\MoneyManager.ico" "%DEST%\MoneyManager.ico" >nul

echo Updating Start menu and desktop shortcuts...
powershell -NoProfile -ExecutionPolicy Bypass -Command "$dest = '%DEST%'; $shell = New-Object -ComObject WScript.Shell; foreach ($folder in @([Environment]::GetFolderPath('CommonPrograms'), [Environment]::GetFolderPath('CommonDesktopDirectory'))) { $link = $shell.CreateShortcut((Join-Path $folder 'Money Manager.lnk')); $link.TargetPath = (Join-Path $dest 'MoneyManager.exe'); $link.WorkingDirectory = $dest; $link.IconLocation = (Join-Path $dest 'MoneyManager.ico') + ',0'; $link.Description = 'Money Manager'; $link.Save() }"

rem Ask Windows to refresh its icon cache so the logo shows right away.
ie4uinit.exe -show >nul 2>&1

echo.
echo Installed to %DEST%
echo Open it from the Start menu or the Money Manager shortcut on your desktop.
echo.
pause
exit 0


:fail_install
echo.
echo Install failed. Your money data was not changed.
pause
exit 1

:fail_build
echo.
echo The build failed, so nothing was installed.
pause
exit /b 1

:fail_elevate
echo.
echo The install didn't finish. If you clicked No on the permission prompt,
echo run this file again and click Yes.
pause
exit /b 1
