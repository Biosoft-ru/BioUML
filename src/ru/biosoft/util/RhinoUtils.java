package ru.biosoft.util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.NativeObject;

/**
 * Mozilla Rhino-related utils
 * @author lan
 *
 */
public class RhinoUtils
{
    public static JSONObject toJSONObject(NativeObject object) throws JSONException
    {
        JSONObject result = new JSONObject();
        for(Object id: object.getIds())
        {
            result.put(id.toString(), convertValue(object.get(id.toString(), null)));
        }
        return result;
    }
    
    public static JSONArray toJSONArray(NativeArray array) throws JSONException
    {
        JSONArray result = new JSONArray();
        long length = array.getLength();
        for(int i=0; i<length; i++)
        {
            result.put(i, convertValue(array.get(i, null)));
        }
        return result;
    }

    private static Object convertValue(Object object) throws JSONException
    {
        if(object instanceof NativeJavaObject) object = ((NativeJavaObject)object).unwrap();
        if(object instanceof NativeObject) return toJSONObject((NativeObject)object);
        if(object instanceof NativeArray) return toJSONArray((NativeArray)object);
        return object;
    }
}
