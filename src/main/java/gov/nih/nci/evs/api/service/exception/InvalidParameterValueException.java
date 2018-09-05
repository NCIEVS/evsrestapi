package gov.nih.nci.evs.api.service.exception;

import java.io.IOException;

public class InvalidParameterValueException extends IOException {

    public static final String DEFAULT_ERROR_MESSAGE = "Invalid Parameter value.";

    public InvalidParameterValueException(String message, Throwable throable) {
        super(message, throable);
    }

    public InvalidParameterValueException(String message) {
        super(message);
    }

    public InvalidParameterValueException() {
        super(DEFAULT_ERROR_MESSAGE);
    }

}