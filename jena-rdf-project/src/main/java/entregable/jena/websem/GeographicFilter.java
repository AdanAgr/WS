package entregable.jena.websem;

import java.util.ArrayList;
import java.util.List;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;

public class GeographicFilter {
    
    private static final String GEO_NS = "http://www.w3.org/2003/01/geo/wgs84_pos#";
    private static final Property GEO_LAT = ResourceFactory.createProperty(GEO_NS + "lat");
    private static final Property GEO_LONG = ResourceFactory.createProperty(GEO_NS + "long");
    private static final Resource GEO_SPATIAL_THING = ResourceFactory.createResource(GEO_NS + "SpatialThing");
    
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
        
        public double getMinLat() { return minLat; }
        public double getMaxLat() { return maxLat; }
        public double getMinLon() { return minLon; }
        public double getMaxLon() { return maxLon; }
    }
    
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
        
        public Resource getResource() { return resource; }
        public String getName() { return name; }
        public double getLatitude() { return latitude; }
        public double getLongitude() { return longitude; }
    }
    
    public static List<Station> filterStationsInBounds(Model model, GeographicBounds bounds) {
        List<Station> stationsInBounds = new ArrayList<>();
        
        System.out.println("Filtrando estaciones en el área: " + bounds);
        
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
    
    private static Station extractStationData(Resource stationResource) {
        try {
            Statement nameStmt = stationResource.getProperty(ResourceFactory.createProperty("http://www.w3.org/2000/01/rdf-schema#label"));
            String name = nameStmt != null ? nameStmt.getString() : "Sin nombre";
            
            Statement latStmt = stationResource.getProperty(GEO_LAT);
            if (latStmt == null) {
                throw new IllegalStateException("Falta propiedad geo:lat");
            }
            double latitude = latStmt.getDouble();
            
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
    
    public static Model createFilteredModel(Model originalModel, GeographicBounds bounds) {
        Model filteredModel = ModelFactory.createDefaultModel();
        filteredModel.setNsPrefixes(originalModel.getNsPrefixMap());
        
        List<Station> filteredStations = filterStationsInBounds(originalModel, bounds);
        
        for (Station station : filteredStations) {
            Resource originalResource = station.getResource();
            StmtIterator stmts = originalModel.listStatements(originalResource, null, (RDFNode) null);
            while (stmts.hasNext()) {
                Statement stmt = stmts.nextStatement();
                filteredModel.add(stmt);
            }
        }
        
        System.out.println("Modelo filtrado creado con " + filteredModel.size() + " triples");
        return filteredModel;
    }
    
    public static void printFilteredStations(List<Station> stations) {
        System.out.println("\\nEstaciones en el área especificada:");
        if (stations.isEmpty()) {
            System.out.println("No se encontraron estaciones en el área especificada.");
            return;
        }
        
        for (int i = 0; i < stations.size(); i++) {
            System.out.println((i + 1) + ". " + stations.get(i));
        }
    }
    
    public static void demonstrateFiltering(Model model) {
        System.out.println("\\nDemostración de filtrado geográfico:");
        
        GeographicBounds madridArea = new GeographicBounds(40.0, 41.0, -4.0, -3.0);
        List<Station> madridStations = filterStationsInBounds(model, madridArea);
        System.out.println("\\n--- Estaciones en área de Madrid ---");
        printFilteredStations(madridStations);
        
        GeographicBounds centralSpain = new GeographicBounds(39.0, 41.0, -5.0, -3.0);
        List<Station> centralStations = filterStationsInBounds(model, centralSpain);
        System.out.println("\\n--- Estaciones en centro de España ---");
        printFilteredStations(centralStations);
        
        GeographicBounds extremadura = new GeographicBounds(38.0, 40.0, -7.0, -5.0);
        List<Station> extremaduraStations = filterStationsInBounds(model, extremadura);
        System.out.println("\\n--- Estaciones en Extremadura ---");
        printFilteredStations(extremaduraStations);

        GeographicBounds catalonia = new GeographicBounds(40.0, 42.0, 0.0, 3.0);
        List<Station> cataloniaStations = filterStationsInBounds(model, catalonia);
        System.out.println("\\n--- Estaciones en Cataluña ---");
        printFilteredStations(cataloniaStations);

    }
    
    public static void main(String[] args) {
        System.out.println("Filtro Geográfico de Estaciones:");
        
        RDFGenerator generator = new RDFGenerator();
        generator.processStopsFile("../google_transit/stops.txt");
        
        Model model = generator.getModel();
        
        demonstrateFiltering(model);
        
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
