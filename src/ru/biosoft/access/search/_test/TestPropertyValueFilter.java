package ru.biosoft.access.search._test;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.stream.Stream;

import junit.framework.TestCase;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.search.PropertyValueFilter;

public class TestPropertyValueFilter extends TestCase
{
    public TestPropertyValueFilter( String name )
    {
        super(name);
    }

    public void testIntegerFiltering() throws Exception
    {
        BeanInfo bi = Introspector.getBeanInfo( SimpleBean.class );
        PropertyDescriptor[] pds = bi.getPropertyDescriptors();
        PropertyDescriptor descriptor = Stream.of( pds ).filter( pd -> pd.getName().equalsIgnoreCase( "id" ) ).findFirst().get();

        PropertyValueFilter filter = new PropertyValueFilter( descriptor );
        filter.setValue( 1 );
        DataElement el_01 = new SimpleBean(null,"bean 01",01);
        DataElement el_02 = new SimpleBean(null,"bean 02",02);
        assertTrue( "Filter should accept this element "+el_01.getName(),filter.isAcceptable(el_01) );
        assertTrue( "Filter should reject this element "+el_02.getName(),!filter.isAcceptable(el_02) );
    }
}