@echo off
cd /d "%~dp0"
if not exist out mkdir out
javac -encoding UTF-8 -d out src\Batalha.java src\ServidorArena.java
if errorlevel 1 (
  echo Erro ao compilar. Verifique se o JDK 17+ esta instalado.
  pause
  exit /b 1
)
java -cp out ServidorArena
pause
