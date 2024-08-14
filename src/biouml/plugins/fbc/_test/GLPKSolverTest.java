package biouml.plugins.fbc._test;

import biouml.model.Diagram;
import biouml.plugins.fbc.FbcModel;
import biouml.plugins.fbc.GLPKModelCreator;
import biouml.plugins.fbc.table.FbcBuilderDataTableAnalysis;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.table.TableDataCollection;


public class GLPKSolverTest extends AbstractBioUMLTest
{
    private static final String PATH = "data/Collaboration/AnnaTestProject/Data/FBC";

    public void testGLPKModelCreator() throws Exception
    {
        Diagram diagram = getDiagram();

        FbcBuilderDataTableAnalysis analysis = new FbcBuilderDataTableAnalysis( null, null );
        TableDataCollection table = analysis.getFbcData( diagram );

        GLPKModelCreator modelCreator = new GLPKModelCreator();
        FbcModel model = modelCreator.createModel( diagram, table, "maximize" );
        model.optimize();
        System.out.print( "Optimal value = " + model.getValueObjFunc() );

    }

    private Diagram getDiagram() throws Exception
    {
        CollectionFactory.createRepository( "../data_resources" );
        DataCollection<?> collection = CollectionFactory.getDataCollection( PATH );
        //Diagram diagram = (Diagram)collection.get( "fbc_diagram" );
        //Diagram diagram = (Diagram)collection.get( "iMK1321_Model2" );
        Diagram diagram = (Diagram)collection.get( "fbc_small" );
        //Diagram diagram = (Diagram)collection.get( "ScoreTestDiagram" );
        return diagram;
    }
}
