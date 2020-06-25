package life.qbic.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class StringUtilTest {

  @Test
  public void endsWithIgnoreCase() {
    // success with same case
    assertEquals(true, StringUtil.endsWithIgnoreCase("thisissomerandomTESTBLA", "randomTESTBLA"));
    assertEquals(true, StringUtil.endsWithIgnoreCase("thisissomerandomTESTBLA", "randomTESTBlA"));
     assertEquals(true, StringUtil.endsWithIgnoreCase("thisissomerandomTESTBLA", "randomteSTBlA"));
    // success with different case
    assertEquals(true, StringUtil.endsWithIgnoreCase("thisissomerandomTESTBLA", "randomTESTbla"));
    assertEquals(false, StringUtil.endsWithIgnoreCase("thisissomerandomTESTBLA", "ayyyyynope"));
  }
}
