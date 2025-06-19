package org.company.dept;

import org.ek9tooling.Ek9Method;
import org.ek9tooling.Ek9Service;

@Ek9Service("Site :/site")
public class Site {

  @Ek9Method("""
      index() as GET for :/index.html
        <- response as HTTPResponse?""")
  public String index() {
    return "Simulated Response, will need a real EK9 HTTPResponse object in time";
  }
}
