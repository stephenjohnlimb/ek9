package ek9;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Metadata for an EK9 program including its signature and execution details.
 * Used by the program launcher to validate arguments and dispatch execution.
 */
public final class ProgramMetadata {

  private final String qualifiedName;
  private final List<String> parameterTypes;
  private final String methodName;

  /**
   * Create program metadata.
   *
   * @param qualifiedName   Fully qualified program name (e.g., "introduction1::HelloWorld")
   * @param parameterTypes  Array of parameter type names (e.g., ["org.ek9.lang::String"])
   * @param methodName      Name of the method to execute (e.g., "executeHelloWorld")
   */
  public ProgramMetadata(final String qualifiedName,
                         final String[] parameterTypes,
                         final String methodName) {
    this.qualifiedName = Objects.requireNonNull(qualifiedName, "Program name cannot be null");
    this.parameterTypes = parameterTypes != null
        ? List.of(Arrays.copyOf(parameterTypes, parameterTypes.length))
        : List.of();
    this.methodName = Objects.requireNonNull(methodName, "Method name cannot be null");

    if (qualifiedName.trim().isEmpty()) {
      throw new IllegalArgumentException("Program name cannot be empty");
    }
    if (methodName.trim().isEmpty()) {
      throw new IllegalArgumentException("Method name cannot be empty");
    }
  }

  /**
   * Get the fully qualified program name.
   *
   * @return Program name like "introduction1::HelloWorld"
   */
  public String getQualifiedName() {
    return qualifiedName;
  }

  /**
   * Get the parameter types for this program.
   *
   * @return Immutable list of parameter type names
   */
  public List<String> getParameterTypes() {
    return parameterTypes;
  }

  /**
   * Get the number of parameters this program expects.
   *
   * @return Parameter count
   */
  public int getParameterCount() {
    return parameterTypes.size();
  }

  /**
   * Get the method name to execute for this program.
   *
   * @return Method name like "executeHelloWorld"
   */
  public String getMethodName() {
    return methodName;
  }

  /**
   * Check if this program takes parameters.
   *
   * @return true if program has parameters, false if parameterless
   */
  public boolean hasParameters() {
    return !parameterTypes.isEmpty();
  }

  /**
   * Get a human-readable signature for this program.
   *
   * @return Signature like "HelloWorld()" or "HelloMessage(message: String)"
   */
  public String getSignature() {
    final var simpleName = extractSimpleName(qualifiedName);
    if (parameterTypes.isEmpty()) {
      return simpleName + "()";
    }

    final var params = new StringBuilder();
    for (int i = 0; i < parameterTypes.size(); i++) {
      if (i > 0) {
        params.append(", ");
      }
      params.append("arg").append(i).append(": ").append(simplifyTypeName(parameterTypes.get(i)));
    }

    return simpleName + "(" + params + ")";
  }

  /**
   * Extract simple name from qualified name.
   * "introduction1::HelloWorld" → "HelloWorld"
   */
  private String extractSimpleName(final String qualifiedName) {
    final var lastColon = qualifiedName.lastIndexOf("::");
    return lastColon >= 0 ? qualifiedName.substring(lastColon + 2) : qualifiedName;
  }

  /**
   * Simplify type name for display.
   * "org.ek9.lang::String" → "String"
   */
  private String simplifyTypeName(final String typeName) {
    final var lastColon = typeName.lastIndexOf("::");
    return lastColon >= 0 ? typeName.substring(lastColon + 2) : typeName;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final var that = (ProgramMetadata) obj;
    return Objects.equals(qualifiedName, that.qualifiedName)
        && Objects.equals(parameterTypes, that.parameterTypes)
        && Objects.equals(methodName, that.methodName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(qualifiedName, parameterTypes, methodName);
  }

  @Override
  public String toString() {
    return "ProgramMetadata{"
        + "qualifiedName='" + qualifiedName + '\''
        + ", parameterTypes=" + parameterTypes
        + ", methodName='" + methodName + '\''
        + '}';
  }
}