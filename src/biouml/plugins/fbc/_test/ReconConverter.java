package biouml.plugins.fbc._test;

import java.io.BufferedReader;
import java.util.HashMap;
import java.util.Map;

import com.developmentontheedge.application.ApplicationUtils;

import biouml.plugins.fbc.FbcConstant;
import biouml.plugins.fbc.analysis.ReconTransformerAnalysis;
import biouml.plugins.fbc.analysis.ReconTransformerParameters;
import junit.framework.TestSuite;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataElementPath;

public class ReconConverter extends AbstractBioUMLTest implements FbcConstant
{
    public ReconConverter(String name)
    {
        super(name);
    }

    public static TestSuite suite()
    {
        TestSuite suite = new TestSuite(ReconConverter.class.getName());
        suite.addTest(new ReconConverter("convert"));

        return suite;
    }

    final static DataElementPath COLLECTION_NAME = DataElementPath.create("databases/FluxBalance/Diagrams");


    public void convert() throws Exception
    {
        String FILE_PATH = "biouml/plugins/fbc/resources/Models.txt";

        Map<String, String> listObj = new HashMap<>();
        listObj.put("OBJF", "maximize");
        String repositoryPath = "../data";
        CollectionFactory.createRepository(repositoryPath);

        ReconTransformerAnalysis analysis = new ReconTransformerAnalysis( COLLECTION_NAME.getDataCollection(), "test" );
        ReconTransformerParameters parameters;
        String diagramName;
        try (BufferedReader input = ApplicationUtils.asciiReader( FILE_PATH ))
        {
            while( ( diagramName = input.readLine() ) != null )
            {
                parameters = new ReconTransformerParameters();
                parameters.setDiagramPath( COLLECTION_NAME.getChildPath( diagramName ) );

                analysis.setParameters( parameters );
                analysis.justAnalyzeAndPut();
            }
        }
    }
}
