package ru.biosoft.bsa.server;

import java.util.ListResourceBundle;

/**
 *
 */
public class MessageBundle  extends ListResourceBundle
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
        catch (Throwable th)
        {

        }
        return key;
    }
    
    public static final String STATUS_KNOWN     = "KNOWN";
    public static final String STATUS_NOVEL     = "NOVEL";
    public static final String STATUS_PUTATIVE  = "PUTATIVE";
    public static final String STATUS_PREDICTED = "PREDICTED";
    
    private Object[][] contents =
    {
        // ClientSequence constants
        {"CN_SEQUENCE"                  , "Sequence"},
        {"CD_SEQUENCE"                  , "Sequence"},

        {"PN_SEQUENCE_LENGTH"             , "Length"},
        {"PD_SEQUENCE_LENGTH"             , "Sequence length"},
    };
}
