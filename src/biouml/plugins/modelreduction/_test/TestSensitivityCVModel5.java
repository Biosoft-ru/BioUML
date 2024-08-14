package biouml.plugins.modelreduction._test;

import java.io.BufferedWriter;
import java.io.File;
import java.util.logging.Level;

import com.developmentontheedge.application.ApplicationUtils;

import biouml.model.Diagram;
import biouml.plugins.agentmodeling.AgentModelSimulationEngine;
import biouml.plugins.modelreduction.SensitivityAnalysis;
import biouml.plugins.modelreduction.SensitivityAnalysis.SensitivityAnalysisResults;
import biouml.plugins.modelreduction.VariableSet;
import biouml.standard.diagram.DiagramUtility;
import junit.framework.Test;
import junit.framework.TestSuite;
import one.util.streamex.DoubleStreamEx;
import one.util.streamex.StreamEx;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;

public class TestSensitivityCVModel5 extends AbstractBioUMLTest
{
    public TestSensitivityCVModel5(String name)
    {
        super( name );
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite( TestSensitivityCVModel5.class );
        return suite;
    }

//    public void copy(String from, String to) throws Exception
//    {
//        Diagram diagramFrom = getDiagram( "data/Collaboration/Ilya/Data/Diagrams/CVS models/Fedors Disser/", "agent" );
//        Diagram diagramTo = getDiagram( "data/Collaboration/Ilya/Data/Diagrams/CVS models/Fedors Disser/", "agent" );
//        
//       for (SubDiagram subDiagramFrom: Util.getSubDiagrams( diagramFrom))
//       {
//           SubDiagram subDiagramTo = (SubDiagram)diagramTo.get( subDiagramFrom.getName() );
//           
//           Diagram innerFrom = subDiagramFrom.getDiagram();
//           
//           Diagram innerTo = subDiagramTo.getDiagram();
//           
//           for (Variable varFrom: innerFrom.getRole( EModel.class ).getVariables())
//           {
//               Variable varTo = innerTo.getRole( EModel.class ).getVariable( varFrom.getName() );
//               varTo.setInitialValue( varFrom.getInitialValue() );
//           }
//       }
//       
//       diagramTo.save();
//    }
    
    public void test() throws Exception
    {
        Diagram diagram = getDiagram( "data/Collaboration/Ilya/Data/Diagrams/CVS models/Fedors Disser/", "agent" );

//        String file = "Selected";
//
//        List<String> patientsData = ApplicationUtils.readAsList( getTestFile( file ) );
//        String[] header = patientsData.get( 0 ).split( "\t" ); //variable names in file
//        for( int i = 1; i < patientsData.size(); i++ )
//        {
//
//            String data = patientsData.get( i );
//            double[] patientsValues = StreamEx.of( data.split( "\t" ) ).mapToDouble( s -> Double.parseDouble( s ) ).toArray();
//            for( int j = 0; j < header.length; j++ )
//            {
//                if( header[j].contains( "time" ) ) //avoid initial time setting
//                    continue;
//
//                if( header[j].equals( "ID" ) )
//                    continue;
//
//                String varPath = header[j];
//                Variable var = Util.getVariable( diagram, varPath.replace( "\\", "/" ) );
//                if( var == null )
//                {
//                    System.out.println( "Variable not found " + header[j] );
//                }
//                else
//                var.setInitialValue( patientsValues[j] );
        //            }

        String resultName = "/home/axec/axec/CVS/Data 2021/Treatment/Sensitivity_71";
        File f = new File(resultName);
        f.createNewFile();
        
//        result.append( "1E-6, 0.1, 1E5\n" );        
//        result.append( runAnalysis( diagram, 1E-6, 0.1, 1E5 ) );
//        
//        result.append( "\n-1E-6, -0.1, 1E5\n" );
//        result.append( runAnalysis( diagram, -1E-6, -0.1, 1E5 ));
//        
//        result.append( "\n1E-6, 0, 1E5\n" );
//        result.append( runAnalysis( diagram, 1E-6, 0, 1E5 ));
//        
//        result.append( "\n-1E-6, 0, 1E5\n" );
//        result.append( runAnalysis( diagram, -1E-6, 0, 1E5 ));
//        
//        result.append( "\n1E-6, 0.1, 1E5\n" );        
//        result.append( runAnalysis( diagram, 1E-6, 0.01, 1E5 ) );
//        
        StringBuffer result = new StringBuffer();
        for( int i = 1; i < 2; i++ )
        {
            try
            {
            TestSetValues.setValues("71" );
           
            result.append( "\n" + i+ "\n" );
//            result.append( runAnalysis( diagram, 1E-6, 0.01, 1E6 ) );
//            result.append( "\n" );
            
//            TestSetValues.setValues( i );
            result.append( runAnalysis( diagram, 2E-6, 0.01, 2E6 ) );
            result.append( "\n" );
            
            BufferedWriter bw = ApplicationUtils.utfAppender( f );
            bw.append( result.toString() );
            System.out.println( result.toString() );
            bw.close();
            }
            catch (Exception ex)
            {
                System.out.println( ex.getMessage() );
            }
        }
//        result.append( "\n1E-6, 0.1, 2E6\n" );
//        result.append( runAnalysis( diagram, 1E-6, 0.01, 2E6 ));
//        
//        result.append( "\nE-6, 0.1, 3E6\n" );
//        result.append( runAnalysis( diagram, 1E-6, 0.01, 3E6 ));
        
//        result.append( "\n1E-6, 0.1, 1E7\n" );
//        result.append( runAnalysis( diagram, 1E-6, 0.1, 1E7 ));
        
        
//        result.append( "\n-1E-7, -0.1, 1E6\n" );
//        result.append( runAnalysis( diagram, -1E-6, 0, 1E6 ));
        
//        System.out.println( result );
        //        }
    }

