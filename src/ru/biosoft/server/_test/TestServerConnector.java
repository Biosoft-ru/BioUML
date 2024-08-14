package ru.biosoft.server._test;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.server.ServerConnector;

/**
 * @author lan
 *
 */
public class TestServerConnector extends AbstractBioUMLTest
{
    public void testContent()
    {
        ServerConnector connector = new ServerConnector( "https://ict.biouml.org/biouml" );
        connector.login("", "");
        assertEquals("<p>See <a href=\"http://dx.doi.org/10.12704/vb/e8\">BioUML Genome Browser</a> paper for details.</p>", 
                connector.getContent(DataElementPath.create("data/Examples/GenomeBrowser/Data/Readme")));
        connector.logout();
    }
}
