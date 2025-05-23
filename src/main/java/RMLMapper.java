import be.ugent.rml.Executor;
import be.ugent.rml.Utils;
import be.ugent.rml.functions.FunctionLoader;
import be.ugent.rml.functions.lib.IDLabFunctions;
import be.ugent.rml.records.RecordsFactory;
import be.ugent.rml.store.QuadStore;
import be.ugent.rml.store.QuadStoreFactory;
import be.ugent.rml.store.RDF4JStore;
import be.ugent.rml.term.NamedNode;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * The class that provides a method to use an RML map and a source dataset to extract triples.
 * Adapted from https://github.com/csd-auth-kgoe/rmlmapper-example/blob/472f888733723fb52c1cadc2576006aaa00b5934/src/main/java/Example1.java.
 */
public class RMLMapper {
    /**
     * Extracts triples using a defined RML mapping template.
     * @param templateName The name of the template to use. Can be one of the templates in src/main/resources/mappings.
     * @param dataDirectoryPath The path to the directory that contains the raw data to extract from. The results will also be stored there.
     * @return A path to the file containing the extracted triples.
     */
    public String extractTriples(String templateName, String dataDirectoryPath) {
        try {
            // Get the mapping string stream
            if (templateName.endsWith(".ttl")){
                templateName = templateName.substring(0, templateName.length()-4);
            }
            InputStream mappingStream = this.getClass().getClassLoader().getResourceAsStream("mappings/" + templateName + ".ttl");

            // Load the mapping in a QuadStore
            QuadStore rmlStore = QuadStoreFactory.read(mappingStream);

            // Set up the basepath for the records factory, i.e., the basepath for the (local file) data sources
            RecordsFactory factory = new RecordsFactory(dataDirectoryPath);

            // Set up the functions used during the mapping
            Map<String, Class> libraryMap = new HashMap<>();
            libraryMap.put("IDLabFunctions", IDLabFunctions.class);

            FunctionLoader functionLoader = new FunctionLoader(null, libraryMap);

            // Set up the outputstore (needed when you want to output something else than nquads
            QuadStore outputStore = new RDF4JStore();

            // Create the Executor
            Executor executor = new Executor(rmlStore, factory, functionLoader, outputStore, Utils.getBaseDirectiveTurtle(mappingStream));

            // Execute the mapping
            QuadStore result = executor.executeV5(null).get(new NamedNode("rmlmapper://default.store"));

            // Output the results in a file
            String outPath = dataDirectoryPath + "/" + templateName + "OutputTriples.ttl";
            Writer output = new FileWriter(outPath);
            result.write(output, "turtle");
            output.close();

            return outPath;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        RMLMapper mapper = new RMLMapper();

        mapper.extractTriples("Tracks.mappings", "raw_data");
    }
}