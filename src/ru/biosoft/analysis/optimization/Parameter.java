package ru.biosoft.analysis.optimization;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;

import java.util.List;

import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

public class Parameter extends Option implements DataElement
{
    public Parameter()
    {
    }

    public Parameter(String name, Double value)
    {
        this.name = name;
        this.title = name;
        this.value = value;
        this.lowerBound = 0;
        this.upperBound = value == 0 ? 1.0 : 5 * value;
    }

    public Parameter(String name, double value, double lowerBound, double upperBound)
    {
        this.name = name;
        this.title = name;
        this.value = value;
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
    }

    private String name;

    @Override
    @PropertyName("Name")
    @PropertyDescription("Parameter name")
    public String getName()
    {
        return name;
    }
    public void setName(String name)
    {
        this.name = name;
    }

    private String title;

    @PropertyName("Title")
    @PropertyDescription("Parameter title")
    public String getTitle()
    {
        return this.title;
    }
    public void setTitle(String title)
    {
        this.title = title;
    }

    private String parentDiagramName = "";

    @PropertyName("Diagram")
    @PropertyDescription("Parent diagram")
    public String getParentDiagramName()
    {
        return this.parentDiagramName;
    }
    public void setParentDiagramName(String parentDiagramName)
    {
        this.parentDiagramName = parentDiagramName;
    }

    private double value;

    @PropertyName("Value")
    @PropertyDescription("Parameter value")
    public double getValue()
    {
        return value;
    }
    public void setValue(double value)
    {
        double oldValue = this.value;
        this.value = value;
        firePropertyChange("value", oldValue, value);
    }

    private double lowerBound;

    @PropertyName("Lower bound")
    @PropertyDescription("A lower bound for the parameter")
    public double getLowerBound()
    {
        return lowerBound;
    }
    public void setLowerBound(double lowerBound)
    {
        double oldValue = this.lowerBound;
        this.lowerBound = lowerBound;
        firePropertyChange("lowerBound", oldValue, lowerBound);
    }

    private double upperBound;

    @PropertyName("Upper bound")
    @PropertyDescription("An upper bound for the parameter")
    public double getUpperBound()
    {
        return upperBound;
    }
    public void setUpperBound(double upperBound)
    {
        double oldValue = this.upperBound;
        this.upperBound = upperBound;
        firePropertyChange("upperBound", oldValue, upperBound);
    }

    private String units = "";

    @PropertyName("Units")
    @PropertyDescription("Parameter units")
    public String getUnits()
    {
        return this.units;
    }
    public void setUnits(String units)
    {
        String oldValue = this.units;
        this.units = units;
        firePropertyChange("units", oldValue, units);
    }

    private String comment = "";

    @PropertyName("Comment")
    @PropertyDescription("Comment")
    public String getComment()
    {
        return this.comment;
    }
    public void setComment(String comment)
    {
        String oldValue = this.comment;
        this.comment = comment;
        firePropertyChange("comment", oldValue, comment);
    }

    private String locality = Locality.GLOBAL.toString();

    @PropertyName("Locality")
    @PropertyDescription("Parameter locality")
    public String getLocality()
    {
        return this.locality;
    }
    public void setLocality(String locality)
    {
        String oldValue = this.locality;
        this.locality = locality;
        firePropertyChange("locality", oldValue, locality);
    }

    public boolean isLocal()
    {
        return !locality.equals(Locality.GLOBAL.toString());
    }

    private List<String> scope;
    public void setScope(List<String> scope)
    {
        this.scope = scope;
    }
    public List<String> getScope()
    {
        return this.scope;
    }

    public Parameter copy()
    {
        Parameter param = new Parameter(this.name, this.value, this.lowerBound, this.upperBound);
        param.setTitle(title);
        param.setUnits(units);
        param.setComment(comment);
        param.setLocality(locality);
        param.setParentDiagramName(parentDiagramName);
        return param;
    }

    @Override
    public DataCollection getOrigin()
    {
        return null;
    }

    @Override
    public String toString()
    {
        return name + " = " + value + ", bounds: [" + lowerBound + ", " + upperBound + "]";
    }

    public static enum Locality
    {
        GLOBAL, LOCAL_IN_CELL_LINES, LOCAL_IN_EXPERIMENTS;

        public String toString()
        {
            return toString(this);
        }

        public static String toString(Locality locality)
        {
            switch( locality )
            {
                case GLOBAL:
                    return "";

                case LOCAL_IN_CELL_LINES:
                    return "cell lines";

                case LOCAL_IN_EXPERIMENTS:
                    return "experiments";

                default:
                    return "";
            }
        }

        public static String[] getLocality()
        {
            String[] arr = new String[values().length];
            int i = 0;
            for( Locality locality : values() )
                arr[i++] = toString(locality);
            return arr;
        }
    }
}
