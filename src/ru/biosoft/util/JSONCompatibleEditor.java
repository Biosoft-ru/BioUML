package ru.biosoft.util;

import org.json.JSONObject;

import com.developmentontheedge.beans.model.Property;

import ru.biosoft.server.JSONUtils;

/**
 * Special interface for editors which can not be serialized/deserialized to JSON by default way.
 * See {@link JSONUtils} for usage example
 */
public interface JSONCompatibleEditor
{
    public void fillWithJSON(Property property, JSONObject jsonObject) throws Exception;
    public void addAsJSON(Property property, JSONObject p, FieldMap fieldMap, int showMode) throws Exception;
}
