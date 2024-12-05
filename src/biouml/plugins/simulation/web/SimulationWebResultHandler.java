package biouml.plugins.simulation.web;

import java.awt.image.BufferedImage;
import java.util.logging.Logger;

import biouml.plugins.simulation.CycledResultListener;
import biouml.plugins.simulation.ResultWriter;
import biouml.plugins.simulation.SimulationEngine;
import biouml.standard.simulation.ResultListener;
import biouml.standard.simulation.SimulationResult;
import one.util.streamex.StreamEx;
import ru.biosoft.access.core.DataElementPath;

public class SimulationWebResultHandler implements CycledResultListener
{
    protected static final Logger log = Logger.getLogger(SimulationWebResultHandler.class.getName());

    protected SimulationEngine engine;
    protected WebResultPlot plots[];
    protected String errorMessage = null;
    protected boolean isInit = false;
    int percent = 0;
    private SimulationResult simulationResult;
    private ResultWriter resultWriter = null;

    public SimulationWebResultHandler(SimulationEngine simulationEngine)
    {
        this.engine = simulationEngine;
    }

    public ResultListener[] getResultListeners(DataElementPath diagramPath)
    {
        simulationResult = engine.generateSimulationResult();
        if ( simulationResult == null )
            return new ResultListener[0];

        resultWriter = new ResultWriter(simulationResult);
        simulationResult.setDiagramPath(diagramPath);

        if ( !engine.needToShowPlot )
            return new ResultListener[] { resultWriter };

        int type = engine.getSimulationType();
        if ( type == SimulationEngine.STOCHASTIC_TYPE )
            plots = StreamEx.of(engine.getPlots()).map(p -> new StochasticWebResultPlot(p)).toArray(WebResultPlot[]::new);
        else
            plots = StreamEx.of(engine.getPlots()).map(p -> new WebResultPlot(p)).toArray(WebResultPlot[]::new);

        return new ResultListener[] { this, resultWriter };
    }

    @Override
    public void start(Object model)
    {
        isInit = true;
        for ( WebResultPlot plot : plots )
            plot.init(engine);
        for ( WebResultPlot plot : plots )
            plot.start(model);
        resultWriter.start(model);
    }

    @Override
    public void add(double t, double[] y) throws Exception
    {
        for ( WebResultPlot plot : plots )
            plot.add(t, y);
        percent = (int) Math.floor(((t - engine.getInitialTime()) / (engine.getCompletionTime() - engine.getInitialTime())) * 100.0);
    }

    @Override
    public void finish()
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void addAsFirst(double t, double[] y)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void update(double t, double[] y)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void startCycle()
    {
        for ( WebResultPlot plot : plots )
            plot.startCycle();
    }

    public String getErrorMessage()
    {
        return errorMessage;
    }

    public BufferedImage[] generateResultImage()
    {
        return isInit ? StreamEx.of(plots).map(p -> p.getImage()).toArray(BufferedImage[]::new) : null;
    }

    public int getPreparedness()
    {
        return percent;
    }

    public SimulationResult getSimulationResult()
    {
        return simulationResult;
    }
}
