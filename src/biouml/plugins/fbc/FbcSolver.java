package biouml.plugins.fbc;

import java.util.HashMap;
import java.util.Map;

import ru.biosoft.jobcontrol.FunctionJobControl;

import biouml.plugins.simulation.Model;
import biouml.plugins.simulation.Options;
import biouml.plugins.simulation.Simulator;
import biouml.plugins.simulation.SimulatorInfo;
import biouml.plugins.simulation.SimulatorProfile;
import biouml.plugins.simulation.SimulatorSupport;
import biouml.plugins.simulation.Span;
import biouml.standard.simulation.ResultListener;

public class FbcSolver extends SimulatorSupport
{
    FbcModelCreator fbcModelCreator;

    Simulator simulator;
    Model model;

    Map<String, Integer> reactionRateIndices;
    Map<String, Integer> constraintIndices;
    Map<String, Double> constraintValues;

    public FbcSolver(Simulator simulator )
    {
        this.simulator = simulator;
    }

    @Override
    public boolean doStep() throws Exception
    {
        double[] values = model.getCurrentValues();

        for( Map.Entry<String, Integer> entry : constraintIndices.entrySet() )
            constraintValues.put( entry.getKey(), values[entry.getValue()] );

        FbcModel fbcModel = fbcModelCreator.getUpdatedModel(constraintValues);
        fbcModel.optimize();

        for( String rName : fbcModel.getReactionNames() )
        {
            int i = reactionRateIndices.get(rName);
            double val = fbcModel.getOptimValue(rName);
            values[i] = val;
        }

        model.setCurrentValues(values);
        return simulator.doStep();
    }

    @Override
    public SimulatorInfo getInfo()
    {
        return simulator.getInfo();
    }

    @Override
    public Object getDefaultOptions()
    {
        return simulator.getDefaultOptions();
    }

    @Override
    public void init(Model model, double[] x0, Span tspan, ResultListener[] listeners, FunctionJobControl jobControl) throws Exception
    {
        constraintValues = new HashMap<>();
        this.model = model;
        simulator.init(model, x0, tspan, listeners, jobControl);
    }

    @Override
    public void setInitialValues(double[] x0) throws Exception
    {
        simulator.setInitialValues(x0);
    }

    @Override
    public Options getOptions()
    {
        return simulator.getOptions();
    }

    @Override
    public void setOptions(Options options)
    {
        simulator.setOptions(options);
    }


    @Override
    public void stop()
    {
        simulator.stop();
    }

    @Override
    public SimulatorProfile getProfile()
    {
        return simulator.getProfile();
    }

    @Override
    public int[] getEvents()
    {
        if( simulator instanceof SimulatorSupport )
            return ( (SimulatorSupport)simulator ).getEvents();
        return null;
    }

    @Override
    public void setStarted()
    {
        if( simulator instanceof SimulatorSupport )
            ( (SimulatorSupport)simulator ).setStarted();
        terminated = false;
    }

    @Override
    public void setFireInitialValues(boolean val)
    {
        fireInitialValues = val;
        if( simulator instanceof SimulatorSupport )
            ( (SimulatorSupport)simulator ).setFireInitialValues(val);
    }
}
