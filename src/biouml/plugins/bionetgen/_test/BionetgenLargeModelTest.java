package biouml.plugins.bionetgen._test;

import java.awt.Point;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import junit.framework.Test;
import junit.framework.TestSuite;
import biouml.model.DefaultSemanticController;
import biouml.model.Diagram;
import biouml.model.Node;
import biouml.model.dynamics.Assignment;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Equation;
import biouml.model.dynamics.Event;
import biouml.model.dynamics.plot.PlotsInfo;
import biouml.model.dynamics.Variable;
import biouml.model.dynamics.VariableRole;
import biouml.model.dynamics.plot.Curve;
import biouml.model.dynamics.plot.PlotInfo;
import biouml.model.util.DiagramXmlWriter;
import biouml.plugins.bionetgen.diagram.BionetgenConstants;
import biouml.plugins.bionetgen.diagram.BionetgenToGlycanConverter;
import biouml.plugins.bionetgen.diagram.BionetgenDiagramDeployer;
import biouml.plugins.simulation.ArraySpan;
import biouml.plugins.simulation.java.JavaSimulationEngine;
import biouml.plugins.simulation.ode.jvode.JVodeOptions;
import biouml.plugins.simulation.ode.jvode.JVodeSolver;
import biouml.plugins.simulation.ode.jvode.JVodeSupport;
import biouml.standard.simulation.SimulationResult;
import biouml.standard.diagram.DiagramUtility;
import biouml.standard.type.Base;
import biouml.standard.type.SpecieReference;
import biouml.standard.type.Stub;
import biouml.standard.type.Type;

import com.developmentontheedge.beans.DynamicProperty;

public class BionetgenLargeModelTest extends BionetgenDiagramGeneratorTest
{
    public BionetgenLargeModelTest(String name)
    {
        super( name );
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite( BionetgenLargeModelTest.class.getName() );

        suite.addTest( new BionetgenLargeModelTest( "testDiagram_fceri_fyn_lig" ) );
        suite.addTest( new BionetgenLargeModelTest( "testDiagram_glycobiology" ) );
        suite.addTest( new BionetgenLargeModelTest( "testSimulation_glycobiology" ) );

        return suite;
    }

    protected String directory = testDirectory + "../results/temp/";
    protected String outputDir = directory + "java_out/";

    public void testDiagram_glycobiology() throws Exception
    {
        File file = new File( directory );
        if( !file.exists() && !file.mkdirs() )
            throw new Exception( "Failed to create directory" );
        file = new File( directory + "glycobiology_type0.xml" );
        FileOutputStream stream = new FileOutputStream( file );
        DiagramXmlWriter dXmlW = new DiagramXmlWriter( stream );
        BionetgenTestUtility.initPreferences();

        long time = System.currentTimeMillis();
        Diagram diagram = generateDiagram( "glycobiology", false );
        try
        {
            diagram = BionetgenDiagramDeployer.deployBNGDiagram( diagram, false );
            time = System.currentTimeMillis() - time;
            System.out.println( "Total time spent: " + ( (double)time / 1000 ) + "s." );
            writeReport( diagram, time );
            addStates( diagram );
            dXmlW.write( diagram );
            System.out.println( "File has been written: " + file );
            System.out.println();
        }
        catch( Throwable t )
        {
            System.out.println( "Error during diagram convertation: " + t.getMessage() );
            throw t;
        }
    }

