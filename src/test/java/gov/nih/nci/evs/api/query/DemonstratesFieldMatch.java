package gov.nih.nci.evs.api.query;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks test code allowed to use {@link FieldMatch}. {@link FieldMatch} is for developers building
 * new query nodes, not for API users, so tests that show how to use the API must not use it. Only
 * tests of the node-building machinery itself, e.g. one that needs any node at all or one that
 * applies a value match to a field, should carry this. {@code FieldMatchUsageSourceCodeTest} fails
 * on any use of {@link FieldMatch} in test code outside a class, method, or field marked with it.
 */
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD, ElementType.CONSTRUCTOR})
public @interface DemonstratesFieldMatch {}
