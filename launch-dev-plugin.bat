@echo off
setlocal EnableExtensions EnableDelayedExpansion

cd /d "%~dp0"
set "PROJECT_DIR=%CD%"
set "LOG_DIR=%PROJECT_DIR%\logs"
if not exist "%LOG_DIR%" mkdir "%LOG_DIR%" >nul 2>&1
set "LOG_FILE=%LOG_DIR%\launch-dev-plugin.log"
set "LAST_COMMAND_LOG_FILE=%LOG_DIR%\last-command-output.log"
set "ESP_LOG_FILE=%LOG_FILE%"
break > "%LOG_FILE%"
break > "%LAST_COMMAND_LOG_FILE%"

set "CHECK_ONLY="
set "NO_PAUSE="

:parse_args
if "%~1"=="" goto after_args
if /I "%~1"=="--check-only" set "CHECK_ONLY=1"
if /I "%~1"=="--no-pause" set "NO_PAUSE=1"
shift
goto parse_args

:after_args
call :log "ESP dev launcher started."
call :log "Project directory: %PROJECT_DIR%"
call :log "Log file: %LOG_FILE%"

if not exist "%PROJECT_DIR%\gradlew.bat" (
	call :fail "gradlew.bat was not found in the project folder."
	exit /b 1
)

call :repair_gradle_java_home "%USERPROFILE%\.gradle\gradle.properties"
call :sanitize_java_option "GRADLE_OPTS"
call :sanitize_java_option "JAVA_OPTS"

call :find_java_home
if not defined SELECTED_JAVA_HOME (
	call :log "No working JDK was found. Trying to install Eclipse Adoptium Temurin 21 with winget."
	where winget >nul 2>&1
	if errorlevel 1 (
		call :log "winget is not available. Automatic JDK install was skipped."
	) else (
		call :run "winget install -e --id EclipseAdoptium.Temurin.21.JDK --accept-package-agreements --accept-source-agreements"
		call :find_java_home
	)
)

if not defined SELECTED_JAVA_HOME (
	call :fail "No working JDK was found. Install Java 17 or 21 and run this file again."
	exit /b 1
)

set "JAVA_HOME=%SELECTED_JAVA_HOME%"
set "PATH=%JAVA_HOME%\bin;%PATH%"
call :write_gradle_java_home "%PROJECT_DIR%\gradle.properties"
call :log "Using JAVA_HOME=%JAVA_HOME%"

call :run "java -version"
if errorlevel 1 (
	call :fail "Java still failed after the environment repair."
	exit /b 1
)

call :run "gradlew.bat --version"
if errorlevel 1 (
	call :fail "Gradle could not start with the repaired Java configuration."
	exit /b 1
)

if defined CHECK_ONLY (
	call :success "Environment check passed. Run launch-dev-plugin.bat without --check-only to start the plugin."
	exit /b 0
)

call :run "gradlew.bat run"
if errorlevel 1 (
	call :fail "Gradle run failed. See the log file for details."
	exit /b 1
)

call :success "ESP dev launch completed."
exit /b 0

:log
set "MESSAGE=%~1"
echo [%date% %time%] %MESSAGE%
>> "%LOG_FILE%" echo [%date% %time%] %MESSAGE%
exit /b 0

:run
set "ESP_RUN_COMMAND=%~1"
call :log "Running: %ESP_RUN_COMMAND%"
set "RUN_OUTPUT_FILE=%LAST_COMMAND_LOG_FILE%"
break > "%RUN_OUTPUT_FILE%"
cmd /c %ESP_RUN_COMMAND% > "%RUN_OUTPUT_FILE%" 2>&1
set "RUN_EXIT_CODE=%ERRORLEVEL%"
type "%RUN_OUTPUT_FILE%"
call :log "Exit code: %RUN_EXIT_CODE%"
exit /b %RUN_EXIT_CODE%

:repair_gradle_java_home
set "TARGET_FILE=%~1"
if not exist "%TARGET_FILE%" (
	call :log "No Gradle override file at %TARGET_FILE%"
	exit /b 0
)

set "ESP_TARGET_FILE=%TARGET_FILE%"
set "REPAIR_RESULT="
for /f "usebackq delims=" %%R in (`powershell -NoProfile -ExecutionPolicy Bypass -Command "$file = $env:ESP_TARGET_FILE; $lines = Get-Content -LiteralPath $file -ErrorAction SilentlyContinue; if ($null -eq $lines) { 'missing'; exit 0 }; $changed = $false; $kept = New-Object System.Collections.Generic.List[string]; foreach ($line in $lines) { if ($line -match '^\s*org\.gradle\.java\.home\s*=\s*(.+)\s*$') { $path = $Matches[1].Trim().Trim('\"'); if ($path -and -not (Test-Path -LiteralPath (Join-Path $path 'bin\java.exe'))) { $changed = $true; continue } }; $kept.Add($line) }; if ($changed) { Set-Content -LiteralPath $file -Value $kept -Encoding ASCII; 'removed' } else { 'ok' }"`) do set "REPAIR_RESULT=%%R"

