@echo off
title %0
setlocal enabledelayedexpansion

set ERROR_CODE=0
set "WRAPPER_JAR=%~dp0.mvn\wrapper\maven-wrapper.jar"
set "WRAPPER_PROPERTIES=%~dp0.mvn\wrapper\maven-wrapper.properties"
set "LAUNCHER_CLASS=org.apache.maven.wrapper.MavenWrapperMain"

if not exist "%~dp0.mvn\wrapper" (
    mkdir "%~dp0.mvn\wrapper"
)

if not exist "%WRAPPER_JAR%" (
    echo Downloading Maven Wrapper Jar...
    powershell -Command "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; try { (New-Object Net.WebClient).DownloadFile('https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar', '%WRAPPER_JAR%') } catch { Write-Error $_.Exception.Message; exit 1 }"
    if errorlevel 1 (
        echo Failed to download Maven Wrapper Jar.
        goto error
    )
)

if not "%JAVA_HOME%" == "" (
    set "JAVACMD=%JAVA_HOME%\bin\java.exe"
) else (
    set "JAVACMD=java"
)

"%JAVACMD%" %MAVEN_OPTS% -classpath "%WRAPPER_JAR%" "-Dmaven.multiModuleProjectDirectory=%~dp0." %LAUNCHER_CLASS% %*
if ERRORLEVEL 1 goto error
goto end

:error
set ERROR_CODE=1

:end
@endlocal & set ERROR_CODE=%ERROR_CODE%
exit /b %ERROR_CODE%
