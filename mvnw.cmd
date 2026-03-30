@REM ----------------------------------------------------------------------------
@REM Maven Wrapper startup batch script for Windows
@REM ----------------------------------------------------------------------------

@IF "%__MVNW_ARG0_NAME__%"=="" (SET __MVNW_ARG0_NAME__=%~nx0)
@SET __MVNW_CMD__=
@SET __MVNW_ERROR__=
@SET __MVNW_PSMODULEP_SAVE=%PSModulePath%
@SET PSModulePath=
@FOR /F "usebackq tokens=1* delims==" %%A IN (`powershell -noprofile "& {$scriptDir='%~dp0'; $env:__MVNW_CMD__=Join-Path $scriptDir '.mvn\wrapper\maven-wrapper.jar'; if (-not (Test-Path -Path $env:__MVNW_CMD__ -PathType Leaf)) { $URL=(Get-Content -Raw (Join-Path $scriptDir '.mvn\wrapper\maven-wrapper.properties') | ConvertFrom-StringData).wrapperUrl; Write-Host \"Downloading $URL\"; [Net.ServicePointManager]::SecurityProtocol=[Net.SecurityProtocolType]::Tls12; try { (New-Object Net.WebClient).DownloadFile($URL, $env:__MVNW_CMD__) } catch { $env:__MVNW_ERROR__=$_.Exception.Message } }; echo __MVNW_CMD__=$env:__MVNW_CMD__ ; echo __MVNW_ERROR__=$env:__MVNW_ERROR__}"`) DO @SET %%A=%%B
@SET PSModulePath=%__MVNW_PSMODULEP_SAVE%
@SET __MVNW_PSMODULEP_SAVE=
@SET __MVNW_ARG0_NAME__=
@IF NOT "%__MVNW_ERROR__%"=="" @(
    @ECHO %__MVNW_ERROR__%
    @SET __MVNW_ERROR__=
    @EXIT /b 1
)
@IF NOT EXIST "%__MVNW_CMD__%" @(
    @ECHO Error: could not download maven wrapper jar
    @EXIT /b 1
)

@SET MVNW_MAVEN_COMMAND=%%MAVEN_HOME%%\bin\mvn
@SET __MVNW_JAVA_EXE__=java
@IF NOT "%JAVA_HOME%"=="" @SET __MVNW_JAVA_EXE__=%JAVA_HOME%\bin\java.exe
@SET __MVNW_PROJECT_BASEDIR__=%~dp0
@IF "%__MVNW_PROJECT_BASEDIR__:~-1%"=="\" @SET __MVNW_PROJECT_BASEDIR__=%__MVNW_PROJECT_BASEDIR__:~0,-1%

@REM Find Maven distribution from wrapper
@FOR /F "usebackq tokens=1,2 delims==" %%A IN ("%~dp0.mvn\wrapper\maven-wrapper.properties") DO @(
    @IF "%%A"=="distributionUrl" @SET MVNW_DIST_URL=%%B
)

"%__MVNW_JAVA_EXE__%" %JVM_CONFIG_MAVEN_PROPS% %MAVEN_OPTS% %MAVEN_DEBUG_OPTS% -classpath "%__MVNW_CMD__%" "-Dmaven.multiModuleProjectDirectory=%__MVNW_PROJECT_BASEDIR__%" org.apache.maven.wrapper.MavenWrapperMain %*
