package ExtraDataProcessing;

import org.apache.commons.csv.CSVRecord;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.TreeModel;
import org.eclipse.rdf4j.model.util.Values;

import java.util.ArrayList;
import java.util.List;


public class TracksExtraProcessing extends ExtraProcessingBase {
    public TracksExtraProcessing(String csvFilePath, String base) {
        super(csvFilePath, base);
    }

    @Override
    public List<Statement> processRow(CSVRecord row) {
        Model tempModel = new TreeModel();

        IRI trackIRI = Values.iri(this.base, row.get("id"));
        IRI madeBy = Values.iri(this.base, row.get("madeBy"));

        // Add a triple giving an alternative URI to the triple in the form of spotify:track:{id}
        tempModel.add(trackIRI, owlSameAs, Values.iri("spotify:track:" + row.get("id")));

        // Adds a triple for each artist described
        List<String> artists = decodeList(row.get("artist_ids"));
        List<String> artistNames = decodeList(row.get("artists"));
        for (int i = 0; i < artists.size(); i++) {
            String artistID = artists.get(i);
            String artistName = artistNames.get(i);

            IRI artistIRI = Values.iri("https://open.spotify.com/artist/"+artistID);
            // URI linking to Spotify.
            tempModel.add(trackIRI, madeBy, artistIRI);

            // Name a second URI for the artist.
            tempModel.add(artistIRI, owlSameAs, Values.iri("spotify:artist:" + artistID));

            // Give a name to the artist.
            tempModel.add(artistIRI, Values.iri(this.base, row.get("artistName")), Values.literal(artistName));
        }

        return List.of(tempModel.toArray(new Statement[0]));
    }

}
