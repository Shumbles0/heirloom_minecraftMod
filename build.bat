@echo off

if not defined JAVA_HOME set "JAVA_HOME=C:\Users\manue\.jdks\temurin-24.0.2"
if "%~1"=="" (
    call "%~dp0gradlew.bat" build
) else (
    call "%~dp0gradlew.bat" %*
)

pause
