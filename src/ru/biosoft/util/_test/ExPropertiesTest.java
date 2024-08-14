package ru.biosoft.util._test;

import java.util.HashMap;
import java.util.Map;

import one.util.streamex.EntryStream;
import ru.biosoft.util.ExProperties;
import junit.framework.TestCase;

public class ExPropertiesTest extends TestCase
{
    public void testSubProperties()
    {
        ExProperties prop = new ExProperties();
        prop.put( "bioHub.speciesSpecificTF", "biouml.plugins.transpath.biohub.TranspathTFSpecificHub;name=Transpath (TF specific)" );
        prop.put( "bioHub.sql", "biouml.plugins.transpath.biohub.TranspathSqlHub;name=Transpath sql;hubDepth=3" );
        prop.put( "bioHub.transfacProtein",
                "biouml.plugins.transpath.biohub.TransfacProteinHub;transfacFactorsCollection = databases/TRANSFAC(R) 2015.2/Data/factor" );
        prop.put( "bioHub.uniprot", "biouml.plugins.transpath.biohub.TranspathUniprotHub" );
        prop.put( "anotherProp", "test" );
        prop.put( "bioHub", "extra" );

        Map<String, Map<String, String>> actual = prop.getSubProperties( "bioHub" );
        Map<String, Map<String, String>> expected = new HashMap<>();
        expected.put( "speciesSpecificTF",
                EntryStream.of( "default", "biouml.plugins.transpath.biohub.TranspathTFSpecificHub", "name", "Transpath (TF specific)" )
                        .toMap() );
        expected.put( "sql",
                EntryStream.of( "default", "biouml.plugins.transpath.biohub.TranspathSqlHub", "name", "Transpath sql", "hubDepth", "3" )
                        .toMap() );
        expected.put(
                "transfacProtein",
                EntryStream.of( "default", "biouml.plugins.transpath.biohub.TransfacProteinHub", "transfacFactorsCollection",
                        "databases/TRANSFAC(R) 2015.2/Data/factor" ).toMap() );
        expected.put( "uniprot", EntryStream.of( "default", "biouml.plugins.transpath.biohub.TranspathUniprotHub" ).toMap() );
        assertEquals( actual, expected );
    }
}
