
package gov.nih.nci.evs.api.controller;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import gov.nih.nci.evs.api.ConceptSampleTester;
import gov.nih.nci.evs.api.SampleRecord;
import gov.nih.nci.evs.api.model.Terminology;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class GoSampleTest {

    private static Map<String, List<SampleRecord>> samples;

    /** The test properties. */
    ConceptSampleTester conceptSampleTester = new ConceptSampleTester();

    /** The mvc. */
    @Autowired
    private MockMvc mvc;

    /** The logger. */
    private static final Logger log = LoggerFactory.getLogger(GoSampleTest.class);

    private static String goFile = "src/test/resources/go_Sampling_OWL.txt";

    @BeforeClass
    public static void setupClass() throws IOException {
        // load tab separated txt file as resource and load into samples
        try {
            BufferedReader fileReader = new BufferedReader(new FileReader(goFile));
            String line = "";
            samples = new HashMap<>();
            while ((line = fileReader.readLine()) != null) {
                String[] parts = line.split("\t");
                if (parts.length >= 3) {
                    SampleRecord record = new SampleRecord();
                    record.setUri(parts[0]);
                    record.setCode(parts[1]);
                    record.setKey(parts[2]);
                    if (parts.length > 3)
                        record.setValue(parts[3]);
                    if (samples.containsKey(parts[1])) {
                        List<SampleRecord> sampleList = samples.get(parts[1]);
                        sampleList.add(record);
                        samples.replace(parts[1], sampleList);
                    } else {
                        List<SampleRecord> sampleList = new ArrayList<SampleRecord>();
                        sampleList.add(record);
                        samples.put(parts[1], sampleList);
                    }
                }
            }
            fileReader.close();
        } catch (Exception e) {
            log.error(e.getMessage());
            e.printStackTrace();
        }
    }

    @Test
    public void testMetadata() throws Exception {
        this.conceptSampleTester.performMetadataTests(new Terminology(), samples, mvc);
    }

    @Test
    public void testContent() throws Exception {
        this.conceptSampleTester.performContentTests(new Terminology(), samples, mvc);
    }

    @Test
    public void testPathsSubtreeAndRoots() {
        // do test
    }

    @Test
    public void testSearch() {
        // do test
    }

    @Test
    public void testSubsets() {
        // do test
    }

    @Test
    public void testAssociationEntries() {
        // do test
    }
}