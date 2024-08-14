package ru.biosoft.access.biohub;

import java.util.ListResourceBundle;

/**
 * Constants for graph search package
 */
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
        catch (Throwable th)
        {

        }
        return key;
    }

    private final Object[][] contents =
    {
        {"CN_COLLECTIONS", "Collections"},
        {"CD_COLLECTIONS", "Collections"},

        {"CN_COLLECTION_NAME", "Name"},
        {"CD_COLLECTION_NAME", "Collection name"},

        {"CN_COLLECTION_USE", "Use in search"},
        {"CD_COLLECTION_USE", "Use in search"},

        {"CN_SEARSH_ENGINE", "Search engine"},
        {"CD_SEARSH_ENGINE", "Search engine"},
    };
}
