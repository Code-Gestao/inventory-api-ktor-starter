@echo off
setlocal
set DIR=%~dp0
set JAVA_EXE=java
if not "%JAVA_HOME%"=="" set JAVA_EXE=%JAVA_HOME%\bin\java.exe
"%JAVA_EXE%" -version >NUL 2>&1 || (
  echo Java is required. Please install JDK 17 and set JAVA_HOME.
  exit /b 1
)
gradle %*
