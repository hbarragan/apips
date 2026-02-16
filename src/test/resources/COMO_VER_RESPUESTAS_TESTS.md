# 📊 Cómo Ver las Respuestas de los Tests

## ✅ Implementación Completa

Ahora **TODOS** los tests de integración muestran automáticamente:
1. **Petición HTTP** que se envía
2. **Respuesta HTTP** completa del servidor
3. **Resultado final** (✅ PASADO o ❌ FALLIDO)

## 🎯 Lo que Verás al Ejecutar Tests

### Ejemplo de Test Exitoso:

```
═══════════════════════════════════════════════════════════════
🔵 EJECUTANDO: GET /api/recipe accessPrivilege
───────────────────────────────────────────────────────────────
   Método:  GET
   URI:     /api/recipe?accessPrivilege=ESCU_LH_Visualizacion&lastMonthsInterval=3
═══════════════════════════════════════════════════════════════

───────────────────────────────────────────────────────────────
✅ RESPUESTA: GET /api/recipe accessPrivilege
───────────────────────────────────────────────────────────────
   Status:       200 OK
   Content-Type: application/json;charset=UTF-8
   Body Length:  1523 bytes
   Body:         [
                   {
                     "id": 12345,
                     "name": "Paracetamol 500mg",
                     "description": "Analgesic medication",
                     "status": "Active"
                   },
                   {
                     "id": 12346,
                     "name": "Ibuprofen 400mg",
                     "description": "Anti-inflammatory",
                     "status": "Active"
                   }
                 ]
   
═══════════════════════════════════════════════════════════════

✅✅✅ TEST PASADO: GET /api/recipe accessPrivilege
═══════════════════════════════════════════════════════════════
```

### Ejemplo de Test Fallido:

```
═══════════════════════════════════════════════════════════════
🔵 EJECUTANDO: GET /api/recipe accessPrivilege
───────────────────────────────────────────────────────────────
   Método:  GET
   URI:     /api/recipe?accessPrivilege=INVALID_PRIVILEGE
═══════════════════════════════════════════════════════════════

───────────────────────────────────────────────────────────────
✅ RESPUESTA: GET /api/recipe accessPrivilege
───────────────────────────────────────────────────────────────
   Status:       403 Forbidden
   Content-Type: application/json;charset=UTF-8
   Body Length:  89 bytes
   Body:         {
                   "error": "Access Denied",
                   "message": "Invalid access privilege"
                 }
   
═══════════════════════════════════════════════════════════════

❌❌❌ TEST FALLIDO: GET /api/recipe accessPrivilege
───────────────────────────────────────────────────────────────
   Razón: GET /api/recipe accessPrivilege -> status
          Expecting:
            <403>
          to be equal to:
            <200>
═══════════════════════════════════════════════════════════════
```

## 🚀 Cómo Ejecutar

### Desde IntelliJ IDEA:
1. Clic derecho en `RecipeControllerITTest.java`
2. Seleccionar "Run RecipeControllerITTest"
3. Ver la pestaña "Run" en la parte inferior
4. **¡Los logs aparecerán automáticamente!**

### Desde Gradle:
```bash
./gradlew test --tests "RecipeControllerITTest"
```

### Ejecutar un Test Específico:
```bash
./gradlew test --tests "RecipeControllerITTest" --info
```

## 🎛️ Control del Logging

### ✅ Por Defecto (Recomendado)
El logging detallado está **activado automáticamente**. No necesitas hacer nada.

```java
@TestFactory
Stream<DynamicTest> recipe_cases_from_files() {
    var repository = new ClasspathHttpCaseRepository(objectMapper, CASES_PATTERN);
    var executor   = new RestClientHttpExecutor(getRestClient());
    var runner     = new HttpCaseRunner(objectMapper, executor); // verbose=true
    
    return repository.load()
            .map(tc -> dynamicTest(tc.displayName(), () -> runner.run(tc)));
}
```

### ❌ Desactivar Logging (Ejecución Rápida)
Si quieres ejecutar tests SIN ver las respuestas (más rápido):

```java
@TestFactory
Stream<DynamicTest> recipe_cases_from_files() {
    var repository = new ClasspathHttpCaseRepository(objectMapper, CASES_PATTERN);
    var executor   = new RestClientHttpExecutor(getRestClient());
    var runner     = new HttpCaseRunner(objectMapper, executor, false); // verbose=false
    
    return repository.load()
            .map(tc -> dynamicTest(tc.displayName(), () -> runner.run(tc)));
}
```

### ⚙️ Control Fino desde logback-test.xml
Editar `src/test/resources/logback-test.xml`:

