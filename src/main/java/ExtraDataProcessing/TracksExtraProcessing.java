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
 * </ul>
 */
public class TracksExtraProcessing extends BaseExtraProcessing {
    public TracksExtraProcessing(String csvFilePath, String base) {
        super(csvFilePath, base);
    }

    @Override
    public List<Statement> processRow(CSVRecord row) {
        Model tempModel = new TreeModel();

        IRI trackIRI = Values.iri(base, "spotify:track:" + row.get("id"));
        String trackURL = "https://open.spotify.com/track/" + row.get("id");
        tempModel.add(trackIRI, spotifyURL, Values.literal(trackURL));

        IRI madeBy = Values.iri(this.base, "madeBy");
        IRI artistName = Values.iri(this.base, "artistName");

        // Adds a triple for each artist described
        List<String> artists = decodeList(row.get("artist_ids"));
        List<String> artistNames = decodeList(row.get("artists"));
        for (int i = 0; i < artists.size(); i++) {
            if (artists.size() != artistNames.size()) {
                System.out.println("Artists and artistNames lengths do not match:");
                System.out.println("Artists: " + artists);
                System.out.println("Artist names: " + artistNames);
                System.out.println("Artists original: " + row.get("artist_ids"));
                System.out.println("Artists names original: " + row.get("artists"));
                throw new RuntimeException("Artists and artistNames lengths do not match");
            }

            String artistID = artists.get(i);
            String artistNameString = artistNames.get(i);

            IRI artistIRI = Values.iri(base, "spotify:artist:" + artistID);
            String artistURL = "https://open.spotify.com/artist/" + artistID;
            // URI linking the track to its artist.
            tempModel.add(trackIRI, madeBy, artistIRI);

            // Add a URL to spotify.
            tempModel.add(artistIRI, spotifyURL, Values.literal(artistURL));

            // Give a name to the artist.
            tempModel.add(artistIRI, artistName, Values.literal(artistNameString));
        }

        return List.of(tempModel.toArray(new Statement[0]));
    }

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
