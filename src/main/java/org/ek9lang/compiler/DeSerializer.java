package org.ek9lang.compiler;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.function.Function;
import org.ek9lang.core.CompilerException;
import org.ek9lang.core.SharedThreadContext;

/**
 * De-Serializes a byte array back to compilable program.
 * Note that some specific transient data will not be reconstituted. But the compilable program will
 * have all the modules and symbols present - it will be just transient ANTLR type aspect that are no longer present.
 * If de-serialisation fails it is a runtime exception as that is a major failing in the serialisation mechanism.
 * The main reason for this is to speed up bootstrapping from a basic compiler once loaded it is always the same
 * structure.
 */
public class DeSerializer implements Function<byte[], SharedThreadContext<CompilableProgram>> {
  @Override
  @SuppressWarnings("unchecked")
  public SharedThreadContext<CompilableProgram> apply(final byte[] bytes) {

    final SharedThreadContext<CompilableProgram> program;
    try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
      program = (SharedThreadContext<CompilableProgram>) input.readObject();
    } catch (IOException | ClassNotFoundException e) {
      throw new CompilerException(e.toString());
    }

    return program;
  }
}
