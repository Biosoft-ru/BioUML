
package ru.biosoft.graphics.chart;

import java.util.ListResourceBundle;

/**
* Chart message bundle
*/

public class ChartMessageBundle extends ListResourceBundle
{
    @Override
    protected Object[][] getContents()
    {
        return contents;
    }

    private Object[][] contents =
    {
        { "DISPLAY_NAME",       "Chart" },
        { "SHORT_DESCRIPTION",  "Chart properties" },
    };
}// end of class MessagesBundle
