package life.qbic.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class StringUtilTest {

  @Test
  public void endsWithIgnoreCase() {
    // success with same case
    assertTrue(StringUtil.endsWithIgnoreCase("thisissomerandomTESTBLA", "randomTESTBLA"));
    assertTrue(StringUtil.endsWithIgnoreCase("thisissomerandomTESTBLA", "randomTESTBlA"));
    assertTrue(StringUtil.endsWithIgnoreCase("thisissomerandomTESTBLA", "randomteSTBlA"));
    // success with different case
    assertTrue(StringUtil.endsWithIgnoreCase("thisissomerandomTESTBLA", "randomTESTbla"));
    assertFalse(StringUtil.endsWithIgnoreCase("thisissomerandomTESTBLA", "ayyyyynope"));
  }
}
