package org.ek9lang.compiler.symbols;

import java.io.Serial;

/**
 * Intended to model the pipeline flow in a streaming of cat or for loop though | and map etc.
 * So really all we're trying to do in the symbol modelling is ensuring that the piping with the
 * types and commands are compatible with each other. Phase 7 the IR phase will have to create
 * objects that can be linked.
 */
public class StreamPipeLineSymbol extends Symbol {

  @Serial
  private static final long serialVersionUID = 1L;

  public StreamPipeLineSymbol(String name) {
    super(name);
  }

  @Override
  public StreamPipeLineSymbol clone(IScope withParentAsAppropriate) {
    return cloneIntoStreamPipeLineSymbol(new StreamPipeLineSymbol(getName()));
  }

  protected StreamPipeLineSymbol cloneIntoStreamPipeLineSymbol(StreamPipeLineSymbol newCopy) {
    super.cloneIntoSymbol(newCopy);
    return newCopy;
  }

}
