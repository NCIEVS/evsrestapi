
package gov.nih.nci.evs.api.controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import gov.nih.nci.evs.api.ConceptSampleTester;
import gov.nih.nci.evs.api.SampleRecord;
import gov.nih.nci.evs.api.model.Terminology;

public class NcitSampleTest {

    private static Map<String, List<SampleRecord>> samples;

    /** The test properties. */
    @Autowired
    ConceptSampleTester conceptSampleTester;

    @BeforeClass
    public static void setupClass() throws IOException {
        // load tab separated txt file as resource and load into samples
        ClassLoader classLoader = NcitSampleTest.class.getClassLoader();
        InputStream resourceStream = classLoader.getResourceAsStream("sample.txt");
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(resourceStream));
        String line;
        samples = new HashMap<>();
        while ((line = bufferedReader.readLine()) != null) {
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
    }

    @Test
    public void testMetadata() throws Exception {
        this.conceptSampleTester.performMetadataTests(new Terminology(), samples);
    }

    @Test
    public void testContent() throws Exception {
        this.conceptSampleTester.performContentTests(new Terminology(), samples);
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