package life.qbic

import java.nio.file.Path

/**
 * A small helper class that simplifies to write files out of Java source code
 * with the JDK enhancements of Groovy.
 *
 * @author: Sven Fillinger
 */
class WriterHelper {

    static writeCheckSum(Path filePath, String content) {
        def newFile = new File(filePath.toString())
        newFile.withWriter {
            it.write(content)
        }
    }

}
