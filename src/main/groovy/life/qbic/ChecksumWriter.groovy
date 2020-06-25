package life.qbic

/**
 * This interface can be used if you want to provide
 * functionality to write summary reports for matching and invalid checksums
 * of downloaded files.
 *
 * @author Sven Fillinger
 * @since 0.4.0
 */
interface ChecksumWriter {

    /**
     * Writes a matching checksum into a summary report for valid checksums.
     *
     * @param expectedChecksum The checksum that was expected from the file.
     * @param computedChecksum The checksum that was calculated from the file.
     * @param fileLocation The file location for which the checksum was compared.
     */
    void reportValidChecksum(String expectedChecksum, String computedChecksum, URL fileLocation)

    /**
     * Writes a invalid checksum into a summary report for invalid checksums.
     * @param expectedChecksum The checksum that was expected from the file.
     * @param computedChecksum The checksum that was calculated from the file.
     * @param fileLocation The file location for which the checksum was compared.
     */
    void writeInvalidChecksum(String expectedChecksum, String computedChecksum, URL fileLocation)

}
