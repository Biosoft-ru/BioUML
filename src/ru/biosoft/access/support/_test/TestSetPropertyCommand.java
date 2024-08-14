package ru.biosoft.access.support._test;

import junit.framework.TestCase;
import ru.biosoft.access.support.SetPropertyCommand;

import com.developmentontheedge.beans.PropertyDescriptorEx;

public class TestSetPropertyCommand extends TestCase
{
    public TestSetPropertyCommand( String name )
    {
        super(name);
    }

    /**
     *
     * @pending
     */
    public void testGetTaggedValue() throws Exception
    {
        SetPropertyCommand command = new SetPropertyCommand("FT",new PropertyDescriptorEx("setProperty"),null);
        String value = null;
        value = command.getTaggedValue("");
        assertEquals( "Wrong length",4+endl.length(),value.length() );
    }

    private static String endl = System.getProperty("line.separator");

}