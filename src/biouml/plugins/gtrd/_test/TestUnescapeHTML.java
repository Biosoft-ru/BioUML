package biouml.plugins.gtrd._test;

import org.apache.commons.lang.StringEscapeUtils;

import junit.framework.TestCase;

public class TestUnescapeHTML extends TestCase
{
    public void test1()
    {
        assertEquals( "ERα", StringEscapeUtils.unescapeHtml( "ER&#945;" ) );
    }
}
