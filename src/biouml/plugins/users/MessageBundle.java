package biouml.plugins.users;

import java.util.ListResourceBundle;

public class MessageBundle extends ListResourceBundle
{
    @Override
    protected Object[][] getContents()
    {
        return contents;
    }

    public String getResourceString(String key)
    {
        try
        {
            return getString(key);
        }
        catch( Throwable th )
        {

        }
        return key;
    }

    private final static Object[][] contents = {
        //chat dialog properties
        {"CHAT_DIALOG_TITLE", "Chat with {0}"},
        {"CHAT_DIALOG_STATUS", "Status: "},
        {"CHAT_DIALOG_SEND", "Send"},
        {"CHAT_DIALOG_CLOSE", "Close"}
    };
}