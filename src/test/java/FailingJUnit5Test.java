import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Use this test to see whether maven executes JUnit5 tests or not.
 */
@Disabled
public class FailingJUnit5Test {

    @Test
    @DisplayName("always fails")
    void alwaysFails() {
        fail("test fails as expected");
    }
}
