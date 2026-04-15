@echo off
setlocal

set "ROOT_DIR=%~dp0"
cd /d "%ROOT_DIR%"

if not exist "%ROOT_DIR%backend\pom.xml" (
  echo [ERROR] backend\pom.xml not found. Please run this script in project root.
  pause
  exit /b 1
)

if not exist "%ROOT_DIR%frontend\package.json" (
  echo [ERROR] frontend\package.json not found. Please run this script in project root.
  pause
  exit /b 1
)

where npm >nul 2>nul
if errorlevel 1 (
  echo [ERROR] npm is not available in PATH.
  pause
  exit /b 1
)

netstat -ano | findstr ":8080" | findstr "LISTENING" >nul
if not errorlevel 1 (
  echo [WARN] Port 8080 is already in use. Backend may already be running.
)

netstat -ano | findstr ":5173" | findstr "LISTENING" >nul
if not errorlevel 1 (
  echo [WARN] Port 5173 is already in use. Frontend may already be running.
)

if not exist "%ROOT_DIR%start-backend.bat" (
  echo [ERROR] start-backend.bat not found.
  pause
  exit /b 1
)

if not exist "%ROOT_DIR%start-frontend.bat" (
  echo [ERROR] start-frontend.bat not found.
  pause
  exit /b 1
)

echo [INFO] Starting backend...
start "Lung Backend" /D "%ROOT_DIR%" cmd /k call "%ROOT_DIR%start-backend.bat"

echo [INFO] Starting frontend...
start "Lung Frontend" /D "%ROOT_DIR%" cmd /k call "%ROOT_DIR%start-frontend.bat"

echo [OK] Startup commands sent.
echo      Backend window:  Lung Backend
echo      Frontend window: Lung Frontend
echo.
echo Default URLs:
echo   Backend API:  http://localhost:8080
echo   Frontend UI:  http://localhost:5173

endlocal
