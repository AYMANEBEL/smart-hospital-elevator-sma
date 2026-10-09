@echo off
chcp 65001 >nul
title Smart Elevator SMA
where mvn >nul 2>nul
if errorlevel 1 (
  echo [ERREUR] Maven n'est pas disponible dans PATH.
  echo Installe Maven ou ajoute son dossier bin dans PATH.
  pause
  exit /b 1
)

echo ============================================
echo   SMART ELEVATOR - SYSTEME MULTI-AGENT
echo ============================================
echo Compilation et lancement...
call mvn -q clean compile exec:java -Dexec.mainClass=com.smartelevator.Main
if errorlevel 1 (
  echo.
  echo Si JADE ne se telecharge pas, verifie ta connexion Internet.
  pause
)
