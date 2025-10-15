package es.websem;

import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;

import java.util.ArrayList;
import java.util.List;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Filtro geográfico para estaciones de tren
 * Parte 2: Recorre el modelo RDF (sin SPARQL) y filtra recursos dentro de coordenadas específicas
 * 
 * Implementa los requisitos del entregable:
 * - Recorre el modelo RDF directamente (sin usar SPARQL)
 * - Filtra por coordenadas geográficas rectangulares
 * - Trabaja con recursos geo:SpatialThing
 */
public class FiltroGeografico {
    
    // Propiedades geográficas WGS84
    private static final String GEO_NS = "http://www.w3.org/2003/01/geo/wgs84_pos#";
    private static final Property GEO_LAT = ResourceFactory.createProperty(GEO_NS + "lat");
    private static final Property GEO_LONG = ResourceFactory.createProperty(GEO_NS + "long");
    private static final Resource GEO_SPATIAL_THING = ResourceFactory.createResource(GEO_NS + "SpatialThing");
    private static final Property RDFS_LABEL = ResourceFactory.createProperty(RDFS.getURI() + "label");
    
    /**
     * Clase para representar los límites de un área geográfica rectangular
     */
    public static class LimitesGeograficos {
        private final double latMin, latMax, lonMin, lonMax;
        private final String nombre;
        
        public LimitesGeograficos(double latMin, double latMax, double lonMin, double lonMax, String nombre) {
            this.latMin = latMin;
            this.latMax = latMax;
            this.lonMin = lonMin;
            this.lonMax = lonMax;
            this.nombre = nombre;
        }
        
        public boolean contiene(double lat, double lon) {
            return lat >= latMin && lat <= latMax && lon >= lonMin && lon <= lonMax;
        }
        
        @Override
        public String toString() {
            return String.format("%s [lat: %.3f-%.3f, lon: %.3f-%.3f]", 
                               nombre, latMin, latMax, lonMin, lonMax);
        }
        
        // Getters
        public double getLatMin() { return latMin; }
        public double getLatMax() { return latMax; }
        public double getLonMin() { return lonMin; }
        public double getLonMax() { return lonMax; }
        public String getNombre() { return nombre; }
    }
    
    /**
     * Clase para representar una estación con datos extraídos del RDF
     */
    public static class EstacionRDF {
        private final Resource recurso;
        private final String nombre;
        private final double latitud;
        private final double longitud;
        
        public EstacionRDF(Resource recurso, String nombre, double latitud, double longitud) {
            this.recurso = recurso;
            this.nombre = nombre;
            this.latitud = latitud;
            this.longitud = longitud;
        }
        
        public boolean estaDentroDelArea(LimitesGeograficos limites) {
            return limites.contiene(latitud, longitud);
        }
        
        @Override
        public String toString() {
            return String.format("%-40s (%.4f, %.4f) [%s]", 
                               nombre, latitud, longitud, recurso.getLocalName());
        }
        
        // Getters
        public Resource getRecurso() { return recurso; }
        public String getNombre() { return nombre; }
        public double getLatitud() { return latitud; }
        public double getLongitud() { return longitud; }
    }
    
    /**
     * MÉTODO PRINCIPAL DE FILTRADO (SIN SPARQL)
     * Recorre manualmente el modelo RDF para encontrar estaciones en el área especificada
     */
    public static List<EstacionRDF> filtrarEstacionesEnArea(Model modelo, LimitesGeograficos limites) {
        List<EstacionRDF> estacionesFiltradas = new ArrayList<>();
        
        System.out.println("\\n=== PARTE 2: FILTRADO GEOGRÁFICO (SIN SPARQL) ===");
        System.out.println("Área de filtrado: " + limites);
        System.out.println("Recorriendo modelo RDF manualmente...");
        
        // 1. Obtener todos los recursos de tipo geo:SpatialThing
        ResIterator iteradorEstaciones = modelo.listSubjectsWithProperty(RDF.type, GEO_SPATIAL_THING);
        
        int totalEstaciones = 0;
        int estacionesEnArea = 0;
        
        // 2. Recorrer cada estación manualmente (sin SPARQL)
        while (iteradorEstaciones.hasNext()) {
            totalEstaciones++;
            Resource estacion = iteradorEstaciones.nextResource();
            
            try {
                // 3. Extraer datos de la estación recorriendo sus propiedades
                EstacionRDF datosEstacion = extraerDatosEstacion(modelo, estacion);
                
                if (datosEstacion != null) {
                    // 4. Verificar si está dentro del área
                    if (datosEstacion.estaDentroDelArea(limites)) {
                        estacionesFiltradas.add(datosEstacion);
                        estacionesEnArea++;
                    }
                }
                
            } catch (Exception e) {
                System.err.println("Error procesando estación " + estacion.getURI() + ": " + e.getMessage());
            }
        }
        
        System.out.println("✓ Filtrado completado:");
        System.out.println("  - Total estaciones examinadas: " + totalEstaciones);
        System.out.println("  - Estaciones en el área: " + estacionesEnArea);
        System.out.println("  - Porcentaje: " + String.format("%.1f%%", (estacionesEnArea * 100.0 / totalEstaciones)));
        
        return estacionesFiltradas;
    }
    
