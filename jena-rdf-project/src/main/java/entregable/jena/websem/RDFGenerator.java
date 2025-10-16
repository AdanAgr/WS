package entregable.jena.websem;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;


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
    

    private void processStopLine(String line) {
        String[] fields = line.split(",");
        
        if (fields.length < 6) {
            throw new IllegalArgumentException("Línea con formato incorrecto, faltan campos");
        }
        
        String stopId = fields[0].trim();
        String stopName = fields[2].trim();
        String stopLat = fields[4].trim();
        String stopLon = fields[5].trim();
        
        if (stopId.isEmpty() || stopName.isEmpty() || stopLat.isEmpty() || stopLon.isEmpty()) {
            throw new IllegalArgumentException("Campos obligatorios vacíos");
        }
        
        Resource station = model.createResource(EX_NS + stopId);
        
        station.addProperty(RDF.type, GEO_SPATIAL_THING);
        
        station.addProperty(RDFS.label, model.createLiteral(stopName, "es"));
        
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
    

    public void exportToTurtle(String outputPath) {
        try (FileWriter writer = new FileWriter(outputPath)) {
            model.write(writer, "TURTLE");
            System.out.println("Modelo exportado a: " + outputPath);
        } catch (IOException e) {
            System.err.println("Error escribiendo archivo: " + e.getMessage());
        }
    }
    

    public void exportToRDFXML(String outputPath) {
        try (FileWriter writer = new FileWriter(outputPath)) {
            model.write(writer, "RDF/XML-ABBREV");
            System.out.println("Modelo exportado a: " + outputPath);
        } catch (IOException e) {
            System.err.println("Error escribiendo archivo: " + e.getMessage());
        }
    }
    

    public void showStatistics() {
        System.out.println("Estadísticas del modelo RDF:");
        System.out.println("Número de triples: " + model.size());
        System.out.println("Número de estaciones: " + countStations());
    }
    
    private long countStations() {
        return model.listSubjectsWithProperty(RDF.type, GEO_SPATIAL_THING).toList().size();
    }
    

    public void printSample() {
        System.out.println("Muestra del modelo RDF (primeras 20 triples):");
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
        String stopsFile = "../google_transit/stops.txt"; 
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
            
            generator.exportToTurtle(outputTurtle);
            generator.exportToRDFXML(outputRDFXML);
            
            System.out.println("\\n=== Proceso completado exitosamente ===");
            
        } catch (Exception e) {
            System.err.println("Error durante el procesamiento: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
