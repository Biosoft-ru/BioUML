
package ru.biosoft.table;

import java.util.ListResourceBundle;

/**
* String set message bundle
*/

public class StringSetMessageBundle extends ListResourceBundle
{
    @Override
    protected Object[][] getContents()
    {
        return contents;
    }

    private Object[][] contents =
    {
        { "DISPLAY_NAME",       "String set" },
        { "SHORT_DESCRIPTION",  "String set properties" },
    };
}// end of class MessagesBundle
