package ExtraDataProcessing;

import org.apache.commons.csv.*;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.util.Values;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Class that handles the additional processing that cannot be done in RML.
 */
public abstract class BaseExtraProcessing {
    /**
     * The path to the csv file.
     */
    private String csvFilePath;

    /**
     * The base URI for this ontology.
     */
    protected String base;

    // Shortcuts to predefined resources.
    /**
     * Shortcut to rdf:type.
     * Equivalent to {@literal <http://www.w3.org/1999/02/22-rdf-syntax-ns#type>}
     */
    protected final IRI a = Values.iri("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");

    /**
     * Shortcut to owl:sameAs.
     * Equivalent to {@literal <http://www.w3.org/2002/07/owl#sameAs>}
     */
    protected final IRI owlSameAs = Values.iri("http://www.w3.org/2002/07/owl#sameAs");
    /**
     * Shortcut to spoto:spotifyURL.
     * Equivalent to {@literal <https://github.com/JohnBarakl/RML-Spotify-MiniProject#spotifyURL>}
     */
    protected final IRI spotifyURL = Values.iri("https://github.com/JohnBarakl/RML-Spotify-MiniProject#spotifyURL");

    /**
     * Prepares the object by passing in essential information for the extraction.
     *
     * @param csvFilePath A path to the file to be processed.
     * @param base        The base URI for the extracted custom resources of the triplet.
     */
    public BaseExtraProcessing(String csvFilePath, String base) {
        this.csvFilePath = csvFilePath;
        this.base = base;
    }


    /**
     * The customizable part of the class that must be implemented by inheritors.
     * It consumes one row (CSVRecord) and returns a list of triplets extracted.
     *
     * @param row A row of a CSV file.
     * @return A list of triplets.
     */
    public abstract List<Statement> processRow(CSVRecord row);

    public List<Statement> processCSV() {
        // Open CSV file and start reading.
        try (Reader in = new InputStreamReader(this.getClass().getClassLoader().getResourceAsStream(this.csvFilePath))) {
            Iterable<CSVRecord> records = CSVFormat.EXCEL.builder().setHeader().setSkipHeaderRecord(true).build().parse(in);

            // For each record, extract triplets and collect them
            ArrayList<Statement> collectedStatements = new ArrayList<>();
            int i = 1;
            for (CSVRecord record : records) {
                collectedStatements.addAll(this.processRow(record));

                System.out.printf("\rProcessed %d rows from the %s file. Total triples extracted: %d", i++, this.csvFilePath, collectedStatements.size());
            }

            System.out.printf("\rFinished processing %s file. Total triples extracted: %d\n", this.csvFilePath, collectedStatements.size());

            return collectedStatements;
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Decodes a string represented Python-like list of strings into a {@literal List<String>}.
     * The input string is expected to be in a format like "['item1', 'item2', 'item3']".
     *
     * @param list The input list in string format.
     * @return A List of strings extracted from the input text.
     */
    protected static List<String> decodeList(String list) {
        // String the surrounding "[]" and whitespaces.
        String cleanedText = list.trim();
        if (cleanedText.startsWith("[") && cleanedText.endsWith("]")) {
            cleanedText = cleanedText.substring(1, cleanedText.length() - 1);
        } else {
            throw new RuntimeException("The list provided does not have the correct format (is not surrounded with \"[]\"");
        }

        // Handle cases where the list is empty (e.g., "[]", or an empty string is given).
        // After removing "[]", an empty list string will result in an empty cleanedText.
        if (cleanedText.isEmpty()) {
            return new ArrayList<>();
        }

        // Isolate the list items by splitting the string on commas, but only on commas after a "'",
        // to avoid cutting items' names in pieces.
        String[] itemsArray = cleanedText.split("(['\"]),\\s*['\"]");

        // Clean around each item.
        List<String> resultItems = new ArrayList<>();
        for (String item : itemsArray) {
            String cleanedItem = item.replaceAll("(^[ \"'\\s]+)|([ \"'\\s]+$)", "");
            resultItems.add(cleanedItem);
        }

        return resultItems;
    }


}