    public String runAnalysis(Diagram diagram, double aStep, double rStep, double time) throws Exception
    {
//                System.out.println(    engine.getEngines()[0].getStepType());
//                engine.getEngines()[0].getTimeIncrement();
//        System.out.println(engine.getEngines()[1].getStepType());
        
        SensitivityAnalysis analysis = new SensitivityAnalysis( null, "" );

        analysis.getParameters().setEngine( DiagramUtility.getEngine( diagram ) );
        analysis.getParameters().setStartSearchTime( time );
        analysis.getParameters().setValidationSize( 2 );
        analysis.getParameters().setAbsoluteTolerance( 10 );
        analysis.getParameters().setDiagram( diagram );
        analysis.getParameters().setAbsoluteStep( aStep );
        analysis.getParameters().setRelativeStep( rStep );
        
        AgentModelSimulationEngine engine = (AgentModelSimulationEngine)DiagramUtility.getEngine( diagram );
        engine.setDiagram( diagram );
        engine.setCompletionTime( 3E7 );
        engine.setTimeIncrement( 6000 );
        
        engine.setLogLevel( Level.OFF );
        analysis.getParameters().getEngineWrapper().setEngine( engine );
        
//        analysis.getParameters().getEngineWrapper().getEngine().setCompletionTime( 2E5 );
        //        analysis.getParameters().getEngineWrapper().getEngine().setTimeIncrement(0.1);

//        AgentModelSimulationEngine engine = (AgentModelSimulationEngine)analysis.getParameters().getEngineWrapper().getEngine();
//System.out.println(    engine.getEngines()[0].getStepType());
//System.out.println(engine.getEngines()[1].getStepType());
//System.out.println(engine.getEngines()[2].getStepType());
        VariableSet inputSet1 = new VariableSet();
        VariableSet inputSet2 = new VariableSet();
        VariableSet targetSet1 = new VariableSet();
        analysis.getParameters().setInputVariables( new VariableSet[] {inputSet1, inputSet2} );
        analysis.getParameters().setTargetVariables( new VariableSet[] {targetSet1} );
        inputSet1.setDiagram( diagram );
                inputSet1.setSubdiagramName("kidney");
                inputSet1.setVariableNames(new String[] {VariableSet.CONSTANT_PARAMETERS, VariableSet.RATE_VARIABLES});
        inputSet2.setDiagram( diagram );
        inputSet2.setSubdiagramName( "heart" );
        inputSet2.setVariableNames( new String[] {VariableSet.CONSTANT_PARAMETERS, VariableSet.RATE_VARIABLES});

        targetSet1.setDiagram( diagram );
        targetSet1.setSubdiagramName( "heart" );
        targetSet1.setVariableNames( new String[] {"P_S", "P_D"} );
        VariableSet steadySet = new VariableSet();
        steadySet.setDiagram( diagram );
        analysis.getParameters().setVariableNames( new VariableSet[] {steadySet} );
        steadySet.setSubdiagramName( "heart" );
        steadySet.setVariableNames( new String[] {"P_S", "P_D"} );
        SensitivityAnalysisResults pResults = analysis.performAnalysis();

        double[][] unscaled = pResults.differences;
        String[] parameters = pResults.parameters;
        String[] targets = pResults.targets;


        StringBuffer result = new StringBuffer();
        result.append( "Target" + "\t"
                + StreamEx.of( parameters ).map( s -> s.substring( s.lastIndexOf( "\\" ) + 1, s.length() ) ).joining( "\t" ) );
        for( int i = 0; i < targets.length; i++ )
        {
            String shortName = targets[i].substring( targets[i].lastIndexOf( "\\" ) + 1, targets[i].length() );
            result.append( "\n");
            result.append( shortName + "\t" + DoubleStreamEx.of( unscaled[i] ).joining( "\t" ) );
        }
        return result.toString();
    }

    private Diagram getDiagram(String path, String name) throws Exception
    {
        CollectionFactory.createRepository( "../data_resources" );
        DataCollection collection = CollectionFactory.getDataCollection( path );
        DataElement de = collection.get( name );
        return (Diagram)de;
    }
}
