package biouml.plugins.modelreduction._test;

import java.io.BufferedWriter;
import java.io.File;
import java.util.List;

import com.developmentontheedge.application.ApplicationUtils;

import biouml.model.Diagram;
import biouml.model.SubDiagram;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.plot.PlotInfo;
import biouml.model.dynamics.Variable;
import biouml.plugins.agentmodeling.AgentModelSimulationEngine;
import biouml.plugins.modelreduction.SensitivityAnalysis;
import biouml.plugins.modelreduction.SensitivityAnalysis.SensitivityAnalysisResults;
import biouml.plugins.modelreduction.VariableSet;
import biouml.plugins.simulation.Model;
import biouml.plugins.simulation.ResultPlotPane;
import biouml.plugins.simulation.SimulationEngine;
import biouml.standard.diagram.DiagramUtility;
import biouml.standard.diagram.Util;
import biouml.standard.simulation.ResultListener;
import biouml.standard.simulation.SimulationResult;
import junit.framework.Test;
import junit.framework.TestSuite;
import one.util.streamex.DoubleStreamEx;
import one.util.streamex.StreamEx;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;

public class TestSetValues extends AbstractBioUMLTest
{
    public TestSetValues(String name)
    {
        super( name );
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite( TestSetValues.class );
        return suite;
    }
    //
    public void test() throws Exception
    {
//        setValues("53.0");
//                checkPopulation();
        checkDiagram();
    }

    public void checkDiagram() throws Exception
    {
        Diagram diagram = getDiagram("data/Collaboration/Ilya/Data/CVS 2021", "agent");

        SimulationEngine engine = (SimulationEngine)DiagramUtility.getEngine(diagram);
        engine.setDiagram(diagram);

        PlotInfo[] plots = engine.getPlots();
        ResultListener[] listeners = new ResultListener[plots.length];
        for( int i = 0; i < plots.length; i++ )
            listeners[i] = new ResultPlotPane(engine, null, plots[i]);

        Model model = engine.createModel();
        engine.simulate(model, listeners);
    }
    
    public void checkPopulation() throws Exception
    {
        String fileName = "C:/Users/Ilya/Treatment/Nontreated_200_partients.txt";

        String resultName = "C:\\Users\\Ilya\\BloodVolume.txt";
        File resultFile = new File( resultName );
        resultFile.delete();
        //resultFile.mkdirs();
        resultFile.createNewFile();
        Diagram diagram = getDiagram( "data/Collaboration/Ilya/Data/CVS 2021", "heart" );
        
        SimulationEngine engine = (SimulationEngine)DiagramUtility.getEngine( diagram );

        File f = new File( fileName );
        List<String> patientsData = ApplicationUtils.readAsList( f );
        String[] header = patientsData.get( 0 ).split( "\t" ); //variable names in file
        for( int i = 1; i <patientsData.size(); i++ )
        {

            String data = patientsData.get( i );
            double[] patientsValues = StreamEx.of( data.split( "\t" ) ).mapToDouble( s -> Double.parseDouble( s ) ).toArray();
            double ID = Double.NaN;
            for( int j = 0; j < header.length; j++ )
            {
                if( header[j].contains( "time" ) ) //avoid initial time setting
                    continue;


                if( header[j].equals( "ID" ) )
                    ID = patientsValues[j];

                String varPath = header[j];
                //              varPath = varPath.replace( "Kidney_", "kidney/" );
                              varPath = varPath.replace( "heart/", "" );
                Variable var = Util.getVariable( diagram, varPath );//.replace( "\\", "/" ) );
                if( var == null )
                {
                    System.out.println( "Variable not found " + varPath );
                }
                else
                {
                    var.setInitialValue( patientsValues[j] );
                    if (varPath.equals("V"))
                    {
                        System.out.println("V "+patientsValues[j]);
                    }
                }
                    

                engine.setDiagram( diagram );
                engine.setCompletionTime( 1000 );
            }
            
            double v = diagram.getRole( EModel.class ).getVariable("V").getInitialValue();
            double ps = diagram.getRole( EModel.class ).getVariable("P_S").getInitialValue();
            SimulationResult result = new SimulationResult( null, "" );
Model model = engine.createModel();
            engine.simulate( model, result );

            double valuesPS = result.getValues( "P_S" )[result.getCount() - 1];
double valuesV = result.getValues( "V" )[result.getCount() - 1];
            
            
            BufferedWriter bw = ApplicationUtils.utfAppender( resultFile );
            bw.append( "\n" + ID+"\t"+ps+"\t"+valuesPS+"\t"+v+"\t"+valuesV);
            bw.close();
            //          SimulationEngine engine = DiagramUtility.getEngine( diagram );

            //          String[] names = engine.getVarPathIndexMapping().keySet().toArray( new String[engine.getVarPathIndexMapping().size()]  );
            //          double[][] vals = result.getValues(names);

        }

    }

