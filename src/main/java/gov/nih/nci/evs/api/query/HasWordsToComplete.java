package gov.nih.nci.evs.api.query;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Requires typed text with at least one word to complete, by the rule in {@link
 * Typeahead#getHasWordsToComplete(String)}. Null, blank, and punctuation-only text is invalid.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = HasWordsToComplete.Validator.class)
public @interface HasWordsToComplete {

  /**
   * Returns the message when the text has no words.
   *
   * @return the message
   */
  String message() default "must have at least one word to complete";

  /**
   * Returns the validation groups.
   *
   * @return the groups
   */
  Class<?>[] groups() default {};

  /**
   * Returns the payload.
   *
   * @return the payload
   */
  Class<? extends Payload>[] payload() default {};

  /** Checks text with {@link Typeahead#getHasWordsToComplete(String)}. */
  class Validator implements ConstraintValidator<HasWordsToComplete, String> {

    /* see superclass */
    @Override
    public boolean isValid(final String term, final ConstraintValidatorContext context) {
      return Typeahead.getHasWordsToComplete(term);
    }
  }
}