    public void testSimulation_glycobiology() throws Exception
    {
        BionetgenTestUtility.initPreferences();
        System.out.println( "Started" );
        String modelName = "glycobiology";
        Diagram diagram = generateDiagram( modelName, false );
        long time = System.currentTimeMillis();
        try
        {
            diagram = BionetgenDiagramDeployer.deployBNGDiagram( diagram, false );
            addStates( diagram );
            time = System.currentTimeMillis() - time;
            System.out.println( "Convertation time: " + ( (double)time / 1000 ) + "s." );
            writeReport( diagram, time );
        }
        catch( Throwable t )
        {
            System.out.println( "Error during diagram convertation: " + t.getMessage() );
            t.printStackTrace();
            throw t;
        }

        double[] timePoints = new double[225];
        for( int i = 0; i <= 224; i++ )
        {
            timePoints[i] = i / 10.0;
        }
        ArraySpan span = new ArraySpan( timePoints );
        System.out.println( "Read successfully" );

        JVodeSolver solver = new JVodeSolver();
        JVodeOptions opts = solver.getOptions();
        opts.setIterations( JVodeSupport.IterationType.FUNCTIONAL );
        JavaSimulationEngine engine = new JavaSimulationEngine();
        engine.setSolver( solver );
        engine.setOutputDir( outputDir );
        engine.setAbsTolerance( 1e-17 );
        engine.setRelTolerance( 1e-12 );
        engine.setSpan( span );
        engine.setDiagram( diagram );
        engine.setCompletionTime(200.0);
        System.out.println( "Ready to simulate" );

        SimulationResult result = new SimulationResult( null, modelName + "_result" );
        try
        {
            time = System.currentTimeMillis();
            engine.simulate( result );
            time = System.currentTimeMillis() - time;
            System.out.println( "Simulation time: " + ( (double)time / 1000 ) + "s." );
        }
        finally
        {
            BionetgenSimulationTest.deleteUnnecessaryFiles( outputDir, false );
        }
        System.out.println( "Simulated" );

        String[] varNames = new String[] {"P_1", "P_2_1", "P_2_2", "P_4", "P_5_1", "P_5_2", "P_6_1", "P_6_2", "P_7", "P_8_1", "P_8_2",
                "P_9", "P_10_1", "P_10_2", "P_11", "P_12_1", "P_12_2", "P_13_1", "P_13_2", "P_14_1", "P_14_2", "P_16_1", "P_16_2",
                "P_17_1", "P_17_2", "P_18_1", "P_18_2",};
        double[][] bioumlResult = new double[varNames.length][];
        for( int i = 0; i < varNames.length; i++ )
        {
            bioumlResult[i] = result.getValues( new String[] {varNames[i]} )[0];
        }
        BionetgenSimulationTest.writeSimulationResult( varNames, bioumlResult, directory + modelName + "-results.csv" );

        varNames = new String[] {"P1_1", "P1_2", "P2_1", "P2_2", "P2_3", "P3_1", "P4_1", "P4_2", "P5_1", "P5_2", "P5_3", "P6_1", "P6_2",
                "P6_3", "P7_1", "P7_2", "P8_1", "P8_2", "P8_3", "P9_1", "P9_2", "P10_1", "P10_2", "P10_3", "P11_1", "P11_2", "P12_1",
                "P12_2", "P12_3", "P13_1", "P13_2", "P13_3", "P14_1", "P14_2", "P14_3", "P15_1", "P16_1", "P16_2", "P16_3", "P17_1",
                "P17_2", "P17_3", "P18_1", "P18_2", "P18_3",};
        bioumlResult = new double[varNames.length][];
        for( int i = 0; i < varNames.length; i++ )
        {
            bioumlResult[i] = result.getValues( new String[] {varNames[i]} )[0];
        }
        BionetgenSimulationTest.writeSimulationResult( varNames, bioumlResult, directory + modelName + "-observables-results.csv" );

        varNames = new String[] {"P1", "P2", "P3", "P4", "P5", "P6", "P7", "P8", "P9", "P10", "P11", "P12", "P13", "P14", "P15", "P16",
                "P17", "P18",};
        bioumlResult = new double[varNames.length][];
        for( int i = 0; i < varNames.length; i++ )
        {
            bioumlResult[i] = result.getValues( new String[] {varNames[i]} )[0];
        }
        BionetgenSimulationTest.writeSimulationResult( varNames, bioumlResult, directory + modelName + "-peaks-results.csv" );

        System.out.println( "Result has been written. Ended" );
    }

    public void testDiagram_fceri_fyn_lig() throws Exception
    {
        boolean needLayout = false;
        List<String> data = new ArrayList<>();
        BionetgenTestUtility.initPreferences();
        Diagram diagram = BionetgenDiagramDeployer.deployBNGDiagram( generateDiagram( "fceri_fyn_lig", false ), needLayout );

        int reactionCounter = 0;
        int observableCounter = 0;
        int moleculeCounter = 0;
        for( Node node : diagram.getNodes() )
        {
            if( node.getKernel().getType().equals( "reaction" ) )
                reactionCounter++;
            if( node.getKernel().getType().equals( Type.MATH_EQUATION ) && node.getName().endsWith( BionetgenConstants.OBSERVABLE ) )
                observableCounter++;
            if( node.getKernel().getType().equals( Type.TYPE_SUBSTANCE ) )
                moleculeCounter++;
        }
        data.add( String.valueOf( reactionCounter ) );
        data.add( String.valueOf( observableCounter ) );
        data.add( String.valueOf( moleculeCounter ) );

        String expected = "[32776, 0, 2506]";
        assertEquals( expected, data.toString() );
    }

