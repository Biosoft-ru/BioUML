package ru.biosoft.bsa.analysis.trackutil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;

import one.util.streamex.StreamEx;
import ru.biosoft.analysis.aggregate.NumericAggregator;
import ru.biosoft.table.datatype.DataType;

public class SiteProperty
{
    private final String name;
    private final Class<?> type;
    private final List<Object> values = new ArrayList<>();

    public SiteProperty(String name, Class<?> type)
    {
        this.name = name;
        this.type = type;
    }
    public void addValue(Object value)
    {
        values.add( value );
    }
    public DynamicProperty createDP(NumericAggregator aggregator)
    {
        DataType dataType = DataType.fromClass( type );
        if( dataType.isNumeric() )
        {
            double[] valuesArr = StreamEx.of( values ).select( Number.class ).mapToDouble( Number::doubleValue ).toArray();
            return new DynamicProperty( name, type, dataType.convertValue( aggregator.aggregate( valuesArr ) ) );
        }
        else
        {
            return new DynamicProperty( name, type, values.get( 0 ) );
        }
    }

    public static Map<String, SiteProperty> copyDynamicProperties(final Set<String> selectedProperties, DynamicPropertySet fromDPS,
            Map<String, SiteProperty> properties)
    {
        for( String selectedProperty : selectedProperties )
        {
            DynamicProperty fromDP = fromDPS.getProperty( selectedProperty );
            if( fromDP != null )
                properties.computeIfAbsent( selectedProperty, name -> new SiteProperty( name, fromDP.getType() ) )
                        .addValue( fromDP.getValue() );
        }
        return properties;
    }
}
