package ru.biosoft.table.columnbeans;

import com.developmentontheedge.beans.Option;

public class Column extends Option implements Comparable<Column>
{
    private String name = "new column";
    private String newName;
    private Double timePoint = 0.0;

    public Column(Option parent, String name)
    {
        this(parent, name, generateTimePoint(name));
    }

    public Column(Option parent, String name, double timePoint)
    {
        this(parent, name, name, timePoint);
    }

    public Column(Option parent, String name, String newName, double timePoint)
    {
        super(parent);
        this.name = name;
        this.newName = newName;
        this.timePoint = timePoint;
    }

    public static double generateTimePoint(String name)
    {
        try
        {
            if( name.contains("Column") )
                return Double.parseDouble(name.replace("Column", ""));

            return Double.parseDouble(name);
        }
        catch( Exception ex )
        {
            return 0;
        }
    }
    public String getName()
    {
        return name;
    }

    public void setName(String str)
    {
        name = str;
    }

    public String getNewName()
    {
        return newName;
    }
    public void setNewName(String str)
    {
        String oldVal = newName;
        newName = str;
        firePropertyChange("newName", oldVal, newName);
    }

    public double getTimePoint()
    {
        return timePoint;
    }

    public void setTimePoint(double tp)
    {
        double oldVal = timePoint;
        timePoint = tp;
        firePropertyChange("timePoint", oldVal, tp);
        if(getParent() != null)
            ( (ColumnGroup)getParent() ).resetColumns();
    }

    @Override
    public String toString()
    {
        return getName();
    }

    @Override
    public boolean equals(Object o)
    {
        if( o == this )
            return true;
        if( o == null || ! ( o instanceof Column ) )
            return false;
        Column c = (Column)o;
        if( ( name == null && c.name != null ) || ( name != null && !name.equals(c.name) ) )
            return false;
        if( ( newName == null && c.newName != null ) || ( newName != null && !newName.equals(c.newName) ) )
            return false;
        if( ( timePoint == null && c.timePoint != null ) || ( timePoint != null && !timePoint.equals(c.timePoint) ) )
            return false;
        return true;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( name == null ) ? 0 : name.hashCode() );
        result = prime * result + ( ( newName == null ) ? 0 : newName.hashCode() );
        result = prime * result + ( ( timePoint == null ) ? 0 : timePoint.hashCode() );
        return result;
    }

    @Override
    public int compareTo(Column other)
    {
        int result = Double.compare( this.timePoint, other.timePoint );
        if(result == 0)
            result = this.name.compareTo(other.name);
        return result;
    }

    public Column clone(Option parent)
    {
        return new Column(parent, this.getName(), this.getNewName(), this.getTimePoint());
    }
}
