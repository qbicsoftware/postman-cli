package life.qbic.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FileUtilsTest {

    @Test
    @DisplayName("longestCommonPrefix is the directory of two files")
    void longestCommonPathIsTheDirectoryOfTwoFiles() {
        var p1 = Path.of("/some/path/to/dir/file1.txt");
        var p2 = Path.of("/some/path/to/dir/file2.txt");
        Assertions.assertEquals(Path.of("/some/path/to/dir"), FileUtils.longestCommonPrefix(p1, p2));
    }

    @Test
    @DisplayName("longestCommonPrefix is the longest common path of two directories")
    void longestCommonPathIsTheLongestCommonPathOfTwoDirectories() {
        var p1 = Path.of("/some/path/to/foo/test/dir1/");
        var p2 = Path.of("/some/path/to/bar/test/dir2/");
        assertEquals(Path.of("/some/path/to/"), FileUtils.longestCommonPrefix(p1, p2));
    }

    @Test
    @DisplayName("longestCommonPrefix is the longest common path of two files")
    void longestCommonPathIsTheLongestCommonPathOfTwoFiles() {
        var p1 = Path.of("/some/path/to/dir1/some/other/dir/file.txt");
        var p2 = Path.of("/some/path/to/dir2/some/other/dir/file2.txt");
        assertEquals(Path.of("/some/path/to/"), FileUtils.longestCommonPrefix(p1, p2));
    }
    @Test
    @DisplayName("longestCommonPrefix of the same path is the path")
    void longestCommonPathOfTheSamePathIsThePath() {
        Path expected = Path.of("/some/path/to/dir/file.txt");
        assertEquals(expected, FileUtils.longestCommonPrefix(expected, expected));
    }

    @Test
    @DisplayName("longestCommonPrefix of a file and its parent directory is the parent directory")
    void longestCommonPathOfAFileAndItsParentDirectoryIsTheParentDirectory() {
        var p1 = Path.of("/some/path/to/dir/file.txt");
        var p2 = Path.of("/some/path/to/dir/");
        assertEquals(Path.of("/some/path/to/dir"), FileUtils.longestCommonPrefix(p1, p2));
    }
}
