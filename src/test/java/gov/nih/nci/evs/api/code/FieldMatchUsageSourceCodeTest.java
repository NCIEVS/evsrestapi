package gov.nih.nci.evs.api.code;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.SimpleTreeVisitor;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import javax.lang.model.element.Name;
import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Test;

/**
 * Source code test to verify that test code only uses the query package's {@code FieldMatch} inside
 * a class, method, or field annotated with {@code @DemonstratesFieldMatch}. {@code FieldMatch} is
 * for developers building new query nodes, so tests showing how to use the API must stick to what
 * API users can write. The test files are parsed, not searched as text, so comments and strings
 * that mention {@code FieldMatch} don't count.
 */
public class FieldMatchUsageSourceCodeTest {

  /** The simple name of the restricted class. */
  private static final String FIELD_MATCH = "FieldMatch";

  /** The simple name of the annotation that allows using {@link #FIELD_MATCH}. */
  private static final String ALLOWING_ANNOTATION = "DemonstratesFieldMatch";

  /** The root of the test sources. */
  private static final Path TEST_SOURCE_ROOT = Paths.get("src/test/java");

  /** The extension of Java source files. */
  private static final String JAVA_EXTENSION = ".java";

  /** Gets the modifiers of a class, method, or variable, and null for any other tree. */
  private static final SimpleTreeVisitor<ModifiersTree, Void> MODIFIERS_GETTER =
      new SimpleTreeVisitor<>() {
        @Override
        public ModifiersTree visitClass(final ClassTree classTree, final Void unused) {
          return classTree.getModifiers();
        }

        @Override
        public ModifiersTree visitMethod(final MethodTree method, final Void unused) {
          return method.getModifiers();
        }

        @Override
        public ModifiersTree visitVariable(final VariableTree variable, final Void unused) {
          return variable.getModifiers();
        }
      };

  /**
   * Test that every use of FieldMatch in test code is inside an annotated class, method, or field.
   *
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Test
  public void testFieldMatchIsOnlyUsedWhereAnnotated() throws IOException {
    final List<String> unannotatedUsages = fetchUnannotatedUsages(fetchTestSourceFiles());

    assertTrue(
        unannotatedUsages.isEmpty(),
        FIELD_MATCH
            + " is for building query nodes. Use public nodes instead, or annotate the test with @"
            + ALLOWING_ANNOTATION
            + " if it tests node building:\n"
            + String.join("\n", unannotatedUsages));
  }

  /**
   * Fetches every Java file under the test sources.
   *
   * @return the files
   * @throws IOException Signals that an I/O exception has occurred.
   */
  private static List<File> fetchTestSourceFiles() throws IOException {
    try (final Stream<Path> paths = Files.walk(TEST_SOURCE_ROOT)) {
      return paths
          .filter(path -> path.toString().endsWith(JAVA_EXTENSION))
          .map(Path::toFile)
          .toList();
    }
  }

  /**
   * Parses the files and finds every use of FieldMatch outside an annotated class, method, or
   * field.
   *
   * @param files the files to parse
   * @return each unannotated use, as "file:line"
   * @throws IOException Signals that an I/O exception has occurred.
   */
  private static List<String> fetchUnannotatedUsages(final List<File> files) throws IOException {
    final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    try (final StandardJavaFileManager fileManager =
        compiler.getStandardFileManager(null, null, null)) {
      final JavacTask task =
          (JavacTask)
              compiler.getTask(
                  null,
                  fileManager,
                  null,
                  null,
                  null,
                  fileManager.getJavaFileObjectsFromFiles(files));
      final UnannotatedUsageScanner scanner =
          new UnannotatedUsageScanner(Trees.instance(task).getSourcePositions());

      for (final CompilationUnitTree compilationUnit : task.parse()) {
        scanner.scan(compilationUnit, null);
      }

      return scanner.getUnannotatedUsages();
    }
  }

  /**
   * Indicates whether the tree or anything enclosing it carries the allowing annotation.
   *
   * @param path the path from the tree up to its compilation unit
   * @return whether the use is allowed
   */
  private static boolean getHasAllowingAnnotation(final TreePath path) {
    for (final Tree enclosingTree : path) {
      final ModifiersTree modifiers = enclosingTree.accept(MODIFIERS_GETTER, null);
      final boolean isAnnotated = modifiers != null && getHasAllowingAnnotation(modifiers);
      if (isAnnotated) {
        return true;
      }
    }
    return false;
  }

  /**
   * Indicates whether the modifiers include the allowing annotation, by simple or qualified name.
   *
   * @param modifiers the modifiers
   * @return whether the allowing annotation is present
   */
  private static boolean getHasAllowingAnnotation(final ModifiersTree modifiers) {
    return modifiers.getAnnotations().stream()
        .map(AnnotationTree::getAnnotationType)
        .map(Tree::toString)
        .anyMatch(
            annotationName ->
                annotationName.equals(ALLOWING_ANNOTATION)
                    || annotationName.endsWith("." + ALLOWING_ANNOTATION));
  }

  /** Records every reference to FieldMatch that isn't inside an annotated declaration. */
  private static final class UnannotatedUsageScanner extends TreePathScanner<Void, Void> {

    /** Finds where trees start, to report line numbers. */
    private final SourcePositions positions;

    /** Each unannotated use, as "file:line". */
    private final List<String> unannotatedUsages = new ArrayList<>();

    /**
     * Instantiates an {@link UnannotatedUsageScanner}.
     *
     * @param positions finds where trees start
     */
    UnannotatedUsageScanner(final SourcePositions positions) {
      this.positions = positions;
    }

    /**
     * Returns each unannotated use, as "file:line".
     *
     * @return the unannotated uses
     */
    List<String> getUnannotatedUsages() {
      return unannotatedUsages;
    }

    /* see superclass */
    @Override
    public Void visitIdentifier(final IdentifierTree identifier, final Void unused) {
      recordIfUnannotatedFieldMatch(identifier.getName(), identifier);
      return super.visitIdentifier(identifier, unused);
    }

    /* see superclass */
    @Override
    public Void visitMemberSelect(final MemberSelectTree memberSelect, final Void unused) {
      recordIfUnannotatedFieldMatch(memberSelect.getIdentifier(), memberSelect);
      return super.visitMemberSelect(memberSelect, unused);
    }

    /**
     * Records the reference if it names FieldMatch and nothing enclosing it is annotated.
     *
     * @param name the name referenced
     * @param tree the referencing tree
     */
    private void recordIfUnannotatedFieldMatch(final Name name, final Tree tree) {
      final boolean isFieldMatch = name.contentEquals(FIELD_MATCH);
      if (!isFieldMatch) {
        return;
      }

      final boolean isAllowed = getHasAllowingAnnotation(getCurrentPath());
      if (isAllowed) {
        return;
      }

      final CompilationUnitTree compilationUnit = getCurrentPath().getCompilationUnit();
      final long line =
          compilationUnit
              .getLineMap()
              .getLineNumber(positions.getStartPosition(compilationUnit, tree));
      unannotatedUsages.add(compilationUnit.getSourceFile().getName() + ":" + line);
    }
  }
}
