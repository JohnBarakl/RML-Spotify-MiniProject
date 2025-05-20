package ExtraDataProcessing;

import org.apache.commons.csv.*;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.util.Values;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

/**
 * Class that handles the additional processing that cannot be done in RML.
 */
public abstract class ExtraProcessingBase {
    /**
     * The path to the csv file.
     */
    private String csvFilePath;

    protected String base;

    protected final IRI a = Values.iri("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
    protected final IRI owlSameAs = Values.iri("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");

    public ExtraProcessingBase(String csvFilePath, String base) {
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
        try (Reader in = new FileReader(this.csvFilePath)) {
            Iterable<CSVRecord> records = CSVFormat.EXCEL.parse(in);

            // For each record, extract triplets and collect them
            ArrayList<Statement> collectedStatements = new ArrayList<>();
            for (CSVRecord record : records) {
                collectedStatements.addAll(this.processRow(record));
            }

            return collectedStatements;
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Decodes a string representation of a Python-like list of strings into a List<String>.
     * The input string is expected to be in a format like "['item1', 'item2', 'item3']".
     *
     * @param listInText The input string representing the list.
     * @return A List of strings decoded from the input text.
     */
    public static List<String> decodeList(String listInText) {
        // Step 1: Remove leading/trailing whitespace and the enclosing "[]".
        String cleanedText = listInText.trim();
        if (cleanedText.startsWith("[") && cleanedText.endsWith("]")) {
            cleanedText = cleanedText.substring(1, cleanedText.length() - 1);
        }

        // Handle cases where the list is empty (e.g., "[]", "   []   ", or an empty string).
        // After removing "[]", an empty list string will result in an empty cleanedText.
        if (cleanedText.isEmpty()) {
            return new ArrayList<>();
        }

        // Step 2: Isolate the list items by splitting the string.
        String[] itemsArray = cleanedText.split("',\\s*");

        // Step 3: Clean around each item.
        List<String> resultItems = new ArrayList<>();
        for (String item : itemsArray) {
            String cleanedItem = item.replaceAll("^[ '\\s]+|[ '\\s]+$", "");
            resultItems.add(cleanedItem);
        }

        return resultItems;
    }


}
