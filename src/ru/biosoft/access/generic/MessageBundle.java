package ru.biosoft.access.generic;

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
        catch (Throwable th)
        {

        }
        return key;
    }
    private static final Object[][] contents =
    {
        {"FOLDER_NAME_INPUT", "Folder name:"},
        
        {"CN_GENERIC_DC", "Generic collection"},
        {"CD_GENERIC_DC", "Generic collection"},

        {"PN_DATABASE_URL", "Database URL"},
        {"PD_DATABASE_URL", "Database URL"},
        {"PN_DATABASE_USER", "Database user"},
        {"PD_DATABASE_USER", "Database user"},
        {"PN_DATABASE_PASSWORD", "Database password"},
        {"PD_DATABASE_PASSWORD", "Database password"},
        {"PN_TABLE_IMPLEMENTATION", "Table implementation"},
        {"PD_TABLE_IMPLEMENTATION", "Table implementation"},
    };
}