    public static void setValues(int index, Diagram diagram, File source) throws Exception
    {
//        Diagram diagram = getDiagram( "data/Collaboration/Ilya/Data/Diagrams/CVS models/Fedors Disser/", "agent" );
        String file = "C:\\Users\\Ilya\\Treatment\\Nontreated_200_partients.txt";
        List<String> patientsData = ApplicationUtils.readAsList( new File( file ) );
        String[] header = patientsData.get( 0 ).split( "\t" ); //variable names in file
        String data = patientsData.get( index );
        double[] patientsValues = StreamEx.of( data.split( "\t" ) ).mapToDouble( s -> Double.parseDouble( s ) ).toArray();
        for( int j = 0; j < header.length; j++ )
        {
            if( header[j].contains( "time" ) ) //avoid initial time setting
                continue;

            if( header[j].equals( "ID" ) )
                continue;

            String varPath = header[j];
            Variable var = Util.getVariable( diagram, varPath );
            if( var == null )
            {
                System.out.println( "Variable not found " + header[j] );
            }
            else
                var.setInitialValue( patientsValues[j] );
        }
        
        for (SubDiagram subDiagram: Util.getSubDiagrams( diagram ))
        {
            Diagram innerDiagram = subDiagram.getDiagram();
            innerDiagram.save();
        }
    }
    
    public static void setValues(String line) throws Exception
    {
        //        Diagram diagram = getDiagram( "data/Collaboration (git)/Cardiovascular system/Complex model/", "Complex model new2" );

        Diagram diagram = getDiagram( "data/Collaboration/Ilya/Data/CVS 2021", "agent" );
        String file = "C:\\Users\\Ilya\\Treatment\\Nontreated_200_partients.txt";

        AgentModelSimulationEngine engine = (AgentModelSimulationEngine)DiagramUtility.getEngine( diagram );
        //        engine.setDiagram( diagram );
        //        engine.simulate();
        //        
        //        for( Node s : diagram.getNodes() )
        //        {
        //            if( s instanceof SubDiagram )
        //            {
        //                Diagram innerDiagram = ( (SubDiagram)s ).getDiagram();
        //
        //                for( Node node : innerDiagram.getNodes() )
        //                {
        //                    if( node.getRole() instanceof Equation
        //                            && ( (Equation)node.getRole() ).getType().equals( Equation.TYPE_INITIAL_ASSIGNMENT ) )
        //                    {
        //                        diagram.remove( node.getName() );
        //                        System.out.println( "Removed " + node.getRole( Equation.class ).getVariable() + "(0) = "
        //                                + node.getRole( Equation.class ).getFormula() );
        //                    }
        //                }
        //            }
        //        }
        //        
        List<String> patientsData = ApplicationUtils.readAsList( new File( file ) );
        int index = -1;
        String[] header = patientsData.get( 0 ).split( "\t" ); //variable names in file
                for( int i = 1; i < patientsData.size(); i++ )
                {
                    String id = patientsData.get( i ).split( "\t" )[0];
                    if (id.equals( line ))
                    {
                      index = i;
                      break;
                    }
                }
        String data = patientsData.get( index );
        double[] patientsValues = StreamEx.of( data.split( "\t" ) ).mapToDouble( s -> Double.parseDouble( s ) ).toArray();
        for( int j = 0; j < header.length; j++ )
        {
            if( header[j].contains( "time" ) ) //avoid initial time setting
                continue;

            if( header[j].equals( "ID" ) )
                continue;

            String varPath = header[j];
            //                varPath = varPath.replace( "Kidney\\", "kidney\\" );
            //                varPath = varPath.replace( "Heart\\", "heart\\" );
            Variable var = Util.getVariable( diagram, varPath );//.replace( "\\", "/" ) );
            if( var == null )
            {
                System.out.println( "Variable not found " + header[j] );
            }
            else
                var.setInitialValue( patientsValues[j] );
        }

        //            SimulationEngine engine = DiagramUtility.getEngine( diagram );
        //            engine.setDiagram( diagram );
        //            engine.setCompletionTime( 2E5 );
        //            SimulationResult result = new SimulationResult(null, "");
        //            engine.simulate( result );

        //            String[] names = engine.getVarPathIndexMapping().keySet().toArray( new String[engine.getVarPathIndexMapping().size()]  );
        //            double[][] vals = result.getValues(names);//.getValues( name );
        //            for (int j = 0; j<names.length; j++)
        //            {
        //                double[] varValues = vals[j];
        //                Variable var = Util.getVariable( diagram, names[j] );
        //                if (var == null)
        //                    System.out.println( names[j]+" not found" );
        //                else
        //                var.setInitialValue( varValues[varValues.length - 1] );
        //            }

        //            result.ge

                    for (SubDiagram subDiagram: Util.getSubDiagrams( diagram ))
                    {
                        Diagram innerDiagram = subDiagram.getDiagram();
//                        for( Node node : innerDiagram.getNodes() )
//                        {
//                            if( node.getRole() instanceof Equation
//                                    && ( (Equation)node.getRole() ).getType().equals( Equation.TYPE_INITIAL_ASSIGNMENT ) )
//                            {
//                                innerDiagram.remove( node.getName() );
//                                System.out.println( "Removed " + node.getRole( Equation.class ).getVariable() + "(0) = "
//                                        + node.getRole( Equation.class ).getFormula() );
//                            }
//                        }
//                        innerDiagram.getRole(EModel.class).setComment( "dsad" );
                        innerDiagram.save();
                    }
        //            diagram.save();
        //            return;
        //            runAnalysis( diagram );
        //        }
    }

