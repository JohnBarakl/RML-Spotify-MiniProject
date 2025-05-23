import ExtraDataProcessing.ArtistsExtraProcessing;
import ExtraDataProcessing.PlaylistTracksExtraProcessing;
import ExtraDataProcessing.PlaylistsExtraProcessing;
import ExtraDataProcessing.TracksExtraProcessing;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.http.HTTPRepository;
import org.eclipse.rdf4j.rio.RDFFormat;

import java.io.*;
import java.util.HashMap;
import java.util.Map;


public class Main {
    /**
     * The repository of GraphDB to connect to.
     */
    private HTTPRepository repository;

    /**
     * The connection to a live GraphDB instance.
     */
    private RepositoryConnection graphDBConnection;


    /**
     * Prepares the connection to GraphDB.
     *
     * @param repositoryName The name of the repository to connect to.
     */
    public void setup(String repositoryName) {
        // Initialize connection to GraphDB.
        repository = new HTTPRepository("http://localhost:7200/repositories/" + repositoryName);
        graphDBConnection = repository.getConnection();

        // Ensure that the repository is empty at the beginning.
        graphDBConnection.clear();
    }

    /**
     * Closes the connection to GraphDB.
     */
    public void teardown() {
        graphDBConnection.close();
        repository.shutDown();
    }

    public static void main(String[] args) throws IOException {
        Main main = new Main();

        // Initialize the connection to GraphDB and prepare the repository.
        String repositoryName = "MiniProject";  // The repository's name is "MiniProject" as described by the requirements.
        main.setup(repositoryName);

        // Add the base ontology triples into the database.
        main.addTtlFileToGraphDB(main.getClass().getClassLoader().getResourceAsStream("ontology/SpotifyOntology.ttl"));

//        // For all data files, map triplets out and add to the database.
//        main.processWithRML("Artists.mappings", "raw_data");
//        System.out.println("Done Artists.mappings");
//        main.processWithRML("Playlists.mappings", "raw_data");
//        System.out.println("Done Playlists.mappings");
//        main.processWithRML("Playlists_Tracks.mappings", "raw_data");
//        System.out.println("Done Playlists_Tracks.mappings");
//        main.processWithRML("Tracks.mappings", "raw_data");
//        System.out.println("Done Tracks.mappings");
//        main.processWithRML("Users.mappings", "raw_data");
//        System.out.println("Done Users.mappings");

        // Proceed with the extra triple extraction that could not be completed by RML alone.
        (new ArtistsExtraProcessing("raw_data/Artists.csv", "https://github.com/JohnBarakl/RML-Spotify-MiniProject#")).
                processCSVToTtlFile("ArtistsOutputTriplesExtra.ttl");
        main.addTtlFileToGraphDB("raw_data/ArtistsOutputTriplesExtra.ttl");
        System.out.println("Done ArtistsOutputTriplesExtra.ttl");

        (new PlaylistsExtraProcessing("raw_data/Playlists.csv", "https://github.com/JohnBarakl/RML-Spotify-MiniProject#")).
                processCSVToTtlFile("PlaylistsOutputTriplesExtra.ttl");
        main.addTtlFileToGraphDB("raw_data/PlaylistsOutputTriplesExtra.ttl");
        System.out.println("Done PlaylistsOutputTriplesExtra.ttl");

        (new PlaylistTracksExtraProcessing("raw_data/Playlists_Tracks.csv", "https://github.com/JohnBarakl/RML-Spotify-MiniProject#")).
                processCSVToTtlFile("Playlists_TracksOutputTriplesExtra.ttl");
        main.addTtlFileToGraphDB("raw_data/Playlists_TracksOutputTriplesExtra.ttl");
        System.out.println("Done PlaylistsTracksOutputTriplesExtra.ttl");

        (new TracksExtraProcessing("raw_data/Tracks.csv", "https://github.com/JohnBarakl/RML-Spotify-MiniProject#")).
                processCSVToTtlFile("TracksOutputTriplesExtra.ttl");
        main.addTtlFileToGraphDB("raw_data/TracksOutputTriplesExtra.ttl");
        System.out.println("Done TracksOutputTriplesExtra.ttl");


        System.out.println("Completed Uploads");

        main.teardown();
    }

    /**
     * Adds at .ttl file to the connected GraphDB database.
     *
     * @param ttlFilePath A path to the file to be loaded.
     */
    private void addTtlFileToGraphDB(String ttlFilePath) throws IOException {
        try (FileInputStream fis = new FileInputStream(ttlFilePath)) {
            addTtlFileToGraphDB(fis);
        }
    }

    /**
     * Adds at .ttl file to the connected GraphDB database.
     *
     * @param inputStream Direct stream to the file to be loaded.
     */
    private void addTtlFileToGraphDB(InputStream inputStream) throws IOException {
        graphDBConnection.begin();
        graphDBConnection.add(inputStream,
                "https://github.com/JohnBarakl/RML-Spotify-MiniProject#",
                RDFFormat.TURTLE);

        // Committing the transaction persists the data
        graphDBConnection.commit();
    }

    /**
     * Extracts triples using a defined RML mapping template and places all the created RML files to a connected GraphDB database.
     *
     * @param templateName      The name of the template to use. Can be one of the templates in src/main/resources/mappings.
     * @param dataDirectoryPath The path to the directory that contains the raw data to extract from. The results will also be stored there.
     */
    private void processWithRML(String templateName, String dataDirectoryPath) throws IOException {
        RMLMapper rmlMapper = new RMLMapper();

        // Extract triplets to file using RML. The return value is the file to be added in the database.
        String ttlFilePath = rmlMapper.extractTriples(templateName, dataDirectoryPath);
        addTtlFileToGraphDB(ttlFilePath);
    }
}