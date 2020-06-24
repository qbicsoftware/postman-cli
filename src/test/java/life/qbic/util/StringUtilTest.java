package life.qbic.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class StringUtilTest {

  @Test
  public void endsWithIgnoreCase() {
    assertEquals(true, StringUtil.endsWithIgnoreCase("thisissomerandomTESTBLA", "randomTESTbla"));
    assertEquals(false, StringUtil.endsWithIgnoreCase("thisissomerandomTESTBLA", "ayyyyynope"));
  }
}
