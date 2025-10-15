package es.websem;

import org.apache.jena.rdf.model.Model;
import java.io.File;
import java.util.List;
import java.util.Scanner;

/**
 * Aplicación principal que combina ambas partes del entregable
 * Ejecuta la generación de RDF y el filtrado geográfico de forma integrada
 */
public class EntregableRDFCompleto {
    
    public static void main(String[] args) {
        System.out.println("===============================================================");
        System.out.println("            ENTREGABLE RDF - WEB SEMÁNTICA 2025");
        System.out.println("        GENERACIÓN Y FILTRADO CON APACHE JENA");
        System.out.println("===============================================================");
        
        Scanner scanner = new Scanner(System.in);
        
        try {
            // Configuración inicial
            String archivoEstaciones = "../google_transit/stops.txt";
            String directorioSalida = "output";
            
            // Verificar que existe el archivo de datos
            File archivoGTFS = new File(archivoEstaciones);
            if (!archivoGTFS.exists()) {
                System.err.println("❌ Error: No se encuentra el archivo " + archivoEstaciones);
                System.err.println("   Asegúrate de que los datos de Google Transit están en ../google_transit/");
                return;
            }
            
            // Crear directorio de salida
            new File(directorioSalida).mkdirs();
            
            System.out.println("\\n🚂 Archivo de estaciones encontrado: " + archivoEstaciones);
            System.out.println("📁 Directorio de salida: " + directorioSalida);
            
            // ============== PARTE 1: GENERACIÓN DE RDF ==============
            System.out.println("\\n" + "=".repeat(60));
            System.out.println("                    PARTE 1: GENERACIÓN RDF");
            System.out.println("=".repeat(60));
            
            EstacionesRDFGenerator generador = new EstacionesRDFGenerator();
            generador.procesarArchivoEstaciones(archivoEstaciones);
            
            // Mostrar estadísticas
            generador.mostrarEstadisticas();
            
            // Preguntar si mostrar muestra
            System.out.print("\\n¿Desea ver una muestra del RDF generado? (s/n): ");
            String respuesta = scanner.nextLine().trim().toLowerCase();
            if (respuesta.startsWith("s")) {
                generador.mostrarMuestraRDF();
            }
            
            // Exportar archivos
            String archivoTurtle = directorioSalida + "/estaciones.ttl";
            String archivoRDFXML = directorioSalida + "/estaciones.rdf";
            
            generador.exportarTurtle(archivoTurtle);
            generador.exportarRDFXML(archivoRDFXML);
            
            System.out.println("\\n✅ PARTE 1 COMPLETADA");
            
            // ============== PARTE 2: FILTRADO GEOGRÁFICO ==============
            System.out.println("\\n" + "=".repeat(60));
            System.out.println("                PARTE 2: FILTRADO GEOGRÁFICO");
            System.out.println("=".repeat(60));
            
            Model modelo = generador.getModelo();
            
            // Mostrar opciones de filtrado
            mostrarMenuFiltrado();
            
            System.out.print("Seleccione una opción (1-6): ");
            int opcion = 0;
            try {
                opcion = Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                opcion = 6; // Mostrar todas por defecto
            }
            
            FiltroGeografico.LimitesGeograficos limitesSeleccionados = null;
            String nombreArea = "";
            
            switch (opcion) {
                case 1:
                    limitesSeleccionados = new FiltroGeografico.LimitesGeograficos(
                        40.0, 41.0, -4.0, -3.0, "Madrid y alrededores");
                    nombreArea = "madrid";
                    break;
                case 2:
                    limitesSeleccionados = new FiltroGeografico.LimitesGeograficos(
                        39.0, 41.0, -5.0, -3.0, "Centro de España");
                    nombreArea = "centro_espana";
                    break;
                case 3:
                    limitesSeleccionados = new FiltroGeografico.LimitesGeograficos(
                        38.0, 40.0, -7.0, -5.0, "Extremadura");
                    nombreArea = "extremadura";
                    break;
                case 4:
                    limitesSeleccionados = new FiltroGeografico.LimitesGeograficos(
                        41.0, 44.0, -3.0, 3.0, "Norte de España");
                    nombreArea = "norte_espana";
                    break;
                case 5:
                    limitesSeleccionados = obtenerCoordenadasPersonalizadas(scanner);
                    nombreArea = "personalizada";
                    break;
                case 6:
                default:
                    FiltroGeografico.demostrarFiltradoCompleto(modelo);
                    scanner.close();
                    return;
            }
            
            if (limitesSeleccionados != null) {
                // Filtrar estaciones
                List<FiltroGeografico.EstacionRDF> estacionesFiltradas = 
                    FiltroGeografico.filtrarEstacionesEnArea(modelo, limitesSeleccionados);
                
                FiltroGeografico.mostrarEstacionesFiltradas(estacionesFiltradas, limitesSeleccionados);
                
                // Crear y guardar modelo filtrado
                if (!estacionesFiltradas.isEmpty()) {
                    Model modeloFiltrado = FiltroGeografico.crearModeloFiltrado(modelo, limitesSeleccionados);
                    
                    String archivoFiltrado = directorioSalida + "/estaciones_" + nombreArea + ".ttl";
                    try {
                        java.io.FileWriter writer = new java.io.FileWriter(archivoFiltrado);
                        modeloFiltrado.write(writer, "TURTLE");
                        writer.close();
                        System.out.println("💾 Modelo filtrado guardado: " + archivoFiltrado);
                    } catch (java.io.IOException e) {
                        System.err.println("Error guardando modelo filtrado: " + e.getMessage());
                    }
                } else {
                    System.out.println("⚠️  No se guardó archivo filtrado (área vacía)");
                }
            }
            
            System.out.println("\\n✅ PARTE 2 COMPLETADA");
            
            // ============== RESUMEN FINAL ==============
            System.out.println("\\n" + "=".repeat(60));
            System.out.println("                    RESUMEN DE ARCHIVOS");
            System.out.println("=".repeat(60));
            
            mostrarArchivosGenerados(directorioSalida);
            
            System.out.println("\\n🎉 ENTREGABLE COMPLETADO EXITOSAMENTE");
            System.out.println("\\n📝 Los archivos RDF han sido generados según la especificación:");
            System.out.println("   • Vocabulario WGS84 para geolocalización");
            System.out.println("   • Tipos de datos XSD para coordenadas");
            System.out.println("   • Filtrado sin uso de SPARQL");
            System.out.println("   • Formatos Turtle y RDF/XML");
            
        } catch (Exception e) {
            System.err.println("\\n❌ Error durante la ejecución: " + e.getMessage());
            e.printStackTrace();
        } finally {
            scanner.close();
        }
    }
    