    public void runAnalysis(Diagram diagram) throws Exception
    {
        SensitivityAnalysis analysis = new SensitivityAnalysis( null, "" );

        analysis.getParameters().setStartSearchTime( 50000 );
        analysis.getParameters().setValidationSize( 1 );
        analysis.getParameters().setAbsoluteTolerance( 1000 );
        analysis.getParameters().setDiagram( diagram );
        analysis.getParameters().getEngineWrapper().getEngine().setCompletionTime( 1E5 );
        //        analysis.getParameters().getEngineWrapper().getEngine().setTimeIncrement(0.1);

        analysis.getParameters().getEngineWrapper().getEngine();

        diagram.getRole( EModel.class ).detectVariableTypes();
        VariableSet inputSet1 = new VariableSet();
        VariableSet inputSet2 = new VariableSet();
        VariableSet targetSet1 = new VariableSet();
        analysis.getParameters().setInputVariables( new VariableSet[] {inputSet1, inputSet2} );
        analysis.getParameters().setTargetVariables( new VariableSet[] {targetSet1} );

        //                inputSet1.setSubdiagramName("kidney");
        //                inputSet1.setVariableNames(new String[] {VariableSet.CONSTANT_PARAMETERS});//VariableSet.CONSTANT_PARAMETERS});
        inputSet2.setDiagram( diagram );
        inputSet2.setSubdiagramName( "heart" );
        inputSet2.setVariableNames( new String[] {"VO_2_pre"} );//VariableSet.CONSTANT_PARAMETERS} );//.CONSTANT_PARAMETERS});

        targetSet1.setDiagram( diagram );
        targetSet1.setSubdiagramName( "heart" );
        targetSet1.setVariableNames( new String[] {"P_S", "P_D"} );
        VariableSet steadySet = new VariableSet();
        steadySet.setDiagram( diagram );
        analysis.getParameters().setVariableNames( new VariableSet[] {steadySet} );
        steadySet.setSubdiagramName( "heart" );
        steadySet.setVariableNames( new String[] {"P_S", "P_D"} );
        SensitivityAnalysisResults pResults = analysis.performAnalysis();

        double[][] unscaled = pResults.unscaledSensitivities;
        String[] parameters = pResults.parameters;
        String[] targets = pResults.targets;


        System.out.println( "Target" + "\t"
                + StreamEx.of( parameters ).map( s -> s.substring( s.lastIndexOf( "\\" ) + 1, s.length() ) ).joining( "\t" ) );
        for( int i = 0; i < targets.length; i++ )
        {
            String shortName = targets[i].substring( targets[i].lastIndexOf( "\\" ) + 1, targets[i].length() );
            System.out.println( shortName + "\t" + DoubleStreamEx.of( unscaled[i] ).joining( "\t" ) );
        }
    }

    private static Diagram getDiagram(String path, String name) throws Exception
    {
        CollectionFactory.createRepository( "../data_resources" );
        DataCollection collection = CollectionFactory.getDataCollection( path );
        DataElement de = collection.get( name );
        return (Diagram)de;
    }
}
