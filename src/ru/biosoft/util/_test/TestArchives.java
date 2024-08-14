package ru.biosoft.util._test;

import java.io.BufferedInputStream;
import java.io.File;

import ru.biosoft.util.archive.ArchiveEntry;
import ru.biosoft.util.archive.ArchiveFactory;
import ru.biosoft.util.archive.ArchiveFile;
import ru.biosoft.util.archive.ComplexArchiveFile;

import junit.framework.TestCase;

/**
 * @author lan
 *
 */
public class TestArchives extends TestCase
{
    private static final File BASE = new File("ru/biosoft/util/_test/resources");
    
    private void checkArchive(String file, boolean checkSize, boolean complex) throws Exception
    {
        ArchiveFile archiveFile = ArchiveFactory.getArchiveFile(new File(BASE, file));
        if(complex) archiveFile = new ComplexArchiveFile(archiveFile);
        assertNotNull(archiveFile);
        assertTrue(archiveFile.isValid());
        ArchiveEntry entry = archiveFile.getNextEntry();
        assertNotNull(entry);
        assertEquals("simple.txt", entry.getName());
        assertFalse(entry.isDirectory());
        if(checkSize)
            assertEquals(11, entry.getSize());
        else
            assertEquals(-1, entry.getSize());
        try (BufferedInputStream inputStream = entry.getInputStream())
        {
            byte[] b = new byte[9];
            assertEquals(b.length, inputStream.read( b ));
            inputStream.close();
            assertEquals( "text file", new String( b ) );
        }
        archiveFile.close();
    }
    
    public void testGZip() throws Exception
    {
        checkArchive("simple_gz.dat", false, false);
    }
    
    public void testTar() throws Exception
    {
        checkArchive("simple_tar.dat", true, false);
    }
    
    public void testZip() throws Exception
    {
        checkArchive("simple_zip.dat", true, false);
    }
    
    public void testComplex() throws Exception
    {
        checkArchive("complex.dat", false, true);
    }
}
