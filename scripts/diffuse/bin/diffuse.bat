@rem
@rem Copyright 2015 the original author or authors.
@rem
@rem Licensed under the Apache License, Version 2.0 (the "License");
@rem you may not use this file except in compliance with the License.
@rem You may obtain a copy of the License at
@rem
@rem      https://www.apache.org/licenses/LICENSE-2.0
@rem
@rem Unless required by applicable law or agreed to in writing, software
@rem distributed under the License is distributed on an "AS IS" BASIS,
@rem WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
@rem See the License for the specific language governing permissions and
@rem limitations under the License.
@rem

@if "%DEBUG%"=="" @echo off
@rem ##########################################################################
@rem
@rem  diffuse startup script for Windows
@rem
@rem ##########################################################################

@rem Set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" setlocal

set DIRNAME=%~dp0
if "%DIRNAME%"=="" set DIRNAME=.
@rem This is normally unused
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%..

@rem Resolve any "." and ".." in APP_HOME to make it shorter.
for %%i in ("%APP_HOME%") do set APP_HOME=%%~fi

@rem Add default JVM options here. You can also use JAVA_OPTS and DIFFUSE_OPTS to pass JVM options to this script.
set DEFAULT_JVM_OPTS=

@rem Find java.exe
if defined JAVA_HOME goto findJavaFromJavaHome

set JAVA_EXE=java.exe
%JAVA_EXE% -version >NUL 2>&1
if %ERRORLEVEL% equ 0 goto execute

echo. 1>&2
echo ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH. 1>&2
echo. 1>&2
echo Please set the JAVA_HOME variable in your environment to match the 1>&2
echo location of your Java installation. 1>&2

goto fail

:findJavaFromJavaHome
set JAVA_HOME=%JAVA_HOME:"=%
set JAVA_EXE=%JAVA_HOME%/bin/java.exe

if exist "%JAVA_EXE%" goto execute

echo. 1>&2
echo ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME% 1>&2
echo. 1>&2
echo Please set the JAVA_HOME variable in your environment to match the 1>&2
echo location of your Java installation. 1>&2

goto fail

:execute
@rem Setup the command line

set CLASSPATH=%APP_HOME%\lib\diffuse-0.3.0.jar;%APP_HOME%\lib\reports-0.3.0.jar;%APP_HOME%\lib\formats-0.3.0.jar;%APP_HOME%\lib\io-0.3.0.jar;%APP_HOME%\lib\clikt-jvm.jar;%APP_HOME%\lib\mordant-jvm.jar;%APP_HOME%\lib\okio-jvm-3.8.0.jar;%APP_HOME%\lib\picnic-jvm.jar;%APP_HOME%\lib\colormath-jvm.jar;%APP_HOME%\lib\markdown-jvm-0.5.2.jar;%APP_HOME%\lib\crossword-jvm.jar;%APP_HOME%\lib\kotlin-stdlib-jdk8-1.9.10.jar;%APP_HOME%\lib\kotlin-stdlib-jdk7-1.9.10.jar;%APP_HOME%\lib\kotlin-stdlib-1.9.22.jar;%APP_HOME%\lib\annotations-13.0.jar;%APP_HOME%\lib\byteunits-0.9.1.jar;%APP_HOME%\lib\asm-9.6.jar;%APP_HOME%\lib\apksig-8.2.2.jar;%APP_HOME%\lib\binary-resources-31.4.0-alpha10.jar;%APP_HOME%\lib\dalvik-dx-14.0.0_r21.jar;%APP_HOME%\lib\bundletool-1.15.6.jar;%APP_HOME%\lib\aapt2-proto-8.2.2-10154469.jar;%APP_HOME%\lib\protobuf-java-util-3.19.2.jar;%APP_HOME%\lib\protobuf-java-3.25.3.jar;%APP_HOME%\lib\java-diff-utils-4.12.jar;%APP_HOME%\lib\annotations-31.4.0-alpha10.jar;%APP_HOME%\lib\guava-30.1-jre.jar;%APP_HOME%\lib\auto-value-annotations-1.6.2.jar;%APP_HOME%\lib\error_prone_annotations-2.5.1.jar;%APP_HOME%\lib\dagger-2.28.3.jar;%APP_HOME%\lib\javax.inject-1.jar;%APP_HOME%\lib\jose4j-0.7.0.jar;%APP_HOME%\lib\slf4j-api-1.7.30.jar;%APP_HOME%\lib\jna-5.13.0.jar;%APP_HOME%\lib\failureaccess-1.0.1.jar;%APP_HOME%\lib\listenablefuture-9999.0-empty-to-avoid-conflict-with-guava.jar;%APP_HOME%\lib\jsr305-3.0.2.jar;%APP_HOME%\lib\checker-qual-3.5.0.jar;%APP_HOME%\lib\j2objc-annotations-1.3.jar;%APP_HOME%\lib\gson-2.8.6.jar;%APP_HOME%\lib\fastutil-core-8.5.12.jar


@rem Execute diffuse
"%JAVA_EXE%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %DIFFUSE_OPTS%  -classpath "%CLASSPATH%" com.jakewharton.diffuse.Diffuse %*

:end
@rem End local scope for the variables with windows NT shell
if %ERRORLEVEL% equ 0 goto mainEnd

:fail
rem Set variable DIFFUSE_EXIT_CONSOLE if you need the _script_ return code instead of
rem the _cmd.exe /c_ return code!
set EXIT_CODE=%ERRORLEVEL%
if %EXIT_CODE% equ 0 set EXIT_CODE=1
if not ""=="%DIFFUSE_EXIT_CONSOLE%" exit %EXIT_CODE%
exit /b %EXIT_CODE%

:mainEnd
if "%OS%"=="Windows_NT" endlocal

:omega
