package ru.biosoft.access.repository;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Object which supports serializing to JSON
 * @author lan
 */
public interface JSONSerializable
{
    /**
     * Serialize editor to JSONObject including current value and all necessary information
     * to render editor elsewhere (allowed values, etc.)
     */
    public JSONObject toJSON() throws JSONException;
    
    /**
     * Extract value from JSONObject and set it to the edited property.
     * Note that editor.fromJSON(editor.toJSON()) should not harm the object.
     * If object contains unwanted fields, they should be ignored.
     * @param input - JSONObject to get value from
     */
    public void fromJSON(JSONObject input) throws JSONException;
}
