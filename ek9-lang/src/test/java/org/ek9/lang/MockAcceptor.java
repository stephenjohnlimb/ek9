package org.ek9.lang;

/**
 * Rather than use Mockito, I prefer to use simple classes like this.
 */
public class MockAcceptor extends Acceptor {
  private Any calledWith;

  @Override
  public void _call(final Any t) {
    super._call(t);
    this.calledWith = t;
  }

  public boolean verifyNotCalled() {
    return calledWith == null;
  }

  public boolean verifyCalledWith(final Any t) {
    if (calledWith == null) {
      return false;
    }
    return calledWith == t;
  }
}