    protected void writeReport(Diagram diagram, long timeSpent) throws Exception
    {
        File file = new File(directory + "report.txt");
        try (PrintWriter pw = new PrintWriter( file, StandardCharsets.UTF_8.toString() );
                PrintWriter glyWriter = new PrintWriter( directory + "glycans.txt", StandardCharsets.UTF_8.toString() ))
        {
            int size = 0;
            List<Rule> ruleList = initRules();
            for( Rule rule : ruleList )
                size += rule.reactionRulesIn;
            int[] reactionNumbers = new int[size];
            int reactionCounter = 0;
            int elementCounter = 0;
            pw.println( "-----------------------------------" );
            EModel emodel = diagram.getRole( EModel.class );
            Variable var = new Variable( "tGolgi", emodel, emodel.getVariables() );
            var.setInitialValue( 5.66666666666667 );
            var.setConstant( true );
            emodel.put( var );
            int summNumber = 0;
            int summCount = 0;
            StringBuilder fullSumm = new StringBuilder( "0.0" );
            StringBuilder currentSumm = new StringBuilder( "0.0" );
            Set<String> species = new HashSet<>();
            for( Node node : diagram.getNodes() )
            {
                Base nodeKernel = node.getKernel();
                String nodeKernelType = nodeKernel.getType();
                if( nodeKernelType.equals( "reaction" ) )
                {
                    ++reactionCounter;
                    String name = nodeKernel.getName();
                    int number = Integer.parseInt( name.substring( 1, name.indexOf( '_' ) ) ) - 1;
                    if( number < size )
                    {
                        reactionNumbers[number]++;
                    }
                    else
                    {
                        pw.println( "Unregisted reaction: " + node.getName() );
                    }
                }
                else if( nodeKernelType.equals( Type.TYPE_SUBSTANCE ) )
                {
                    ++elementCounter;
                    DynamicProperty graphAttr = node.getAttributes().getProperty( "BioNetGen Graph" );
                    if( graphAttr == null || graphAttr.getValue() == null )
                        continue;
                    String str = graphAttr.getValue().toString();
                    if( str.contains( "." ) || str.contains( "(" ) || BionetgenToGlycanConverter.isGlycanMol( str ) )
                    {
                        String number = node.getName().replaceFirst( "Species", "" );
                        species.add( number );
                        createTransferReaction( diagram, emodel, node, number );

                        currentSumm.append( "+" ).append( node.getRole( VariableRole.class ).getName() );
                        String converted = BionetgenToGlycanConverter.convert( str );
                        node.getAttributes().add( new DynamicProperty( "glycanStructure", String.class, converted ) );
                        glyWriter.print( converted );
                        summCount++;
                    }
                }
                else if( nodeKernelType.equals( Type.MATH_EQUATION ) && node.getName().endsWith( BionetgenConstants.OBSERVABLE ) )
                {
                    Equation eq = node.getRole( Equation.class );
                    pw.println( eq.toString() );
                }
                if( summCount > 3000 )
                {
                    summCount = 0;
                    String currentVar = "FConc" + summNumber;
                    addEquation( currentVar, currentSumm.toString(), diagram );
                    currentSumm = new StringBuilder( "0.0" );
                    fullSumm.append( "+" ).append( currentVar );
                    summNumber++;
                }
            }

            if( summNumber == 0 )
                addEquation( "FConc", currentSumm.toString(), diagram );
            else
            {
                String currentVar = "FConc" + summNumber;
                addEquation( currentVar, currentSumm.toString(), diagram );
                fullSumm.append( "+" ).append( currentVar );
                addEquation( "FConc", fullSumm.toString(), diagram );
            }

            pw.println( "-----------------------------------" );

            pw.println( "Total rules number: " + size );
            pw.println( "Time spent: " + ( (double)timeSpent / 1000 ) + "s." );
            pw.println( "Substances: " + elementCounter );
            pw.println( "Reactions: " + reactionCounter );
            int counted = 0;
            pw.println( "-----------------------------------" );
            for( Rule rule : ruleList )
            {
                if( counted > size )
                {
                    pw.println();
                    pw.println( "Reactions number mismatch!!!" );
                    break;
                }
                int inCurrentRule = 0;
                int rulesToAdd = rule.reactionRulesIn;
                while( rulesToAdd > 0 )
                {
                    pw.println( "Reaction rule #" + ( counted + 1 ) + " : " + reactionNumbers[counted] );
                    inCurrentRule += reactionNumbers[counted];
                    ++counted;
                    --rulesToAdd;
                }
                pw.println( "in Rule #" + rule.name + " : " + inCurrentRule );
                pw.println( "-----------------------------------" );
            }

            int i = 0;
            var = new Variable( "c", emodel, emodel.getVariables() );
            var.setInitialValue( 0 );
            emodel.put( var );
            Assignment[] actions = new Assignment[species.size() + 1];
            for( String specie : species )
            {
                actions[i] = new Assignment( "in" + specie, "$Species" + specie );
                ++i;
            }
            actions[i] = new Assignment( "c", "c+1" );
            Node node = new Node( diagram, new Stub( null, "math_event_5", Type.MATH_EVENT ) );
            Event event = new Event( node,
                    "( time >= 0.0 && c == 0 ) || ( time >= inGolgiTime  && c == 1 ) || ( time >= 2*inGolgiTime  && c == 2 ) "
                            + "|| ( time >= 3*inGolgiTime  && c == 3 )", "0", actions );
            event.setTriggerInitialValue( false );
            node.setRole( event );
            node.setLocation( 1020, 100 );
            diagram.put( node );
        }
    }

