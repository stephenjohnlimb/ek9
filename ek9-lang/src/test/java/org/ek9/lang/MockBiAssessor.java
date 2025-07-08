package org.ek9.lang;

/**
 * Simple mock, removes the need for mockito.
 */
public class MockBiAssessor extends BiAssessor {

  private Any calledWithT;
  private Any calledWithU;
  private Boolean respondWith = new Boolean();

  @Override
  public Boolean _call(Any t, Any u) {
    super._call(t, u);
    this.calledWithT = t;
    this.calledWithU = u;
    return respondWith;
  }

  public void respondWith(final Boolean response) {
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
