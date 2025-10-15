package entregable.jena.websem;

import org.apache.jena.rdf.model.Model;
import java.io.File;
import java.util.List;
import java.util.Scanner;

/**
 * Clase principal que ejecuta ambas partes del entregable
 */
public class MainApplication {
    
    public static void main(String[] args) {
        System.out.println("===================================================================");
        System.out.println("          ENTREGABLE RDF - WEB SEMÁNTICA CON APACHE JENA          ");
        System.out.println("===================================================================");
        
        Scanner scanner = new Scanner(System.in);
        
        try {
            // Configurar rutas
            String stopsFile = "../google_transit/stops.txt";
            String outputDir = "output";
            
            // Crear directorio de salida
            new File(outputDir).mkdirs();
            
            System.out.println("\\n--- PARTE 1: Generación de RDF ---");
            System.out.println("Procesando archivo de estaciones: " + stopsFile);
            
            // Crear generador RDF
            RDFGenerator generator = new RDFGenerator();
            generator.processStopsFile(stopsFile);
            
            // Mostrar estadísticas
            generator.showStatistics();
            
            // Exportar archivos
            String turtleFile = outputDir + "/estaciones.ttl";
            String rdfxmlFile = outputDir + "/estaciones.rdf";
            
            generator.exportToTurtle(turtleFile);
            generator.exportToRDFXML(rdfxmlFile);
            
            System.out.println("\\n¿Desea ver una muestra del RDF generado? (s/n): ");
            String showSample = scanner.nextLine();
            if (showSample.toLowerCase().startsWith("s")) {
                generator.printSample();
            }
            
            System.out.println("\\n--- PARTE 2: Filtrado Geográfico ---");
            
            Model model = generator.getModel();
            
            // Mostrar opciones de filtrado
            System.out.println("\\nSeleccione un área geográfica para filtrar:");
            System.out.println("1. Madrid y alrededores (40.0-41.0 lat, -4.0--3.0 lon)");
            System.out.println("2. Centro de España (39.0-41.0 lat, -5.0--3.0 lon)");
            System.out.println("3. Extremadura (38.0-40.0 lat, -7.0--5.0 lon)");
            System.out.println("4. Área personalizada");
            System.out.println("5. Ver todas las áreas");
            
            System.out.print("Opción (1-5): ");
            int option = Integer.parseInt(scanner.nextLine());
            
            GeographicFilter.GeographicBounds selectedBounds = null;
            String areaName = "";
            
            switch (option) {
                case 1:
                    selectedBounds = new GeographicFilter.GeographicBounds(40.0, 41.0, -4.0, -3.0);
                    areaName = "Madrid";
                    break;
                case 2:
                    selectedBounds = new GeographicFilter.GeographicBounds(39.0, 41.0, -5.0, -3.0);
                    areaName = "Centro_España";
                    break;
                case 3:
                    selectedBounds = new GeographicFilter.GeographicBounds(38.0, 40.0, -7.0, -5.0);
                    areaName = "Extremadura";
                    break;
                case 4:
                    selectedBounds = getCustomBounds(scanner);
                    areaName = "Personalizada";
                    break;
                case 5:
                    GeographicFilter.demonstrateFiltering(model);
                    return;
                default:
                    System.out.println("Opción no válida");
                    return;
            }
            
            if (selectedBounds != null) {
                // Filtrar estaciones
                List<GeographicFilter.Station> filteredStations = 
                    GeographicFilter.filterStationsInBounds(model, selectedBounds);
                
                GeographicFilter.printFilteredStations(filteredStations);
                
                // Crear modelo filtrado
                Model filteredModel = GeographicFilter.createFilteredModel(model, selectedBounds);
                
                // Guardar modelo filtrado
                String filteredFile = outputDir + "/estaciones_" + areaName.toLowerCase() + ".ttl";
                try {
                    java.io.FileWriter writer = new java.io.FileWriter(filteredFile);
                    filteredModel.write(writer, "TURTLE");
                    writer.close();
                    System.out.println("\\nModelo filtrado guardado en: " + filteredFile);
                } catch (java.io.IOException e) {
                    System.err.println("Error guardando modelo filtrado: " + e.getMessage());
                }
            }
            
            System.out.println("\\n===================================================================");
            System.out.println("                         PROCESO COMPLETADO                        ");
            System.out.println("===================================================================");
            System.out.println("Archivos generados en el directorio 'output/':");
            System.out.println("- estaciones.ttl (formato Turtle)");
            System.out.println("- estaciones.rdf (formato RDF/XML)");
            if (selectedBounds != null) {
                System.out.println("- estaciones_" + areaName.toLowerCase() + ".ttl (filtrado)");
            }
            
        } catch (Exception e) {
            System.err.println("Error durante la ejecución: " + e.getMessage());
            e.printStackTrace();
        } finally {
            scanner.close();
        }
    }
    
    /**
     * Solicita al usuario coordenadas personalizadas
     */
    private static GeographicFilter.GeographicBounds getCustomBounds(Scanner scanner) {
        System.out.println("\\nIntroduzca las coordenadas del área rectangular:");
        
        System.out.print("Latitud mínima: ");
        double minLat = Double.parseDouble(scanner.nextLine());
        
        System.out.print("Latitud máxima: ");
        double maxLat = Double.parseDouble(scanner.nextLine());
        
        System.out.print("Longitud mínima: ");
        double minLon = Double.parseDouble(scanner.nextLine());
        
        System.out.print("Longitud máxima: ");
        double maxLon = Double.parseDouble(scanner.nextLine());
        
        return new GeographicFilter.GeographicBounds(minLat, maxLat, minLon, maxLon);
    }
}
