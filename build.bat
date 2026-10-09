@echo off
chcp 65001 >nul
echo Creation du JAR executable...
call mvn clean package
if errorlevel 1 pause
if not errorlevel 1 (
  echo.
  echo OK: target\smart-elevator-sma-1.0.0.jar
  echo Lancement: java -jar target\smart-elevator-sma-1.0.0.jar
  pause
)
