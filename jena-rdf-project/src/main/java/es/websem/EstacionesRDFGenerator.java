package es.websem;

import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.datatypes.xsd.XSDDatatype;

import java.io.*;
import java.util.Scanner;

/**
 * Generador de RDF a partir de datos de transporte público (GTFS)
 * Parte 1: Convierte el archivo stops.txt en un grafo RDF describiendo estaciones de tren
 * 
 * Cumple con los requisitos del entregable de Web Semántica:
 * - Utiliza el vocabulario WGS84 para geolocalización
 * - Genera IRIs basadas en stop_id
 * - Aplica tipos de datos XSD para coordenadas
 * - Exporta en formatos Turtle y RDF/XML
 */
public class EstacionesRDFGenerator {
    
    // Definición de namespaces según especificación
    private static final String EX_NS = "http://www.ejemplo.com/";
    private static final String GEO_NS = "http://www.w3.org/2003/01/geo/wgs84_pos#";
    
    // Propiedades y clases WGS84
    private static final Property GEO_LAT = ResourceFactory.createProperty(GEO_NS + "lat");
    private static final Property GEO_LONG = ResourceFactory.createProperty(GEO_NS + "long");
    private static final Resource GEO_SPATIAL_THING = ResourceFactory.createResource(GEO_NS + "SpatialThing");
    
    private Model model;
    
    public EstacionesRDFGenerator() {
        this.model = ModelFactory.createDefaultModel();
        // Configurar prefijos según especificación
        model.setNsPrefix("ex", EX_NS);
        model.setNsPrefix("geo", GEO_NS);
        model.setNsPrefix("rdfs", RDFS.getURI());
        model.setNsPrefix("xsd", XSDDatatype.XSD + "#");
    }
    
