package ru.biosoft.server.servlets.webservices;

/**
 * Exceptions thrown by web-services
 * @author lan
 */
public class WebException extends Exception
{
    private Object[] parameters;
    private String id;
    
    public WebException(String id, Object... parameters)
    {
        super(constructMessage(id, parameters));
        this.parameters = parameters;
        this.id = id;
    }
    
    public WebException(Throwable cause, String id, Object... parameters)
    {
        super(constructMessage(id, parameters), cause);
        this.parameters = parameters;
        this.id = id;
    }
    
    public String getId()
    {
        return id;
    }
    
    public Object[] getParameters()
    {
        return parameters;
    }

    private static String constructMessage(String id, Object[] parameters)
    {
        String message = MessageBundle.getInstance().getString(id);
        for(int i=0; i<parameters.length; i++)
        {
            message = message.replace("$"+(i+1), parameters[i] == null ? "(empty)" : parameters[i].toString());
        }
        return message;
    }
}
