package org.ek9lang.compiler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.function.Function;
import org.ek9lang.core.CompilerException;
import org.ek9lang.core.SharedThreadContext;

/**
 * Serializes a compilable program to a byte array, so it can be saved/reused as required.
 * If serialisation fails it is a runtime exception as that is a major failing in the serialisation mechanism.
 */
public class Serializer implements Function<SharedThreadContext<CompilableProgram>, byte[]> {
  @Override
  public byte[] apply(final SharedThreadContext<CompilableProgram> program) {

    final var byteStream = new ByteArrayOutputStream();

    try (final var output = new ObjectOutputStream(byteStream)) {
      output.writeObject(program);
    } catch (IOException e) {
      throw new CompilerException(e.toString());
    }

    return byteStream.toByteArray();
  }
}