    /**
     * Procesa el archivo stops.txt del formato GTFS y genera el modelo RDF
     * Procesa hasta 200 líneas para evitar saturar la memoria en pruebas
     */
    public void procesarArchivoEstaciones(String rutaArchivo) {
        System.out.println("=== PARTE 1: GENERACIÓN DE RDF ===");
        System.out.println("Procesando archivo: " + rutaArchivo);
        
        try (BufferedReader reader = new BufferedReader(new FileReader(rutaArchivo))) {
            String cabecera = reader.readLine(); // Leer cabecera CSV
            System.out.println("Cabecera CSV: " + cabecera);
            
            String linea;
            int contadorLineas = 0;
            int procesadas = 0;
            
            while ((linea = reader.readLine()) != null && contadorLineas < 200) {
                contadorLineas++;
                
                if (linea.trim().isEmpty()) {
                    continue;
                }
                
                try {
                    procesarLineaEstacion(linea);
                    procesadas++;
                    
                    if (procesadas % 50 == 0) {
                        System.out.println("Procesadas " + procesadas + " estaciones...");
                    }
                } catch (Exception e) {
                    System.err.println("Error en línea " + contadorLineas + ": " + e.getMessage());
                }
            }
            
            System.out.println("✓ Procesamiento completado: " + procesadas + " estaciones de " + contadorLineas + " líneas");
            
        } catch (IOException e) {
            System.err.println("Error leyendo archivo: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Procesa una línea individual del archivo stops.txt
     * Formato esperado: stop_id,stop_code,stop_name,stop_desc,stop_lat,stop_lon,zone_id,stop_url,location_type,parent_station,stop_timezone,wheelchair_boarding
     */
    private void procesarLineaEstacion(String linea) {
        String[] campos = linea.split(",");
        
        if (campos.length < 6) {
            throw new IllegalArgumentException("Formato CSV incorrecto - faltan campos obligatorios");
        }
        
        String stopId = campos[0].trim();
        String stopName = campos[2].trim();
        String stopLat = campos[4].trim();
        String stopLon = campos[5].trim();
        
        // Validar campos obligatorios
        if (stopId.isEmpty() || stopName.isEmpty() || stopLat.isEmpty() || stopLon.isEmpty()) {
            throw new IllegalArgumentException("Campos obligatorios vacíos");
        }
        
        // Crear el recurso para la estación usando su stop_id
        Resource estacion = model.createResource(EX_NS + stopId);
        
        // Asignar tipo geo:SpatialThing según especificación
        estacion.addProperty(RDF.type, GEO_SPATIAL_THING);
        
        // Añadir nombre con idioma español
        estacion.addProperty(RDFS.label, model.createLiteral(stopName, "es"));
        
        // Añadir coordenadas como xsd:decimal
        try {
            Literal latLiteral = model.createTypedLiteral(stopLat, XSDDatatype.XSDdecimal);
            Literal lonLiteral = model.createTypedLiteral(stopLon, XSDDatatype.XSDdecimal);
            
            estacion.addProperty(GEO_LAT, latLiteral);
            estacion.addProperty(GEO_LONG, lonLiteral);
            
        } catch (Exception e) {
            throw new IllegalArgumentException("Error parseando coordenadas: " + stopLat + ", " + stopLon);
        }
    }
    
    /**
     * Exporta el modelo a archivo Turtle
     */
    public void exportarTurtle(String rutaSalida) {
        try (FileWriter writer = new FileWriter(rutaSalida)) {
            model.write(writer, "TURTLE");
            System.out.println("✓ Exportado a Turtle: " + rutaSalida);
        } catch (IOException e) {
            System.err.println("Error escribiendo archivo Turtle: " + e.getMessage());
        }
    }
    
    /**
     * Exporta el modelo a archivo RDF/XML
     */
    public void exportarRDFXML(String rutaSalida) {
        try (FileWriter writer = new FileWriter(rutaSalida)) {
            model.write(writer, "RDF/XML-ABBREV");
            System.out.println("✓ Exportado a RDF/XML: " + rutaSalida);
        } catch (IOException e) {
            System.err.println("Error escribiendo archivo RDF/XML: " + e.getMessage());
        }
    }
    
    /**
     * Muestra estadísticas del modelo generado
     */
    public void mostrarEstadisticas() {
        System.out.println("\\n=== ESTADÍSTICAS DEL MODELO RDF ===");
        System.out.println("Número total de triples: " + model.size());
        System.out.println("Número de estaciones: " + contarEstaciones());
        System.out.println("Número de statements por estación: " + (model.size() / contarEstaciones()));
    }
    
    private long contarEstaciones() {
        return model.listSubjectsWithProperty(RDF.type, GEO_SPATIAL_THING).toList().size();
    }
    
    /**
     * Muestra una muestra del RDF generado en formato Turtle
     */
    public void mostrarMuestraRDF() {
        System.out.println("\\n=== MUESTRA DE RDF GENERADO ===");
        
        // Mostrar las primeras 3 estaciones completas
        ResIterator estaciones = model.listSubjectsWithProperty(RDF.type, GEO_SPATIAL_THING);
        int contador = 0;
        
        while (estaciones.hasNext() && contador < 3) {
            Resource estacion = estaciones.nextResource();
            System.out.println("\\nEstación " + (contador + 1) + ":");
            
            StmtIterator props = model.listStatements(estacion, null, (RDFNode) null);
            while (props.hasNext()) {
                Statement stmt = props.nextStatement();
                System.out.println("  " + formatearTriple(stmt));
            }
            contador++;
        }
    }
    
    private String formatearTriple(Statement stmt) {
        String predicado = stmt.getPredicate().getLocalName();
        String objeto = stmt.getObject().toString();
        
        if (stmt.getObject().isLiteral()) {
            Literal lit = stmt.getObject().asLiteral();
            if (lit.getLanguage() != null && !lit.getLanguage().isEmpty()) {
                objeto = "\"" + lit.getString() + "\"@" + lit.getLanguage();
            } else if (lit.getDatatype() != null) {
                objeto = "\"" + lit.getString() + "\"^^" + lit.getDatatype().getURI();
            }
        }
        
        return predicado + " → " + objeto;
    }
    
    public Model getModelo() {
        return model;
    }
    
    /**
     * Método principal para ejecutar solo la Parte 1
     */
    public static void main(String[] args) {
        System.out.println("===============================================================");
        System.out.println("  ENTREGABLE RDF - PARTE 1: GENERACIÓN CON APACHE JENA");
        System.out.println("===============================================================");
        
        // Configurar rutas
        String archivoEstaciones = "../google_transit/stops.txt";
        String directorioSalida = "output";
        
        // Crear directorio de salida
        new File(directorioSalida).mkdirs();
        
        try {
            EstacionesRDFGenerator generador = new EstacionesRDFGenerator();
            
            // Procesar datos
            generador.procesarArchivoEstaciones(archivoEstaciones);
            
            // Mostrar estadísticas y muestra
            generador.mostrarEstadisticas();
            generador.mostrarMuestraRDF();
            
            // Exportar en ambos formatos
            generador.exportarTurtle(directorioSalida + "/estaciones.ttl");
            generador.exportarRDFXML(directorioSalida + "/estaciones.rdf");
            
            System.out.println("\\n✓ PARTE 1 COMPLETADA EXITOSAMENTE");
            System.out.println("Archivos generados en directorio: " + directorioSalida);
            
        } catch (Exception e) {
            System.err.println("❌ Error durante la generación: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
