@echo off
setlocal

set "ROOT_DIR=%~dp0"
set "AI_DIR=%ROOT_DIR%lung_nodule_project"
set "AI_PYTHON=E:\pyhtonEnv\envs_dirs\py310torch2\python.exe"

if not exist "%AI_DIR%\infer\ai_inference_server.py" (
  echo [ERROR] AI server entry not found: %AI_DIR%\infer\ai_inference_server.py
  pause
  exit /b 1
)

cd /d "%AI_DIR%"

if not exist "%AI_PYTHON%" (
  echo [WARN] Custom python path not found: %AI_PYTHON%
  echo [WARN] Fallback to python in PATH.
  set "AI_PYTHON=python"
)

echo [INFO] Starting AI inference server with: %AI_PYTHON%
echo [INFO] URL: http://localhost:8000
echo [INFO] Note: Use module mode to enable real pipeline import path.
call "%AI_PYTHON%" -m infer.ai_inference_server --host 127.0.0.1 --port 8000

if errorlevel 1 (
  echo.
  echo [ERROR] AI server startup failed.
  pause
)

endlocal
