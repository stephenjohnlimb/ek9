/**
 * Test class with field and constructor for BytecodeNormalizer testing.
 * <p>
 * This class contains a field, constructor, and getter method to verify
 * normalization correctly handles field access instructions (getfield/putfield)
 * and multiple methods.
 * </p>
 */
public class TestClass3 {

  private int field;

  /**
   * Constructor that initializes the field.
   *
   * @param value Initial value for field
   */
  public TestClass3(int value) {
    this.field = value;
  }

  /**
   * Getter for field - generates getfield bytecode.
   *
   * @return Current value of field
   */
  public int getField() {
    return field;
  }
}
