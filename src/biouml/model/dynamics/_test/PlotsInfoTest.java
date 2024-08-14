package biouml.model.dynamics._test;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.plot.Curve;
import biouml.model.dynamics.plot.PlotInfo;
import biouml.model.dynamics.plot.PlotsInfo;
import biouml.model.dynamics.plot.PlotsInfo.AutoPenSelector;
import biouml.standard.type.Base;
import biouml.standard.type.Stub;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import one.util.streamex.StreamEx;
import ru.biosoft.graphics.Pen;

public class PlotsInfoTest extends TestCase
{
    public PlotsInfoTest(String name)
    {
        super( name );
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite( PlotsInfoTest.class.getName() );

        suite.addTest( new PlotsInfoTest( "testAutoPenSelector" ) );
        suite.addTest( new PlotsInfoTest( "testPlotInfoAutoPen" ) );

        return suite;
    }

    public void testAutoPenSelector()
    {
        AutoPenSelector aps = new AutoPenSelector();
        assertEquals( "Initialization failed", 0, aps.getNumber() );

        aps.getNextPen();
        aps.getNextPen();
        Pen pen1 = aps.getNextPen();
        assertEquals( "Incorrect increment", 3, aps.getNumber() );
        assertEquals( "Incorrect color", PlotsInfo.POSSIBLE_COLORS[2], pen1.getColor() );
        assertEquals( "Incorrect stroke", "Solid", pen1.getStrokeAsString() );

        aps.updateNumber( 18 );
        Pen pen2 = aps.getNextPen();
        assertEquals( "Incorrect increment", 19, aps.getNumber() );
        assertEquals( "Incorrect color", PlotsInfo.POSSIBLE_COLORS[2], pen2.getColor() );
        assertEquals( "Incorrect stroke", "Dashed", pen2.getStrokeAsString() );

        aps.updateNumber( 65 );
        Pen pen3 = aps.getNextPen();
        assertEquals( "Incorrect increment", 2, aps.getNumber() );
        assertEquals( "Incorrect color", PlotsInfo.POSSIBLE_COLORS[1], pen3.getColor() );
        assertEquals( "Incorrect stroke", "Solid", pen3.getStrokeAsString() );
    }

    public void testPlotInfoAutoPen()
    {
        Diagram diagram = new Diagram( null, new Stub( null, "test" ), null );
        assertNotNull( "Can't create diagram", diagram );
        EModel emodel = new EModel( diagram );
        assertNotNull( "Can't create model", emodel );
        Base compartmentKernel = new Stub( null, "compartmentKernel" );
        assertNotNull( "Can't create compartment kernel", compartmentKernel );
        Compartment compartment = new Compartment( diagram, compartmentKernel );
        diagram.put( compartment );

        PlotsInfo plotsInfo = new PlotsInfo( emodel );
        PlotInfo plots = new PlotInfo();
        plotsInfo.setPlots( new PlotInfo[] {plots} );

        List<Curve> curves = new ArrayList<>();
        curves.add( plots.getYVariables()[0] );
        assertEquals( "Incorrect curve count (constructor)", curves.size(), plots.getYVariables().length );
        //auto color number shows number used colors
        assertEquals( "Incorrect auto counter (constructor)", curves.size(), plots.getAutoColorNumber() );

        //test of adding curve with not null pen
        Curve curve = new Curve();
        curve.setPen( new Pen( 1.0f, Color.WHITE ) );
        curves.add( curve );
        curve = new Curve();
        curve.setPen( new Pen( 1.0f, Color.BLACK ) );
        curves.add( curve );

        plots.setYVariables( curves.toArray( new Curve[0] ) );
        assertEquals( "Incorrect curve count (nonnull pen)", curves.size(), plots.getYVariables().length );
        //auto color number shows number used colors
        assertEquals( "Incorrect auto counter (nonnull pen)", curves.size() - 2, plots.getAutoColorNumber() );

        //add curves with null pen
        curves.add( 1, new Curve() );
        curves.add( 1, new Curve() );
        curves.add( 0, new Curve() );
        curves.add( new Curve() );
        curves.add( new Curve() );

        plots.setYVariables( curves.toArray( new Curve[0] ) );
        assertEquals( "Incorrect curve count (null pen)", curves.size(), plots.getYVariables().length );
        //auto color number shows number used colors
        assertEquals( "Incorrect auto counter (null pen)", curves.size() - 2, plots.getAutoColorNumber() );

        //remove some curves and add new
        //check that counter increased
        Curve[] processedCurves = plots.getYVariables();
        curves = StreamEx.of( processedCurves ).toList();
        curves.remove( processedCurves.length - 2 );
        curves.remove( 1 );
        curves.add( new Curve() );
        curves.add( new Curve() );
        plots.setYVariables( curves.toArray( new Curve[0] ) );
        assertEquals( "Incorrect curve count (remove/add)", curves.size(), plots.getYVariables().length );
        //auto color number shows number used colors
        assertEquals( "Incorrect auto counter (remove/add)", curves.size(), plots.getAutoColorNumber() );
    }
}
