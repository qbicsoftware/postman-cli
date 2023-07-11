package life.qbic.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import life.qbic.qpostman.common.functions.EndsWithIgnoreCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;


public class StringUtilsTest {

  @ParameterizedTest(name = "input = {0}")
  @ValueSource(strings = {"test", "thisIs4L0n9_Text!"})
  @DisplayName("endsWithIgnoreCase is true for identical Strings")
  void endsWithIgnoreCaseIsTrueForIdenticalStrings(String input) {
    assertTrue(EndsWithIgnoreCase.endsWithIgnoreCase(input, input));
  }

  @ParameterizedTest(name = "input = {0} - {1}")
  @CsvSource({
          "test, TEST",
          "thisIs4L0n9_Text!, THISIS4L0N9_TEXT!"})
  @DisplayName("endsWithIgnoreCase is true for same Strings ignoring case")
  void endsWithIgnoreCaseIsTrueForIdenticalStrings(String lowerCase, String upperCase) {
    assertTrue(EndsWithIgnoreCase.endsWithIgnoreCase(lowerCase, upperCase));
  }

  @ParameterizedTest(name = "{0}\t{1} == {1}\t{0}")
  @CsvSource({
          "test, TEST",
          "thisIs4L0n9_Text!, THISIS4L0N9_TEXT!"})
  @DisplayName("endsWithIgnoreCase is symmetrical")
  void endsWithIgnoreCaseIsSymmetrical(String one, String two) {
    assertEquals(EndsWithIgnoreCase.endsWithIgnoreCase(one, two), EndsWithIgnoreCase.endsWithIgnoreCase(two, one));
  }

  @Test
  @DisplayName("endsWithIgnoreCase is false for different Strings")
  void endsWithIgnoreCaseIsFalseForDifferentStrings() {
    assertFalse(EndsWithIgnoreCase.endsWithIgnoreCase("a", "b"));
  }
}