if /I "%REPAIR_RESULT%"=="removed" (
	call :log "Removed invalid org.gradle.java.home from %TARGET_FILE%"
) else (
	call :log "Gradle override file checked: %TARGET_FILE%"
)
exit /b 0

:write_gradle_java_home
set "TARGET_FILE=%~1"
set "ESP_TARGET_FILE=%TARGET_FILE%"
set "ESP_JAVA_HOME=%JAVA_HOME%"
for /f "usebackq delims=" %%R in (`powershell -NoProfile -ExecutionPolicy Bypass -Command "$file = $env:ESP_TARGET_FILE; $javaHome = ($env:ESP_JAVA_HOME -replace '\\','/'); $lines = @(); if (Test-Path -LiteralPath $file) { $lines = Get-Content -LiteralPath $file -ErrorAction SilentlyContinue }; $changed = $false; $kept = New-Object System.Collections.Generic.List[string]; foreach ($line in $lines) { if ($line -match '^\s*org\.gradle\.java\.home\s*=') { if (-not $changed) { $kept.Add('org.gradle.java.home=' + $javaHome); $changed = $true }; continue }; $kept.Add($line) }; if (-not $changed) { $kept.Add('org.gradle.java.home=' + $javaHome) }; Set-Content -LiteralPath $file -Value $kept -Encoding ASCII; 'written'"`) do set "WRITE_RESULT=%%R"
call :log "Wrote project Gradle Java home override to %TARGET_FILE%"
exit /b 0

:sanitize_java_option
set "OPTION_NAME=%~1"
call set "OPTION_VALUE=%%%OPTION_NAME%%%"
if not defined OPTION_VALUE exit /b 0

echo %OPTION_VALUE% | findstr /I /C:"org.gradle.java.home" /C:"jre1.8.0" >nul
if errorlevel 1 exit /b 0

call :log "Clearing %OPTION_NAME% because it contains a stale Java override."
set "%OPTION_NAME%="
exit /b 0

:find_java_home
set "SELECTED_JAVA_HOME="
call :try_java_home "%JAVA_HOME%"
call :scan_jdk_dir "%ProgramFiles%\Eclipse Adoptium" "jdk-21*"
call :scan_jdk_dir "%ProgramFiles%\Eclipse Adoptium" "jdk-17*"
call :scan_jdk_dir "%ProgramFiles%\Microsoft" "jdk-21*"
call :scan_jdk_dir "%ProgramFiles%\Microsoft" "jdk-17*"
call :scan_jdk_dir "%ProgramFiles%\Java" "jdk-21*"
call :scan_jdk_dir "%ProgramFiles%\Java" "jdk-17*"
call :scan_jdk_dir "%ProgramFiles%\OpenJDK" "jdk-21*"
call :scan_jdk_dir "%ProgramFiles%\OpenJDK" "jdk-17*"
call :scan_jdk_dir "%ProgramFiles%\Zulu" "zulu*"
call :scan_jdk_dir "%ProgramFiles(x86)%\Eclipse Adoptium" "jdk-21*"
call :scan_jdk_dir "%ProgramFiles(x86)%\Eclipse Adoptium" "jdk-17*"
call :scan_jdk_dir "%LocalAppData%\Programs\Eclipse Adoptium" "jdk-21*"
call :scan_jdk_dir "%LocalAppData%\Programs\Eclipse Adoptium" "jdk-17*"

for /f "delims=" %%J in ('where java 2^>nul') do (
	call :try_java_from_exe "%%~fJ"
	if defined SELECTED_JAVA_HOME goto :eof
)
exit /b 0

:scan_jdk_dir
if defined SELECTED_JAVA_HOME exit /b 0
set "SCAN_ROOT=%~1"
set "SCAN_PATTERN=%~2"
if not defined SCAN_ROOT exit /b 0
if not exist "%SCAN_ROOT%" exit /b 0
pushd "%SCAN_ROOT%" >nul 2>&1 || exit /b 0
for /d %%D in (%SCAN_PATTERN%) do (
	popd >nul 2>&1
	call :try_java_home "%SCAN_ROOT%\%%D"
	exit /b 0
)
popd >nul 2>&1
exit /b 0

:try_java_from_exe
if defined SELECTED_JAVA_HOME exit /b 0
for %%D in ("%~dp1..") do call :try_java_home "%%~fD"
exit /b 0

:try_java_home
if defined SELECTED_JAVA_HOME exit /b 0
set "CANDIDATE=%~1"
if not defined CANDIDATE exit /b 0
for %%D in ("%CANDIDATE%\.") do set "CANDIDATE=%%~fD"
if not exist "%CANDIDATE%\bin\java.exe" exit /b 0
set "SELECTED_JAVA_HOME=%CANDIDATE%"
call :log "Found JDK at %SELECTED_JAVA_HOME%"
exit /b 0

:success
call :log "%~1"
echo.
echo Debug log: "%LOG_FILE%"
if not defined NO_PAUSE pause
exit /b 0

:fail
call :log "ERROR: %~1"
echo.
echo Debug log: "%LOG_FILE%"
if not defined NO_PAUSE pause
exit /b 1
