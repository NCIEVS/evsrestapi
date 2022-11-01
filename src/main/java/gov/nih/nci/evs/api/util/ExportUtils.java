
package gov.nih.nci.evs.api.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.Definition;
import gov.nih.nci.evs.api.model.Property;

/**
 * Utilities for handling the "include" flag, and converting EVSConcept to
 * Concept.
 */
@Component
public final class ExportUtils {

    /** The Constant logger. */
    @SuppressWarnings("unused")
    private static final Logger log = LoggerFactory.getLogger(ConceptUtils.class);

    private static final List<String> validColumns = Arrays.asList("Code", "Preferred Name", "Synonyms", "Definitions",
            "Semantic Type",
            "Highlights");

    /**
     * Instantiates an empty {@link ConceptUtils}.
     */
    private ExportUtils() {
        // n/a
    }

    public String exportFormatter(List<Concept> concepts, List<String> columns) {
        // format all the text in here
        String toJoin = "";
        for (Concept conc : concepts) {
            for (String col : columns) {
                if (!validColumns.contains(col)) {
                    throw new ResponseStatusException(HttpStatus.EXPECTATION_FAILED,
                            "Invalid column for export: " + col);
                }
            }

            if (columns.contains("Code"))
                toJoin += conc.getCode() + "\t";
            if (columns.contains("Preferred Name"))
                toJoin += conc.getName() + "\t";

            if (columns.contains("Synonyms")) {
                String synonymString = "";
                if (conc.getSynonyms().size() > 0) {
                    synonymString += "\"";
                    // get unique synonyms
                    Set<String> uniqueSynonyms = new HashSet<>(conc.getSynonyms().size());
                    conc.getSynonyms().removeIf(p -> !uniqueSynonyms.add(p.getName()));
                    for (String name : uniqueSynonyms) {
                        synonymString += name + "\n";
                    }
                    // remove last newline
                    synonymString = synonymString.substring(0, synonymString.length() - 1) + "\"";
                }
                synonymString += "\t";
                toJoin += synonymString;
            }

            if (columns.contains("Definitions")) {
                String defString = "";
                if (conc.getDefinitions().size() > 0) {
                    defString += "\"";
                    for (Definition def : conc.getDefinitions()) {
                        defString += def.getSource() + ": " + def.getDefinition() + "\n";
                    }
                    // remove last newline
                    defString = defString.substring(0, defString.length() - 1) + "\"";
                }
                defString += "\t";
                toJoin += defString;
            }
            if (columns.contains("Semantic Type")) {
                String semString = "";
                if (conc.getProperties().size() > 0) {
                    for (Property prop : conc.getProperties()) {
                        if (prop.getType().equals("Semantic_Type")) {
                            semString += prop.getValue();
                            // only one semantic type
                            break;
                        }
                    }
                }
                toJoin += semString;
            }
            // every concept needs a newline at the end
            toJoin += "\n";

        }
        return toJoin;
    }
}
