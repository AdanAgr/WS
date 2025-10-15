package entregable.jena.websem;

import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.datatypes.xsd.XSDDatatype;

import java.io.*;
import java.util.Scanner;

/**
 * Generador de RDF a partir de datos de transporte público (GTFS)
 * Parte 1: Convierte el archivo stops.txt en un grafo RDF describiendo estaciones de tren
 */
public class RDFGenerator {
    
    // Definición de namespaces
    private static final String EX_NS = "http://www.ejemplo.com/";
    private static final String GEO_NS = "http://www.w3.org/2003/01/geo/wgs84_pos#";
    
    // Propiedades y clases
    private static final Property GEO_LAT = ResourceFactory.createProperty(GEO_NS + "lat");
    private static final Property GEO_LONG = ResourceFactory.createProperty(GEO_NS + "long");
    private static final Resource GEO_SPATIAL_THING = ResourceFactory.createResource(GEO_NS + "SpatialThing");
    
    private Model model;
    
    public RDFGenerator() {
        this.model = ModelFactory.createDefaultModel();
        // Configurar prefijos
        model.setNsPrefix("ex", EX_NS);
        model.setNsPrefix("geo", GEO_NS);
        model.setNsPrefix("rdfs", RDFS.getURI());
        model.setNsPrefix("xsd", XSDDatatype.XSD + "#");
    }
    
    /**
     * Procesa el archivo stops.txt y genera el modelo RDF
     */
    public void processStopsFile(String stopsFilePath) {
        System.out.println("Procesando archivo: " + stopsFilePath);
        
        try (BufferedReader reader = new BufferedReader(new FileReader(stopsFilePath))) {
            String headerLine = reader.readLine(); // Leer cabecera
            System.out.println("Cabecera: " + headerLine);
            
            String line;
            int lineCount = 0;
            int processedCount = 0;
            
            while ((line = reader.readLine()) != null && lineCount < 200) { // Limitar a 200 líneas para pruebas
                lineCount++;
                
                if (line.trim().isEmpty()) {
                    continue;
                }
                
                try {
                    processStopLine(line);
                    processedCount++;
                } catch (Exception e) {
                    System.err.println("Error procesando línea " + lineCount + ": " + line);
                    System.err.println("Error: " + e.getMessage());
                }
            }
            
            System.out.println("Líneas procesadas: " + processedCount + " de " + lineCount);
            
        } catch (IOException e) {
            System.err.println("Error leyendo el archivo: " + e.getMessage());
        }
    }
    
    /**
     * Procesa una línea individual del archivo stops.txt
     */
    private void processStopLine(String line) {
        // Parsear CSV básico (sin manejar comillas por simplicidad)
        String[] fields = line.split(",");
        
        if (fields.length < 6) {
            throw new IllegalArgumentException("Línea con formato incorrecto, faltan campos");
        }
        
        String stopId = fields[0].trim();
        String stopName = fields[2].trim();
        String stopLat = fields[4].trim();
        String stopLon = fields[5].trim();
        
        // Validar que tenemos los datos necesarios
        if (stopId.isEmpty() || stopName.isEmpty() || stopLat.isEmpty() || stopLon.isEmpty()) {
            throw new IllegalArgumentException("Campos obligatorios vacíos");
        }
        
        // Crear el recurso para la estación
        Resource station = model.createResource(EX_NS + stopId);
        
        // Añadir tipo
        station.addProperty(RDF.type, GEO_SPATIAL_THING);
        
        // Añadir nombre con idioma español
        station.addProperty(RDFS.label, model.createLiteral(stopName, "es"));
        
        // Añadir coordenadas
        try {
            Literal latLiteral = model.createTypedLiteral(stopLat, XSDDatatype.XSDdecimal);
            Literal lonLiteral = model.createTypedLiteral(stopLon, XSDDatatype.XSDdecimal);
            
            station.addProperty(GEO_LAT, latLiteral);
            station.addProperty(GEO_LONG, lonLiteral);
            
        } catch (Exception e) {
            throw new IllegalArgumentException("Error parseando coordenadas: " + e.getMessage());
        }
        
        System.out.println("Procesada estación: " + stopId + " - " + stopName);
    }
    
    /**
     * Exporta el modelo a un archivo en formato Turtle
     */
    public void exportToTurtle(String outputPath) {
        try (FileWriter writer = new FileWriter(outputPath)) {
            model.write(writer, "TURTLE");
            System.out.println("Modelo exportado a: " + outputPath);
        } catch (IOException e) {
            System.err.println("Error escribiendo archivo: " + e.getMessage());
        }
    }
    
    /**
     * Exporta el modelo a un archivo en formato RDF/XML
     */
    public void exportToRDFXML(String outputPath) {
        try (FileWriter writer = new FileWriter(outputPath)) {
            model.write(writer, "RDF/XML-ABBREV");
            System.out.println("Modelo exportado a: " + outputPath);
        } catch (IOException e) {
            System.err.println("Error escribiendo archivo: " + e.getMessage());
        }
    }
    
    /**
     * Muestra estadísticas del modelo
     */
    public void showStatistics() {
        System.out.println("=== Estadísticas del modelo RDF ===");
        System.out.println("Número de triples: " + model.size());
        System.out.println("Número de estaciones: " + countStations());
    }
    
    private long countStations() {
        return model.listSubjectsWithProperty(RDF.type, GEO_SPATIAL_THING).toList().size();
    }
    
    /**
     * Imprime una muestra del modelo generado
     */
    public void printSample() {
        System.out.println("=== Muestra del modelo RDF (primeras 20 triples) ===");
        StmtIterator iter = model.listStatements();
        int count = 0;
        while (iter.hasNext() && count < 20) {
            Statement stmt = iter.nextStatement();
            System.out.println(stmt.getSubject().getURI() + " " + 
                             stmt.getPredicate().getURI() + " " + 
                             stmt.getObject().toString());
            count++;
        }
    }
    
    public Model getModel() {
        return model;
    }
    
    public static void main(String[] args) {
        System.out.println("=== Generador RDF para datos de transporte ===");
        
        // Configurar rutas
        String stopsFile = "../google_transit/stops.txt"; // Relativo al directorio del proyecto
        String outputTurtle = "output/estaciones.ttl";
        String outputRDFXML = "output/estaciones.rdf";
        
        // Crear directorio de salida
        new File("output").mkdirs();
        
        // Crear generador y procesar datos
        RDFGenerator generator = new RDFGenerator();
        
        try {
            generator.processStopsFile(stopsFile);
            generator.showStatistics();
            generator.printSample();
            
            // Exportar en ambos formatos
            generator.exportToTurtle(outputTurtle);
            generator.exportToRDFXML(outputRDFXML);
            
            System.out.println("\\n=== Proceso completado exitosamente ===");
            
        } catch (Exception e) {
            System.err.println("Error durante el procesamiento: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
