package ru.biosoft.server.servlets.webservices;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.developmentontheedge.beans.DynamicPropertySet;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.exception.LoggedException;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.server.Response;
import ru.biosoft.server.servlets.webservices.providers.WebScriptsProvider.WebJSEnvironment;

/**
 * Response for JSON
 */
public class JSONResponse extends Response
{
    protected static final Logger log = Logger.getLogger(JSONResponse.class.getName());

    public static final int TYPE_OK = 0;
    public static final int TYPE_ERROR = 1;
    public static final int TYPE_ADDITIONAL = 2;
    public static final int TYPE_INVALID = 3;

    public static final String ATTR_TYPE = "type";
    public static final String ATTR_MESSAGE = "message";
    public static final String ATTR_ERROR_CODE = "code";
    public static final String ATTR_ERROR_PARAMETERS = "parameters";
    public static final String ATTR_VALUES = "values";
    public static final String ATTR_DICTIONARIES = "dictionaries";
    public static final String ATTR_ACTIONS = "actions";
    public static final String ATTR_LEFT = "left";
    public static final String ATTR_TOP = "top";
    public static final String ATTR_WIDTH = "width";
    public static final String ATTR_HEIGHT = "height";
    public static final String ATTR_SIZE = "size";
    public static final String ATTR_REFRESH_AREA = "refreshArea";
    public static final String ATTR_TABLES = "tables";
    public static final String ATTR_IMAGES = "images";
    public static final String ATTR_HTML = "html";
    public static final String ATTR_STATUS = "status";
    public static final String ATTR_PERCENT = "percent";
    public static final String ATTR_EXPORTERS = "exporters";
    public static final String ATTR_COLUMNS = "columns";
    public static final String ATTR_FROM = "from";
    public static final String ATTR_TO = "to";
    public static final String ATTR_RESULTS = "results";
    public static final String ATTR_ATTRIBUTES = "attributes";

    public JSONResponse(BiosoftWebResponse resp)
    {
        this(resp.getOutputStream());
        resp.setContentType("application/json");
    }

    public JSONResponse(OutputStream os)
    {
        super(os, null);
    }

    private boolean jsonSent = false;
    @Override
    public void error(String message) throws IOException
    {
        if( jsonSent )
        {
            log.log(Level.SEVERE, "Attempting to send JSON twice: second attempt is ignored; message was " + message);
            return;
        }
        OutputStreamWriter ow = new OutputStreamWriter(os, "UTF8");
        new JsonObject().add( ATTR_TYPE, TYPE_ERROR ).add( ATTR_MESSAGE, message ).writeTo( ow );
        ow.flush();
        jsonSent = true;
    }

    @Override
    public void error(Throwable t) throws IOException
    {
        if( jsonSent )
        {
            log.log(Level.SEVERE, "Attempting to send JSON twice: second attempt is ignored; error was " + t);
            return;
        }
        OutputStreamWriter ow = new OutputStreamWriter(os, "UTF8");
        JsonObject obj = new JsonObject();
        obj.add(ATTR_TYPE, TYPE_ERROR);
        obj.add(ATTR_MESSAGE, t.getMessage() == null ? "Internal server error: "+t.getClass().getName() : t.getMessage());
        if(t instanceof WebException)
        {
            obj.add(ATTR_ERROR_CODE, ((WebException)t).getId());
            JsonArray arr = new JsonArray();
            for( Object parameter : ( (WebException)t ).getParameters() )
            {
                arr.add( parameter == null ? "" : parameter.toString() );
            }
            obj.add( ATTR_ERROR_PARAMETERS, arr );
        } else if(t instanceof LoggedException)
        {
            obj.add( ATTR_ERROR_CODE, ( (LoggedException)t ).getDescriptor().getCode() );
        } else
        {
            obj.add(ATTR_ERROR_CODE, t.getClass().getName());
        }
        obj.writeTo( ow );
        ow.flush();
        jsonSent = true;
    }