    private void createTransferReaction(Diagram diagram, EModel emodel, Node node, String elementNumber) throws Exception
    {
        Variable auxiliary = new Variable( "in" + elementNumber, emodel, emodel.getVariables() );
        emodel.put( auxiliary );

        String reactionName = "transferReaction_" + elementNumber;
        List<SpecieReference> list = new ArrayList<>();

        String id = reactionName + ": " + node.getName() + " as " + SpecieReference.PRODUCT;
        SpecieReference ref = new SpecieReference( null, id, SpecieReference.PRODUCT );
        ref.setTitle( "" );
        ref.setSpecie( node.getName() );
        list.add( ref );

        DiagramUtility.createReactionNode( diagram, diagram, null, list,
                "(" + "in" + elementNumber + "-" + node.getRole( VariableRole.class ).getName() + ")/tGolgi", new Point( 0, 0 ),
                "reaction", reactionName );
    }

    protected void addStates(Diagram diagram) throws Exception
    {
        List<StateAssignment> list = initStateAssignments();
        String name = "math_event";
        Node node1 = new Node( diagram, new Stub( null, name + "_1", Type.MATH_EVENT ) );
        Node node2 = new Node( diagram, new Stub( null, name + "_2", Type.MATH_EVENT ) );
        Node node3 = new Node( diagram, new Stub( null, name + "_3", Type.MATH_EVENT ) );
        Node node4 = new Node( diagram, new Stub( null, name + "_4", Type.MATH_EVENT ) );

        int size = list.size();
        Assignment[] actions1 = new Assignment[size];
        Assignment[] actions2 = new Assignment[size];
        Assignment[] actions3 = new Assignment[size];
        Assignment[] actions4 = new Assignment[size];
        EModel emodel = diagram.getRole( EModel.class );
        for( int i = 0; i < size; i++ )
        {
            StateAssignment assig = list.get( i );
            actions1[i] = new Assignment( assig.variable, assig.onEntry[0] );
            actions2[i] = new Assignment( assig.variable, assig.onEntry[1] );
            actions3[i] = new Assignment( assig.variable, assig.onEntry[2] );
            actions4[i] = new Assignment( assig.variable, assig.onEntry[3] );
            try
            {
                emodel.getVariableRoles().get( assig.variable ).setBoundaryCondition( true );
            }
            catch( Throwable t )
            {
                System.out.println( "Failed to find " + assig.variable );
            }
        }
        double inGolgiTime = 50;
        Variable var = new Variable( "inGolgiTime", emodel, emodel.getVariables() );
        var.setInitialValue( inGolgiTime );
        var.setConstant( true );
        emodel.put( var );
        Event event1 = new Event( node1, "time >= 0.0 && time <= inGolgiTime", "0", actions1 );
        Event event2 = new Event( node2, "time > inGolgiTime && time <= 2*inGolgiTime", "0", actions2 );
        Event event3 = new Event( node3, "time > 2*inGolgiTime && time <= 3*inGolgiTime", "0", actions3 );
        Event event4 = new Event( node4, "time > 3*inGolgiTime", "0", actions4 );
        event1.setTriggerInitialValue( false );
        node1.setRole( event1 );
        node2.setRole( event2 );
        node3.setRole( event3 );
        node4.setRole( event4 );
        node1.setLocation( 0, 100 );
        node2.setLocation( 240, 100 );
        node3.setLocation( 480, 100 );
        node4.setLocation( 720, 100 );
        diagram.put( node1 );
        diagram.put( node2 );
        diagram.put( node3 );
        diagram.put( node4 );
        //add parameters with full peak concentration
        addEquation( "FullPeakConc", "P1_1 + P1_2 + P2_1 + P2_2 + P2_3 + P3_1 + P4_1 + P4_2 + P5_1 + P5_2 + P5_3 + P6_1 + P6_2 + P6_3 + "
                + "P7_1 + P7_2 + P8_1 + P8_2 + P8_3 + P9_1 + P9_2 + P10_1 + P10_2 + P10_3 + P11_1 + P11_2 + P12_1 + P12_2 + P12_3 + "
                + "P13_1 + P13_2 + P13_3 + P14_1 + P14_2 + P14_3 + P15_1 + P16_1 + P16_2 + P16_3 + P17_1 + P17_2 + P17_3 + "
                + "P18_1 + P18_2 + P18_3", diagram );
        addEquation( "PeakToGly", "FullPeakConc/FConc", diagram );
        addEquation( "P1", "100*(P1_1+P1_2)/FullPeakConc", diagram );
        addEquation( "P2", "100*(P2_1+P2_2+P2_3)/FullPeakConc", diagram );
        addEquation( "P3", "100*P3_1/FullPeakConc", diagram );
        addEquation( "P4", "100*(P4_1+P4_2)/FullPeakConc", diagram );
        addEquation( "P5", "100*(P5_1+P5_2+P5_3)/FullPeakConc", diagram );
        addEquation( "P6", "100*(P6_1+P6_2+P6_3)/FullPeakConc", diagram );
        addEquation( "P7", "100*(P7_1+P7_2)/FullPeakConc", diagram );
        addEquation( "P8", "100*(P8_1+P8_2+P8_3)/FullPeakConc", diagram );
        addEquation( "P9", "100*(P9_1+P9_2)/FullPeakConc", diagram );
        addEquation( "P10", "100*(P10_1+P10_2+P10_3)/FullPeakConc", diagram );
        addEquation( "P11", "100*(P11_1+P11_2)/FullPeakConc", diagram );
        addEquation( "P12", "100*(P12_1+P12_2+P12_3)/FullPeakConc", diagram );
        addEquation( "P13", "100*(P13_1+P13_2+P13_3)/FullPeakConc", diagram );
        addEquation( "P14", "100*(P14_1+P14_2+P14_3)/FullPeakConc", diagram );
        addEquation( "P15", "100*P15_1/FullPeakConc", diagram );
        addEquation( "P16", "100*(P16_1+P16_2+P16_3)/FullPeakConc", diagram );
        addEquation( "P17", "100*(P17_1+P17_2+P17_3)/FullPeakConc", diagram );
        addEquation( "P18", "100*(P18_1+P18_2+P18_3)/FullPeakConc", diagram );

        PlotsInfo plotsInfo = new PlotsInfo( emodel );
        PlotInfo varPlots = new PlotInfo();
        plotsInfo.setPlots( new PlotInfo[] {varPlots} );
        List<Curve> curves = new ArrayList<>();
        String[] varsToPlot = new String[] {"P1", "P2", "P4", "P5", "P6", "P7", "P8", "P9", "P10", "P11", "P12", "P13", "P14", "P15", "P16",
                "P17", "P18"};
        for( String str : varsToPlot )
            curves.add( new Curve( "", str, str, emodel ) );
        varPlots.setYVariables( curves.stream().toArray( Curve[]::new ) );
        DiagramUtility.setPlotsInfo( diagram, plotsInfo );
    }

