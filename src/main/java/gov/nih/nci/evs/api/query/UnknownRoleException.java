package gov.nih.nci.evs.api.query;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Thrown when a query names a role the searched terminology doesn't have, by code or by name. The
 * message names the role and lists the terminology's roles, so the client can fix the query; the
 * query endpoint reports it as a bad request.
 */
public class UnknownRoleException extends IllegalArgumentException {

  /** The serial version UID. */
  private static final long serialVersionUID = 1L;

  /**
   * Instantiates an {@link UnknownRoleException}.
   *
   * @param role the role the query named
   * @param terminology the terminology searched, e.g. ncit 25.12e
   * @param roleNameByCode the terminology's roles, each code with its name
   */
  public UnknownRoleException(
      final String role, final String terminology, final Map<String, String> roleNameByCode) {
    super(
        "role: '"
            + role
            + "' isn't a role of "
            + terminology
            + "; its roles are "
            + getRoleList(roleNameByCode));
  }

  /**
   * Returns the roles as "code (name)", separated by commas.
   *
   * @param roleNameByCode the roles, each code with its name
   * @return the role list
   */
  private static String getRoleList(final Map<String, String> roleNameByCode) {
    return roleNameByCode.entrySet().stream()
        .map(entry -> entry.getKey() + " (" + entry.getValue() + ")")
        .collect(Collectors.joining(", "));
  }
}
