package biouml.plugins.hemodynamics;

import java.awt.Color;
import java.io.BufferedWriter;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.jfree.data.general.Series;
import org.jfree.data.xy.XYSeries;

import com.developmentontheedge.application.ApplicationUtils;
import ru.biosoft.jobcontrol.FunctionJobControl;

import biouml.plugins.simulation.ResultPlotPane;
import biouml.plugins.simulation.SimulationEngine;
import biouml.plugins.simulation.SimulationEngine.Var;
import one.util.streamex.DoubleStreamEx;
import one.util.streamex.StreamEx;
import ru.biosoft.graphics.Pen;

public class VesselVisualizationListener extends ResultPlotPane
{

    public VesselVisualizationListener(String vesselName, SimulationEngine simulationEngine, FunctionJobControl jobControl)
    {
        super(simulationEngine, jobControl);
        this.vesselName = new ArrayList<>();
        this.vesselName.add(vesselName);
        title = vesselName;
    }

    public VesselVisualizationListener(SimulationEngine simulationEngine, FunctionJobControl jobControl, String... vessels)
    {
        super(simulationEngine, jobControl);
        this.vesselName = StreamEx.of(vessels).toList();
        title = "Chain";
    }

    private final String title;
    private double length;
    private int segments;
    private int segmentsPerVessel;
    private final List<String> vesselName;
    public File resultFile;// = new File("vessel_wall.txt";
    public boolean saveResult = false;
    public double startSave = 5.8;
    public double endSave = 6.8;

    List<double[]> results;
    List<Double> time;

    List<SimpleVessel> v;
    ArterialBinaryTreeModel model;
    @Override
    public void start(Object model)
    {
        super.start(model);
        this.model = (ArterialBinaryTreeModel)model;
        v = StreamEx.of(vesselName).map(s -> this.model.vesselMap.get(s)).toList();
        length = 0;
        for( SimpleVessel vessel : v )
            length += vessel.length;

        segmentsPerVessel = ((int)this.model.vesselSegments + 1);
        segments = segmentsPerVessel * v.size();

        if( saveResult )
        {
            results = new ArrayList<>();
            time = new ArrayList<>();
        }

    }

    @Override
    public Map<Var, List<Series>> getVariables()
    {
        Map<Var, List<Series>> result = new HashMap<>();
        result.put( new Var( title, 0, -1, new Pen( 2 ) ), Arrays.asList( new XYSeries[] {new XYSeries( title )} ) );
        result.put( new Var( "baseLine", 0, -1, new Pen( 1, Color.GRAY ) ),
                Arrays.asList( new XYSeries[] {new XYSeries( "baseLine" )} ) );
        result.put( new Var( "down", 0, -1, new Pen( 1, Color.GRAY ) ), Arrays.asList( new XYSeries[] {new XYSeries( "down" )} ) );
        result.put( new Var( "up", 0, -1, new Pen( 1, Color.GRAY ) ), Arrays.asList( new XYSeries[] {new XYSeries( "up" )} ) );
        return result;
    }

    private double getArgument(int i)
    {
        int vesselIndex = Math.floorDiv(i, segmentsPerVessel);
        int segment = Math.floorMod(i, segmentsPerVessel);
        SimpleVessel vessel = v.get(vesselIndex);

        double length = vessel.length * segment/ segmentsPerVessel;

        vessel = vessel.parent;
        while (vessel != null)
        {
            length += vessel.length;
            vessel = vessel.parent;
        }
        return length;
    }

    private double getValue(int i)
    {
        int vesselIndex = Math.floorDiv(i, segmentsPerVessel);
        int segment = Math.floorMod(i, segmentsPerVessel);
        return v.get(vesselIndex).getPressure()[segment];// - 70;
//        return v.get(vesselIndex).getArea()[segment];
    }


    @Override
    public void addAsFirst(double t, double[] y)
    {
        for( Entry<SimulationEngine.Var, List<Series>> e : variableIndeces.entrySet() )
        {
            y = new double[segments];
            for( int i = 0; i < segments; i++ )
            {
                double arg = getArgument(i);
                y[i] = getValue(i);
                ((XYSeries)e.getValue().get( 0 )).add( arg, 0 );
            }
            if( saveResult && t > startSave && t < endSave )
            {
                results.add(y);
                time.add(t);
            }
        }

        this.startCycle();
    }

    @Override
    public void finish()
    {
        try (BufferedWriter bw = ApplicationUtils.utfWriter(resultFile))
        {
            double[] args = new double[segments];
            for( int i = 0; i < args.length; i++ )
                args[i] = length / model.vesselSegments * i;

            bw.write("\t\t" + DoubleStreamEx.of(args).joining("\t") + "\n");

            if (saveResult)
                for( int i = 0; i < results.size(); i++ )
                    bw.write(time.get(i) + "\t" + DoubleStreamEx.of(results.get(i)).joining("\t") + "\n");
        }
        catch( Exception ex )
        {
            ex.printStackTrace();
        }
    }

    @Override
    public void update(double t, double[] y)
    {
        for( Entry<SimulationEngine.Var, List<Series>> e : variableIndeces.entrySet() )
        {
            y = new double[segments];
            for( int i = 0; i < y.length; i++ )
            {
                double arg = getArgument(i);

                if (e.getKey().name.equals("baseLine"))
                    ((XYSeries)e.getValue().get(0)).update(arg, 0);
                else if (e.getKey().name.equals("up"))
                    ((XYSeries)e.getValue().get(0)).update(arg, 12);
                else if (e.getKey().name.equals("down"))
                    ((XYSeries)e.getValue().get(0)).update(arg, -4);
                else if (e.getKey().name.equals(title))
                {
                    y[i] = getValue(i);
                    ((XYSeries)e.getValue().get(0)).update(arg, y[i]);
                }
            }

            if( saveResult && t > startSave && t < endSave )
            {
                if( Math.abs( ( Math.round(t * 100) - t * 100 )) < 0.000001 )
                {
                    results.add(y);
                    time.add(t);
                }
            }
        }
    }

}
