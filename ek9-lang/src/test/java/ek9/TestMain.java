package ek9;

import java.util.ArrayList;

/**
 * Test main class for ProgramLauncher testing.
 * Provides methods that match the expected signatures for EK9 programs.
 * Used instead of the production "ek9.Main" class which doesn't exist during testing.
 * <p>
 * While these don't look like they are referenced anywhere, we are using reflection from
 * the programLauncher configuration to call them.
 * </p>
 */
public class TestMain {

  //We use a thread local so that the tests can run concurrently without affecting each output.
  private static final ThreadLocal<Interaction> threadLocalOutput = ThreadLocal.withInitial(Interaction::new);

  public static Interaction getInteraction() {
    return threadLocalOutput.get().reset();
  }

  /**
   * Test method for parameterless programs.
   */
  public static synchronized void executeHelloWorld() {
    final var interaction = threadLocalOutput.get();
    interaction.lastMethodCalled = "executeHelloWorld";
    interaction.lastArgumentsPassed = new Object[0];
    interaction.outputMessage = "Hello, World!";
  }

  /**
   * Test method for programs with one String parameter.
   */
  public static synchronized void executeHelloMessage(org.ek9.lang.String message) {
    final var interaction = threadLocalOutput.get();
    interaction.lastMethodCalled = "executeHelloMessage";
    interaction.lastArgumentsPassed = new Object[] {message};
    interaction.outputMessage = "Hello, " + message;
  }

  /**
   * Test method for programs with Integer parameter.
   */
  public static synchronized void executeIntegerProgram(org.ek9.lang.Integer value) {
    final var interaction = threadLocalOutput.get();
    interaction.lastMethodCalled = "executeIntegerProgram";
    interaction.lastArgumentsPassed = new Object[] {value};
    interaction.outputMessage = "Integer value: " + value;
  }

  /**
   * Test method for programs with Boolean parameter.
   */
  public static synchronized void executeBooleanProgram(org.ek9.lang.Boolean flag) {
    final var interaction = threadLocalOutput.get();
    interaction.lastMethodCalled = "executeBooleanProgram";
    interaction.lastArgumentsPassed = new Object[] {flag};
    interaction.outputMessage = "Boolean value: " + flag;
  }

  /**
   * Test method for programs with Float parameter.
   */
  public static synchronized void executeFloatProgram(org.ek9.lang.Float value) {
    final var interaction = threadLocalOutput.get();
    interaction.lastMethodCalled = "executeFloatProgram";
    interaction.lastArgumentsPassed = new Object[] {value};
    interaction.outputMessage = "Float value: " + value;
  }

  /**
   * Test method for programs with Character parameter.
   */
  public static synchronized void executeCharacterProgram(org.ek9.lang.Character character) {
    final var interaction = threadLocalOutput.get();
    interaction.lastMethodCalled = "executeCharacterProgram";
    interaction.lastArgumentsPassed = new Object[] {character};
    interaction.outputMessage = "Character value: " + character;
  }

  /**
   * Test method for programs with multiple parameters.
   */
  public static synchronized void executeMultiParamProgram(org.ek9.lang.String name, org.ek9.lang.Integer age) {
    final var interaction = threadLocalOutput.get();
    interaction.lastMethodCalled = "executeMultiParamProgram";
    interaction.lastArgumentsPassed = new Object[] {name, age};
    interaction.outputMessage = "Name: " + name + ", Age: " + age;

  }

  public static class Interaction {

    public String lastMethodCalled;
    public Object[] lastArgumentsPassed;
    public String outputMessage;
    public ArrayList<String> errorMessages = new ArrayList<>();
    public ArrayList<Integer> exitCodes = new ArrayList<>();

    Interaction reset() {
      lastMethodCalled = null;
      lastArgumentsPassed = new Object[0];
      outputMessage = null;
      errorMessages = new ArrayList<>();
      exitCodes = new ArrayList<>();
      return this;
    }
  }
}