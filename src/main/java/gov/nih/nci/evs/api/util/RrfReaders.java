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
    MRCONSO,

    /** The mrdef. */
    MRDEF,

    /** The mrcols. */
    MRCOLS,

    /** The mrdoc. */
    MRDOC,

    /** The mrmap. */
    MRMAP,

    /** The mrsmap. */
    MRSMAP,

    /** The mrrank. */
    MRRANK,

    /** The mrrel. */
    MRREL,

    /** The mrhier. */
    MRHIER,

    /** The mrsab. */
    MRSAB,

    /** The mrsat. */
    MRSAT,

    /** The mrsty. */
    MRSTY,

    /** The mrcui. */
    MRCUI,

    /** The mraui. */
    MRAUI,

    /** The srdef. */
    SRDEF;

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
    // n/a
  }

  /**
   * Open original readers.
   *
   * @param prefix the prefix (e.g. MR or RXN)
   * @throws Exception the exception
   */
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
  @SuppressWarnings("resource")
  private PushBackReader getReader(final String filename) throws Exception {
    final File file = new File(inputDir, filename);
    if (file != null && file.exists()) {
      return new PushBackReader(
          new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8")));
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
