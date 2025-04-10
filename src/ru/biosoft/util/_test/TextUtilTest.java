package ru.biosoft.util._test;

import java.util.regex.Pattern;

import junit.framework.TestCase;
import ru.biosoft.util.TextUtil;
import ru.biosoft.util.TextUtil2;
import ru.biosoft.util.bean.BeanInfoEx2;
import ru.biosoft.util.bean.JSONBean;

/**
 * @author lan
 *
 */
public class TextUtilTest extends TestCase
{
    public void testInsertBreaks()
    {
        assertEquals("xxxxxxxxxxx,xxxxxxxx,xxxxxxxxx,xxxxxxxxx,xxxxxxxxxx, xxxxxxxxxx,xxxxxxxxxx,xxxxxxxxxxxx",
                TextUtil2.insertBreaks( "xxxxxxxxxxx,xxxxxxxx,xxxxxxxxx,xxxxxxxxx,xxxxxxxxxx,xxxxxxxxxx,xxxxxxxxxx,xxxxxxxxxxxx" ));
        assertEquals("xxxxxxxxxxx,xxxxxxxx,xxxxxxxxx,xxxxxxxxx,xxxxxxxxxx, "
                + "xxxxxxxxxx,xxxxxxxxxx,xxxxxxxxxxxxxxxxxxxxx,xxxxxxxxx, "
                + "xxxxxxxxxx,xxxxxxxxxx,xxxxxxxxxx,xxxxxxxxxxxxxxxxxxxxx, "
                + "xxxxxxxxx,xxxxxxxxxx,xxxxxxxxxx,xxxxxxxxxx,xxxxxxxxxxxx",
                TextUtil2.insertBreaks( "xxxxxxxxxxx,xxxxxxxx,xxxxxxxxx,xxxxxxxxx,xxxxxxxxxx,"
                        + "xxxxxxxxxx,xxxxxxxxxx,xxxxxxxxxxxxxxxxxxxxx,xxxxxxxxx,"
                        + "xxxxxxxxxx,xxxxxxxxxx,xxxxxxxxxx,xxxxxxxxxxxxxxxxxxxxx,"
                        + "xxxxxxxxx,xxxxxxxxxx,xxxxxxxxxx,xxxxxxxxxx,xxxxxxxxxxxx" ));
        assertEquals("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxA Bxxxxxxxxxxxxxxxxxx ddd",
                TextUtil2.insertBreaks( "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxABxxxxxxxxxxxxxxxxxx ddd" ));
        assertEquals("Axxxxxxxxxx,xxxxxxxx,xxxxxxxxx,xxxxxxxxx,xxxxxxxxxx, "
                + "Axxxxxxxxx,xxxxxxxxxx,xxxxxxxxxxxxxxxxxxxxx,xxxxxxxxx, "
                + "AxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxB "
                + "Axxx,xxxxxxxxxx,xxxxxxxxxx,xxxxxxxxxx,xxxxxxxxxxxx",
                TextUtil2.insertBreaks( "Axxxxxxxxxx,xxxxxxxx,xxxxxxxxx,xxxxxxxxx,xxxxxxxxxx,"
                        + "Axxxxxxxxx,xxxxxxxxxx,xxxxxxxxxxxxxxxxxxxxx,xxxxxxxxx,"
                        + "AxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxB"
                        + "Axxx,xxxxxxxxxx,xxxxxxxxxx,xxxxxxxxxx,xxxxxxxxxxxx" ));

        assertEquals( "V$AFP1_Q6,V$ALX3_01,V$ALX3_03,V$ALX3_04,V$ALX4_02,V$ALX4_05, "
                + "V$AMEF2_Q6,V$APA1_02,V$ARNTL_01,V$ARX_01,V$ARX_03,V$ATF3_05, "
                + "V$ATF4_02,V$ATF4_Q5,V$ATF7_01,V$BARHL2_04,V$BARHL2_05, "
                + "V$BARX1_03,V$BARX1_04,V$BARX2_01,V$BATF3_01,V$BATF3_02, "
                + "V$BATF_02,V$BATF_03,V$BCL6B_05,V$BCL6B_06,V$BCL6_01,V$BCL6_ "
                + "02,V$BCL6_Q3_01,V$BHLHE23_01,V$BHLHE41_02,V$BLIMP1_02, "
                + "V$BRN2_01,V$BRN3B_01,V$BRN3B_Q2,V$BRN3C_01,V$BRN4_01,V$BSX_ "
                + "03,V$CART1_01,V$CART1_02,V$CART1_03,V$CDC5_01,V$CDP_02, "
                + "V$CDX1_01,V$CDX1_03,V$CDX1_Q5,V$CDX2_01,V$CDX2_03,V$CDX2_04, "
                + "V$CDX2_Q5,V$CDX2_Q5_01,V$CDX2_Q5_02,V$CDX_Q5,V$CEBPA_01, "
                + "V$CEBPA_03,V$CEBPA_Q4,V$CEBPA_Q6,V$CEBPB_01",
                TextUtil2.insertBreaks(
                        "V$AFP1_Q6,V$ALX3_01,V$ALX3_03,V$ALX3_04,V$ALX4_02,V$ALX4_05,V$AMEF2_Q6,V$APA1_02,V$ARNTL_01,V$ARX_01,"
                                + "V$ARX_03,V$ATF3_05,V$ATF4_02,V$ATF4_Q5,V$ATF7_01,V$BARHL2_04,V$BARHL2_05,V$BARX1_03,V$BARX1_04,V$BARX2_01,"
                                + "V$BATF3_01,V$BATF3_02,V$BATF_02,V$BATF_03,V$BCL6B_05,V$BCL6B_06,V$BCL6_01,V$BCL6_02,V$BCL6_Q3_01,V$BHLHE23_01,"
                                + "V$BHLHE41_02,V$BLIMP1_02,V$BRN2_01,V$BRN3B_01,V$BRN3B_Q2,V$BRN3C_01,V$BRN4_01,V$BSX_03,V$CART1_01,V$CART1_02,"
                                + "V$CART1_03,V$CDC5_01,V$CDP_02,V$CDX1_01,V$CDX1_03,V$CDX1_Q5,V$CDX2_01,V$CDX2_03,V$CDX2_04,V$CDX2_Q5,V$CDX2_Q5_01,"
                                + "V$CDX2_Q5_02,V$CDX_Q5,V$CEBPA_01,V$CEBPA_03,V$CEBPA_Q4,V$CEBPA_Q6,V$CEBPB_01" ) );
    }

