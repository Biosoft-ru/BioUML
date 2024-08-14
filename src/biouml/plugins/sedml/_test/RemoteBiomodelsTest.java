package biouml.plugins.sedml._test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.beans.Preferences;

import biouml.model.Diagram;
import biouml.model.Module;
import biouml.model.util.DiagramXmlReader;
import biouml.model.util.DiagramXmlWriter;
import biouml.standard.type.DiagramInfo;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import ru.biosoft.access.core.CollectionFactory;

public class RemoteBiomodelsTest extends TestCase
{
    public RemoteBiomodelsTest(String name)
    {
        super(name);
    }

    public static void main(String[] args)
    {
        junit.textui.TestRunner.run(RemoteBiomodelsTest.class);
    }

    public static junit.framework.Test suite()
    {
        TestSuite suite = new TestSuite(RemoteBiomodelsTest.class.getName());
        suite.addTest(new RemoteBiomodelsTest("testRemoteDiagram"));
        return suite;
    }

    public static junit.framework.Test suiteAuto()
    {
        return suite();
    }

    public void testRemoteDiagram() throws Exception
    {
        Application.setPreferences(new Preferences());

        CollectionFactory.createRepository("../data");
        Diagram localDiagram = (Diagram)CollectionFactory.getDataElement("databases/Biomodels-local/Diagrams/BIOMD0000000012");
        assertNotNull("Cannot load local diagram", localDiagram);
        //Diagram remoteDiagram = (Diagram)CollectionFactory.getDataElement("databases/Biomodels/Diagrams/BIOMD0000000012");
        //assertNotNull("Cannot load remote diagram", remoteDiagram);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DiagramXmlWriter writer = new DiagramXmlWriter(baos);
        writer.write(localDiagram);
        Diagram remoteDiagram = DiagramXmlReader.readDiagram(localDiagram.getName(), new ByteArrayInputStream(baos.toByteArray()),
                (DiagramInfo)localDiagram.getKernel(), localDiagram.getOrigin(), Module.getModule(localDiagram));
        assertNotNull("Cannot load remote diagram", remoteDiagram);
    }
}
