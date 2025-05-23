package ExtraDataProcessing;

import org.apache.commons.csv.CSVRecord;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.TreeModel;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.List;


/**
 * Provides the method to extract extra information from the Artists.csv file that couldn't be extracted using RML.
 * That info is:
 * <ul>
 *     <li> The artist's spotify URL in the format https://open.spotify.com/artist/{id}. The csv file contains it in the format spotify:artist:{id}, so it needs to be extracted and RML cannot help with that. </li>
 * </ul>
 */
public class ArtistsExtraProcessing extends BaseExtraProcessing {
    public ArtistsExtraProcessing(String csvFilePath, String base) {
        super(csvFilePath, base);
    }

    @Override
    public List<Statement> processRow(CSVRecord row) {
        // As the subject, use the URI of the artist as present in the CSV. It has the format of "spotify:artist:{id}"
        IRI artistIRI = Values.iri(this.base, row.get("artist_uri"));

        // Extract the raw ID from the URI and use it to form the URL to Spotify.
        String artistURL = "https://open.spotify.com/track/"+ row.get("artist_uri").substring(15);

        // Create a temporary model and put the triplet into it
        Model tempModel = new TreeModel();
        tempModel.add(artistIRI, spotifyURL, Values.literal(artistURL));

        // Return the collection of triplets generated as a list.
        return List.of(tempModel.toArray(new Statement[0]));
    }

    /**
     * A main method used to test the validity of the extraction. Stores extracted triples in the "rdf_output_turtle.ttl" file.
     * @param args
     */
    public static void main(String[] args) {
        ArtistsExtraProcessing artistsProcessor = new ArtistsExtraProcessing("raw_data/artists.csv", "http://example.com#");
        List<Statement> extracted_statements = artistsProcessor.processCSV();

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
