package org.ek9.lang;

/**
 * Simple mock, removes the need for mockito.
 */
public class MockRoutine extends Routine {

  private Any calledWith;
  private Any respondWith = new Any() {};

  @Override
  public Any _call(final Any t) {
    super._call(t);
    this.calledWith = t;
    return respondWith;
  }

  public void respondWith(final Boolean response) {
    this.respondWith = response;
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
