package gov.nih.nci.evs.api.configuration;

import gov.nih.nci.evs.api.model.Include;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/**
 * Converts one value of the {@code include} request parameter, as a client writes it (e.g. {@code
 * inverseRoles}), to an {@link Include}. Spring splits the parameter on commas and converts each
 * value, so a controller can take the parameter as a {@code Set<Include>}. Spring's own enum
 * conversion would need the constant's name ({@code INVERSE_ROLES}).
 */
@Component
public class IncludeConverter implements Converter<String, Include> {

  /* see superclass */
  @Override
  public Include convert(final String parameterValue) {
    final String value = parameterValue.trim();

    // An empty value, e.g. between two commas, turns on nothing, like minimal
    final boolean isEmptyValue = value.isEmpty();
    if (isEmptyValue) {
      return Include.MINIMAL;
    }

    return Include.getByParameterValue(value);
  }
}
