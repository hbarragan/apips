# 📊 Guía de Logging para Pruebas de Integración

## 🎯 Objetivo

Proporcionar visibilidad completa de las peticiones y respuestas HTTP durante la ejecución de pruebas de integración, facilitando el debugging y la comprensión de qué está devolviendo cada endpoint.

## ✨ Características

### 1. **Logging Automático**
Por defecto, `HttpCaseRunner` registra automáticamente:
- ✅ Método HTTP y URI de cada petición
- ✅ Status code de la respuesta
- ✅ Content-Type
- ✅ Longitud del body
- ✅ Contenido del body (formateado como JSON si es posible)

### 2. **Formato Visual Claro**
Los logs utilizan separadores visuales para facilitar la lectura:

```
══════════════════════════════════════════════════════════════════
🔵 EJECUTANDO: GET /api/recipe accessPrivilege
───────────────────────────────────────────────────────────────────
   Método:  GET
   URI:     /api/recipe?accessPrivilege=ESCU_LH_Visualizacion&lastMonthsInterval=3
══════════════════════════════════════════════════════════════════

───────────────────────────────────────────────────────────────────
✅ RESPUESTA: GET /api/recipe accessPrivilege
───────────────────────────────────────────────────────────────────
   Status:       200 OK
   Content-Type: application/json
   Body Length:  1523 bytes
   Body:         [
                   {
                     "id": 123,
                     "name": "Recipe 1",
                     ...
                   }
                 ]
   
══════════════════════════════════════════════════════════════════
```

### 3. **JSON Formateado**
Si el body es JSON válido, se formatea automáticamente con indentación para mejor legibilidad.

### 4. **Truncamiento Inteligente**
- Bodies largos (>1000 caracteres) se truncan automáticamente
- Se muestra la longitud total para referencia
- Se indica claramente cuando el contenido está truncado

## 🔧 Configuración

### Opción 1: Verbose Logging (Por Defecto)
```java
@TestFactory
Stream<DynamicTest> recipe_cases_from_files() {
    var repository = new ClasspathHttpCaseRepository(objectMapper, CASES_PATTERN);
    var executor   = new RestClientHttpExecutor(getRestClient());
    var runner     = new HttpCaseRunner(objectMapper, executor); // verbose=true por defecto

    return repository.load()
            .map(tc -> dynamicTest(tc.displayName(), () -> runner.run(tc)));
}
```

### Opción 2: Desactivar Logging Detallado
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

### Opción 3: Control por Variable de Entorno
```java
@TestFactory
Stream<DynamicTest> recipe_cases_from_files() {
    boolean verbose = Boolean.parseBoolean(System.getProperty("test.verbose", "true"));
    
    var repository = new ClasspathHttpCaseRepository(objectMapper, CASES_PATTERN);
    var executor   = new RestClientHttpExecutor(getRestClient());
    var runner     = new HttpCaseRunner(objectMapper, executor, verbose);

    return repository.load()
            .map(tc -> dynamicTest(tc.displayName(), () -> runner.run(tc)));
}
```

Luego ejecutar con:
```bash
./gradlew test -Dtest.verbose=false
```

## 📁 Configuración de Logback

El archivo `src/test/resources/logback-test.xml` controla el formato y destino de los logs:

```xml
<!-- Logger específico para los resultados de tests HTTP -->
<logger name="com.adasoft.tomcat.integration.common.HttpCaseRunner" level="INFO" additivity="false">
    <appender-ref ref="TEST_RESULTS" />
</logger>
```

### Niveles de Logging

- **INFO**: Muestra todas las peticiones y respuestas (recomendado)
- **DEBUG**: Incluiría información adicional de depuración (si se implementa)
- **WARN**: Solo muestra warnings y errores (para ejecución rápida)
- **OFF**: Desactiva completamente el logging de HttpCaseRunner

Para cambiar el nivel:
```xml
<logger name="com.adasoft.tomcat.integration.common.HttpCaseRunner" level="WARN"/>
```

## 🎨 Personalización del Formato

### Cambiar Longitud Máxima del Body

En `HttpCaseRunner.java`:
```java
private static final int MAX_BODY_LOG_LENGTH = 1000; // Cambiar este valor
```

Valores recomendados:
- **500**: Para respuestas pequeñas o muchos tests
- **1000**: Balance óptimo (valor por defecto)
- **5000**: Para ver respuestas completas en la mayoría de casos
- **Integer.MAX_VALUE**: Sin límite (no recomendado)

### Personalizar Emojis y Separadores

En el método `logRequest()` y `logResponse()`:
```java
// Cambiar los emojis
"🔵 EJECUTANDO" → "▶️ REQUEST" 
"✅ RESPUESTA"  → "📥 RESPONSE"

// Cambiar los separadores
"═══" → "---"
"───" → "==="
```

## 📊 Casos de Uso

### Debugging de un Test Específico
```java
@Test
void debug_specific_endpoint() throws Exception {
    var runner = new HttpCaseRunner(objectMapper, executor, true); // verbose ON
    
    var testCase = new HttpCaseDefinition(
        "Debug test",
        HttpMethod.GET,
        "/api/recipe/123",
        null,
        new HttpCaseDefinition.Expect(200, HttpCaseDefinition.BodyType.OBJECT, List.of())
    );
    
    runner.run(testCase);
}
```

