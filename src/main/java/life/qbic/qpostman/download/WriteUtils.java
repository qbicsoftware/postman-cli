package life.qbic.qpostman.download;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.zip.CRC32;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * TODO!
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
public class WriteUtils {

  private static final Logger log = LogManager.getLogger(WriteUtils.class);

  public static long write(int bufferSize, InputStream inputStream, OutputStream outputStream,
      WriteProgressListener progressListener)
      throws IOException {
    CRC32 crc32 = new CRC32();
    byte[] buffer = new byte[bufferSize];
    int bytesRead;
    while ((bytesRead = inputStream.read(buffer)) > 0) {
      crc32.update(buffer, 0, bytesRead);
      outputStream.write(buffer, 0, bytesRead);
      progressListener.update(bytesRead);
      outputStream.flush();
    }
    progressListener.finish();
    return crc32.getValue();
  }

  public static long readCrc32(Path file, int bufferSize) {
    if (!file.toFile().exists()) {
      throw new IllegalArgumentException("File " + file.toAbsolutePath() + " was expected but not found.");
    }
    return readCrc32FromFile(file).orElseGet(() -> calculateCrc32(file, bufferSize));
  }

  public static boolean doesExistWithCrc32(Path file, long expectedCrc32, int bufferSize) {
    return file.toFile().exists() && expectedCrc32 == readCrc32(file, bufferSize);
  }

  private static long calculateCrc32(Path file, int bufferSize) {
    byte[] buffer = new byte[bufferSize];
    try (InputStream inputStream = new FileInputStream(file.toFile())) {
      int bytesRead;
      CRC32 crc32 = new CRC32();
      while ((bytesRead = inputStream.read(buffer)) > 0) {
        crc32.update(buffer, 0, bytesRead);
      }
      return crc32.getValue();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static Optional<Long> readCrc32FromFile(Path file) {
    Path crc32FileName = Path.of(file.toAbsolutePath() + ".crc32");

    if (!crc32FileName.toFile().exists()) {
      return Optional.empty();
    }

    try (BufferedReader reader = Files.newBufferedReader(crc32FileName);) {
      String firstLine = reader.readLine();
      return Optional.ofNullable(firstLine)
          .map(line -> Long.parseLong(line.split("\\s+")[0], 16));
    } catch (IOException e) {
      log.warn("Could not open " + crc32FileName.getFileName());
      return Optional.empty();
    }
  }
}
