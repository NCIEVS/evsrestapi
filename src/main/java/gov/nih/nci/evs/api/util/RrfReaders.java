
package gov.nih.nci.evs.api.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

/**
 * Container for RF2 readers.
 */
public class RrfReaders {

  /** The sorted rf2 dir. */
  private File inputDir;

  /** The readers. */
  private Map<Keys, PushBackReader> readers = new HashMap<>();

  /**
   * The Enum Keys.
   */
  public enum Keys {

    /** The mrconso. */
    MRCONSO(new int[] {
        0
    }, 11),

    /** The mrdef. */
    MRDEF(new int[] {
        0
    }, 4),

    /** The mrdoc. */
    MRDOC(null, -1),

    /** The mrmap. */
    MRMAP(new int[] {
        0
    }, 1),

    /** The mrrank. */
    MRRANK(null, 1),

    /** The mrrel. */
    MRREL(new int[] {
        0, 4
    }, 10),

    /** The mrhier. */
    MRHIER(new int[] {
        0
    }, 4),

    /** The mrsab. */
    MRSAB(null, 3),

    /** The mrsat. */
    MRSAT(new int[] {
        0
    }, 9),

    /** The mrsty. */
    MRSTY(new int[] {
        0
    }, -1),

    /** The mrcui. */
    MRCUI(new int[] {
        5
    }, -1),

    /** The mraui. */
    MRAUI(null, -1),

    /** The srdef. */
    SRDEF(null, -1),

    /** The mrcols. */
    MRCOLS(null, -1),

    /** The mrfiles. */
    MRFILES(null, -1);

    /** The sab field. */
    private int sabField;

    /** The cui fields. */
    private int[] cuiFields;

    /**
     * Instantiates a new keys.
     *
     * @param cuiFields the cui fields
     * @param sabField the sab field
     */
    private Keys(int[] cuiFields, int sabField) {
      this.cuiFields = cuiFields;
      this.sabField = sabField;
    }

    /**
     * Gets the cui fields.
     *
     * @return the cui fields
     */
    public int[] getCuiFields() {
      return cuiFields;
    }

    /**
     * Gets the sab field.
     *
     * @return the sab field
     */
    public int getSabField() {
      return sabField;
    }
  }

  /**
   * Instantiates an empty {@link RrfReaders}.
   *
   * @param inputDir the input dir
   * @throws Exception if anything goes wrong
   */
  public RrfReaders(final File inputDir) throws Exception {
    this.inputDir = inputDir;
  }

  /**
   * Open readers.
   *
   * @throws Exception the exception
   */
  public void openReaders() throws Exception {

    // N/A - sorting is assumed

    // readers.put(Keys.MRCONSO, getReader("consoByConcept.sort"));
    // readers.put(Keys.MRDEF, getReader("defByConcept.sort"));
    // readers.put(Keys.MRDOC, getReader("docByKey.sort"));
    // readers.put(Keys.MRMAP, getReader("mapByConcept.sort"));
    // readers.put(Keys.MRRANK, getReader("rankByRank.sort"));
    // readers.put(Keys.MRREL, getReader("relByConcept.sort"));
    // readers.put(Keys.MRHIER, getReader("relByConcept.sort"));
    // readers.put(Keys.MRSAB, getReader("sabBySab.sort"));
    // readers.put(Keys.MRSAT, getReader("satByConcept.sort"));
    // readers.put(Keys.MRSTY, getReader("styByConcept.sort"));
    // readers.put(Keys.SRDEF, getReader("srdef.sort"));
    // readers.put(Keys.MRCUI, getReader("cuiHistory.sort"));
    // readers.put(Keys.MRAUI, getReader("auiHistory.sort"));

  }

  /**
   * Open original readers.
   *
   * @param prefix the prefix (e.g. MR or RXN)
   * @throws Exception the exception
   */
  @SuppressWarnings("resource")
  public void openOriginalReaders(final String prefix) throws Exception {

    for (final Keys key : Keys.values()) {
      readers.put(key, getReader(key.toString().replace("MR", prefix) + ".RRF"));
    }
    readers.put(Keys.SRDEF, getReader("SRDEF"));
  }

  /**
   * Close readers.
   *
   * @throws Exception the exception
   */
  public void closeReaders() throws Exception {
    for (final BufferedReader reader : readers.values()) {
      try {
        reader.close();
      } catch (final Exception e) {
        // do nothing;
      }
    }
  }

  /**
   * Returns the reader.
   *
   * @param filename the filename
   * @return the reader
   * @throws Exception the exception
   */
  private PushBackReader getReader(final String filename) throws Exception {
    final File file = new File(inputDir, filename);
    if (file.exists()) {
      return new PushBackReader(new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8")));
    } else {
      // if no file, return an empty stream
      return new PushBackReader(new StringReader(""));
    }
  }

  /**
   * Returns the reader.
   *
   * @param key the key
   * @return the reader
   */
  public PushBackReader getReader(final Keys key) {
    return readers.get(key);
  }

}
