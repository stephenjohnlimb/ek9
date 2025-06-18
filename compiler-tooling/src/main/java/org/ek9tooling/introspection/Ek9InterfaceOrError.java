package org.ek9tooling.introspection;

/**
 * Either a valid ek9 extern interface or an error message as to why the interface could not be extracted.
 */
public record Ek9InterfaceOrError(String ek9Interface, String errorMessage) {
}
