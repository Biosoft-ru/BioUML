package ru.biosoft.access.support;

import java.awt.Component;
import java.awt.Dimension;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.util.OkCancelDialog;

public class DataCollectionMultyChoiceDialog extends OkCancelDialog
{
    protected DataCollectionMultyChoicePane pane;

    public DataCollectionMultyChoiceDialog(Component parent, String title, DataCollection dc, String[] selectedValues, boolean isSorted)
    {
        this(parent, title, dc, null, selectedValues, isSorted);
    }

    public DataCollectionMultyChoiceDialog(Component parent, String title, DataCollection dc, String property, String[] selectedValues,
            boolean isSorted)
    {
        super(parent, title);
        setPreferredSize(new Dimension(700, 400));

        pane = new DataCollectionMultyChoicePane(dc, property, selectedValues, isSorted);
        setContent(pane);
    }

    public String[] getSelectedValues()
    {
        return pane.getSelectedValues();
    }
}
