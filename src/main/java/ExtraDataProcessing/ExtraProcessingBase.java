package ExtraDataProcessing;

import org.apache.commons.csv.*;
import org.eclipse.rdf4j.model.Statement;

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

    public ExtraProcessingBase(String csvFilePath) {
        this.csvFilePath = csvFilePath;
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
}
