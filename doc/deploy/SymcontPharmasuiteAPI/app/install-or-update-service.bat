@echo off
setlocal EnableExtensions EnableDelayedExpansion

set "LOG_FILE=%~dp0install.log"
break > "%LOG_FILE%"

call :log "===== INICIO INSTALACION ====="
call :log "Script: %~nx0"
call :log "Fecha : %DATE% %TIME%"
call :log "Base  : %~dp0"

set "BASE_DIR=%~dp0"
set "CONFIG_FILE=%BASE_DIR%config.ini"

if not exist "%CONFIG_FILE%" (
  call :log [ERROR] No se encontro config.ini en %BASE_DIR%
  exit /b 1
)

call :log [INFO] Cargando configuracion desde config.ini...

for /f "usebackq tokens=1,* delims==" %%A in ("%CONFIG_FILE%") do (
  set "key=%%A"
  set "value=%%B"

  rem Ignorar comentarios y lineas vacias
  if not "!key!"=="" (
    if "!key:~0,1!" neq ";" if "!key:~0,1!" neq "#" (
      set "!key!=!value!"
    )
  )
)
cls
call :log [OK] Configuracion cargada

rem =====================================================
rem  CONFIG BASE
rem =====================================================
set "APP_HOME=%~dp0"
if "%APP_HOME:~-1%"=="\" set "APP_HOME=%APP_HOME:~0,-1%"
set "INSTALL_ROOT=%APP_HOME%\runtime"
set "JAVA_EXE=%APP_HOME%\lib\core\jdk\corretto-17.0.11\bin\java.exe"
set "NSSM=%APP_HOME%\lib\core\nssm.exe"

set "KEYTOOL=%APP_HOME%\lib\core\jdk\corretto-17.0.11\bin\keytool.exe"
set "SVC_NAME=Adasoft Pharmasuite Api"
set "SVC_DESCRIPTION=Adasoft pharmasuite API REST"
set "LOG4J2_RUNTIME=log4j2.xml"
set "INSTALLER_PS=%APP_HOME%\lib\%VERSION%"
set "RUNTIME_PS=%INSTALL_ROOT%\%VERSION%"


set "MEMORY_MIN=3g"
set "MEMORY_MAX=8g"
@call :log off
setlocal EnableExtensions EnableDelayedExpansion

call :log ======================================
call :log        CONFIGURACION DEL SERVICIO
call :log ======================================
call :log VERSION_PS      = %VERSION%
call :log ENABLE_CERT     = %ENABLE_CERT%
call :log APP_HOME        = %APP_HOME%
call :log INSTALLER_PS    = %INSTALLER_PS%
call :log SVC_NAME        = %SVC_NAME%
call :log DESCRIPTION     = %SVC_DESCRIPTION%
call :log JAVA_EXE        = %JAVA_EXE%
call :log NSSM            = %NSSM%
call :log MEMORY_MIN      = %MEMORY_MIN%
call :log MEMORY_MAX      = %MEMORY_MAX%
call :log CERT_STORE      = %CERT_STORE%
call :log SSL_TYPE        = %TYPE_SSL%
call :log SSL_PASSWORD    = ********
call :log ======================================


rem =====================================================
rem  Validaciones
rem =====================================================
if not exist "%JAVA_EXE%" call :log [ERROR] JAVA no encontrado path: %JAVA_EXE% & call :log [INFO] JAVA encontrado.
if not exist "%NSSM%" call :log [ERROR] NSSM no encontrado path: %NSSM%  & call :log [INFO] NSSM encontrado.


if "%ENABLE_CERT%"=="1" (


	rem =====================================================
	rem  VALIDAR CERTIFICADO SSL (password correcto)
	rem =====================================================
	call :log [INFO] Validando certificado SSL...

	if not exist "%KEYTOOL%" (
	  call :log [ERROR] keytool no encontrado: %KEYTOOL%
	  exit /b 1
	)

	"%KEYTOOL%" -list ^
	  -keystore "%CERT_STORE%" ^
	  -storepass "%CERT_PSWD_SSL%" ^
	  -storetype "%TYPE_SSL%" >nul 2>&1

	if errorlevel 1 (
	  call :log [ERROR] No se pudo abrir el certificado.
	  call :log [ERROR] La password es incorrecta o el certificado esta corrupto.
	  exit /b 1
	)

	call :log [OK] Certificado valido y password correcta
	call :log "Si es correcto continuar.."
)

rem =====================================================
rem  PREPARAR RUNTIME (DETECTAR / CONFIRMAR / BORRAR)
rem =====================================================
call :log [INFO] Preparando runtime en: %INSTALL_ROOT%

