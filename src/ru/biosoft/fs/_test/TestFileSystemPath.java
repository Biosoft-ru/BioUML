package ru.biosoft.fs._test;

import java.util.Arrays;

import junit.framework.TestCase;
import ru.biosoft.fs.FileSystemPath;

public class TestFileSystemPath extends TestCase
{
    public void testFileSystemPath()
    {
        FileSystemPath path = FileSystemPath.of().child( "test" ).child( "me/ga" ).child( "path" );
        assertTrue(Arrays.equals( new String[] {"test",  "me/ga", "path" }, path.components() ));
        FileSystemPath path2 = FileSystemPath.of("test", "me/ga", "path");
        assertEquals(path, path2);
        assertTrue(Arrays.equals( new String[] {"test",  "me/ga", "path" }, path2.components() ));
        assertTrue(Arrays.equals( new String[] {}, FileSystemPath.of().components() ));
    }
}
