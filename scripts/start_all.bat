@echo off
setlocal

REM Finventory Local Startup Script (BAT)
REM Starts Database, Backend, and Frontend in separate PowerShell windows

set "SCRIPT_DIR=%~dp0"
for %%I in ("%SCRIPT_DIR%..") do set "PROJECT_ROOT=%%~fI"

set "ACTIVATE_PS1=%PROJECT_ROOT%\activate.ps1"
set "DB_PS1=%SCRIPT_DIR%start_db.ps1"
set "BACKEND_DIR=%PROJECT_ROOT%\backend"
set "FRONTEND_DIR=%PROJECT_ROOT%\frontend"

echo Activating Local Environment...

echo Starting Database (New Window)...
start "Finventory DB" powershell -NoExit -ExecutionPolicy Bypass -Command ". '%ACTIVATE_PS1%'; & '%DB_PS1%' -ShowLogs"

echo Waiting 5 seconds for Database initialization...
timeout /t 5 /nobreak >nul

echo Starting Backend (New Window)...
start "Finventory Backend" powershell -NoExit -ExecutionPolicy Bypass -Command ". '%ACTIVATE_PS1%'; $env:SPRING_PROFILES_ACTIVE='local'; $env:SPRING_DATASOURCE_URL='jdbc:postgresql://localhost:5432/finventory'; $env:SPRING_DATASOURCE_USERNAME='postgres'; $env:SPRING_DATASOURCE_PASSWORD='password'; $env:SPRING_DATASOURCE_DRIVER_CLASS_NAME='org.postgresql.Driver'; cd '%BACKEND_DIR%'; mvn -DskipTests spring-boot:run"

echo Starting Frontend (New Window)...
start "Finventory Frontend" powershell -NoExit -ExecutionPolicy Bypass -Command ". '%ACTIVATE_PS1%'; cd '%FRONTEND_DIR%'; npm run dev"

echo --------------------------------------------------------
echo Finventory Backend is starting in a new window.
echo Finventory Frontend is starting in a new window.
echo Database is running in a separate window.
echo Logs are visible in their respective windows.
echo --------------------------------------------------------
echo To stop the application:
echo 1. Close the Backend PowerShell window.
echo 2. Close the Frontend PowerShell window.
echo 3. Run 'scripts\stop_all.ps1' to stop the database.
echo    (Closing the DB window will NOT stop the database service)

endlocal