if exist "%INSTALL_ROOT%" (
  call :log [WARN] Instalacion previa detectada en:
  call :log [WARN] %INSTALL_ROOT%
  echo.
  echo =====================================================
  echo   SE HA DETECTADO UNA INSTALACION PREVIA
  echo   Ruta: %INSTALL_ROOT%
  echo.
  echo   Si continuas, la instalacion anterior se eliminara
  echo   por completo y se copiara una nueva.
  echo =====================================================
  echo.
)

rem --- borrar runtime si existe ---
if exist "%INSTALL_ROOT%" (
  call :log [INFO] Eliminando runtime existente...
  rmdir /s /q "%INSTALL_ROOT%"
)

rem --- verificar borrado ---
if exist "%INSTALL_ROOT%" (
  call :log [ERROR] No se pudo eliminar la carpeta runtime
  call :log [ERROR] Ruta: %INSTALL_ROOT%
  call :log [ERROR] Cierre procesos o revise permisos
  exit /b 1
)

rem --- crear runtime limpio ---
md "%INSTALL_ROOT%"

if not exist "%INSTALL_ROOT%" (
  call :log [ERROR] No se pudo crear la carpeta runtime
  exit /b 1
)

rem --- copiar contenido del instalador ---
call :log [INFO] Copiando contenido desde: %INSTALLER_PS% a: %RUNTIME_PS%


xcopy "%INSTALLER_PS%\*" "%RUNTIME_PS%\" /E /I /H /R /K /Y
set "RC=%ERRORLEVEL%"
call :log [INFO] XCOPY exit code: "%RC%"


if "%RC%"==1 (
  call :log [ERROR] XCOPY no encontro archivos para copiar en: %INSTALLER_PS%
  exit /b 1
)

call :log [OK] Runtime preparado correctamente

rem =====================================================
rem  JVM OPTIONS (UNA SOLA LINEA REAL)
rem =====================================================
set "JAVA_OPTS=-Xms%MEMORY_MIN% -Xmx%MEMORY_MIN% -XX:MaxGCPauseMillis=200"
set "JAVA_OPTS=%JAVA_OPTS% -XX:+UseG1GC"
set "JAVA_OPTS=%JAVA_OPTS% -XX:+ParallelRefProcEnabled -XX:+AlwaysPreTouch"
rem set "JAVA_OPTS=%JAVA_OPTS% -Xlog:gc*,gc+heap=info:file=gc/logs/gc.log:time,uptime,level,tags"
set "JAVA_OPTS=%JAVA_OPTS% -Djava.awt.headless=true"
if "%ENABLE_CERT%"=="1" (
  call :log [INFO] Añadimos parametros ssl a JAVA_OPTS
  set "JAVA_OPTS=!JAVA_OPTS! -Djavax.net.ssl.trustStore=!CERT_STORE!"
  set "JAVA_OPTS=!JAVA_OPTS! -Djavax.net.ssl.trustStorePassword=!CERT_PSWD_SSL!"
  set "JAVA_OPTS=!JAVA_OPTS! -Djavax.net.ssl.trustStoreType=!TYPE_SSL!"
)
set "JAVA_OPTS=%JAVA_OPTS% --add-opens=java.desktop/java.beans=ALL-UNNAMED"


rem set "JAVA_OPTS=%JAVA_OPTS% -Dcom.rockwell.test.password=PASWORD"
rem set "JAVA_OPTS=%JAVA_OPTS% -Dcom.rockwell.test.username=USER"

rem =====================================================
rem  Spring / App args
rem =====================================================
set "LOG4J2_RUNTIME=%INSTALL_ROOT_FWD%/%VERSION%/log4j2.xml"
call :log [OK] Usando LOG4J2: %LOG4J2_RUNTIME%
set "APP_ARGS=--spring.profiles.active=dev --logging.config=log4j2.xml"


rem =====================================================
rem  Localizar JAR (más reciente)
rem =====================================================
set "LATEST_JAR="
set "PATH_SEARCH_JAR=%INSTALL_ROOT%\%VERSION%\*.jar"

call :log [INFO] Buscando JAR en: %PATH_SEARCH_JAR%

for /f "delims=" %%F in ('dir "%PATH_SEARCH_JAR%" /b /o-d 2^>nul') do (
  set "LATEST_JAR=%INSTALL_ROOT%\%VERSION%\%%F"
  goto :jar_found
)

:jar_found
if not defined LATEST_JAR (
  call :log [ERROR] No se encontro ningun JAR en %INSTALL_ROOT%\%VERSION%
  exit /b 1
)

call :log [OK] JAR seleccionado: %LATEST_JAR%

