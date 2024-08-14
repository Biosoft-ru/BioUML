package ru.biosoft.plugins.jri.rdirect;

class Message
{
    static enum MessageType
    {
        OUT, ERR, EXIT, CANCEL, TIME_OUT;
    }
    
    final MessageType type;
    final String data;

    public Message(MessageType type, String data)
    {
        this.type = type;
        this.data = data;
        if(data != null)
        {
            System.out.print(data);
        }
    }
}