package biouml.plugins.users;

import java.util.Date;

public class MessageItem
{
    protected String msg;
    protected Date date;
    protected String from;
    protected String resource;
    
    public MessageItem(String msg, String from, String resource)
    {
        this.msg = msg;
        this.from = from;
        this.resource = resource;
        this.date = new Date();
    }

    public String getMsg()
    {
        return msg;
    }

    public Date getDate()
    {
        return date;
    }

    public String getFrom()
    {
        return from;
    }

    public String getResource()
    {
        return resource;
    }
}