rem =====================================================
rem  Parar y eliminar servicio si existe
rem =====================================================
call :log ======================================
call :log        DETECTAR SERVICIO
call :log ======================================

sc query "%SVC_NAME%" >nul 2>&1
if %errorlevel%==0 (
    call :log [INFO] Servicio existente detectado: %SVC_NAME%

    call :log [INFO] Parando servicio...
    "%NSSM%" stop "%SVC_NAME%" >nul 2>&1
    if %errorlevel% neq 0 (
        call :log [WARN] No fue posible parar el servicio o ya estaba parado
    ) else (
        call :log [OK] Servicio parado
    )

    timeout /t 2 /nobreak >nul

    call :log [INFO] Eliminando servicio existente...
    "%NSSM%" remove "%SVC_NAME%" confirm >nul 2>&1
    if %errorlevel% neq 0 (
        call :log [ERROR] Fallo eliminando el servicio

        exit /b 1
    ) else (
        call :log [OK] Servicio eliminado
    )

    timeout /t 1 /nobreak >nul
) else (
    call :log [INFO] El servicio no existe. Instalación limpia.
)

rem =====================================================
rem  Crear / Instalar servicio con NSSM
rem =====================================================
call :log ======================================
call :log        INSTALAR SERVICIO
call :log ======================================

call :log [INFO] Instalando servicio %SVC_NAME%...

"%NSSM%" install "%SVC_NAME%" "%JAVA_EXE%" >nul 2>&1
if %errorlevel% neq 0 (
    call :log [ERROR] Error instalando servicio con NSSM

    exit /b 1
)

call :log [OK] Servicio creado

rem =====================================================
rem  Configurar propiedades del servicio
rem =====================================================

"%NSSM%" set "%SVC_NAME%" AppDirectory "%INSTALL_ROOT%\%VERSION%"

"%NSSM%" set "%SVC_NAME%" AppNoConsole 1

rem -----------------------------------------------------
rem  Variables de entorno para el servicio
rem -----------------------------------------------------

"%NSSM%" set "%SVC_NAME%" AppEnvironmentExtra "SPRING_CONFIG_ADDITIONAL_LOCATION=optional:file:%INSTALL_ROOT_FWD%/%VERSION%/" "JAVA_TOOL_OPTIONS=%JAVA_OPTS%"

rem -----------------------------------------------------
rem  Parámetros de ejecución
rem -----------------------------------------------------

call :log [INFO] Comando JAVA completo a usar:
call :log "%JAVA_EXE% %JAVA_OPTS% -jar \"%LATEST_JAR%\" %APP_ARGS%"

"%NSSM%" set "%SVC_NAME%" AppParameters ^
"-jar \"%LATEST_JAR%\" %APP_ARGS%"

"%NSSM%" set "%SVC_NAME%" Description "%SVC_DESCRIPTION%"



rem Adicional — guarda en un .bat para prueba
echo @echo off > "%INSTALL_ROOT%\run_debug.bat"
echo "%JAVA_EXE%" %JAVA_OPTS% -jar "%LATEST_JAR%" %APP_ARGS% >> "%INSTALL_ROOT%\run_debug.bat"
echo pause >> "%INSTALL_ROOT%\run_debug.bat"

rem -----------------------------------------------------
rem  Logs de salida y errores
rem -----------------------------------------------------
"%NSSM%" set "%SVC_NAME%" AppStdout "%INSTALL_ROOT%\logs\service-console.log"
"%NSSM%" set "%SVC_NAME%" AppStderr "%INSTALL_ROOT%\logs\service-console.log"
"%NSSM%" set "%SVC_NAME%" AppRotateFiles 1

rem =====================================================
rem  Configuración de inicio automático y recuperación
rem =====================================================
"%NSSM%" set "%SVC_NAME%" Start SERVICE_AUTO_START

sc failure "%SVC_NAME%" reset= 86400 actions= restart/5000/restart/5000/restart/5000 >nul 2>&1

rem =====================================================
rem  Iniciar servicio
rem =====================================================
call :log [INFO] Iniciando servicio...

"%NSSM%" start "%SVC_NAME%" >nul 2>&1

if %errorlevel% neq 0 (
    call :log [ERROR] No fue posible iniciar el servicio, revisa el .bat en debug en la carpeta de instalacion
    sc query "%SVC_NAME%"
    exit /b 1
)

call :log [OK] Servicio iniciado correctamente

sc query "%SVC_NAME%" | findstr /i "RUNNING" >nul
if %errorlevel%==0 (
    call :log [OK] El servicio está corriendo
) else (
    call :log [WARN] El servicio no está en estado RUNNING
)

exit /b 0


:log
echo %*
>> "%LOG_FILE%" echo %*
exit /b