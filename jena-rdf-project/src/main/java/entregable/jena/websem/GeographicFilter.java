package entregable.jena.websem;

import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.datatypes.xsd.XSDDatatype;

import java.util.ArrayList;
import java.util.List;

/**
 * Filtro geográfico para estaciones de tren
 * Parte 2: Filtra recursos RDF que se encuentren dentro de un rectángulo geográfico
 */
public class GeographicFilter {
    
    // Propiedades geográficas
    private static final String GEO_NS = "http://www.w3.org/2003/01/geo/wgs84_pos#";
    private static final Property GEO_LAT = ResourceFactory.createProperty(GEO_NS + "lat");
    private static final Property GEO_LONG = ResourceFactory.createProperty(GEO_NS + "long");
    private static final Resource GEO_SPATIAL_THING = ResourceFactory.createResource(GEO_NS + "SpatialThing");
    
    /**
     * Clase para representar un área geográfica rectangular
     */
    public static class GeographicBounds {
        private final double minLat, maxLat, minLon, maxLon;
        
        public GeographicBounds(double minLat, double maxLat, double minLon, double maxLon) {
            this.minLat = minLat;
            this.maxLat = maxLat;
            this.minLon = minLon;
            this.maxLon = maxLon;
        }
        
        public boolean contains(double lat, double lon) {
            return lat >= minLat && lat <= maxLat && lon >= minLon && lon <= maxLon;
        }
        
        @Override
        public String toString() {
            return String.format("Bounds[lat: %.4f to %.4f, lon: %.4f to %.4f]", 
                               minLat, maxLat, minLon, maxLon);
        }
        
        // Getters
        public double getMinLat() { return minLat; }
        public double getMaxLat() { return maxLat; }
        public double getMinLon() { return minLon; }
        public double getMaxLon() { return maxLon; }
    }
    
    /**
     * Clase para representar una estación con sus coordenadas
     */
    public static class Station {
        private final Resource resource;
        private final String name;
        private final double latitude;
        private final double longitude;
        
        public Station(Resource resource, String name, double latitude, double longitude) {
            this.resource = resource;
            this.name = name;
            this.latitude = latitude;
            this.longitude = longitude;
        }
        
        public boolean isWithinBounds(GeographicBounds bounds) {
            return bounds.contains(latitude, longitude);
        }
        
        @Override
        public String toString() {
            return String.format("Station[%s] %s (%.4f, %.4f)", 
                               resource.getURI(), name, latitude, longitude);
        }
        
        // Getters
        public Resource getResource() { return resource; }
        public String getName() { return name; }
        public double getLatitude() { return latitude; }
        public double getLongitude() { return longitude; }
    }
    
    /**
     * Filtra las estaciones del modelo que están dentro del área geográfica especificada
     */
    public static List<Station> filterStationsInBounds(Model model, GeographicBounds bounds) {
        List<Station> stationsInBounds = new ArrayList<>();
        
        System.out.println("Filtrando estaciones en el área: " + bounds);
        
        // Obtener todos los recursos de tipo SpatialThing
        ResIterator spatialThings = model.listSubjectsWithProperty(RDF.type, GEO_SPATIAL_THING);
        
        int totalStations = 0;
        while (spatialThings.hasNext()) {
            totalStations++;
            Resource station = spatialThings.nextResource();
            
            try {
                Station stationData = extractStationData(station);
                if (stationData != null && stationData.isWithinBounds(bounds)) {
                    stationsInBounds.add(stationData);
                }
            } catch (Exception e) {
                System.err.println("Error procesando estación " + station.getURI() + ": " + e.getMessage());
            }
        }
        
        System.out.println(String.format("Encontradas %d estaciones dentro del área de %d totales", 
                                        stationsInBounds.size(), totalStations));
        
        return stationsInBounds;
    }
    
