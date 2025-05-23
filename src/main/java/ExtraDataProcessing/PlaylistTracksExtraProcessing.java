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
 *     <li> The artist(s) that have created a track. The csv file contains a Python-like list for that, so RML couldn't help. </li>
 *     <li> The names artist(s) that have created a track. The csv file contains a Python-like list for that, so RML couldn't help. </li>
 * </ul>
 */
public class PlaylistTracksExtraProcessing extends BaseExtraProcessing {
    public PlaylistTracksExtraProcessing(String csvFilePath, String base) {
        super(csvFilePath, base);
    }

    @Override
    public List<Statement> processRow(CSVRecord row) {
        Model tempModel = new TreeModel();

        IRI trackIRI = Values.iri(base, row.get("track_uri"));
        String trackURL = "https://open.spotify.com/track/" + row.get("track_uri").substring(14);
        tempModel.add(trackIRI, spotifyURL, Values.literal(trackURL));

        IRI madeBy = Values.iri(this.base, "madeBy");
        IRI artistName = Values.iri(this.base, "artistName");

        // Adds a data relevant to each artist described
        List<String> artists = decodeList(row.get("artists_uris"));
        List<String> artistNames = decodeList(row.get("artists_names"));
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

            IRI artistIRI = Values.iri(base, artistID);
            String artistURL = "https://open.spotify.com/artist/" + artistID.substring(15);
            // URI linking the track to its artist.
            tempModel.add(trackIRI, madeBy, artistIRI);

            // Add a URL to spotify.
            tempModel.add(artistIRI, spotifyURL, Values.literal(artistURL));

            // Give a name to the artist.
            tempModel.add(artistIRI, artistName, Values.literal(artistNameString));
        }

        // Adds data for each playlist.
        IRI isPartOf = Values.iri(this.base, "isPartOf");
        List<String> playlists = decodeList(row.get("playlist_uris"));
        // Surround by if as a track may not be in a playlist.
        for (int i = 0; i < playlists.size(); i++) {
            String playlistURI = playlists.get(i);

            if (playlistURI.isEmpty()){
                continue;
            }

            IRI playlistIRI = Values.iri(base, playlistURI);
            String playlistURL = "https://open.spotify.com/playlist/" + playlistURI.substring(17);

            // Link to track
            tempModel.add(trackIRI, isPartOf, playlistIRI);

            // URI linking to Spotify.
            tempModel.add(playlistIRI, spotifyURL, Values.literal(playlistURL));
        }

        return List.of(tempModel.toArray(new Statement[0]));
    }

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
