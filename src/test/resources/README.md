# Paquete de Pruebas de Integración HTTP

Este paquete contiene la infraestructura común para ejecutar pruebas de integración basadas en casos de prueba definidos en archivos JSON.

## 📋 Descripción General

El sistema permite definir casos de prueba HTTP en archivos JSON y ejecutarlos automáticamente contra un servidor real. Es ideal para pruebas de integración end-to-end de APIs REST.

## 🏗️ Arquitectura

### Flujo de ejecución:

```
┌──────────────────┐
│  Archivos JSON   │  Casos de prueba definidos en JSON
│  (it-cases/...)  │
└────────┬─────────┘
         │
         ↓
┌──────────────────────────────┐
│ ClasspathHttpCaseRepository  │  Carga y deserializa los JSON
└────────┬─────────────────────┘
         │
         ↓
┌──────────────────┐
│ HttpCaseDefinition│  Modelo de datos del caso de prueba
└────────┬──────────┘
         │
         ↓
┌──────────────────┐
│  HttpCaseRunner  │  Ejecuta el caso y valida resultados
└────────┬──────────┘
         │
         ↓
┌──────────────────────────┐
│ RestClientHttpExecutor   │  Ejecuta la petición HTTP real
└──────────────────────────┘
```

## 📦 Clases Principales

### 1. **ITTestCommon** (Clase Base)
Clase base para todas las pruebas de integración. Proporciona:
- Configuración de puertos HTTP/HTTPS
- Creación de RestClient configurado
- Construcción de URL base del servidor

**Uso:**
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("it")
public class MyControllerITTest extends ITTestCommon {
    // Tus pruebas aquí
}
```

### 2. **HttpCaseDefinition** (Modelo de Datos)
Record que define un caso de prueba HTTP completo:
- **name**: Nombre descriptivo de la prueba
- **method**: Método HTTP (GET, POST, PUT, DELETE, etc.)
- **path**: Ruta del endpoint
- **query**: Parámetros de consulta (query string)
- **expect**: Expectativas de respuesta (status, bodyType, requiredJsonPaths)

**Ejemplo de archivo JSON:**
```json
{
  "name": "GET /api/recipe con filtro",
  "method": "GET",
  "path": "/api/recipe",
  "query": {
    "accessPrivilege": "ESCU_LH_Visualizacion",
    "lastMonthsInterval": 3
  },
  "expect": {
    "status": 200,
    "bodyType": "ARRAY",
    "requiredJsonPaths": ["$[0].id", "$[0].name"]
  }
}
```

### 3. **ClasspathHttpCaseRepository** (Cargador)
Busca y carga archivos JSON del classpath que contengan casos de prueba.

**Características:**
- Soporta wildcards en el patrón de búsqueda
- Ordena archivos alfabéticamente para ejecución predecible
- Deserializa JSON a `HttpCaseDefinition`

**Uso:**
```java
var repository = new ClasspathHttpCaseRepository(
    objectMapper, 
    "classpath*:/it-cases/recipe/*.json"
);
Stream<HttpCaseDefinition> cases = repository.load();
```

### 4. **HTTPExecutor** (Interfaz)
Interfaz que abstrae la ejecución de peticiones HTTP.

**Implementaciones:**
- `RestClientHttpExecutor`: Usa Spring RestClient

**Modelos:**
- `HttpRequestSpec`: Especifica método y URI de la petición
- `HttpResult`: Contiene status code, headers y body de la respuesta

### 5. **RestClientHttpExecutor** (Ejecutor)
Implementación de `HTTPExecutor` usando Spring RestClient.

**Características:**
- Ejecuta peticiones HTTP reales
- Acepta respuestas en formato JSON
- Captura status, headers y body completos

**Uso:**
```java
var executor = new RestClientHttpExecutor(restClient);
HttpResult result = executor.execute(
    new HttpRequestSpec(HttpMethod.GET, uri)
);
```

### 6. **HttpCaseRunner** (Orquestador)
Ejecuta un caso de prueba completo y valida sus resultados.

**Validaciones que realiza:**
1. ✅ Código de estado HTTP coincide
2. ✅ Content-Type es application/json
3. ✅ Tipo de cuerpo (Array u Object) coincide
4. ✅ Todos los JSON paths requeridos existen

**Características especiales:**
- Soporte para parámetros de consulta OData
- Manejo de colecciones y arrays en query params
- Mensajes de error descriptivos con contexto

**Uso:**
```java
var runner = new HttpCaseRunner(objectMapper, executor);
runner.run(httpCaseDefinition); // Lanza excepción si falla
```

## 🚀 Ejemplo Completo

### 1. Crear archivo de caso de prueba

`src/test/resources/it-cases/recipe/getRecipe.json`:
```json
{
  "name": "GET /api/recipe - listar recetas",
  "method": "GET",
  "path": "/api/recipe",
  "query": {
    "lastMonthsInterval": 3
  },
  "expect": {
    "status": 200,
    "bodyType": "ARRAY",
    "requiredJsonPaths": []
  }
}
```

### 2. Crear clase de prueba

```java
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
    classes = ApiApplication.class
)
@ActiveProfiles("it")
@Execution(ExecutionMode.CONCURRENT)
public class RecipeControllerITTest extends ITTestCommon {

