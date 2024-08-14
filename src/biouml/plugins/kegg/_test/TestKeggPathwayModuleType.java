package biouml.plugins.kegg._test;

import junit.framework.TestCase;
import biouml.plugins.kegg.KeggPathwayModuleType;

public class TestKeggPathwayModuleType extends TestCase
{
    public void testInstantiate() throws Exception
    {
        Class<?> clazz = Class.forName( "biouml.plugins.kegg.KeggPathwayModuleType" );
        Object moduleType = clazz.newInstance();
        assertTrue( "Wrong instantiation",moduleType instanceof KeggPathwayModuleType );
    }
}