    /**
     * Extrae datos de una estación recorriendo manualmente sus propiedades RDF
     * NO utiliza SPARQL, solo la API de recorrido del modelo
     */
    private static EstacionRDF extraerDatosEstacion(Model modelo, Resource estacion) {
        try {
            // Extraer nombre (rdfs:label)
            Statement stmtNombre = modelo.getProperty(estacion, RDFS_LABEL);
            String nombre = (stmtNombre != null) ? stmtNombre.getString() : "Sin nombre";
            
            // Extraer latitud (geo:lat)
            Statement stmtLat = modelo.getProperty(estacion, GEO_LAT);
            if (stmtLat == null) {
                throw new IllegalStateException("Falta propiedad geo:lat");
            }
            double latitud = stmtLat.getDouble();
            
            // Extraer longitud (geo:long)
            Statement stmtLon = modelo.getProperty(estacion, GEO_LONG);
            if (stmtLon == null) {
                throw new IllegalStateException("Falta propiedad geo:long");
            }
            double longitud = stmtLon.getDouble();
            
            return new EstacionRDF(estacion, nombre, latitud, longitud);
            
        } catch (Exception e) {
            System.err.println("Error extrayendo datos: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Crea un nuevo modelo RDF que contiene solo las estaciones filtradas
     */
    public static Model crearModeloFiltrado(Model modeloOriginal, LimitesGeograficos limites) {
        Model modeloFiltrado = ModelFactory.createDefaultModel();
        
        // Copiar namespaces del modelo original
        modeloFiltrado.setNsPrefixes(modeloOriginal.getNsPrefixMap());
        
        // Obtener estaciones filtradas
        List<EstacionRDF> estacionesFiltradas = filtrarEstacionesEnArea(modeloOriginal, limites);
        
        // Añadir cada estación filtrada al nuevo modelo
        for (EstacionRDF estacion : estacionesFiltradas) {
            Resource recursoOriginal = estacion.getRecurso();
            
            // Copiar todos los statements de esta estación
            StmtIterator statements = modeloOriginal.listStatements(recursoOriginal, null, (RDFNode) null);
            while (statements.hasNext()) {
                Statement stmt = statements.nextStatement();
                modeloFiltrado.add(stmt);
            }
        }
        
        System.out.println("✓ Modelo filtrado creado con " + modeloFiltrado.size() + " triples");
        return modeloFiltrado;
    }
    
    /**
     * Muestra las estaciones encontradas en formato legible
     */
    public static void mostrarEstacionesFiltradas(List<EstacionRDF> estaciones, LimitesGeograficos limites) {
        System.out.println("\\n=== ESTACIONES EN " + limites.getNombre().toUpperCase() + " ===");
        
        if (estaciones.isEmpty()) {
            System.out.println("❌ No se encontraron estaciones en el área especificada.");
            return;
        }
        
        System.out.println("Estaciones encontradas:");
        for (int i = 0; i < estaciones.size(); i++) {
            System.out.println(String.format("%2d. %s", i + 1, estaciones.get(i)));
        }
    }
    
    /**
     * Demuestra el filtrado con diferentes áreas geográficas de España
     */
    public static void demostrarFiltradoCompleto(Model modelo) {
        System.out.println("\\n=== DEMOSTRACIÓN DE FILTRADO GEOGRÁFICO ===");
        
        // Definir áreas geográficas de interés
        LimitesGeograficos[] areas = {
            new LimitesGeograficos(40.0, 41.0, -4.0, -3.0, "Madrid y alrededores"),
            new LimitesGeograficos(39.0, 41.0, -5.0, -3.0, "Centro de España"),
            new LimitesGeograficos(38.0, 40.0, -7.0, -5.0, "Extremadura"),
            new LimitesGeograficos(41.0, 44.0, -3.0, 3.0, "Norte de España"),
            new LimitesGeograficos(36.0, 39.0, -6.0, -2.0, "Sur de España")
        };
        
        // Procesar cada área
        for (LimitesGeograficos area : areas) {
            List<EstacionRDF> estaciones = filtrarEstacionesEnArea(modelo, area);
            mostrarEstacionesFiltradas(estaciones, area);
            
            // Guardar modelo filtrado
            if (!estaciones.isEmpty()) {
                guardarModeloFiltrado(modelo, area, "output");
            }
        }
    }
    
    /**
     * Guarda un modelo filtrado en archivo Turtle
     */
    private static void guardarModeloFiltrado(Model modeloOriginal, LimitesGeograficos limites, String directorio) {
        try {
            Model modeloFiltrado = crearModeloFiltrado(modeloOriginal, limites);
            String nombreArchivo = directorio + "/estaciones_" + 
                                  limites.getNombre().toLowerCase().replace(" ", "_") + ".ttl";
            
            FileWriter writer = new FileWriter(nombreArchivo);
            modeloFiltrado.write(writer, "TURTLE");
            writer.close();
            
            System.out.println("💾 Guardado: " + nombreArchivo);
            
        } catch (IOException e) {
            System.err.println("Error guardando modelo filtrado: " + e.getMessage());
        }
    }
    
    /**
     * Método principal para ejecutar solo la Parte 2
     */
    public static void main(String[] args) {
        System.out.println("===============================================================");
        System.out.println("  ENTREGABLE RDF - PARTE 2: FILTRADO GEOGRÁFICO");
        System.out.println("===============================================================");
        
        try {
            // Primero necesitamos generar el modelo RDF
            System.out.println("Generando modelo RDF de estaciones...");
            EstacionesRDFGenerator generador = new EstacionesRDFGenerator();
            generador.procesarArchivoEstaciones("../google_transit/stops.txt");
            
            Model modelo = generador.getModelo();
            
            // Crear directorio de salida
            new java.io.File("output").mkdirs();
            
            // Demostrar filtrado completo
            demostrarFiltradoCompleto(modelo);
            
            System.out.println("\\n✓ PARTE 2 COMPLETADA EXITOSAMENTE");
            System.out.println("Modelos filtrados guardados en directorio: output/");
            
        } catch (Exception e) {
            System.err.println("❌ Error durante el filtrado: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
