package ru.biosoft.access.search;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import ru.biosoft.access.core.DataCollection;


public class DataSearchAction extends AbstractAction
{
    public static final String KEY = "Data Search Form";
    public static final String DATA_COLLECTION = "dataCollection";

    public DataSearchAction()
    {
        super(KEY);
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        MessageBundle messageBundle = new MessageBundle();
        DataSearch dataSearch = new DataSearch(messageBundle.getResourceString("DATA_SEARCH_TITLE"),
                (DataCollection)getValue(DATA_COLLECTION));

        dataSearch.show();
        putValue(DATA_COLLECTION, null);
    }
}
