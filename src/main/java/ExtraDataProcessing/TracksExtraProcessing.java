package ExtraDataProcessing;

import org.apache.commons.csv.CSVRecord;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.TreeModel;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.List;


/**
 * Provides the method to extract extra information from the Tracks.csv file that couldn't be extracted using RML.
 * That info is:
 * <ul>
 *     <li> The artist(s) that have created a track. The csv file contains a Python-like list for that, so RML couldn't help. </li>
 *     <li> The names artist(s) that have created a track. The csv file contains a Python-like list for that, so RML couldn't help. </li>
 *     <li> The artists' spotify URL in the format https://open.spotify.com/artist/{id}. The csv file contains a Python-like list in the format spotify:artist:{id}, so it needs to be extracted for that, and RML couldn't help.</li>
 * </ul>
 */
public class TracksExtraProcessing extends BaseExtraProcessing {
    public TracksExtraProcessing(String csvFilePath, String base) {
        super(csvFilePath, base);
    }

    @Override
    public List<Statement> processRow(CSVRecord row) {
        // Create an empty model as a collection of triplets that will be returned at the end.
        Model tempModel = new TreeModel();

        // As the subject, create a URI of the track based on the ID present in the CSV.
        // This is the format the the rest of the CSVs use. The format is "spotify:track:{id}"
        IRI trackIRI = Values.iri(base, "spotify:track:" + row.get("id"));

        // Extract the raw ID from the URI and use it to form the URL to Spotify. Add it as a triplet.
        String trackURL = "https://open.spotify.com/track/" + row.get("id");
        tempModel.add(trackIRI, spotifyURL, Values.literal(trackURL));

        // Make shortcuts to "spoto:madeBy" and "spoto:artistName" properties of the spotify ontology.
        IRI madeBy = Values.iri(this.base, "madeBy");
        IRI artistName = Values.iri(this.base, "artistName");

        // Adds a triple for each artist described
        List<String> artists = decodeList(row.get("artist_ids"));  // Extract the artist URIs from the list-encoded column "artist_ids".
        List<String> artistNames = decodeList(row.get("artists"));  // Extract the artist names from the list-encoded column "artists".
        for (int i = 0; i < artists.size(); i++) {
            // Make sure that the number artist IRIs and names are the same to avoid errors.
            // If not, raise and error and print details to the output.
            if (artists.size() != artistNames.size()) {
                System.out.println("Artists and artistNames lengths do not match:");
                System.out.println("Artists: " + artists);
                System.out.println("Artist names: " + artistNames);
                System.out.println("Artists original: " + row.get("artist_ids"));
                System.out.println("Artists names original: " + row.get("artists"));
                throw new RuntimeException("Artists and artistNames lengths do not match");
            }

            // Get a pair of artist URI and name for the position aligned lists.
            String artistID = artists.get(i);
            String artistNameString = artistNames.get(i);

            // As the subject, create a URI of the artist based on the artist ID present in the CSV.
            // The format is "spotify:track:{id}"
            IRI artistIRI = Values.iri(base, "spotify:artist:" + artistID);
            // Use the raw ID from the URI and form the URL to Spotify.
            String artistURL = "https://open.spotify.com/artist/" + artistID;
            // URI linking the track to its artist.
            tempModel.add(trackIRI, madeBy, artistIRI);

            // Add a URL to spotify.
            tempModel.add(artistIRI, spotifyURL, Values.literal(artistURL));

            // Give a name to the artist.
            tempModel.add(artistIRI, artistName, Values.literal(artistNameString));
        }

        // Return the collection of triplets generated as a list.
        return List.of(tempModel.toArray(new Statement[0]));
    }

    /**
     * A main method used to test the validity of the extraction. Stores extracted triples in the "rdf_output_turtle.ttl" file.
     *
     * @param args
     */
    public static void main(String[] args) {
        TracksExtraProcessing tracksProcessor = new TracksExtraProcessing("raw_data/Tracks.csv", "http://example.com#");
        List<Statement> extracted_statements = tracksProcessor.processCSV();

        Model model = new TreeModel();
        model.addAll(extracted_statements);

        try (OutputStream outputStream = new FileOutputStream("rdf_output_turtle.ttl")) {
            Rio.write(model, outputStream, RDFFormat.TURTLE);
            System.out.println("RDF written to rdf_output_turtle.ttl using OutputStream.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
