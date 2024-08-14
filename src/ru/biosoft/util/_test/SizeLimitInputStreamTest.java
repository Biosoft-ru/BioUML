package ru.biosoft.util._test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import ru.biosoft.util.SizeLimitInputStream;

import junit.framework.TestCase;
import junit.framework.TestSuite;

public class SizeLimitInputStreamTest extends TestCase {

    public SizeLimitInputStreamTest(String name) {
        super(name);
    }
    
    public static junit.framework.Test suite()
    {
        TestSuite suite = new TestSuite(SizeLimitInputStreamTest.class.getName());
        suite.addTest(new SizeLimitInputStreamTest("testReadByteArrayIntInt"));
        return suite;
    }

    public void testReadByteArrayIntInt() throws IOException {
        try( InputStream primary = new ByteArrayInputStream( new byte[] {1, 2, 3, 4, 5, 6} );
                InputStream limited = new SizeLimitInputStream( primary, 3 ) )
        {
            byte[] buffer = new byte[4];

            assertEquals( 1, limited.read( buffer, 0, 1 ) );
            assertEquals( 1, buffer[0] );

            assertEquals( 2, limited.read( buffer, 1, 3 ) );
            assertEquals( 2, buffer[1] );
            assertEquals( 3, buffer[2] );

            assertEquals( -1, limited.read( buffer, 0, 1 ) );
            assertEquals( 0, limited.read( buffer, 0, 0 ) );
        }
    }

}