    public void testToLower() throws Exception
    {
        assertNull(TextUtil2.toLower(null));
        assertEquals("CMA", TextUtil2.toLower("CMA"));
        assertEquals("some text", TextUtil2.toLower("Some text"));
        assertEquals("use HTTP", TextUtil2.toLower("Use HTTP"));
    }

    public void testWildcardToRegex() throws Exception
    {
        assertNull(TextUtil2.wildcardToRegex(null));

        Pattern pattern = Pattern.compile(TextUtil2.wildcardToRegex("*.*"));
        assertFalse(pattern.matcher("test").matches());
        assertTrue(pattern.matcher(".").matches());
        assertTrue(pattern.matcher("test.west").matches());
        assertTrue(pattern.matcher(".west").matches());
        assertTrue(pattern.matcher("test.").matches());

        pattern = Pattern.compile(TextUtil2.wildcardToRegex("test"));
        assertFalse(pattern.matcher("test.").matches());
        assertTrue(pattern.matcher("test").matches());

        pattern = Pattern.compile(TextUtil2.wildcardToRegex("test?"));
        assertTrue(pattern.matcher("test.").matches());
        assertFalse(pattern.matcher("test").matches());

        pattern = Pattern.compile(TextUtil2.wildcardToRegex("????.???"));
        assertTrue(pattern.matcher("test.txt").matches());
        assertTrue(pattern.matcher("test.exe").matches());
        assertFalse(pattern.matcher("test.html").matches());
        assertFalse(pattern.matcher("testhtml").matches());
        assertFalse(pattern.matcher("testt.txt").matches());

        pattern = Pattern.compile(TextUtil2.wildcardToRegex("????.*"));
        assertTrue(pattern.matcher("test.txt").matches());
        assertTrue(pattern.matcher("test.exe").matches());
        assertTrue(pattern.matcher("test.html").matches());
        assertFalse(pattern.matcher("testhtml").matches());
        assertFalse(pattern.matcher("testt.txt").matches());
    }

    public void testFormatSize()
    {
        assertEquals( "0 bytes", TextUtil.formatSize( 0 ) );
        assertEquals( "1 byte", TextUtil.formatSize( 1 ) );
        assertEquals( "2 bytes", TextUtil.formatSize( 2 ) );
        assertEquals( "1,023 bytes", TextUtil.formatSize( 1023 ) );
        assertEquals( "1.0kb (1,024 bytes)", TextUtil.formatSize( 1024 ) );
        assertEquals( "9.5Mb (10,000,000 bytes)", TextUtil.formatSize( 10000000 ) );
        assertEquals( "9.1Tb (10,000,000,000,000 bytes)", TextUtil.formatSize( 10000000000000l ) );
    }

    public void testJSONBeanWithNull()
    {
        Bean bean = new Bean();
        String text = TextUtil2.toString( bean );
        assertEquals("{\"field\":null}", text);
        Bean bean2 = (Bean)TextUtil2.fromString( Bean.class, text );
        assertNotNull(bean2);
        assertNull(bean2.getField());
    }

    public void testArrayWithNull()
    {
        String[] array = {"a", null, "b"};
        String text = TextUtil2.toString( array );
        assertEquals( "[\"a\",null,\"b\"]", text );
        String[] array2 = (String[])TextUtil2.fromString( String[].class, text );
        assertNotNull( array2 );
        assertEquals( 3, array2.length );
        assertEquals( "a", array2[0] );
        assertNull( array2[1] );
        assertEquals( "b", array2[2] );
    }

    public static class Bean implements JSONBean
    {
        private String field;

        public String getField()
        {
            return field;
        }

        public void setField(String field)
        {
            this.field = field;
        }
    }

    public static class BeanBeanInfo extends BeanInfoEx2<Bean>
    {
        public BeanBeanInfo()
        {
            super( Bean.class );
        }

        @Override
        protected void initProperties() throws Exception
        {
            add("field");
        }
    }

}
