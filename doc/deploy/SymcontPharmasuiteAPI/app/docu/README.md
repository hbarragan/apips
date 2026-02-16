# Readme — Adasoft Symcont Pharmasuite API

Este documento describe **rutas** y **conceptos** importantes a tener en cuenta para el funcionamiento de la api Pharmasuite.


## 1) Estructura de carpetas

Al instalar la api se genera una serie de carpetas en la ruta de instalación que hallamos indicado.

Por defecto, sera en `C:\Adasoft\Symcont\PharmasuiteApi`.

Dentro de la carpeta de instalación tendremos:
- `docu` Carpeta en la que tenemos documentación destacable
- `lib`
  - `core`
    - `jdk` Librerías de java necesarias para ejecutar la api
    - `delete-service.bat` Script que permite borrar el servicio `Adasoft Pharmasuite Api` de windows de forma directa.
    - `log4j2.xml` Fichero de configuración de los logs que genera la api por defecto
    - `nssm.exe` Ejecutable necesario para poder lanzar comandos para el mantenimiento de servicios de windows.
- `runtime`
  - `run_debug.bat` Script para lanzar directamente en una consola la api sin tener que instalar el servicio
    - `ps10 o ps11` Tendremos ps10 o ps11 dependiendo de la version que hayamos seleccionado en el instalador
      - `application-dev.yml` Yaml en el que configuraremos parámetros relativos a la api 
      - `ct-Customer-config.xml` Fichero de configuración necesario para las librerías internas de Pharmasuite. No hay que modificarlo.
      - `custConfig.xmll` Fichero de configuración necesario para las librerías internas de Pharmasuite. No hay que modificarlo.
      - `log4j2.xml` Fichero de configuración de los logs que genera la api que utiliza en runtime
      - `symcont-pharmasuite-api-X.X.X.X-psXX.jar` Jar donde tenemos todo el desarrollo de la api de Pharmasuite
- `config.ini` Fichero en el que tenemos variable relevantes que se utilizan en el script `install-or-update-service.bat`
- `install.log` Log que ha escrito la instalación. Comprobar si todo esta correcto
- `install-or-update-service.bat` Script para instalar o actualizar el servicio de la api de Pharmasuite

## 2) Ejecutar debug

Tenemos un script que podemos ejecutar para levantar la api directamente sin 
servicio y poder debuguear por consola.

Posicionándonos en el ruta que donde hemos hecho la instalación , seria en `runtime/run_debug.bat`.
Cuando lo ejecutamos se nos abre una consola CMD y veremos todos los logs y la ejecución de la API ahi.

### Nota

Recordar que deberemos revisar que el servicio de windows `Adasoft Pharmasuite Api` este parado antes de arrancar este script.
Si ejecutamos el script con el servicio arrancado sin cambiar el `application-dev.yml` , tendremos problemas con los puertos.


## 3) Log instalación

En la carpeta de la instalación tendremos el fichero `install.log` que nos describirá detalladamente todos lo pasos que ha 
realizado en la instalación.
