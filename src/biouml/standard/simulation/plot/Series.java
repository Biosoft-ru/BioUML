package biouml.standard.simulation.plot;

import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.standard.simulation.SimulationResult;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.DataElementReadException;
import ru.biosoft.exception.InternalException;
import ru.biosoft.graphics.Pen;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;

public class Series extends Option implements DataElement
{
    protected String name;
    protected String plotName;
    protected Pen spec = null;
    protected String legend = "";
    protected String xPath = "";
    protected String xVar;
    protected String yPath = "";
    protected String yVar;
    private SourceNature sourceNature = SourceNature.SIMULATION_RESULT;
    protected DataElementPath source;
    
    public static enum SourceNature
    {
        SIMULATION_RESULT, EXPERIMENTAL_DATA;
    }
    
    public Series()
    {
    }

    public Series(String name)
    {
        this.name = name;
    }

    @Override
    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        String oldValue = this.name;
        this.name = name;
        firePropertyChange("name", oldValue, name);
    }

    public String getPlotName()
    {
        return plotName;
    }

    public void setPlotName(String plotName)
    {
        String oldValue = this.plotName;
        this.plotName = plotName;
        firePropertyChange("plotName", oldValue, plotName);
    }

    @PropertyName("Specification+")
    @PropertyDescription("Specification of plot type")
    public Pen getSpec()
    {
        return spec;
    }
    public void setSpec(Pen spec)
    {
        Pen oldValue = this.spec;
        this.spec = spec;
        firePropertyChange("spec", oldValue, spec);
    }

    @PropertyName("Legend")
    @PropertyDescription("Description of the plot")
    public String getLegend()
    {
        return legend;
    }
    public void setLegend(String legend)
    {
        String oldValue = this.legend;
        this.legend = legend;
        firePropertyChange("legend", oldValue, legend);
    }

    // properties below should not be changed directly

    /** Name of variable in simulation result set that is used as X variable. */
    @PropertyName("X Variable")
    @PropertyDescription("Variable to be mapped to X axis")
    public String getXVar()
    {
        return xVar;
    }

    public void setXVar(String xVar)
    {
        this.xVar = xVar;
    }
    
    @PropertyName("X Path")
    @PropertyDescription("Path to X variable")
    public String getXPath()
    {
        return xPath;
    }
    public void setXPath(String xPath)
    {
        this.xPath = xPath;
    }  

    /** Name of variable in simulation result set that is used as Y variable. */
    @PropertyName("Y Variable")
    @PropertyDescription("Variable that will be mapped to Y axis")
    public String getYVar()
    {
        return yVar;
    }
    public void setYVar(String yVar)
    {
        this.yVar = yVar;
    }
    
    @PropertyName("Y Path")
    @PropertyDescription("Path to Y variable")
    public String getYPath()
    {
        return yPath;
    }
    public void setYPath(String yPath)
    {
        this.yPath = yPath;
    }

    @PropertyName("Source")
    @PropertyDescription("Name of source, comprising required data")
    public String getSource()
    {
        return source.toString();
    }
    public void setSource(String source)
    {
        this.source = DataElementPath.create(source);
    }

    public SourceNature getSourceNature()
    {
        return sourceNature;
    }
    public void setSourceNature(SourceNature sourceNature)
    {
        this.sourceNature = sourceNature;
    }

    @Override
    public DataCollection getOrigin()
    {
        return null;
    }

    public int getValuesCount()
    {
        try
        {
            switch(sourceNature)
            {
                case SIMULATION_RESULT:
                    return source.getDataElement(SimulationResult.class).getCount();
                case EXPERIMENTAL_DATA:
                    return source.getDataElement(TableDataCollection.class).getSize();
                default:
                    throw new InternalException("Incorrect sourceNature");
            }
        }
        catch( Exception e )
        {
            throw new DataElementReadException(e, this, "values count");
        }
    }

    public double[] getXValues()
    {
        return getValues(getXPath().isEmpty()? getXVar(): getXPath()+"/"+getXVar());
    }

    public double[] getYValues()
    {
        return getValues(getYPath().isEmpty()? getYVar(): getYPath()+"/"+getYVar());
    }

    private double[] getValues(String varName)
    {
        try
        {
            switch(sourceNature)
            {
                case SIMULATION_RESULT:
                    SimulationResult result = source.getDataElement(SimulationResult.class);
                    if( varName.equals("time") )
                        return result.getTimes();
                    int varIndex = result.getVariablePathMap().get(varName);
                    int size = result.getCount();

                    double[] resValues = new double[size];
                    double[][] values = result.getValues();
                    for( int i = 0; i < size; i++ )
                    {
                        resValues[i] = values[i][varIndex];
                    }
                    return resValues;
                case EXPERIMENTAL_DATA:
                    return TableDataCollectionUtils.getColumn(source.getDataElement(TableDataCollection.class), varName);
                default:
                    throw new InternalException("Incorrect sourceNature");
            }
        }
        catch( Exception e )
        {
            throw new DataElementReadException(e, this, "values");
        }
    }

    @Override
    public boolean equals(Object obj)
    {
        if( this == obj )
            return true;
        if( obj == null )
            return false;
        if( getClass() != obj.getClass() )
            return false;
        Series other = (Series)obj;
        if( !getSource().equals( other.getSource() ) )
            return false;
        else if( ! ( xVar.equals( other.getXVar() ) && xPath.equals( other.getXPath() ) )
                || ! ( yVar.equals( other.getYVar() ) && yPath.equals( other.getYPath() ) ) )
            return false;
        //TODO: do we need to compare color and other fields?
        return true;
    }
}