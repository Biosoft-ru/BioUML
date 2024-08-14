
package ru.biosoft.util;

import java.util.ListResourceBundle;

public class FileItemMessageBundle extends ListResourceBundle
{
    @Override
    protected Object[][] getContents()
    {
        return new Object[][] {
            { "CN_FILE",  "File" },
            { "CD_FILE",  "File properties" },
        
            { "PN_NAME",  "Name" },
            { "PD_NAME",  "File name" },
        
            { "PN_SUFFIX",  "Suffix" },
            { "PD_SUFFIX",  "File extension (excluding '.')" },
        
            { "PN_NAME_NO_SUFFIX",  "Name without suffix" },
            { "PD_NAME_NO_SUFFIX",  "Name without suffix" },
        
        };
    }
}
