@echo off
REM One-click build for Heirloom. Sets the JDK for this run, then builds the mod jar.
REM   build.bat            -> builds (jar lands in build\libs\)
REM   build.bat clean build-> any gradle args you pass are forwarded instead
REM Uses your existing JAVA_HOME if set; otherwise edit the fallback path below per machine.
if not defined JAVA_HOME set "JAVA_HOME=C:\Users\Shumbles\.jdks\temurin-24.0.2"
if "%~1"=="" (
    call "%~dp0gradlew.bat" build
) else (
    call "%~dp0gradlew.bat" %*
)
echo.
echo Done. Your mod jar is in: %~dp0build\libs\
pause
