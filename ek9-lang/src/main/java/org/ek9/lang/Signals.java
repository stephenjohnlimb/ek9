package org.ek9.lang;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.ek9tooling.Ek9Class;
import org.ek9tooling.Ek9Constructor;
import org.ek9tooling.Ek9Method;
import org.ek9tooling.Ek9Operator;

/**
 * EK9 Signals type for Unix/Linux signal handling.
 * Supports registration of multiple handlers per signal and System.exit() integration.
 * Uses reference semantics - constructor takes references to arguments, no deep copying.
 * Only set if constructor arguments are valid and signal registration is successful.
 */
@SuppressWarnings("checkstyle:MethodName")
@Ek9Class("Signals")
public class Signals extends BuiltinType {

  // Standard Unix/Linux signals that can be handled
  //You mileage my vary on Windows.
  private static final Set<java.lang.String> STANDARD_SIGNALS = Set.of(
      "HUP", "TRAP", "ABRT", "PIPE", "ALRM", "TERM",
      "CHLD", "TTIN", "TTOU", "PROF", "WINCH", "USR1", "USR2"
  );

  // Map of signal names to their registered handlers
  private final Map<java.lang.String, List<SignalHandler>> signalHandlers = new ConcurrentHashMap<>();

  // Map of registered native signal handlers (to prevent GC)
  private final Map<java.lang.String, sun.misc.SignalHandler> nativeHandlers = new ConcurrentHashMap<>();

  // System exit manager for testability
  private final SystemExitManager systemExitManager;

  @Ek9Constructor("Signals() as pure")
  public Signals() {
    this(new SystemExitManager.Production());
  }

  // Internal constructor for testing with custom SystemExitManager
  Signals(SystemExitManager systemExitManager) {
    this.systemExitManager = systemExitManager;
    set(); // Signals is always set when created
  }

  @Ek9Method("""
      register() as pure
        ->
          signals as List of String
          handler as SignalHandler
        <-
          rtn as List of String?""")
  public _List_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1 register(
      _List_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1 signals,
      SignalHandler handler) {

    if (!canProcess(signals) || !canProcess(handler)) {
      return new _List_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1();
    }

    final var result = new _List_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1();
    final var iterator = signals.iterator();

    while (iterator.hasNext().state) {
      final var signal = iterator.next();
      if (isValid(signal)) {
        final var signalName = signal.state;
        if (registerSingleSignal(signalName, handler)) {
          result._addAss(String._of(signalName));
        }
      }
    }

    return result;
  }

  @Ek9Method("""
      register() as pure
        ->
          signal as String
          handler as SignalHandler""")
  public void register(String signal, SignalHandler handler) {
    if (canProcess(signal) && canProcess(handler)) {
      registerSingleSignal(signal.state, handler);
    }
  }

  /**
   * Internal method to register a single signal handler.
   *
   * @param signalName The signal name to register
   * @param handler    The handler to register
   * @return true if registration was successful
   */
  @SuppressWarnings({"checkstyle:LambdaParameterName", "checkstyle:CatchParameterName"})
  private boolean registerSingleSignal(java.lang.String signalName, SignalHandler handler) {
    // Only handle standard signals
    if (!STANDARD_SIGNALS.contains(signalName)) {
      return false;
    }

    // Add handler to our list
    signalHandlers.computeIfAbsent(signalName, _ -> new ArrayList<>()).add(handler);

    // Register with native signal handling if not already registered
    if (!nativeHandlers.containsKey(signalName)) {
      try {
        final var nativeSignal = new sun.misc.Signal(signalName);
        final sun.misc.SignalHandler nativeHandler = (sun.misc.Signal _) -> handleSignal(signalName);

        sun.misc.Signal.handle(nativeSignal, nativeHandler);
        nativeHandlers.put(signalName, nativeHandler);
        return true;
      } catch (IllegalArgumentException | UnsupportedOperationException _) {
        // Signal not supported on this platform
        // Remove handler from our list since we can't actually handle it
        final var handlers = signalHandlers.get(signalName);
        if (handlers != null) {
          handlers.remove(handler);
          if (handlers.isEmpty()) {
            signalHandlers.remove(signalName);
          }
        }
        return false;
      }
    }

    return true;
  }

  /**
   * Handle a signal by calling all registered handlers.
   * If any handler returns a set Integer, call System.exit() with that value.
   *
   * @param signalName The name of the signal that was triggered.
   */
  @SuppressWarnings("checkstyle:CatchParameterName")
  private void handleSignal(java.lang.String signalName) {
    final var handlers = signalHandlers.get(signalName);
    if (handlers != null) {
      final var signalString = String._of(signalName);

      // Call all handlers for this signal
      for (final var handler : new ArrayList<>(handlers)) { // Copy to avoid concurrent modification
        try {
          final var result = handler._call(signalString);
          if (isValid(result)) {
            // Handler returned a set Integer - exit with that code
            systemExitManager.exit((int) result.state);
            return; // Exit called, no need to continue
          }
        } catch (Exception _) {
          //Consume any exceptions.
        }
      }
    }
  }

  @Ek9Operator("""
      operator $ as pure
        <- rtn as String?""")
  @Override
  public String _string() {
    if (!isSet || signalHandlers.isEmpty()) {
      return String._of("Signals{}");
    }

    final var result = new StringBuilder();
    final var signalNames = new ArrayList<>(signalHandlers.keySet());
    Collections.sort(signalNames);

    for (final var signalName : signalNames) {
      if (!result.isEmpty()) {
        result.append(", ");
      }
      final var handlerCount = signalHandlers.get(signalName).size();
      result.append(signalName).append(":").append(handlerCount);
    }

    return String._of("Signals{" + result + "}");
  }

  @Ek9Operator("""
      override operator ? as pure
        <- rtn as Boolean?""")
  @Override
  public Boolean _isSet() {
    return Boolean._of(isSet);
  }

  // Factory method for convenience
  public static Signals _of() {
    return new Signals();
  }
}