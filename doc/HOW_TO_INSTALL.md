# How to install — Adasoft Symcont Pharmasuite API

Este documento describe **cómo instalar** y **cómo construir** el instalador de la aplicación **“Adasoft - Symcont Pharmasuite API”** (generado como `SymcontPharmasuiteAPI.exe`) a partir de los scripts:

- `installer.nsi` (NSIS)
- `install-builder.bat` (wrapper para compilar con NSIS)


## 1) Construir el instalador (para desarrollo/CI)

### Requisitos
- Instalar **NSIS (Nullsoft Scriptable Install System)**.
- Tener los scripts `installer.nsi` y `install-builder.bat` en el mismo directorio.
- Tener la carpeta `app\` con el contenido que quieres empaquetar.
- Tener la carpeta `app\lib\core\jdk` con la JDK 17 (Corretto 17.0.11). 
- Tener `nssm.exe` en `app\lib\core\` (para gestionar servicios de Windows).
### Estructura esperada

Ejemplo de estructura mínima:

- `installer.nsi`
- `install-builder.bat`
- `install-builder.bat`
- `app\`
    - *(binarios / config / scripts de tu app)*
    - `lib\core`
        - `jdk` -> Debemos tener la jdk 17 , por ejemplo la corretto-17.0.11
        - *delete-service.bat* -> Este script se utiliza para borrar el servicio de la api que esta levantado
        - *nssm.exe* -> ejecutable necesario para hacer operaciones con los servicios de windows.

> Importante: el instalador **re-crea** `config.ini`, así que si existe en `app\` se excluirá.

### Compilar
Opción A (rápida): ejecutar el BAT:
- Doble clic en `install-builder.bat`  
  (internamente ejecuta `makensis.exe installer.nsi`)

Opción B (manual, en una línea):
- `"C:\Program Files (x86)\NSIS\makensis.exe" installer.nsi`

### Resultado
- Se genera el instalador: `SymcontPharmasuiteAPI.exe` (en el mismo directorio).

---


---

## 2) Instalación 

### Requisitos
- Windows **64-bit**.
- Permisos de **administrador** (el instalador solicita elevación: `RequestExecutionLevel admin`).
- Tener a mano (si aplica) el **certificado de cliente** y su contraseña.

### Pasos
1. Ejecuta `SymcontPharmasuiteAPI.exe` (recomendado: **clic derecho → Ejecutar como administrador**).
2. En la pantalla de directorio, elige la carpeta de instalación.
   - Por defecto: `C:\Adasoft\Symcont\PharmasuiteApi`
3. Selecciona la **versión de Pharmasuite** a instalar:
   - `ps11` (por defecto) o `ps10`.
4. Indica si quieres instalar **con certificado de cliente**:
   - **Yes**: se solicitarán los datos del certificado.
   - **No**: se ignorarán los campos de certificado.
5. Si has elegido **Yes**, completa:
   - `TYPE_SSL`: tipo de certificado (`PKCS12`, `JKS` o `PEM`)
   - `CERT_STORE`: ruta al fichero del certificado (botón **Browse...**)
   - `CERT_PSWD_SSL`: contraseña del certificado
6. El instalador copiará los ficheros de la aplicación y generará `config.ini` en el directorio de instalación.
7. El instalador ejecutará el script:
   - `cmd.exe /c "<RUTA_INSTALACION>\install-or-update-service.bat"`
8. Al finalizar, verás el mensaje **“Installation finished successfully.”** y se abrirá `install.log`.

---

## 3) Qué genera/modifica el instalador

### Carpeta de instalación
- Carpeta destino: `$PROGRAMFILES64\Adasoft\SymcontPharmasuiteAPI`
- Se copian todos los ficheros de `app\*` al destino **excepto** `config.ini` (se excluye y se re-crea).

### config.ini (generado)
El instalador crea `config.ini` con estas claves:

- `ENABLE_CERT=<booleano para activar o desactivar SSL>`
- `CERT_STORE=<ruta_del_certificado>`  
- `CERT_PSWD_SSL=<password_del_certificado>`  
- `TYPE_SSL=<PKCS12|JKS|PEM>`  
- `VERSION=<ps11|ps10>`  

> Nota: si en el asistente eliges **No** (sin certificado), el instalador deja vacíos `TYPE_SSL`, `CERT_STORE` y `CERT_PSWD_SSL`.

### Script de post-instalación
Después de copiar ficheros y generar `config.ini`, el instalador ejecuta:
- `install-or-update-service.bat`

Ese BAT es el responsable de la instalación/actualización (por ejemplo, un servicio de Windows, tareas, configuración adicional, etc.) según tu implementación.

---

## 4) Verificación rápida (post-instalación)

1. Revisa el log:
   - `<RUTA_INSTALACION>\install.log`
2. Verifica que `config.ini` contiene los valores esperados.
3. Si tu BAT instala un servicio, revisa el estado en **services.msc** o usando `sc`:
   - Abre una consola como admin y ejecuta (en una línea): `sc query "<NOMBRE_DEL_SERVICIO>"`
   - Si no sabes el nombre, búscalo en `install-or-update-service.bat`.

---

## 5) Problemas que pueden surgir

### El instalador no eleva permisos / falla al instalar
- Asegúrate de ejecutarlo como **administrador**.
- Revisa `install.log` para ver el error exacto.
- 
### Problemas de instalacion acceso a la carpeta runtime
- Para solucionar esto hay que ir a los servicios de windows y para el servicio `Adasoft Pharmasuite Api`
- R

### No se ejecuta `install-or-update-service.bat`
- Confirma que el fichero está dentro de `app\` (se copia al directorio de instalación).
- Prueba a ejecutarlo manualmente como admin (en una línea):
  - `cmd.exe /c "C:\Adasoft\Symcont\PharmasuiteApi\install-or-update-service.bat"`

### Certificado: ruta o contraseña incorrectas
- Vuelve a ejecutar el instalador y selecciona el tipo de certificado correcto (`PKCS12`, `JKS`, `PEM`).
- Verifica que el fichero existe y que la contraseña es válida.

---




