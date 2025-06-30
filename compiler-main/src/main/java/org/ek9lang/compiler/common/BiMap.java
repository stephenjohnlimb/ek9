package org.ek9lang.compiler.common;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Simple bi-directional map for Strings.
 * Useful when needing to look both ways on unique maps of String.
 * <p>
 * getForwards is v1 ->v2 as entered input
 * getBackwards is v2->v1 as entered input
 * </p>
 */
public class BiMap {
  private final Map<String, String> map1 = new HashMap<>();
  private final Map<String, String> map2 = new HashMap<>();

  public BiMap put(String v1, String v2) {
    map1.put(v1, v2);
    map2.put(v2, v1);

    return this;
  }

  public Set<String> getForwardKeys() {
    return map1.keySet();
  }

  public Set<String> getBackwardKeys() {
    return map2.keySet();
  }

  public String getForward(String v1) {
    return map1.get(v1);
  }

  public String getBackward(String v2) {
    return map2.get(v2);
  }

}
