package life.qbic

import java.nio.file.Path

/**
 * <add class description here>
 *
 * @author: Sven Fillinger
 */
class WriterHelper {

    static writeToFileSystem(Path filePath, String content) {
        def newFile = new File(filePath.toString())
        newFile.withWriter {
            it.write(content)
        }
    }

}
