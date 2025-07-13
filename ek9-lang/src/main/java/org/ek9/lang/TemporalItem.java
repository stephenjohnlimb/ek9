package org.ek9.lang;

import java.time.temporal.TemporalAccessor;

/**
 * Unified mechanism for classes that are temporal (date/time related).
 */
public interface TemporalItem extends Any {

  @SuppressWarnings("checkstyle:MethodName")
  TemporalAccessor _getAsJavaTemporalAccessor();
}
