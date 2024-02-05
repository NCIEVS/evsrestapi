
package gov.nih.nci.evs.api.controller;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import gov.nih.nci.evs.api.ConceptSampleTester;
import gov.nih.nci.evs.api.SampleRecord;
import gov.nih.nci.evs.api.properties.ApplicationProperties;
import gov.nih.nci.evs.api.util.TerminologyUtils;

/**
 * Superclass for the terminology sample tests.
 */
@Ignore
public class SampleTest {

  @Autowired
  ApplicationProperties applicationProperties;

  /** The mvc. */
  @Autowired
  private MockMvc mvc;

  /** The samples. */
  private static Map<String, List<SampleRecord>> samples;

  /** The test properties. */
  ConceptSampleTester conceptSampleTester = null;

  /** The terminology. */
  private static String terminology;

  /** Constructor */
  @Autowired
  public void setTermUtils(TerminologyUtils termUtils) {
    conceptSampleTester = new ConceptSampleTester(termUtils);
  }

  /**
   * Returns the samples.
   *
   * @param terminology the terminology
   * @param sampleFile the sample file
   * @return the samples
   * @throws Exception the exception
   */
  public static void loadSamples(final String terminology, final String sampleFile)
    throws Exception {

    samples = new HashMap<>();
    SampleTest.terminology = terminology;
    // load tab separated txt file as resource and load into samples
    try (BufferedReader fileReader = new BufferedReader(new FileReader(sampleFile));) {
      String line;
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
    }
  }

  /**
   * Test metadata.
   *
   * @throws Exception the exception
   */
  @Test
  public void testMetadata() throws Exception {
    conceptSampleTester.setLicenseKey(applicationProperties.getUiLicense());
    conceptSampleTester.performMetadataTests(terminology, samples, mvc);
  }

  /**
   * Test content.
   *
   * @throws Exception the exception
   */
  @Test
  public void testContent() throws Exception {
    conceptSampleTester.setLicenseKey(applicationProperties.getUiLicense());
    conceptSampleTester.performContentTests(terminology, samples, mvc);
  }

  /**
   * Test paths subtree and roots.
   *
   * @throws Exception the exception
   */
  @Test
  public void testPathsSubtreeAndRoots() throws Exception {
    conceptSampleTester.setLicenseKey(applicationProperties.getUiLicense());
    conceptSampleTester.performPathsSubtreeAndRootsTests(terminology, samples, mvc);
  }

  /**
   * Test search.
   *
   * @throws Exception the exception
   */
  @Test
  public void testSearch() throws Exception {
    conceptSampleTester.setLicenseKey(applicationProperties.getUiLicense());
    conceptSampleTester.performSearchTests(terminology, samples, mvc);
  }

  /**
   * Test subsets.
   *
   * @throws Exception the exception
   */
  @Test
  public void testSubsets() throws Exception {
    conceptSampleTester.setLicenseKey(applicationProperties.getUiLicense());
    conceptSampleTester.performSubsetsTests(terminology, samples, mvc);
  }

  /**
   * Test association entries.
   *
   * @throws Exception the exception
   */
  @Test
  public void testAssociationEntries() throws Exception {
    conceptSampleTester.setLicenseKey(applicationProperties.getUiLicense());
    conceptSampleTester.performAssociationEntryTests(terminology, samples, mvc);
  }

}