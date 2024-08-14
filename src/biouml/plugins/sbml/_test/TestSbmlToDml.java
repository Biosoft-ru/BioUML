package biouml.plugins.sbml._test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import biouml.model.Diagram;
import biouml.model.util.DiagramXmlReader;
import biouml.model.util.DiagramXmlWriter;
import biouml.plugins.sbml.SbmlModelFactory;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.util.TempFiles;

/**
 * @author lan
 *
 */
public class TestSbmlToDml extends AbstractBioUMLTest
{
    public void testSbmlToDml() throws Exception
    {
        File file = TempFiles.file(".xml", TestSbmlToDml.class.getResourceAsStream("BIOMD0000000008.xml"));
        Diagram diagram = SbmlModelFactory.readDiagram(file, null, "test");
        File output = TempFiles.file(".dml");
        try (FileOutputStream outStream = new FileOutputStream( output ))
        {
            new DiagramXmlWriter( outStream ).write( diagram );
        }
        FileInputStream inStream = new FileInputStream(output);
        Diagram diagram2 = DiagramXmlReader.readDiagram("qqq", inStream, null, null, null);
        assertEquals(diagram.getSize(), diagram2.getSize());
        assertEquals(diagram.getNameList(), diagram2.getNameList());
    }
}