```xml
<!-- Ver TODO (peticiones, respuestas, resultados) -->
<logger name="com.adasoft.tomcat.integration.common.HttpCaseRunner" level="DEBUG"/>

<!-- Ver solo RESULTADOS (éxito/fallo) -->
<logger name="com.adasoft.tomcat.integration.common.HttpCaseRunner" level="INFO"/>

<!-- Ver solo ERRORES -->
<logger name="com.adasoft.tomcat.integration.common.HttpCaseRunner" level="WARN"/>

<!-- NO ver nada -->
<logger name="com.adasoft.tomcat.integration.common.HttpCaseRunner" level="OFF"/>
```

## 🔍 Qué Información se Muestra

### 1. **Petición (🔵 EJECUTANDO)**
- Nombre del test
- Método HTTP (GET, POST, etc.)
- URI completa con todos los parámetros

### 2. **Respuesta (✅ RESPUESTA)**
- Status code (200, 404, 500, etc.) con descripción
- Content-Type
- Longitud del body en bytes
- Body completo formateado (JSON con indentación)
- Indicador si el body está truncado

### 3. **Resultado Final**
- **✅✅✅ TEST PASADO**: Si todas las validaciones pasan
- **❌❌❌ TEST FALLIDO**: Si alguna validación falla
  - Incluye la razón del fallo

## 📋 Ventajas

1. **Sin Sorpresas**: Siempre ves qué devolvió el servidor
2. **Debugging Rápido**: Identificas problemas inmediatamente
3. **Documentación**: Los logs sirven como documentación de la API
4. **Trazabilidad**: Registro completo de cada interacción HTTP
5. **Automático**: No necesitas agregar código extra

## 🎨 Personalización

### Cambiar Longitud Máxima del Body

En `HttpCaseRunner.java`, línea 24:
```java
private static final int MAX_BODY_LOG_LENGTH = 1000; // Cambiar este valor
```

**Valores sugeridos:**
- `500`: Para respuestas pequeñas
- `1000`: Balance óptimo (por defecto)
- `5000`: Para ver más detalles
- `Integer.MAX_VALUE`: Sin límite

### Cambiar Emojis/Formato

En `HttpCaseRunner.java`, métodos `logRequest()`, `logResponse()`, `logSuccess()`, `logFailure()`:

```java
// Cambiar emojis
"🔵 EJECUTANDO" → "▶️ REQUEST"
"✅ RESPUESTA"  → "📥 RESPONSE"
"✅✅✅"        → "✓✓✓"
"❌❌❌"        → "✗✗✗"

// Cambiar separadores
"═══" → "---"
"───" → "==="
```

## 🐛 Solución de Problemas

### No Veo Ningún Log
**Causa**: El logging puede estar desactivado

**Solución**:
1. Verificar que no pasaste `false` al constructor:
   ```java
   var runner = new HttpCaseRunner(objectMapper, executor); // SIN segundo parámetro
   ```

2. Verificar `logback-test.xml`:
   ```xml
   <logger name="...HttpCaseRunner" level="DEBUG"/>
   ```

### Solo Veo "EJECUTANDO", No "RESPUESTA"
**Causa**: Error al obtener la respuesta del servidor

**Solución**:
1. Verificar que el servidor está corriendo
2. Verificar la URL y puertos en `application-it.yml`
3. Revisar el stack trace completo del error

### Body Truncado
**Causa**: El body es muy largo (>1000 caracteres por defecto)

**Solución**: Aumentar `MAX_BODY_LOG_LENGTH` en `HttpCaseRunner.java`

### Demasiados Logs
**Causa**: Logging muy verboso

**Solución 1**: Cambiar a nivel INFO en logback:
```xml
<logger name="...HttpCaseRunner" level="INFO"/>
```

**Solución 2**: Desactivar verbose logging:
```java
var runner = new HttpCaseRunner(objectMapper, executor, false);
```

## 📚 Más Información

- **[LOGGING_GUIDE.md](LOGGING_GUIDE.md)**: Guía completa de logging
- **[README.md](src/test/java/com/adasoft/tomcat/integration/common/README.md)**: Arquitectura del sistema de tests
- **[RESUMEN_LOGGING_MEJORAS.md](RESUMEN_LOGGING_MEJORAS.md)**: Detalles técnicos de la implementación

## ✨ Resumen

🎉 **¡Ya está todo configurado!**

Simplemente ejecuta tus tests y verás automáticamente:
- ✅ Qué petición se envía
- ✅ Qué respuesta devuelve el servidor
- ✅ Si el test pasó o falló

**No necesitas hacer nada adicional. Todo funciona out-of-the-box.**

---

**¿Preguntas?** Revisa [LOGGING_GUIDE.md](LOGGING_GUIDE.md) para más detalles.
