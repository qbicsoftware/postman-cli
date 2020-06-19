package life.qbic

interface ChecksumWriter {

    def writeMatchingChecksum(String expectedChecksum, String computedChecksum, URL fileLocation)

    def writeFailedChecksum(String expectedChecksum, String computedChecksum, URL fileLocation)

}