    @Override
    public void send(byte[] message, int format) throws IOException
    {
        OutputStreamWriter ow = new OutputStreamWriter(os, "UTF8");
        JsonObject obj = new JsonObject();
        obj.add(ATTR_TYPE, TYPE_OK);
        if( message != null )
        {
            String value = null;
            if( message.length == 1 )
            {
                value = String.valueOf(message[0]);
            }
            else
            {
                value = new String(message, "UTF-16BE");
            }
            obj.add(ATTR_VALUES, value);
        }
        obj.writeTo( ow );
        ow.flush();
    }

    public void sendJSON(JsonValue value) throws IOException
    {
        OutputStreamWriter ow = new OutputStreamWriter(os, "UTF8");
        new JsonObject().add( ATTR_TYPE, TYPE_OK ).add( ATTR_VALUES, value ).writeTo( ow );
        ow.flush();
    }

    public void sendJSON(Object values) throws IOException
    {
        OutputStreamWriter ow = new OutputStreamWriter(os, "UTF8");
        try
        {
            JSONWriter writer = new JSONWriter(ow);
            writer.object()
                .key(ATTR_TYPE)
                .value(TYPE_OK)
                .key(ATTR_VALUES);
            if(values instanceof ByteArrayOutputStream)
            {
                ow.flush();
                ( (ByteArrayOutputStream)values ).writeTo(os);
                os.write('}');
            }
            else
            {
                writer.value(values);
                writer.endObject();
            }
        }
        catch( JSONException e )
        {
            throw ExceptionRegistry.translateException( e );
        }
        ow.flush();
    }

    public void sendString(String values) throws IOException
    {
        OutputStreamWriter ow = new OutputStreamWriter(os, "UTF8");
        new JsonObject().add( ATTR_TYPE, TYPE_OK ).add( ATTR_VALUES, values ).writeTo( ow );
        ow.flush();
    }
    
    public void sendStringArray(String ... values) throws IOException
    {
        OutputStreamWriter ow = new OutputStreamWriter(os, "UTF8");
        JsonArray arr = new JsonArray();
        for(String val : values )
            arr.add(val);
        new JsonObject().add( ATTR_TYPE, TYPE_OK ).add(ATTR_VALUES, arr).writeTo( ow );
        ow.flush();
    }

    @Override
    public void sendDPSArray(DynamicPropertySet[] dpsArray) throws IOException
    {
        throw new UnsupportedOperationException( "DPS array mode not supported in JSON response anymore: use /web/lucene provider instead" );
    }

    public void sendActions(JsonObject[] actions) throws IOException
    {
        JsonArray actionsArray = new JsonArray();
        for( JsonObject action : actions )
        {
            actionsArray.add(action);
        }
        OutputStreamWriter ow = new OutputStreamWriter(os, "UTF8");
        new JsonObject().add(ATTR_TYPE, TYPE_OK).add(ATTR_VALUES, actionsArray).writeTo( ow );
        ow.flush();
    }

    public void sendSizeParameters(Dimension size, Rectangle refreshArea) throws IOException
    {
        JsonObject root = new JsonObject();
        root.add(ATTR_TYPE, TYPE_OK);
        if( size != null )
        {
            root.add(ATTR_SIZE, new JsonObject().add(ATTR_WIDTH, size.width).add(ATTR_HEIGHT, size.height));
        }
        if( refreshArea != null )
        {
            JsonObject areaObj = new JsonObject().add(ATTR_LEFT, refreshArea.x).add(ATTR_TOP, refreshArea.y)
                    .add(ATTR_WIDTH, refreshArea.width).add(ATTR_HEIGHT, refreshArea.height);
            root.add(ATTR_REFRESH_AREA, areaObj);
        }

        OutputStreamWriter ow = new OutputStreamWriter(os, "UTF8");
        ow.write(root.toString());
        ow.flush();
    }

