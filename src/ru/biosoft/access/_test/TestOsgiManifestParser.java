package ru.biosoft.access._test;

import java.util.Arrays;
import ru.biosoft.access.OsgiManifestParser;
import ru.biosoft.access.OsgiManifestParser.ParseException;
import junit.framework.TestCase;

public class TestOsgiManifestParser extends TestCase
{
    public void testParser() throws ParseException {
        assertEquals(Arrays.asList("biouml.plugins.bowtie"), OsgiManifestParser.getStrings( "biouml.plugins.bowtie\n" ));
        assertEquals(Arrays.asList("a.b.c", "a.b.c.d"), OsgiManifestParser.getStrings( "a.b.c,\na.b.c.d\n" ));
    }
}
