package ru.biosoft.util._test;

import ru.biosoft.util.HtmlUtil;
import junit.framework.TestCase;

/**
 * @author lan
 *
 */
public class HtmlUtilTest extends TestCase
{
    public void testStripHtml() throws Exception
    {
        assertEquals(HtmlUtil.stripHtml("test"), "test");
        assertEquals(HtmlUtil.stripHtml("<b>blahblah</b>"), "blahblah");
        assertEquals(HtmlUtil.stripHtml("<b>&amp;blah</b>"), "&blah");
        assertEquals(HtmlUtil.stripHtml("<b>&lt;blah&gt;</b>"), "<blah>");
        assertEquals(HtmlUtil.stripHtml("&quot;&#32;&#x20;"), "\"  ");
    }
    
    public void testConvertToText() throws Exception
    {
        assertEquals("Header\n\nBody", HtmlUtil.convertToText("<h1>Header</h1><p>Body</p>"));
    }
}