    public static void addEquation(String variableName, String formula, Diagram diagram)
    {
        EModel emodel = diagram.getRole( EModel.class );
        String uniqueName = DefaultSemanticController.generateUniqueNodeName( diagram, BionetgenConstants.EQUATION_NAME );
        Node node = new Node( diagram, new Stub( diagram.getOrigin(), uniqueName, Type.MATH_EQUATION ) );
        Variable variable = new Variable( variableName, emodel, emodel.getVariables() );
        variable.setInitialValue( 0.0 );
        emodel.put( variable );
        diagram.setNotificationEnabled( false );
        Equation eq = new Equation( node, Equation.TYPE_SCALAR, variableName, formula );
        node.setRole( eq );
        diagram.setNotificationEnabled( true );
        node.setLocation( 0, 200 );
        diagram.put( node );
    }

    protected List<Rule> initRules()
    {
        List<Rule> result = new ArrayList<>();
        result.add( new Rule( "1", 24 ) );
        result.add( new Rule( "2", 1 ) );
        result.add( new Rule( "5", 1 ) );
        result.add( new Rule( "6", 1 ) );
        result.add( new Rule( "7", 1 ) );
        result.add( new Rule( "8", 1 ) );
        result.add( new Rule( "9", 1 ) );
        result.add( new Rule( "10", 2 ) );
        result.add( new Rule( "11", 5 ) );
        result.add( new Rule( "12", 2 ) );
        result.add( new Rule( "13", 45 ) );
        result.add( new Rule( "14", 320 ) );
        result.add( new Rule( "15", 14 ) );
        result.add( new Rule( "16", 1 ) );
        result.add( new Rule( "17", 7 ) );
        result.add( new Rule( "18", 7 ) );
        result.add( new Rule( "20", 20 ) );
        result.add( new Rule( "21", 20 ) );
        result.add( new Rule( "22", 4 ) );
        result.add( new Rule( "23", 4 ) );
        result.add( new Rule( "24", 16 ) );
        result.add( new Rule( "25", 3 ) );
        result.add( new Rule( "26", 3 ) );
        return result;
    }

