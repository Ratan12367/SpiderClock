@echo off
setlocal
set APP_HOME=%~dp0
set JAR=%APP_HOME%gradle\wrapper\gradle-wrapper.jar
set JAVACMD=java
if defined JAVA_HOME set JAVACMD=%JAVA_HOME%\bin\java.exe

if exist "%JAR%" (
  "%JAVACMD%" -Xmx64m -Xms64m -Dorg.gradle.appname=gradlew -classpath "%JAR%" org.gradle.wrapper.GradleWrapperMain %*
  exit /b %ERRORLEVEL%
)

rem No wrapper jar: bootstrap Gradle 8.9 (version must match gradle-wrapper.properties).
set GV=8.9
set DIR=%USERPROFILE%\.gradle\bootstrap\gradle-%GV%-bin
if not exist "%DIR%\gradle-%GV%\bin\gradle.bat" (
  echo Bootstrapping Gradle %GV% ...
  mkdir "%DIR%" 2>nul
  powershell -NoProfile -Command "Invoke-WebRequest -UseBasicParsing https://services.gradle.org/distributions/gradle-%GV%-bin.zip -OutFile '%DIR%\dist.zip'; Expand-Archive -Force '%DIR%\dist.zip' '%DIR%'"
)
call "%DIR%\gradle-%GV%\bin\gradle.bat" %*
exit /b %ERRORLEVEL%
