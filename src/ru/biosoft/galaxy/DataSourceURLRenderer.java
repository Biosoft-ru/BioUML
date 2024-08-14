package ru.biosoft.galaxy;

import org.json.JSONException;
import org.json.JSONObject;

import ru.biosoft.access.repository.JSONSerializable;

import com.developmentontheedge.beans.editors.CustomEditorSupport;

/**
 * @author lan
 *
 */
public class DataSourceURLRenderer extends CustomEditorSupport implements JSONSerializable
{

    @Override
    public JSONObject toJSON() throws JSONException
    {
        if(getValue() instanceof DataSourceURLBuilder)
        {
            DataSourceURLBuilder urlBuilder = (DataSourceURLBuilder)getValue();
            JSONObject result = new JSONObject();
            result.put("type", "url");
            result.put("value", urlBuilder.getAction());
            result.put("parameters", new JSONObject(urlBuilder.getParameters()));
            return result;
        }
        return null;
    }

    @Override
    public void fromJSON(JSONObject input) throws JSONException
    {
    }
}
