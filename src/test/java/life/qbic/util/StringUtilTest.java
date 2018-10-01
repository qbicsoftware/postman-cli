package life.qbic.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class StringUtilTest {

    @Test
    public void endsWithIgnoreCase() {
        assertEquals(true, StringUtil.endsWithIgnoreCase("thisissomerandomTESTBLA", "randomTESTbla"));
        assertEquals(false, StringUtil.endsWithIgnoreCase("thisissomerandomTESTBLA", "ayyyyynope"));
    }

}
