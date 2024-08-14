package ru.biosoft.access.exception;

import com.eclipsesource.json.JsonObject;

import ru.biosoft.exception.ExceptionDescriptor;
import ru.biosoft.exception.LoggedException;

/**
 * @author lan
 *
 */
public class BiosoftNetworkException extends LoggedException
{
    private static final String KEY_URL = "url";
    private static final String KEY_MESSAGE = "message";

    public static final ExceptionDescriptor ED_NETWORK_COMMON = new ExceptionDescriptor( "Common", LoggingLevel.TraceIfNoCause,
            "Error communicating via url '$url$'.");
    public static final ExceptionDescriptor ED_NETWORK_MALFORMED = new ExceptionDescriptor( "Malformed", LoggingLevel.TraceIfNoCause,
            "Malformed answer received via url '$url$'.");
    public static final ExceptionDescriptor ED_REMOTE_ERROR = new ExceptionDescriptor( "RemoteError", LoggingLevel.TraceIfNoCause,
            "Error reported by remote server with url '$url$': $message$.");

    public BiosoftNetworkException(Throwable t, String url)
    {
        super(t, ED_NETWORK_COMMON);
        properties.put( KEY_URL, url );
    }

    public BiosoftNetworkException(String url, String remoteError)
    {
        super(null, ED_REMOTE_ERROR);
        properties.put( KEY_URL, url );
        properties.put( KEY_MESSAGE, remoteError );
    }

    public BiosoftNetworkException(String url, JsonObject response)
    {
        super(getDescriptor(response));
        properties.put( KEY_URL, url );
        properties.put( KEY_MESSAGE, response == null ? "(no message)" : response.getString( "message", "(no message)" ) );
    }

    private static ExceptionDescriptor getDescriptor(JsonObject response)
    {
        int responseType = response == null ? -1 : response.getInt("type", -1);
        if(responseType == -1)
            return ED_NETWORK_MALFORMED;
        return ED_REMOTE_ERROR;
    }
}