    private static void mostrarMenuFiltrado() {
        System.out.println("\\n🗺️  Seleccione un área geográfica para filtrar:");
        System.out.println("   1. Madrid y alrededores (40.0-41.0 lat, -4.0--3.0 lon)");
        System.out.println("   2. Centro de España (39.0-41.0 lat, -5.0--3.0 lon)");
        System.out.println("   3. Extremadura (38.0-40.0 lat, -7.0--5.0 lon)");
        System.out.println("   4. Norte de España (41.0-44.0 lat, -3.0-3.0 lon)");
        System.out.println("   5. Área personalizada (introducir coordenadas)");
        System.out.println("   6. Mostrar todas las áreas (demostración completa)");
    }
    
    private static FiltroGeografico.LimitesGeograficos obtenerCoordenadasPersonalizadas(Scanner scanner) {
        System.out.println("\\n📍 Introduzca las coordenadas del área rectangular:");
        
        try {
            System.out.print("   Latitud mínima (ej: 40.0): ");
            double latMin = Double.parseDouble(scanner.nextLine());
            
            System.out.print("   Latitud máxima (ej: 41.0): ");
            double latMax = Double.parseDouble(scanner.nextLine());
            
            System.out.print("   Longitud mínima (ej: -4.0): ");
            double lonMin = Double.parseDouble(scanner.nextLine());
            
            System.out.print("   Longitud máxima (ej: -3.0): ");
            double lonMax = Double.parseDouble(scanner.nextLine());
            
            return new FiltroGeografico.LimitesGeograficos(latMin, latMax, lonMin, lonMax, "Área personalizada");
            
        } catch (NumberFormatException e) {
            System.err.println("Error en formato de coordenadas. Usando Madrid por defecto.");
            return new FiltroGeografico.LimitesGeograficos(40.0, 41.0, -4.0, -3.0, "Madrid (por defecto)");
        }
    }
    
    private static void mostrarArchivosGenerados(String directorio) {
        File dir = new File(directorio);
        File[] archivos = dir.listFiles((d, name) -> name.endsWith(".ttl") || name.endsWith(".rdf"));
        
        if (archivos != null && archivos.length > 0) {
            System.out.println("📂 Archivos generados en '" + directorio + "':");
            for (File archivo : archivos) {
                long tamaño = archivo.length();
                String tamaañoStr = tamaño > 1024 ? (tamaño / 1024) + " KB" : tamaño + " B";
                System.out.println("   • " + archivo.getName() + " (" + tamaañoStr + ")");
            }
        } else {
            System.out.println("⚠️  No se encontraron archivos en " + directorio);
        }
    }
}
