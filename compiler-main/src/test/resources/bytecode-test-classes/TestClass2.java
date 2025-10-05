/**
 * Test class with branching for BytecodeNormalizer testing.
 * <p>
 * This class contains conditional logic (null check) to verify
 * that normalization correctly handles branch instructions and labels.
 * </p>
 */
public class TestClass2 {

  /**
   * Method with null check - generates branching bytecode.
   *
   * @param value Input string (may be null)
   * @return "null" if input is null, otherwise returns the input value
   */
  public String checkNull(String value) {
    if (value == null) {
      return "null";
    }
    return value;
  }
}
