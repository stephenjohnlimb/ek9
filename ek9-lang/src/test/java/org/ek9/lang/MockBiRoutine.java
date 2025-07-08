package org.ek9.lang;

/**
 * Simple mock, removes the need for mockito.
 */
public class MockBiRoutine extends BiRoutine {

  private Any calledWithT;
  private Any calledWithU;
  private Any respondWith = new Any() {};

  @Override
  public Any _call(Any t, Any u) {
    super._call(t, u);
    this.calledWithT = t;
    this.calledWithU = u;
    return respondWith;
  }

  public void respondWith(final Any response) {
    this.respondWith = response;
  }

  public boolean verifyNotCalled() {
    return calledWithT == null && calledWithU == null;
  }

  public boolean verifyCalledWith(final Any t, final Any u) {
    if (calledWithT == null || calledWithU == null) {
      return false;
    }
    return calledWithT == t && calledWithU == u;
  }
}
