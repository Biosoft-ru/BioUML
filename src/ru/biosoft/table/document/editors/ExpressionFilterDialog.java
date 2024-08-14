package ru.biosoft.table.document.editors;

import ru.biosoft.table.TableDataCollection;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.dialog.OkCancelDialog;

public class ExpressionFilterDialog extends OkCancelDialog
{
    private ColumnsFilterPanel columnsFilterPanel;

    public ExpressionFilterDialog(TableDataCollection table)
    {
        super(Application.getApplicationFrame(), "Set column expression");
        columnsFilterPanel = new ColumnsFilterPanel();
        setContent(columnsFilterPanel);
        columnsFilterPanel.initPanel(table);
    }

    public String getValue()
    {
        return columnsFilterPanel.getText();
    }

    public void setValue(String value)
    {
        columnsFilterPanel.setText(value);
    }
}
