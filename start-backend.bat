@echo off
setlocal

set "ROOT_DIR=%~dp0"
set "BACKEND_DIR=%ROOT_DIR%backend"
set "MAVEN_CMD=D:\app\Apache\apache-maven-3.9.14\bin\mvn.cmd"
set "STORAGE_BASE_PATH=%ROOT_DIR%storage"

if not exist "%BACKEND_DIR%\pom.xml" (
  echo [ERROR] backend\pom.xml not found.
  pause
  exit /b 1
)

cd /d "%BACKEND_DIR%"

echo [INFO] STORAGE_BASE_PATH=%STORAGE_BASE_PATH%

if exist "%MAVEN_CMD%" (
  echo [INFO] Using Maven: %MAVEN_CMD%
  call "%MAVEN_CMD%" spring-boot:run
) else (
  echo [WARN] Custom Maven path not found, fallback to mvn in PATH.
  call mvn spring-boot:run
)

if errorlevel 1 (
  echo.
  echo [ERROR] Backend startup failed.
  pause
)

endlocal
