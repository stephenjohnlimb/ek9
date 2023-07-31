package org.ek9lang.cli;

import org.ek9lang.core.SigningKeyPair;

/**
 * Generate signing keys.
 */
final class Egk extends E {
  Egk(CompilationContext compilationContext) {
    super(compilationContext);
  }

  @Override
  protected String messagePrefix() {
    return "Keys    : ";
  }

  @Override
  protected boolean doRun() {
    if (getFileHandling().isUsersSigningKeyPairPresent()) {
      log("Already present - not regenerating");
      return true;
    }
    log("Generating new signing keys");
    //Clients only use short key lengths, server uses 2048.
    return getFileHandling().saveToHomeEk9Directory(SigningKeyPair.generate(1024));
  }
}
