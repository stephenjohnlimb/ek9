package org.ek9.lang;

/**
 * Rather than use Mockito, I prefer to use simple classes like this.
 */
public class MockBiConsumer extends BiConsumer {
  private Any calledWithT;
  private Any calledWithU;

  @Override
  public void _call(Any t, Any u) {
    super._call(t, u);
    this.calledWithT = t;
    this.calledWithU = u;
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
