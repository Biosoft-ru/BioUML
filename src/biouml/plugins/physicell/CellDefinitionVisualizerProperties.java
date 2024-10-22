package biouml.plugins.physicell;

import java.util.stream.Stream;

import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.annot.PropertyName;

import one.util.streamex.StreamEx;
import ru.biosoft.physicell.core.Cell;
import ru.biosoft.physicell.core.Model;
import ru.biosoft.physicell.core.SignalBehavior;

public class CellDefinitionVisualizerProperties extends Option
{
    private static final String SMALLER_THAN_MIN = "Smaller than min";
    private static final String LARGER_THAN_MAX = "Larger than max";
    private static final String GRADIENT = "Gradient";
    private static final String FIXED_COLOR = "Fixed color";

    private MulticellEModel model;
    private String[] types = new String[] {FIXED_COLOR, GRADIENT, LARGER_THAN_MAX, SMALLER_THAN_MIN};
    private String[] cellTypes = new String[0];
    private String[] signals = new String[] {"No signal"};

    private double priority = 0;
    private String cellType = "Any cell";
    private String type = FIXED_COLOR;
    private String signal = "No signal";
    private double min = 0;
    private double max = 100;
    private String color1 = "";
    private String color2 = "";

    public void setModel(MulticellEModel model)
    {
        if( model != null )
        {
            cellTypes = StreamEx.of( model.getCellDefinitions() ).map( cd -> cd.getName() ).toArray( String[]::new );
            String[] substrates = model.getSubstrates().stream().map( s -> s.getName() ).toArray( String[]::new );
            String[] custom = Stream.of( model.getCellDefinitions().get( 0 ).getCustomDataProperties().getVariables() )
                    .map( s -> s.getName() ).toArray( String[]::new );
            signals = SignalBehavior.getSignals( substrates, cellTypes, custom ).stream().sorted().toArray( String[]::new );
            this.model = model;//
            //            colorSchemes = StreamEx.of( model.getColorSchemes()).map( cs->cs.getName() ).toArray(String[]::new);
        }
    }

    public StreamEx<String> getCellTypes()
    {
        return StreamEx.of( cellTypes ).prepend( "Any cell" );
    }

    public StreamEx<String> getSignals()
    {
        return StreamEx.of( signals ).prepend( "No signal" );
    }

    public StreamEx<String> getTypes()
    {
        return StreamEx.of( types );
    }

    public StreamEx<String> getColorSchemes()
    {
        return StreamEx.of( model.getColorSchemes() ).map( cs -> cs.getName() );
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
        if( isOneColor() )
            return color1;
        return color2;
    }
    public void setColor2(String color2)
    {
        this.color2 = color2;
    }

    @PropertyName ( "Priority" )
    public double getPriority()
    {
        return priority;
    }
    public void setPriority(double priority)
    {
        this.priority = priority;
    }


    public boolean isOneColor()
    {
        return !type.equals( GRADIENT );
    }

    public ColorScheme calculate(Cell cell)
    {
        if( this.type.equals( FIXED_COLOR ) )
        {
            return getScheme( color1 );
        }
        else if( type.equals( GRADIENT ) )
        {
            double signal = getSignal( cell );
            ColorScheme scheme1 = getScheme( color1 );
            ColorScheme scheme2 = getScheme( color2 );
            return scheme1.offset( scheme2, ( signal - min ) / ( max - min ) );
        }
        else if( type.equals( LARGER_THAN_MAX ) )
        {
            double signal = getSignal( cell );
            if( signal >= max )
                return getScheme( color1 );
            return null;
        }
        else if( type.equals( SMALLER_THAN_MIN ) )
        {
            double signal = getSignal( cell );
            if( signal <= min )
                return getScheme( color1 );
            return null;
        }
        return null;
    }

    private double getSignal(Cell cell)
    {
        Model model = cell.getModel();
        int index = model.getSignals().findSignalIndex( signal );
        return model.getSignals().getSingleSignal( cell, index );
    }

    private ColorScheme getScheme(String name)
    {
        ColorScheme[] schemes = model.getColorSchemes();
        for( ColorScheme scheme : schemes )
        {
            if( scheme.getName().equals( name ) )
                return scheme;
        }
        return null;
    }
}
