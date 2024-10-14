package biouml.plugins.physicell;

import java.util.stream.Stream;

import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.annot.PropertyName;

import one.util.streamex.StreamEx;
import ru.biosoft.physicell.core.SignalBehavior;

public class CellDefinitionVisualizerProperties extends Option
{
    private String[] types = new String[] {"One color", "Gradient"};
    private String[] cellTypes = new String[0];
    private String[] signals = new String[] {"No signal"};
    private String cellType = "Any cell";
    private String type = "One color";
    private String signal = "No signal";
    private double min = 0;
    private double max = 100;
    private String color1 ="";
    private String color2= "";

    public void setModel(MulticellEModel model)
    {
        if( model != null )
        {
            cellTypes = StreamEx.of( model.getCellDefinitions() ).map( cd -> cd.getName() ).toArray( String[]::new );
            String[] substrates = model.getSubstrates().stream().map( s -> s.getName() ).toArray( String[]::new );
            String[] custom = Stream.of( model.getCellDefinitions().get( 0 ).getCustomDataProperties().getVariables() )
                    .map( s -> s.getName() ).toArray( String[]::new );
            signals = SignalBehavior.getSignals( substrates, cellTypes, custom ).stream().sorted().toArray( String[]::new );
        }
    }

    public StreamEx<String> getCellTypes()
    {
        return StreamEx.of( cellTypes );
    }

    public StreamEx<String> getSignals()
    {
        return StreamEx.of( signals );
    }

    public StreamEx<String> getTypes()
    {
        return StreamEx.of( types );
    }

    @PropertyName ( "Cell Type" )
    public String getCellType()
    {
        return cellType;
    }
    public void setCellType(String cellType)
    {
        this.cellType = cellType;
    }

    @PropertyName ( "Color type" )
    public String getType()
    {
        return type;
    }

    public void setType(String type)
    {
        Object oldValue = this.type;
        this.type = type;
        firePropertyChange( "type", oldValue, type );
    }

    @PropertyName ( "Signal" )
    public String getSignal()
    {
        return signal;
    }

    public void setSignal(String signal)
    {
        this.signal = signal;
    }

    @PropertyName ( "Min value" )
    public double getMin()
    {
        return min;
    }

    public void setMin(double min)
    {
        this.min = min;
    }

    @PropertyName ( "Max value" )
    public double getMax()
    {
        return max;
    }

    public void setMax(double max)
    {
        this.max = max;
    }

    @PropertyName ( "Color 1" )
    public String getColor1()
    {
        return color1;
    }

    public void setColor1(String color1)
    {
        this.color1 = color1;
    }

    @PropertyName ( "Color 2" )
    public String getColor2()
    {
        return color2;
    }

    public void setColor2(String color2)
    {
        this.color2 = color2;
    }


    public boolean isOneColor()
    {
        return type.equals( "One color" );
    }
}
