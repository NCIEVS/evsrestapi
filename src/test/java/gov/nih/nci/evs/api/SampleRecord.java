
package gov.nih.nci.evs.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_EMPTY)
public class SampleRecord {

    /** The uri. */
    private String uri;

    /** The code. */
    private String code;

    /** The name. */
    private String key;

    /** The terminology. */
    private String value;

    /**
     * Instantiates an empty {@link SampleRecord}.
     */
    public SampleRecord() {
        // n/a
    }

    /**
     *
     * @param code the code
     */
    public SampleRecord(final String uri, final String code, final String key, final String value) {
        this.uri = uri;
        this.code = code;
        this.key = key;
        this.value = value;
    }

    /**
     * @return the uri
     */
    public String getUri() {
        return uri;
    }

    /**
     * @param uri the uri to set
     */
    public void setUri(String uri) {
        this.uri = uri;
    }

    /**
     * @return the code
     */
    public String getCode() {
        return code;
    }

    /**
     * @param code the code to set
     */
    public void setCode(String code) {
        this.code = code;
    }

    /**
     * @return the key
     */
    public String getKey() {
        return key;
    }

    /**
     * @param key the key to set
     */
    public void setKey(String key) {
        this.key = key;
    }

    /**
     * @return the value
     */
    public String getValue() {
        return value;
    }

    /**
     * @param value the value to set
     */
    public void setValue(String value) {
        this.value = value;
    }

}
