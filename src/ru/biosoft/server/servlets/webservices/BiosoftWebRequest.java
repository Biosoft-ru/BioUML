package ru.biosoft.server.servlets.webservices;

import java.awt.Point;
import java.util.Map;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.json.JSONArray;
import org.json.JSONException;

import biouml.plugins.server.access.AccessProtocol;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;

public class BiosoftWebRequest
{
    /**
     * Name of the parameter representing an action
     */
    public static final String ACTION = "action";

    private final Map<String, String> arguments;

    public BiosoftWebRequest(Map<String, String> arguments)
    {
        this.arguments = arguments;
    }
    
    /**
     * 
     * @return
     * @deprecated try to avoid
     */
    @Deprecated
    public Map<String, String> getArguments()
    {
        return arguments;
    }
    
    private Object fileItem;
    public Object getFileItem()
    {
        return fileItem;
    }
    public void setFileItem(Object fileItem)
    {
        this.fileItem = fileItem;
    }

    public int optInt(String keyName, int defaultValue)
    {
        return (int)optDouble( keyName, defaultValue );
    }
    
    public double optDouble(String keyName, double defaultValue)
    {
        if(keyName == null) return defaultValue;
        String argument = arguments.get(keyName);
        if(argument == null) return defaultValue;
        try
        {
            return Double.parseDouble(argument);
        }
        catch( NumberFormatException e )
        {
            return defaultValue;
        }
    }
    
    public int optInt(String keyName)
    {
        return optInt(keyName, 0);
    }
    
    public int getInt(String keyName) throws WebException
    {
        String str = getString( keyName );
        try {
            return Integer.parseInt( str );
        } catch(NumberFormatException e){
            throw new WebException( "EX_QUERY_PARAM_INVALID_VALUE", keyName );    
        }
    }
    
    public @Nonnull Point getPoint(String xValue, String yValue)
    {
        return new Point(optInt(xValue), optInt(yValue));
    }
    
    public @Nonnull Point getPoint()
    {
        return getPoint("x", "y");
    }
    
    public boolean getBoolean(String keyName)
    {
        return Boolean.parseBoolean(arguments.get(keyName));
    }
    
    public @Nonnull String getString(String keyName) throws WebException
    {
        String str = arguments.get(keyName);
        if(str == null) throw new WebException("EX_QUERY_PARAM_MISSING", keyName);
        return str;
    }
    
    public @CheckForNull String get(String keyName)
    {
        return arguments.get(keyName);
    }
    
    public String getOrDefault(String key, String defValue)
    {
        return arguments.getOrDefault( key, defValue );
    }

    public @Nonnull String getAction() throws WebException
    {
        return getString(ACTION);
    }
    
    public @CheckForNull String optAction()
    {
        return get(ACTION);
    }
    
    public @Nonnull JSONArray getJSONArray(String keyName) throws WebException
    {
        JSONArray result = optJSONArray(keyName);
        if(result == null)
            throw new WebException("EX_QUERY_PARAM_MISSING", keyName);
        return result;
    }
    
    public @CheckForNull JSONArray optJSONArray(String keyName) throws WebException
    {
        String str = get(keyName);
        if(str == null || str.isEmpty())
            return null;
        try
        {
            return new JSONArray(str);
        }
        catch(JSONException e)
        {
            throw new WebException(e, "EX_QUERY_PARAM_NO_JSON", keyName);
        }
    }
    
    public @Nonnull String[] getStrings(String keyName) throws WebException
    {
        String[] result = optStrings(keyName);
        if(result == null)
            throw new WebException("EX_QUERY_PARAM_MISSING", keyName);
        return result;
    }
    
    public @CheckForNull String[] optStrings(String keyName) throws WebException
    {
        JSONArray jsonArray = optJSONArray(keyName);
        if(jsonArray == null) return null;
        String[] result;
        try
        {
            result = new String[jsonArray.length()];
            for(int i=0; i<jsonArray.length(); i++)
            {
                result[i] = jsonArray.getString(i);
            }
        }
        catch( JSONException e )
        {
            throw new WebException(e, "EX_QUERY_PARAM_NO_JSON", keyName);
        }
        return result;
    }
    
    public @Nonnull String getElementName(String keyName) throws WebException
    {
        String str = getString(keyName);
        if(str.isEmpty()) throw new WebException("EX_INPUT_NAME_EMPTY");
        if(str.contains("/")) throw new WebException("EX_INPUT_NAME_INVALID", str);
        return str;
    }
    
    public @Nonnull DataElementPath getDataElementPath(String keyName) throws WebException
    {
        return DataElementPath.create(getString(keyName).trim());
    }
    
    public @Nonnull DataElementPath getDataElementPath() throws WebException
    {
        return getDataElementPath(AccessProtocol.KEY_DE);
    }
    
    public @Nonnull ru.biosoft.access.core.DataElement getDataElement() throws WebException
    {
        return getDataElement(DataElement.class, AccessProtocol.KEY_DE);
    }
    
    public static @Nonnull <T extends DataElement> T castDataElement(DataElement dataElement, Class<T> clazz) throws WebException
    {
        if(dataElement == null)
            throw new WebException("EX_QUERY_NO_ELEMENT_TYPE", "(null)", DataCollectionUtils.getClassTitle(clazz));
        if( !clazz.isInstance(dataElement) )
            throw new WebException("EX_QUERY_INVALID_ELEMENT_TYPE", DataElementPath.create(dataElement), DataCollectionUtils.getClassTitle(clazz));
        return (T)dataElement;
    }
    
    public static @Nonnull <T extends DataElement> T getDataElement(DataElementPath path, Class<T> clazz) throws WebException
    {
        DataElement dataElement = path.optDataElement();
        if( dataElement == null )
            throw new WebException("EX_QUERY_NO_ELEMENT_TYPE", path, DataCollectionUtils.getClassTitle(clazz));
        return castDataElement(dataElement, clazz);
    }
    
    public @Nonnull <T extends DataElement> T getDataElement(Class<T> clazz, String keyName) throws WebException
    {
        return getDataElement(getDataElementPath(keyName), clazz);
    }
    
    public @Nonnull <T extends DataElement> T getDataElement(Class<T> clazz) throws WebException
    {
        return getDataElement(clazz, AccessProtocol.KEY_DE);
    }
    
    public @Nonnull DataCollection<?> getDataCollection(String keyName) throws WebException
    {
        return getDataElement(DataCollection.class, keyName);
    }
    
    public @Nonnull DataCollection<?> getDataCollection() throws WebException
    {
        return getDataCollection(AccessProtocol.KEY_DE);
    }
}
