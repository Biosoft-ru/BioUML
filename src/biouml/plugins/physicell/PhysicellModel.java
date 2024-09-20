package biouml.plugins.physicell;

import java.util.Map;

import biouml.plugins.simulation.Model;
import ru.biosoft.physicell.biofvm.Microenvironment;
import ru.biosoft.physicell.core.Cell;
import ru.biosoft.physicell.ui.Visualizer;

public class PhysicellModel implements Model
{
    private ru.biosoft.physicell.core.Model model;
    private boolean isInit = false;

    public PhysicellModel(ru.biosoft.physicell.core.Model model)
    {
        this.model = model;
    }

    public void doStep() throws Exception
    {
        model.doStep();
    }

    public String display()
    {
        return model.display();
    }

    public PhysicellModel clone()
    {
        return new PhysicellModel( model );
    }

    public double getCurrentTime()
    {
        return model.getCurrentTime();
    }

    public Microenvironment getMicroenvironment()
    {
        return model.getMicroenvironment();
    }

    public Iterable<Visualizer> getVisualizers()
    {
        return model.getVisualizers();
    }

    public Object[] getReport(Cell cell) throws Exception
    {
        return model.getReportGenerator().getReportElements( cell );
    }

    public String[] getReportHeader() throws Exception
    {
        return model.getReportGenerator().getReportHeaderElements();
    }

    public String getLog() throws Exception
    {
        return model.getGlobalReportGenerator().getReportElements( model );
    }

    @Override
    public double[] getInitialValues() throws Exception
    {
        // TODO Auto-generated method stub
        return null;
    }

    public boolean executeEvents() throws Exception
    {
        return model.executeEvents( getCurrentTime() );
    }

    @Override
    public void init() throws Exception
    {
        model.init( false );

        isInit = true;
    }


    @Override
    public void init(double[] initialValues, Map<String, Double> parameters) throws Exception
    {
        // TODO Auto-generated method stub
    }


    @Override
    public boolean isInit()
    {
        return isInit;
    }


    @Override
    public double[] getCurrentValues() throws Exception
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public void setCurrentValues(double[] values) throws Exception
    {
        // TODO Auto-generated method stub

    }


    @Override
    public double[] getCurrentState() throws Exception
    {
        // TODO Auto-generated method stub
        return null;
    }
}
