package biouml.model.dynamics.plot;

import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.model.dynamics.EModel;
import biouml.model.dynamics.plot.PlotsInfo.AutoPenSelector;
import one.util.streamex.StreamEx;
import ru.biosoft.graphics.font.ColorFont;

@PropertyName ( "Simulation Plot" )
public class PlotInfo extends Option
{
    private EModel emodel;
    private PlotVariable xVariable;
    private Curve[] yVariables;
    private Experiment[] experiments;
    boolean isActive = true;
    private String title = "Plot";
    private AxisInfo xAxisInfo = new AxisInfo( "Time" );
    private AxisInfo yAxisInfo = new AxisInfo( "Quantity or Concentration" );

    private final AutoPenSelector penSelector = new AutoPenSelector();

    public PlotInfo(EModel emodel)
    {
        this( emodel, true );
    }

    public PlotInfo(EModel emodel, boolean createTemplateLine)
    {
        this.emodel = emodel;
        xVariable = new PlotVariable( "", PlotVariable.TIME_VARIABLE, PlotVariable.TIME_VARIABLE, emodel );

        if( createTemplateLine )
        {
            yVariables = new Curve[1];
            yVariables[0] = new Curve( "", PlotVariable.TIME_VARIABLE, PlotVariable.TIME_VARIABLE, emodel );
            yVariables[0].setPen( penSelector.getNextPen() );
            yVariables[0].setParent( this );
        }
    }


    public PlotInfo()
    {
        this(null);
    }

    public void setEModel(EModel emodel)
    {
        this.emodel = emodel;
        xVariable.setEModel(emodel);
        for( Curve curve : yVariables )
            curve.setEModel(emodel);
    }

    public EModel getEModel()
    {
        return emodel;
    }

    @PropertyName ( "X variable" )
    public PlotVariable getXVariable()
    {
        return xVariable;
    }
    public void setXVariable(PlotVariable xVariable)
    {
        this.xVariable = xVariable;
        xVariable.setEModel(emodel);
    }

    @PropertyName ( "Curves" )
    public Curve[] getYVariables()
    {
        return yVariables;
    }
    public void setYVariables(Curve[] yVariables)
    {
        Curve[] oldValue = this.yVariables;
        this.yVariables = yVariables;

        for( int i = 0; i < yVariables.length; i++ )
        {
            Curve curve = yVariables[i];
            if( emodel != null && !emodel.equals(curve.getEModel()) )
                curve.setEModel(emodel);

            if( curve.getPen() == null )
                curve.setPen( penSelector.getNextPen() );

            curve.setParent( this );
        }
        this.firePropertyChange( "yVariables", oldValue, yVariables );
    }
    
    @PropertyName ( "Experiments" )
    public Experiment[] getExperiments()
    {
        return experiments;
    }
    public void setExperiments(Experiment[] experiments)
    {
        this.experiments = experiments;

        for( int i = 0; i < experiments.length; i++ )
        {
            Experiment experiment = experiments[i];
            if( experiment.getPen() == null )
                experiment.setPen( penSelector.getNextPen() );
            experiment.setParent( this );
        }
    }

    @PropertyName ( "Active" )
    public boolean isActive()
    {
        return isActive;
    }
    public void setActive(boolean isActive)
    {
        this.isActive = isActive;
    }

    @PropertyName ( "X axis info" )
    public AxisInfo getXAxisInfo()
    {
        return xAxisInfo;
    }
    public void setXAxisInfo(AxisInfo xAxisInfo)
    {
        Object oldValue = this.xAxisInfo;
        this.xAxisInfo = xAxisInfo;
        firePropertyChange( "xAxisInfo", oldValue, xAxisInfo );
    }

    @PropertyName ( "Y axis info" )
    public AxisInfo getYAxisInfo()
    {
        return yAxisInfo;
    }
    public void setYAxisInfo(AxisInfo yAxisInfo)
    {
        Object oldValue = this.yAxisInfo;
        this.yAxisInfo = yAxisInfo;
        firePropertyChange( "yAxisInfo", oldValue, yAxisInfo );
    }

    @PropertyName ( "Title" )
    public String getTitle()
    {
        return title;
    }
    public void setTitle(String title)
    {
        this.title = title;
    }

    @PropertyName ( "X axis title font" )
    @PropertyDescription ( "Font for title of X axis of the plot." )
    public ColorFont getXTitleFont()
    {
        return xAxisInfo.getTitleFont();
    }
    public void setXTitleFont(ColorFont xTitleFont)
    {
        xAxisInfo.setTitleFont( xTitleFont );
    }

    @PropertyName ( "X axis type" )
    @PropertyDescription ( "Type of X axis of the plot." )
    public String getXAxisType()
    {
        return xAxisInfo.getAxisType();
    }

