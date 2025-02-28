package ru.biosoft.table.document.editors;

import java.awt.Component;
import java.awt.Dimension;
import java.net.URL;

import com.developmentontheedge.application.dialog.TextButtonField;
import com.developmentontheedge.beans.editors.CustomEditorSupport;

import ru.biosoft.table.TableDataCollection;
import ru.biosoft.util.IconUtils;

public class ExpressionEditor extends CustomEditorSupport
{
    protected Editor editor = new Editor();

    private void initComponent()
    {
        editor.setText( ( (TableElement)getBean() ).getExpression());
        ( editor.getTextField() ).addActionListener(e -> setValue(editor.getText()));
    }
    @Override
    public Component getCustomRenderer(Component parent, boolean isSelected, boolean hasFocus)
    {
        initComponent();
        return editor;
    }

    @Override
    public Component getCustomEditor(Component parent, boolean isSelected)
    {
        initComponent();
        return editor;
    }

    class Editor extends TextButtonField
    {
        Editor()
        {
            super("");

            URL url = getClass().getResource("resources/edit.gif");
            button.setIcon( IconUtils.getImageIcon( url ) );
            button.setPreferredSize(new Dimension(30, 20));

            button.addActionListener(e -> {
                TableDataCollection tableData = ( (TableElement)getBean() ).getTable();
                ExpressionFilterDialog dialog = new ExpressionFilterDialog(tableData);
                dialog.setValue(getValue().toString());
                if( dialog.doModal() )
                {
                    String expressionValue = dialog.getValue();
                    editor.setText(expressionValue);
                    setValue(expressionValue);
                }
            });
        }

        void setText(String name)
        {
            textField.setText(name);
        }

        public String getText()
        {
            return textField.getText();
        }
    }
}