    private static final String CASES_PATTERN = "classpath*:/it-cases/recipe/*.json";
    private final ObjectMapper objectMapper = new ObjectMapper();

    @TestFactory
    Stream<DynamicTest> recipe_cases_from_files() {
        // 1. Cargar casos de prueba desde archivos JSON
        var repository = new ClasspathHttpCaseRepository(objectMapper, CASES_PATTERN);
        
        // 2. Configurar ejecutor HTTP
        var executor = new RestClientHttpExecutor(getRestClient());
        
        // 3. Configurar runner de casos
        var runner = new HttpCaseRunner(objectMapper, executor);

        // 4. Generar pruebas dinámicas
        return repository.load()
                .map(tc -> dynamicTest(
                    tc.displayName(),
                    () -> runner.run(tc)
                ));
    }
}
```

## 🎯 Ventajas de este Enfoque

1. **Separación de Responsabilidades**: Cada clase tiene un propósito único y claro
2. **Mantenibilidad**: Los casos de prueba están en JSON, fáciles de editar sin tocar código
3. **Reutilización**: La misma infraestructura sirve para probar cualquier API
4. **Extensibilidad**: Fácil añadir nuevas validaciones o tipos de ejecutores
5. **Legibilidad**: Los JSON son auto-documentados y fáciles de entender
6. **Ejecución en Paralelo**: Compatible con ejecución concurrente de JUnit 5

## 📚 Validaciones JSON Path

Puedes usar JSON Path para validar contenido específico en las respuestas:

```json
{
  "expect": {
    "status": 200,
    "bodyType": "ARRAY",
    "requiredJsonPaths": [
      "$[0].id",                    // Primer elemento tiene id
      "$[0].name",                  // Primer elemento tiene name
      "$[*].id",                    // Todos los elementos tienen id
      "$.data.items[0].status"      // Para respuestas anidadas
    ]
  }
}
```

## 🔧 Soporte para Query Parameters Complejos

El sistema soporta varios tipos de query parameters:

```json
{
  "query": {
    "simple": "value",                           // String simple
    "number": 123,                               // Número
    "array": ["val1", "val2"],                   // Array de valores
    "odata": "$filter=Status eq 'Active'",       // Filtros OData
    "complex": "CurrentState eq 'Valid'"         // Expresiones complejas
  }
}
```

## 📝 Buenas Prácticas

1. **Nombrar archivos descriptivamente**: `getAllRecipes_withFilters.json`
2. **Usar nombres de prueba claros**: Aparecerán en los reportes de JUnit
3. **Organizar por controlador**: Crear subdirectorios por cada API
4. **Validar lo necesario**: No sobrecargar con validaciones innecesarias
5. **Reutilizar configuración**: Usar `ITTestCommon` como clase base

## 🐛 Troubleshooting

### Problema: "Invalid character ' ' for QUERY_PARAM"
**Solución**: El sistema ya maneja automáticamente parámetros OData y expresiones complejas.

### Problema: "bodyType=OBJECT expected but was ARRAY"
**Solución**: Verifica que el `bodyType` en tu JSON coincida con la respuesta real de la API.

### Problema: Archivos JSON no se cargan
**Solución**: Verifica que el patrón de búsqueda sea correcto y que los archivos estén en `src/test/resources`.

## 📄 Licencia

Este código es parte del proyecto PharmaSuite de Adasoft.