    protected List<StateAssignment> initStateAssignments()
    {
        List<StateAssignment> result = new ArrayList<>();
        String[] type0 = new String[] {"0.967", "0.032", "0.001", "0"};
        String[] type1 = new String[] {"0.15", "0.45", "0.3", "0.1"};
        String[] type2 = new String[] {"0", "0.6", "0.3", "0.1"};
        String[] type3 = new String[] {"0", "0.05", "0.2", "0.75"};
        result.add( new StateAssignment( "$Species13", "ManI_tot", type0 ) );
        result.add( new StateAssignment( "$Species14", "ManII_tot", type1 ) );
        result.add( new StateAssignment( "$Species17", "a6FucT_tot", type1 ) );
        result.add( new StateAssignment( "$Species5", "GnTI_tot", type1 ) );
        result.add( new StateAssignment( "$Species6", "GnTII_tot", type1 ) );
        result.add( new StateAssignment( "$Species7", "GnTIII_tot", type1 ) );
        result.add( new StateAssignment( "$Species8", "GnTIV_tot", type1 ) );
        result.add( new StateAssignment( "$Species9", "GnTV_tot", type1 ) );
        result.add( new StateAssignment( "$Species21", "iGnT_tot", type2 ) );
        result.add( new StateAssignment( "$Species20", "b4GalT_tot", type3 ) );
        result.add( new StateAssignment( "$Species16", "a3SiaT_tot", type3 ) );
        result.add( new StateAssignment( "$Species10", "IGnT_tot", type3 ) );
        result.add( new StateAssignment( "$Species18", "a6SiaT_tot", type3 ) );
        result.add( new StateAssignment( "$Species19", "b3GalT_tot", type3 ) );
        result.add( new StateAssignment( "$Species1", "FucTLe_tot", type3 ) );
        result.add( new StateAssignment( "$Species0", "FucTH_tot", type3 ) );
        result.add( new StateAssignment( "$Species15", "a3FucT_tot", type3 ) );
        result.add( new StateAssignment( "$Species3", "GalNAcT_A_tot", type3 ) );
        result.add( new StateAssignment( "$Species4", "GalT_B_tot", type3 ) );
        return result;
    }

    protected static class Rule
    {
        String name = "";
        int reactionRulesIn = 0;

        public Rule(String name, int rulesIn)
        {
            this.name = name;
            this.reactionRulesIn = rulesIn;
        }
    }
    protected static class StateAssignment
    {
        String variable = "";
        String[] onEntry = new String[4];

        public StateAssignment(String variable, String parameter, String[] onEntry)
        {
            if( onEntry.length < 4 )
                throw new IllegalArgumentException();
            this.variable = variable;
            for( int i = 0; i < 4; i++ )
            {
                this.onEntry[i] = parameter + "*" + onEntry[i];
            }
        }
    }
}