    public boolean isXAxisTypeLogarithmic()
    {
        return xAxisInfo.isAxisTypeLogarithmic();
    }

    public void setXAxisType(String xAxisType)
    {
        xAxisInfo.setAxisType( xAxisType );
    }

    @PropertyName ( "Y axis type" )
    @PropertyDescription ( "Type of Y axis of the plot." )
    public String getYAxisType()
    {
        return yAxisInfo.getAxisType();
    }

    public void setYAxisType(String yAxisType)
    {
        yAxisInfo.setAxisType( yAxisType );
    }

    public boolean isYAxisTypeLogarithmic()
    {
        return yAxisInfo.isAxisTypeLogarithmic();
    }

    @PropertyName ( "X axis auto range" )
    @PropertyDescription ( "X axis auto range." )
    public boolean isXAutoRange()
    {
        return xAxisInfo.isAutoRange();
    }
    public void setXAutoRange(boolean xAutoRange)
    {
        xAxisInfo.setAutoRange( xAutoRange );
    }

    @PropertyName ( "   X: to " )
    @PropertyDescription ( "Largest value of X coordinate." )
    public double getXTo()
    {
        return xAxisInfo.getTo();
    }
    public void setXTo(double xTo)
    {
        xAxisInfo.setTo( xTo );
    }

    @PropertyName ( "   X: from" )
    @PropertyDescription ( "Smallest value of X coordinate." )
    public double getXFrom()
    {
        return xAxisInfo.getFrom();
    }
    public void setXFrom(double xFrom)
    {
        xAxisInfo.setFrom( xFrom );
    }

    @PropertyName ( "X axis tick font" )
    @PropertyDescription ( "X axis tick label font." )
    public ColorFont getXTickLabelFont()
    {
        return xAxisInfo.getTickLabelFont();
    }
    public void setXTickLabelFont(ColorFont xTickLabelFont)
    {
        xAxisInfo.setTickLabelFont( xTickLabelFont );
    }

    @PropertyName ( "Y axis title font" )
    @PropertyDescription ( "Font for title of YX axis of the plot." )
    public ColorFont getYTitleFont()
    {
        return yAxisInfo.getTitleFont();
    }
    public void setYTitleFont(ColorFont yTitleFont)
    {
        yAxisInfo.setTitleFont( yTitleFont );
    }

    @PropertyName ( "Y axis auto range" )
    @PropertyDescription ( "Y axis auto range." )
    public boolean isYAutoRange()
    {
        return yAxisInfo.isAutoRange();
    }
    public void setYAutoRange(boolean yAutoRange)
    {
        yAxisInfo.setAutoRange( yAutoRange );
    }

    @PropertyName ( "   Y: to " )
    @PropertyDescription ( "Largest value of Y coordinate." )
    public double getYTo()
    {
        return yAxisInfo.getTo();
    }
    public void setYTo(double yTo)
    {
        yAxisInfo.setTo( yTo );
    }

    @PropertyName ( "   Y: from " )
    @PropertyDescription ( "Smallest value of Y coordinate." )
    public double getYFrom()
    {
        return yAxisInfo.getFrom();
    }
    public void setYFrom(double yFrom)
    {
        yAxisInfo.setFrom( yFrom );
    }

    @PropertyName ( "Y axis tick font" )
    @PropertyDescription ( "Y axis tick label font." )
    public ColorFont getYTickLabelFont()
    {
        return yAxisInfo.getTickLabelFont();
    }
    public void setYTickLabelFont(ColorFont yTickLabelFont)
    {
        yAxisInfo.setTickLabelFont( yTickLabelFont );
    }

    public PlotInfo clone(EModel emodel)
    {
        PlotInfo result = new PlotInfo(emodel);
        result.setTitle(getTitle());
        result.setActive(isActive());
        result.setAutoColorNumber( getAutoColorNumber() );
        result.setXVariable(getXVariable().clone(emodel));
        result.setYVariables(StreamEx.of(getYVariables()).map(y -> y.clone(emodel)).toArray(Curve[]::new));
        if( getExperiments() != null )
            result.setExperiments(StreamEx.of(getExperiments()).map(y -> y.clone(emodel)).toArray(Experiment[]::new));
        return result;
    }

    public int getAutoColorNumber()
    {
        return penSelector.getNumber();
    }
    public void setAutoColorNumber(int newNumber)
    {
        penSelector.updateNumber( newNumber );
    }

    @PropertyName ( "X axis title" )
    public String getXTitle()
    {
        return xAxisInfo.getTitle();
    }

    public void setXTitle(String xTitle)
    {
        xAxisInfo.setTitle( xTitle );
    }

    @PropertyName ( "Y axis title" )
    public String getYTitle()
    {
        return yAxisInfo.getTitle();
    }

    public void setYTitle(String yTitle)
    {
        yAxisInfo.setTitle( yTitle );
    }
}