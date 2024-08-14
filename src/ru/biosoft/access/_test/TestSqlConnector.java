package ru.biosoft.access._test;

import ru.biosoft.access.sql.Connectors;
import junit.framework.TestCase;

public class TestSqlConnector extends TestCase
{
    public void testConnector()
    {
        Connectors.getConnection( "geneways" );
        Connectors.getConnection( "reactome" );
        Connectors.getConnection( "ensembl_human_52" );
        Connectors.getConnection( "ensembl_mouse_71" );
    }
}
