package life.qbic.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;


public class StringUtilsTest {

  @ParameterizedTest(name = "input = {0}")
  @ValueSource(strings = {"test", "thisIs4L0n9_Text!"})
  @DisplayName("endsWithIgnoreCase is true for identical Strings")
  void endsWithIgnoreCaseIsTrueForIdenticalStrings(String input) {
    assertTrue(StringUtils.endsWithIgnoreCase(input, input));
  }

  @ParameterizedTest(name = "input = {0} - {1}")
  @CsvSource({
          "test, TEST",
          "thisIs4L0n9_Text!, THISIS4L0N9_TEXT!"})
  @DisplayName("endsWithIgnoreCase is true for same Strings ignoring case")
  void endsWithIgnoreCaseIsTrueForIdenticalStrings(String lowerCase, String upperCase) {
    assertTrue(StringUtils.endsWithIgnoreCase(lowerCase, upperCase));
  }

  @ParameterizedTest(name = "{0}\t{1} == {1}\t{0}")
  @CsvSource({
          "test, TEST",
          "thisIs4L0n9_Text!, THISIS4L0N9_TEXT!"})
  @DisplayName("endsWithIgnoreCase is symmetrical")
  void endsWithIgnoreCaseIsSymmetrical(String one, String two) {
    assertEquals(StringUtils.endsWithIgnoreCase(one, two), StringUtils.endsWithIgnoreCase(two, one));
  }

  @Test
  @DisplayName("endsWithIgnoreCase is false for different Strings")
  void endsWithIgnoreCaseIsFalseForDifferentStrings() {
    assertFalse(StringUtils.endsWithIgnoreCase("a", "b"));
  }
}