### Ejecución Rápida de Muchos Tests
```java
@TestFactory
Stream<DynamicTest> fast_execution() {
    // Desactivar verbose logging para ejecutar más rápido
    var runner = new HttpCaseRunner(objectMapper, executor, false);
    
    return repository.load()
            .map(tc -> dynamicTest(tc.displayName(), () -> runner.run(tc)));
}
```

### Logging Condicional por Test
```java
@TestFactory
Stream<DynamicTest> conditional_logging() {
    var repository = new ClasspathHttpCaseRepository(objectMapper, CASES_PATTERN);
    var executor = new RestClientHttpExecutor(getRestClient());
    
    return repository.load()
            .map(tc -> {
                // Verbose solo para tests que empiezan con "Debug"
                boolean verbose = tc.displayName().startsWith("Debug");
                var runner = new HttpCaseRunner(objectMapper, executor, verbose);
                
                return dynamicTest(tc.displayName(), () -> runner.run(tc));
            });
}
```

## 🔍 Interpretación de Logs

### Status Codes Comunes

| Status | Descripción | Significado |
|--------|-------------|-------------|
| 200 | OK | Petición exitosa |
| 201 | Created | Recurso creado exitosamente |
| 204 | No Content | Exitoso pero sin body |
| 400 | Bad Request | Error en los datos enviados |
| 401 | Unauthorized | Falta autenticación |
| 403 | Forbidden | Sin permisos |
| 404 | Not Found | Recurso no encontrado |
| 500 | Internal Server Error | Error del servidor |

### Body Types

- **Array JSON**: `[ {...}, {...} ]` - Lista de objetos
- **Object JSON**: `{ "key": "value" }` - Objeto único
- **Empty**: `(vacío)` - Sin contenido

## 💡 Tips y Trucos

### 1. Ver Solo Tests Fallidos
En `logback-test.xml`:
```xml
<logger name="com.adasoft.tomcat.integration.common.HttpCaseRunner" level="WARN"/>
```

### 2. Guardar Logs en Archivo
Agregar en `logback-test.xml`:
```xml
<appender name="FILE" class="ch.qos.logback.core.FileAppender">
    <file>test-results.log</file>
    <encoder>
        <pattern>%msg%n</pattern>
    </encoder>
</appender>

<logger name="com.adasoft.tomcat.integration.common.HttpCaseRunner" level="INFO">
    <appender-ref ref="FILE" />
</logger>
```

### 3. Logging por Controlador
```java
public class RecipeControllerITTest extends ITTestCommon {
    private static final Logger testLogger = LoggerFactory.getLogger(RecipeControllerITTest.class);
    
    @TestFactory
    Stream<DynamicTest> recipe_cases_from_files() {
        testLogger.info("========== INICIANDO TESTS DE RECIPE CONTROLLER ==========");
        
        // ... código de configuración ...
        
        return repository.load()
                .map(tc -> dynamicTest(tc.displayName(), () -> {
                    testLogger.debug("Ejecutando: {}", tc.displayName());
                    runner.run(tc);
                }));
    }
}
```

## 📈 Análisis de Performance

Con verbose logging activado, puedes analizar:
- Qué endpoints son más lentos (observando timestamps)
- Tamaño de las respuestas (Body Length)
- Tipos de respuesta más comunes

Para análisis detallado de performance, considera usar timestamps:
```java
long start = System.currentTimeMillis();
runner.run(tc);
long duration = System.currentTimeMillis() - start;
logger.info("Duración: {}ms", duration);
```

## 🐛 Troubleshooting

### Problema: No veo los logs
**Solución**: Verificar `logback-test.xml` y que el nivel sea INFO:
```xml
<logger name="com.adasoft.tomcat.integration.common.HttpCaseRunner" level="INFO"/>
```

### Problema: Demasiados logs, difícil de leer
**Solución**: Cambiar a verbose=false o nivel WARN

### Problema: Body truncado, necesito ver completo
**Solución**: Aumentar `MAX_BODY_LOG_LENGTH` en `HttpCaseRunner.java`

### Problema: JSON no formateado correctamente
**Solución**: Verificar que `ObjectMapper` esté configurado correctamente

## 🎯 Best Practices

1. **Desarrollo**: Usar verbose=true para debugging
2. **CI/CD**: Usar verbose=false para logs más limpios
3. **Debugging**: Aumentar MAX_BODY_LOG_LENGTH temporalmente
4. **Performance Tests**: Desactivar verbose logging
5. **Documentación**: Guardar logs en archivo para documentar comportamiento

## 📝 Resumen

El sistema de logging proporciona:
- ✅ Visibilidad completa de peticiones/respuestas
- ✅ Formato claro y legible
- ✅ Configuración flexible
- ✅ Sin impacto en tests cuando está desactivado
- ✅ Útil para debugging y documentación

Para la mayoría de casos, la configuración por defecto (verbose=true) es ideal durante desarrollo y testing.
