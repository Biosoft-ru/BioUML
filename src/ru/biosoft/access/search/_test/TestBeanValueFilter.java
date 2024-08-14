package ru.biosoft.access.search._test;

import junit.framework.TestCase;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.search.BeanValueFilter;

public class TestBeanValueFilter extends TestCase
{
    public TestBeanValueFilter( String name )
    {
        super(name);
    }

    public void testSimpleFiltering() throws Exception
    {
        BeanValueFilter filter = new BeanValueFilter( SimpleBean.class );
        DataElement el_01 = new SimpleBean(null,"bean 01",01);
        assertTrue( "By default filter should accept any elements",filter.isAcceptable(el_01) );
    }

    public void testComplexFiltering() throws Exception
    {
        BeanValueFilter filter = new BeanValueFilter( ComplexBean.class );
        assertEquals( "BeanValue filter not filter all properties",5,filter.getFilter().length );

        ComplexBean el_01 = new ComplexBean(null,"bean 01");
        assertTrue( "By default filter should accept any elements : "+el_01.getName(),filter.isAcceptable(el_01) );
        ComplexBean el_02 = new ComplexBean(null,"bean 02");
        el_02.setArray( new String[] {"a","b"} );
        el_02.setId( 2 );
        el_02.setBean( new SimpleBean(null,"test",1) );
    }
}