    public void sendEnvironment(WebJSEnvironment environment) throws IOException
    {
        JsonArray tables = new JsonArray();
        for( String tName : environment.getTables() )
        {
            tables.add(tName);
        }

        JsonArray images = new JsonArray();
        for( String iName : environment.getImages() )
        {
            images.add(iName);
        }

        JsonArray htmls = new JsonArray();
        for( String html : environment.getHTML() )
        {
            htmls.add(html);
        }

        JsonObject root = new JsonObject()
            .add(ATTR_TYPE, TYPE_OK)
            .add(ATTR_VALUES, environment.getBuffer())
            .add(ATTR_TABLES, tables)
            .add(ATTR_IMAGES, images)
            .add(ATTR_HTML, htmls);

        OutputStreamWriter ow = new OutputStreamWriter(os, "UTF8");
        ow.write(root.toString());
        ow.flush();
    }

    /**
     * Send bean with additional attributes object
     * @param jsonArray - bean properties
     * @param attributes - bean attributes
     * @throws IOException
     */
    public void sendJSONBean(JSONArray jsonArray, JSONObject attributes) throws IOException
    {
        JSONObject root = new JSONObject();
        try
        {
            root.put(ATTR_TYPE, TYPE_OK);
            root.put(ATTR_VALUES, jsonArray);
            if(attributes != null)
                root.put(ATTR_ATTRIBUTES, attributes);
        }
        catch( JSONException e )
        {
            log.log(Level.SEVERE, "JSON exception", e);
        }

        OutputStreamWriter ow = new OutputStreamWriter(os, "UTF8");
        try
        {
            root.write(ow);
        }
        catch( JSONException e )
        {
            throw new IOException("Unable to write JSON", e);
        }
        ow.flush();
    }

    public void sendJSONBean(JSONArray jsonArray) throws IOException
    {
        sendJSONBean(jsonArray, null);
    }

    public void sendStatus(int status, int percent, String... values) throws IOException
    {
        sendStatus(status, percent, null, values);
    }

    public void sendStatus(int status, int percent,  ru.biosoft.access.core.DataElementPath[] resultPaths, String... values) throws IOException
    {
        JsonObject root = new JsonObject();
        root.add(ATTR_TYPE, TYPE_OK);
        root.add(ATTR_STATUS, status);
        if( percent >= 0 )
        {
            root.add(ATTR_PERCENT, percent);
        }
        if( values != null )
        {
            JsonArray array = new JsonArray();
            for( String val : values )
                array.add(val.toString());
            root.add(ATTR_VALUES, array);
        }
        if( resultPaths != null )
        {
            JsonArray array = new JsonArray();
            for( DataElementPath path : resultPaths )
                array.add(path.toString());
            root.add(ATTR_RESULTS, array);
        }

        OutputStreamWriter ow = new OutputStreamWriter(os, "UTF8");
        ow.write(root.toString());
        ow.flush();
    }

    public void sendTableExportInfo(String exporters, String columns, int from, int to) throws IOException
    {
        OutputStreamWriter ow = new OutputStreamWriter(os, "UTF8");
        new JsonObject()
            .add(ATTR_TYPE, TYPE_OK)
            .add(ATTR_EXPORTERS, exporters)
            .add(ATTR_COLUMNS, columns)
            .add(ATTR_FROM, from)
            .add(ATTR_TO, to).writeTo( ow );
        ow.flush();
    }

    public void sendAdditionalJSON(JSONObject values) throws IOException
    {
        JSONObject root = new JSONObject();
        try
        {
            root.put(ATTR_TYPE, TYPE_ADDITIONAL);
            if(values != null) root.put(ATTR_VALUES, values);
        }
        catch( JSONException e )
        {
            log.log(Level.SEVERE, "JSON exception", e);
        }

        OutputStreamWriter ow = new OutputStreamWriter(os, "UTF8");
        try
        {
            root.write(ow);
        }
        catch( JSONException e )
        {
            throw new IOException("Unable to write JSON", e);
        }
        ow.flush();
    }

    public void sendInvalidResponse(String message) throws IOException
    {
        OutputStreamWriter ow = new OutputStreamWriter(os, "UTF8");
        new JsonObject().add( ATTR_TYPE, TYPE_INVALID ).add( ATTR_MESSAGE, message ).writeTo( ow );
        ow.flush();
    }
}
