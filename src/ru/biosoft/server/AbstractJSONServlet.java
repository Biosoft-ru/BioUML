package ru.biosoft.server;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.json.JSONArray;
import org.json.JSONObject;

import ru.biosoft.access.security.Permission;
import ru.biosoft.access.security.SecurityManager;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author tolstyh
 * Abstract {@link ServletExtension} implementation with common JSON functions
 */
public abstract class AbstractJSONServlet extends AbstractServlet
{
    protected static final Logger log = Logger.getLogger(AbstractJSONServlet.class.getName());

    //
    // Response constants
    //
    public static final String TYPE_OK = "ok";
    public static final String TYPE_ERROR = "error";

    public static final String ATTR_TYPE = "type";
    public static final String ATTR_VALUE = "value";
    public static final String ATTR_MESSAGE = "message";

    /**
     * Get simple successful response
     */
    protected JSONObject simpleOkResponse() throws Exception
    {
        JSONObject root = new JSONObject();
        root.put(ATTR_TYPE, TYPE_OK);
        return root;
    }

    /**
     * Get successful response with values
     */
    protected JSONObject complexOkResponse(JSONObject result) throws Exception
    {
        JSONObject root = new JSONObject();
        root.put(ATTR_TYPE, TYPE_OK);
        root.put(ATTR_VALUE, result);
        return root;
    }

    /**
     * Get successful response with array value
     */
    protected JSONObject arrayOkResponse(JSONArray result) throws Exception
    {
        JSONObject root = new JSONObject();
        root.put(ATTR_TYPE, TYPE_OK);
        root.put(ATTR_VALUE, result);
        return root;
    }

    /**
     * Get result for errors
     */
    protected JSONObject errorResponse(String error) throws Exception
    {
        JSONObject root = new JSONObject();
        root.put(ATTR_TYPE, TYPE_ERROR);
        root.put(ATTR_MESSAGE, error);
        return root;
    }

    /**
     * Get simple string parameter from servlet input parameters
     */
    protected String getStringParameter(Map<Object, Object[]> params, String key)
    {
        Object array = params.get(key);
        if( array != null )
        {
            Object value = ( (Object[])array )[0];
            return (String)value;
        }
        return null;
    }
    
    protected String getStrictParameter(Map params, String key)
    {
        String parameter = getStringParameter(params, key);
        if(parameter == null) throw new IllegalArgumentException("Missing parameter '"+key+"'");
        return parameter;
    }

    /**
     * Get map of string parameter from servlet input parameters
     */
    protected Map<String, String> getStringParameters(Map params)
    {
        Map<String, String> stringParams = new HashMap<>();
        for( Object key : params.keySet() )
        {
            String strKey = key.toString();
            String value = getStringParameter(params, strKey);
            stringParams.put(strKey, value);
        }
        return stringParams;
    }

    protected void login(Map params) throws IllegalArgumentException
    {
        String jwToken = getStringParameter( params, "jwtoken" );
        String user = getStringParameter( params, "user" );
        String pass = getStringParameter( params, "pass" );
        if( pass == null && jwToken == null )
            throw new IllegalArgumentException( "Missing parameters 'pass' and 'jwtoken' (need at least one)" );
        String remoteAddress = getStringParameter( params, "Remote-address" );
        log.log(Level.INFO, "Trying to log in a user '" + user + "', remote address = '" + remoteAddress + "'..." );
        SecurityManager.commonLogin( user, pass, remoteAddress, jwToken );
    }

    protected void checkAdmin() throws SecurityException
    {
        if(!SecurityManager.isAdmin())
        {
            throw new SecurityException("Admin access denied for "+SecurityManager.getSessionUser());
        }   
    }
    
    protected static void checkPermission(Permission permission, String method) throws SecurityException
    {
        if(permission == null || !permission.isMethodAllowed(method))
        {  
            log.log(Level.SEVERE, "Access via method '" + method + "' denied for " +SecurityManager.getSessionUser() + "\n" + 
                 "     Permission = " + permission );
            throw new SecurityException("Access via method '" + method + "' denied for " +SecurityManager.getSessionUser() );
        }  
    }

    protected static String getSQLString(String str)
    {
        String result = str;
        result = result.replaceAll("[@\\.\\-\\ ]+", "_");
        result = result.replaceAll("\\W", "");
        return result;
    }

    private static final String charset = "0123456789abcdefghijklmnopqrstuvwxyz";
    /**
     * Generate random string. Is useful for password generation
     * @param length
     * @return
     */
    protected static String getRandomString(int length)
    {
        Random rand = new Random(System.currentTimeMillis());
        StringBuffer sb = new StringBuffer();
        for( int i = 0; i < length; i++ )
        {
            int pos = rand.nextInt(charset.length());
            sb.append(charset.charAt(pos));
        }
        return sb.toString();
    }
}
