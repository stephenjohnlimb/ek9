package org.ek9lang.compiler.ir;

import java.util.List;

/**
 * Used with CallInstr, holds all the necessary details to make a call.
 */
public record CallDetails(String targetObject,
                          String targetTypeName,
                          String methodName,
                          List<String> parameterTypes,
                          String returnTypeName,
                          List<String> arguments) {
}
