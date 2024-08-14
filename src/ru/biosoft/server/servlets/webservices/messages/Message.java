package ru.biosoft.server.servlets.webservices.messages;

import org.json.JSONObject;

public class Message
{
    private String type;
    private JSONObject content;
    
    public Message(String type, JSONObject content)
    {
        this.type = type;
        this.content = content;
    }
    
    public String getType()
    {
        return type;
    }
    
    public JSONObject getContent()
    {
        return content;
    }
}
