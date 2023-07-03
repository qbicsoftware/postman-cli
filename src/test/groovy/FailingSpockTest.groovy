import spock.lang.Ignore
import spock.lang.Specification

/**
 * Use this test to see whether maven executes Spock tests or not.
 */
@Ignore
class FailingSpockTest extends Specification {
    def "this test always fails"() {
      expect:
      1 == 2
    }
}
