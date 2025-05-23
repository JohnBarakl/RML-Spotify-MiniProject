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
 * Provides the method to extract extra information from the Playlist_Tracks.csv file that couldn't be extracted using RML.
 * That info is:
 * <ul>
 *     <li> The track's spotify URL in the format https://open.spotify.com/track/{id}. The csv file contains it in the format spotify:track:{id}, so it needs to be extracted and RML cannot help with that. </li>
 *     <li> The playlists where this track is a part of. The csv file contains a Python-like list for that, so RML couldn't help.</li>
 *     <li> The playlists' spotify URL in the format https://open.spotify.com/playlist/{id}. The csv file contains a Python-like list in the format spotify:playlist:{id}, so it needs to be extracted for that, and RML couldn't help.</li>
 *     <li> The artist(s) that have created a track. The csv file contains a Python-like list for that, so RML couldn't help. </li>
 *     <li> The names artist(s) that have created a track. The csv file contains a Python-like list for that, so RML couldn't help. </li>
 *     <li> The artists' spotify URL in the format https://open.spotify.com/artist/{id}. The csv file contains a Python-like list in the format spotify:artist:{id}, so it needs to be extracted for that, and RML couldn't help.</li>
 * </ul>
 */
public class PlaylistTracksExtraProcessing extends BaseExtraProcessing {
    public PlaylistTracksExtraProcessing(String csvFilePath, String base) {
        super(csvFilePath, base);
    }

    @Override
    public List<Statement> processRow(CSVRecord row) {
        // Create an empty model as a collection of triplets that will be returned at the end.
        Model tempModel = new TreeModel();

        // As the subject, use the URI of the track as present in the CSV. It has the format of "spotify:track:{id}"
        IRI trackIRI = Values.iri(base, row.get("track_uri"));

        // Extract the raw ID from the URI and use it to form the URL to Spotify. Add it as a triplet.
        String trackURL = "https://open.spotify.com/track/" + row.get("track_uri").substring(14);
        tempModel.add(trackIRI, spotifyURL, Values.literal(trackURL));

        // Make shortcuts to "spoto:madeBy", "spoto:artistName", and "spoto:isPartOf" properties of the spotify ontology.
        IRI madeBy = Values.iri(this.base, "madeBy");
        IRI artistName = Values.iri(this.base, "artistName");
        IRI isPartOf = Values.iri(this.base, "isPartOf");

        // Adds a data relevant to each artist described
        List<String> artists = decodeList(row.get("artists_uris"));  // Extract the artist URIs from the list-encoded column "artists_uris".
        List<String> artistNames = decodeList(row.get("artists_names"));  // Extract the artist names from the list-encoded column "artists_names".
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

            // Use the URI of the artist as present in the CSV. It has the format of "spotify:artist:{id}"
            IRI artistIRI = Values.iri(base, artistID);

            // URI linking the track to its artist.
            tempModel.add(trackIRI, madeBy, artistIRI);

            // Extract the raw ID from the URI and use it to form the URL to Spotify. Add it as a triplet.
            String artistURL = "https://open.spotify.com/artist/" + artistID.substring(15);

            // Add a URL to spotify.
            tempModel.add(artistIRI, spotifyURL, Values.literal(artistURL));

            // Give a name to the artist.
            tempModel.add(artistIRI, artistName, Values.literal(artistNameString));
        }

        // Adds data for each playlist.
        // Extract the playlist URIs from the list-encoded column "playlist_uris".
        List<String> playlists = decodeList(row.get("playlist_uris"));
        for (int i = 0; i < playlists.size(); i++) {
            String playlistURI = playlists.get(i);

            // Avoid problems caused by the row containing a list with empty elements (['']).
            if (playlistURI.isEmpty()) {
                continue;
            }

            // Use the URI of the playlist as present in the CSV. It has the format of "spotify:playlist:{id}"
            IRI playlistIRI = Values.iri(base, playlistURI);
            // Extract the raw ID from the URI and use it to form the URL to Spotify.
            String playlistURL = "https://open.spotify.com/playlist/" + playlistURI.substring(17);

            // Link track to playlist.
            tempModel.add(trackIRI, isPartOf, playlistIRI);

            // URL linking to Spotify.
            tempModel.add(playlistIRI, spotifyURL, Values.literal(playlistURL));
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
        PlaylistTracksExtraProcessing csvProcessor = new PlaylistTracksExtraProcessing("raw_data/Playlists_Tracks.csv", "http://example.com#");
        List<Statement> extracted_statements = csvProcessor.processCSV();

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
