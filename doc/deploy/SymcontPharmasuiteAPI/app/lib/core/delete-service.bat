@echo off
set "SVC_NAME=Adasoft Pharmasuite Api"
rem Trabajar en la carpeta del .bat (y donde estará el jar)
cd /d "%~dp0"
set "NSSM=%~dp0/nssm.exe"
"%NSSM%" stop "%SVC_NAME%"
"%NSSM%" remove "%SVC_NAME%" confirm
PAUSE