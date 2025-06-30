package org.company.dept;

import org.ek9tooling.Ek9Constructor;
import org.ek9tooling.Ek9Operator;
import org.ek9tooling.Ek9Property;
import org.ek9tooling.Ek9Record;

/**
 * Needs to map to EK9 record construct.
 * But this needs fields/properties to be exported out as well as operators.
 * <pre>
 NetworkProperties
   host as StringExample: StringExample()

   operator $ as pure
     &lt;- rtn as String?
 </pre>
 */
@Ek9Record
public class NetworkProperties {

  /**
   * For records all properties are public and so have to be declared and hence accessible.
   */
  @Ek9Property("host as StringExample?")
  public StringExample host = new StringExample();

  @Ek9Property("port as Integer?")
  public Integer port;

  @Ek9Property("packetSize as Integer?")
  public Integer packetSize;

  @Ek9Constructor("NetworkProperties() as pure")
  public NetworkProperties() {
    //default constructor
  }

  /**
   * Need to use 'String' here rather than String example because EK9 mandates. String for $ (Convert To String)
   */
  @Ek9Operator("""
      operator $ as pure
        <- rtn as String?""")
  public String _string() {
    return "Any Value";
  }

}
