# 🐛 Debug: Problema con Logging de Respuestas

## Síntoma Reportado

El usuario ejecuta los tests y ve:
- ✅ Log "EJECUTANDO" con petición
- ✅ Logs del servidor procesando la petición
- ❌ NO ve el log "RESPUESTA"

## Análisis del Log Proporcionado

```
2026-02-06 10:21:59 [Test worker] INFO  HttpCaseRunner.logRequest:260 - 
═══════════════════════════════════════════════════════════════
🔵 EJECUTANDO: GET /api/recipe accessPrivilege
───────────────────────────────────────────────────────────────
Método:  GET
URI:     /api/recipe?accessPrivilege=ESCU_LH_Visualizacion&lastMonthsInterval=3
═══════════════════════════════════════════════════════════════

[... muchos logs del servidor ...]

2026-02-06 10:22:27,629 CET [apips] ERROR java.lang.Class: ### Error mapper: getAccessPrivilegeName not found  For input string: ""

[FIN DEL LOG - NO HAY "RESPUESTA"]
```

##Posibles Causas

### 1. **Excepción no Capturada**
La petición HTTP podría estar lanzando una excepción que no se captura correctamente:
- Error en `executor.execute()`
- Error en `RestClientHttpExecutor`
- Timeout de conexión

### 2. **Problema con el Logger**
El formato del log muestra:
```
2026-02-06 10:21:59 [Test worker] INFO  HttpCaseRunner.logRequest:260
```

Esto indica que el logger SÍ está funcionando, pero solo para `logRequest`, no para `logResponse`.

### 3. **Código Antiguo en Ejecución**
Es posible que el código compilado no incluya los últimos cambios.

### 4. **Error en logResponse() o formatBodyForLog()**
Podría haber una excepción dentro de estos métodos que no se está mostrando.

## Soluciones Implementadas

### Solución 1: Logs de Debug Adicionales

Agregué logs de debug en puntos clave:

```java
public void run(HttpCaseDefinition tc) throws Exception {
    HttpResult result = null;
    try {
        URI uri = buildUri(tc.path(), tc.query());
        
        if (verboseLogging) {
            logRequest(tc, uri);
            logger.debug("[DEBUG] Antes de ejecutar la petición HTTP");
        }
        
        result = executor.execute(new HttpRequestSpec(tc.method(), uri));
        
        if (verboseLogging) {
            logger.debug("[DEBUG] Después de ejecutar. Status: {}", result.statusCode());
            logger.debug("[DEBUG] Antes de logResponse");
            logResponse(tc, result);
            logger.debug("[DEBUG] Después de logResponse");
        }
        
        // ... validaciones ...
        
    } catch (Exception e) {
        // IMPORTANTE: Si tenemos resultado, mostrarlo antes de fallar
        if (verboseLogging && result != null) {
            logger.info("[DEBUG] Excepción capturada pero tenemos respuesta");
            try {
                logResponse(tc, result);
            } catch (Exception logEx) {
                logger.error("[ERROR] No se pudo mostrar respuesta: {}", logEx.getMessage());
            }
        }
        logFailure(tc, e);
        throw e;
    }
}
```

**Beneficio**: Ahora sabremos exactamente dónde falla el código.

### Solución 2: Captura de Respuesta Incluso en Error

Movimos `HttpResult result` fuera del try para que esté disponible en el catch:

```java
HttpResult result = null;  // FUERA del try
try {
    result = executor.execute(...);
    // ...
} catch (Exception e) {
    if (result != null) {
        logResponse(tc, result);  // Mostrar respuesta aunque haya error
    }
}
```

**Beneficio**: Incluso si las validaciones fallan, verás la respuesta.

### Solución 3: Try-Catch Adicional en logResponse

Agregamos protección extra en el catch para evitar que un error en logResponse oculte la respuesta:

```java
try {
    logResponse(tc, result);
} catch (Exception logEx) {
    logger.error("[ERROR] No se pudo mostrar la respuesta: {}", logEx.getMessage());
}
```

## Instrucciones para Depurar

### Paso 1: Recompilar
```bash
./gradlew clean compileTestJava
```

### Paso 2: Verificar que logback-test.xml esté en el lugar correcto
```bash
ls src/test/resources/logback-test.xml
```

### Paso 3: Ejecutar el test con máximo detalle
```bash
./gradlew test --tests "RecipeControllerITTest" --info --stacktrace
```

### Paso 4: Buscar los logs de DEBUG
En la salida, buscar:
- `[DEBUG] Antes de ejecutar la petición HTTP`
- `[DEBUG] Después de ejecutar. Status: XXX`
- `[DEBUG] Antes de logResponse`
- `[DEBUG] Después de logResponse`

## Qué Esperar Ver Ahora

### Si todo funciona correctamente:
```
[DEBUG] Antes de ejecutar la petición HTTP
[DEBUG] Después de ejecutar. Status: 200
[DEBUG] Antes de logResponse
✅ RESPUESTA: GET /api/recipe accessPrivilege
   Status: 200 OK
   [...]
[DEBUG] Después de logResponse
✅✅✅ TEST PASADO
```

### Si falla en execute():
```
[DEBUG] Antes de ejecutar la petición HTTP
❌❌❌ TEST FALLIDO
   Razón: [mensaje del error]
```

### Si falla en logResponse():
```
[DEBUG] Antes de ejecutar la petición HTTP
[DEBUG] Después de ejecutar. Status: 200
[DEBUG] Antes de logResponse
[ERROR] No se pudo mostrar la respuesta: [mensaje]
```

### Si falla en las validaciones:
```
[DEBUG] Antes de ejecutar la petición HTTP
[DEBUG] Después de ejecutar. Status: 200
[DEBUG] Antes de logResponse
✅ RESPUESTA: [...]
[DEBUG] Después de logResponse
[DEBUG] Excepción capturada pero tenemos respuesta
✅ RESPUESTA: [...]  (se muestra otra vez)
❌❌❌ TEST FALLIDO
   Razón: [validación que falló]
```

## Verificación del Problema Original

Basándome en el log proporcionado, sospecho que:

1. **La petición SÍ se ejecuta** (veo logs del servidor)
2. **La respuesta SÍ llega** (el servidor procesó todo)
3. **Pero algo falla entre `execute()` y `logResponse()`**

Posibles causas específicas:
- El RestClientHttpExecutor lanza una excepción al leer el body
- El body es null o vacío
- Hay un problema con la codificación de caracteres
- El test usa una versión antigua del código

## Siguiente Paso

**Por favor ejecuta:**

```bash
./gradlew clean test --tests "RecipeControllerITTest" --info > test-output.log 2>&1
```

Y busca en `test-output.log`:
- Todos los `[DEBUG]`
- Cualquier `[ERROR]`
- El stack trace completo si hay excepción

Esto nos dirá exactamente dónde está el problema.

## Checklist de Verificación

- [ ] `logback-test.xml` existe en `src/test/resources/`
- [ ] Nivel de log es DEBUG o INFO (no WARN o ERROR)
- [ ] El código se recompiló (`./gradlew clean compileTestJava`)
- [ ] El test se ejecuta con `--info` o `--debug`
- [ ] Se ven los logs `[DEBUG]` en la salida

Si todos están ✅ pero aún no ves la respuesta, entonces hay un problema más profundo que necesitamos investigar.
