package ru.biosoft.table.datatype;

import org.json.JSONException;
import org.json.JSONObject;

import ru.biosoft.graphics.CompositeView;

public class ViewDataType extends DataType
{
    public ViewDataType()
    {
        super( CompositeView.class, "View", null );
    }
    @Override
    public Object convertValue(Object value)
    {
        if( value instanceof CompositeView )
            return value;
        if(value == null)
            return new CompositeView();
        try
        {
            return new CompositeView(new JSONObject(value.toString()));
        }
        catch( JSONException ex )
        {
            return new CompositeView();
        }
    }
}