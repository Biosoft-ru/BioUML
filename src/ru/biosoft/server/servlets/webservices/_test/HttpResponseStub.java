package ru.biosoft.server.servlets.webservices._test;

/**
 * Stub HttpResponse for testing purposes
 * @author lan
 *
 */
public class HttpResponseStub
{
    private String contentType;
    
    public HttpResponseStub()
    {
    }

    public void setContentType(String contentType)
    {
        this.contentType = contentType;
    }

    public void setHeader(String key, String value)
    {
    }

    public void clearSession()
    {
    }
    
    public String getContentType()
    {
        return contentType;
    }
}
