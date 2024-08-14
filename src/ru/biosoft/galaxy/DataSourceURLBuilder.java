package ru.biosoft.galaxy;

import java.util.Map;

import one.util.streamex.EntryStream;

import ru.biosoft.galaxy.parameters.Parameter;
import ru.biosoft.util.TextUtil;

/**
 * @author lan
 *
 */
public class DataSourceURLBuilder
{
    public static final String PARAMS_TEMPLATE = "$params$";
    private final DataSourceMethodInfo info;
    private final DataSourceMethodParameters parameters;

    public DataSourceURLBuilder(DataSourceMethodInfo info, DataSourceMethodParameters parameters)
    {
        this.info = info;
        this.parameters = parameters;
    }
    
    public String getAction()
    {
        return info.getAction();
    }
    
    public Map<String, String> getParameters()
    {
        String replacement = "dc=" + TextUtil.encodeURL( parameters.getOutputPath().toString() ) + "&tool="
                + TextUtil.encodeURL( info.getCompletePath().toString() );
        return EntryStream.of( info.getParameters() ).removeValues( Parameter::isOutput ).mapValues( Object::toString )
                .mapValues( value -> value.replace( PARAMS_TEMPLATE, replacement ) ).toMap();
    }
}