    /**
     * Extrae los datos de una estación desde el modelo RDF
     */
    private static Station extractStationData(Resource stationResource) {
        try {
            // Obtener nombre (label)
            Statement nameStmt = stationResource.getProperty(ResourceFactory.createProperty("http://www.w3.org/2000/01/rdf-schema#label"));
            String name = nameStmt != null ? nameStmt.getString() : "Sin nombre";
            
            // Obtener latitud
            Statement latStmt = stationResource.getProperty(GEO_LAT);
            if (latStmt == null) {
                throw new IllegalStateException("Falta propiedad geo:lat");
            }
            double latitude = latStmt.getDouble();
            
            // Obtener longitud
            Statement lonStmt = stationResource.getProperty(GEO_LONG);
            if (lonStmt == null) {
                throw new IllegalStateException("Falta propiedad geo:long");
            }
            double longitude = lonStmt.getDouble();
            
            return new Station(stationResource, name, latitude, longitude);
            
        } catch (Exception e) {
            System.err.println("Error extrayendo datos de estación: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Crea un nuevo modelo con solo las estaciones filtradas
     */
    public static Model createFilteredModel(Model originalModel, GeographicBounds bounds) {
        Model filteredModel = ModelFactory.createDefaultModel();
        
        // Copiar namespaces
        filteredModel.setNsPrefixes(originalModel.getNsPrefixMap());
        
        List<Station> filteredStations = filterStationsInBounds(originalModel, bounds);
        
        // Añadir las estaciones filtradas al nuevo modelo
        for (Station station : filteredStations) {
            Resource originalResource = station.getResource();
            
            // Copiar todas las propiedades de la estación original
            StmtIterator stmts = originalModel.listStatements(originalResource, null, (RDFNode) null);
            while (stmts.hasNext()) {
                Statement stmt = stmts.nextStatement();
                filteredModel.add(stmt);
            }
        }
        
        System.out.println("Modelo filtrado creado con " + filteredModel.size() + " triples");
        return filteredModel;
    }
    
    /**
     * Muestra las estaciones encontradas en el área
     */
    public static void printFilteredStations(List<Station> stations) {
        System.out.println("\\n=== Estaciones en el área especificada ===");
        if (stations.isEmpty()) {
            System.out.println("No se encontraron estaciones en el área especificada.");
            return;
        }
        
        for (int i = 0; i < stations.size(); i++) {
            System.out.println((i + 1) + ". " + stations.get(i));
        }
    }
    
    /**
     * Método de demostración con diferentes áreas geográficas
     */
    public static void demonstrateFiltering(Model model) {
        System.out.println("\\n=== Demostración de filtrado geográfico ===");
        
        // Área 1: Madrid y alrededores (área amplia)
        GeographicBounds madridArea = new GeographicBounds(40.0, 41.0, -4.0, -3.0);
        List<Station> madridStations = filterStationsInBounds(model, madridArea);
        System.out.println("\\n--- Estaciones en área de Madrid ---");
        printFilteredStations(madridStations);
        
        // Área 2: Centro de España (área media)
        GeographicBounds centralSpain = new GeographicBounds(39.0, 41.0, -5.0, -3.0);
        List<Station> centralStations = filterStationsInBounds(model, centralSpain);
        System.out.println("\\n--- Estaciones en centro de España ---");
        printFilteredStations(centralStations);
        
        // Área 3: Extremadura (oeste)
        GeographicBounds extremadura = new GeographicBounds(38.0, 40.0, -7.0, -5.0);
        List<Station> extremaduraStations = filterStationsInBounds(model, extremadura);
        System.out.println("\\n--- Estaciones en Extremadura ---");
        printFilteredStations(extremaduraStations);
    }
    
    /**
     * Método principal para testing
     */
    public static void main(String[] args) {
        System.out.println("=== Filtro Geográfico de Estaciones ===");
        
        // Para probar, primero necesitamos generar el modelo RDF
        RDFGenerator generator = new RDFGenerator();
        generator.processStopsFile("../google_transit/stops.txt");
        
        Model model = generator.getModel();
        
        // Demostrar el filtrado
        demonstrateFiltering(model);
        
        // Crear modelo filtrado para Madrid y guardarlo
        GeographicBounds madridBounds = new GeographicBounds(40.0, 41.0, -4.0, -3.0);
        Model filteredModel = createFilteredModel(model, madridBounds);
        
        try {
            new java.io.File("output").mkdirs();
            java.io.FileWriter writer = new java.io.FileWriter("output/estaciones_madrid.ttl");
            filteredModel.write(writer, "TURTLE");
            writer.close();
            System.out.println("\\nModelo filtrado guardado en: output/estaciones_madrid.ttl");
        } catch (java.io.IOException e) {
            System.err.println("Error guardando modelo filtrado: " + e.getMessage());
        }
    }
}
