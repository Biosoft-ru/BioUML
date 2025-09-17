package biouml.plugins.gtrd._test;

import org.apache.commons.text.StringEscapeUtils;

import junit.framework.TestCase;

public class TestUnescapeHTML extends TestCase
{
    public void test1()
    {
        assertEquals( "ERα", StringEscapeUtils.unescapeHtml4( "ER&#945;" ) );
    }
}
