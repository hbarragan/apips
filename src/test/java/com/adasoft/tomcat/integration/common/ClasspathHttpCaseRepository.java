package com.adasoft.tomcat.integration.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * Repositorio de casos de prueba HTTP cargados desde archivos JSON en el classpath.
 * Busca archivos JSON que coincidan con un patrón y los convierte en objetos HttpCaseDefinition.
 *
 * Ejemplo de uso:
 * <pre>
 *   var repo = new ClasspathHttpCaseRepository(mapper, "classpath*:/it-cases/recipe/*.json");
 *   Stream<HttpCaseDefinition> cases = repo.load();
 * </pre>
 */
public final class ClasspathHttpCaseRepository {

    private final ObjectMapper mapper;   // Convierte JSON a objetos Java
    private final String pattern;        // Patrón de búsqueda (ej: "classpath*:/it-cases/**/*.json")

    /**
     * Constructor que inicializa el repositorio.
     *
     * @param mapper  ObjectMapper de Jackson para deserializar JSON
     * @param pattern Patrón de búsqueda de archivos (soporta wildcards)
     */
    public ClasspathHttpCaseRepository(ObjectMapper mapper, String pattern) {
        this.mapper = mapper;
        this.pattern = pattern;
    }

    /**
     * Carga todos los casos de prueba que coincidan con el patrón configurado.
     * Los archivos se ordenan alfabéticamente por nombre.
     *
     * @return Stream de casos de prueba HTTP
     * @throws IllegalStateException Si ocurre un error al buscar o leer los archivos
     */
    public Stream<HttpCaseDefinition> load() {
        try {
            // Resolver archivos que coincidan con el patrón
            var resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources(pattern);

            // Ordenar archivos alfabéticamente para ejecución predecible
            Arrays.sort(resources, Comparator.comparing(r -> getFilenameOrEmpty(r.getFilename())));

            // Convertir cada archivo a un caso de prueba
            return Arrays.stream(resources)
                    .map(this::loadCaseFromFile);
        } catch (Exception e) {
            throw new IllegalStateException(
                "No se pudieron cargar casos de prueba con el patrón: " + pattern, e
            );
        }
    }

    /**
     * Lee un archivo de recurso y lo convierte en un caso de prueba HTTP.
     *
     * @param resource Recurso que contiene el archivo JSON
     * @return Caso de prueba deserializado
     * @throws IllegalStateException Si el archivo no se puede leer o parsear
     */
    private HttpCaseDefinition loadCaseFromFile(Resource resource) {
        String fileName = getFilenameOrEmpty(resource.getFilename());
        try (InputStream inputStream = resource.getInputStream()) {
            return mapper.readValue(inputStream, HttpCaseDefinition.class);
        } catch (Exception e) {
            throw new IllegalStateException(
                "Error al leer el caso de prueba del archivo: " + fileName, e
            );
        }
    }

    /**
     * Obtiene el nombre del archivo de forma segura, retornando cadena vacía si es null.
     *
     * @param filename Nombre del archivo
     * @return Nombre del archivo o cadena vacía
     */
    private static String getFilenameOrEmpty(String filename) {
        return filename != null ? filename : "";
    }
}
