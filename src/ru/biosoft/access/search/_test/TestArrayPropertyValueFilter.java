package ru.biosoft.access.search._test;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;

import junit.framework.TestCase;
import ru.biosoft.access.search.ArrayPropertyValueFilter;

public class TestArrayPropertyValueFilter extends TestCase
{
    public TestArrayPropertyValueFilter( String name )
    {
        super(name);
    }

    public void testStringArrayFiltering() throws Exception
    {
        PropertyDescriptor descriptor = null;
        BeanInfo bi = Introspector.getBeanInfo( ArrayBean.class );
        PropertyDescriptor[] pds = bi.getPropertyDescriptors();
        for (int i = 0; descriptor==null && i<pds.length; i++)
        {
            if( pds[i].getName().equalsIgnoreCase("array") )
                descriptor = pds[i];
        }

        assertTrue( "Property descriptor must be nonnull", descriptor != null );
        ArrayPropertyValueFilter filter = new ArrayPropertyValueFilter( descriptor );
//        String[] testArray = {"a","b"};
        filter.setValue( "b" );
        ArrayBean el_01 = new ArrayBean(null,"bean 01");
        el_01.setArray( new String[] {"a","b"} );
        ArrayBean el_02 = new ArrayBean(null,"bean 02");
        el_02.setArray( new String[] {"a","c"} );
        assertTrue( "Filter should accept this element "+el_01.getName(),filter.isAcceptable(el_01) );
        assertTrue( "Filter should reject this element "+el_02.getName(),!filter.isAcceptable(el_02) );
    }
}