package ru.biosoft.table.columnbeans;

import java.util.ListResourceBundle;


public class MessageBundle extends ListResourceBundle
{
    @Override
    protected Object[][] getContents()
    {
        return new Object[][] {
                //Column beans
                {"PN_COLUMN_GROUP", "Columns: name\\new name\\time point"},
                {"PD_COLUMN_GROUP", "Group of columns for analysis"},
                {"PN_COLUMN_TIME_POINT", "time points"},
                {"PD_COLUMN_TIME_POINT", "time points"},
                {"PN_COLUMN_NEW_NAME", "new names"},
                {"PD_COLUMN_NEW_NAME", "new names"},
                {"PN_COLUMNS", "Columns"},
                {"PD_COLUMNS", "List of columns to use"},
                {"PN_TABLE", "Table"},
                {"PD_TABLE", "Table path"},
                {"PN_COLUMN_NAME", "name"},
                {"PD_COLUMN_NAME", "name"},
        };
    }
}
