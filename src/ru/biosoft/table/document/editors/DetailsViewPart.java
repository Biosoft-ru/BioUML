package ru.biosoft.table.document.editors;

import java.awt.BorderLayout;
import java.util.List;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import ru.biosoft.access.core.DataElement;
import ru.biosoft.gui.Document;
import ru.biosoft.gui.ViewPartSupport;
import ru.biosoft.table.DescribedString;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.TableColumn;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.document.TableDocument;

/**
 * View part for {@link DescribedString} columns
 */
public class DetailsViewPart extends ViewPartSupport implements ListSelectionListener
{
    private final JEditorPane mainPane;

    public DetailsViewPart()
    {
        mainPane = new JEditorPane("text/html", "");
        add(new JScrollPane(mainPane), BorderLayout.CENTER);
    }

    @Override
    public JComponent getView()
    {
        return this;
    }

    @Override
    public void explore(Object model, Document document)
    {
        if( this.document instanceof TableDocument )
        {
            ( (TableDocument)this.document ).removeSelectionListener(this);
        }
        ( (TableDocument)document ).addSelectionListener(this);

        this.model = model;
        this.document = document;

        updateView();
    }

    @Override
    public boolean canExplore(Object model)
    {
        if( model instanceof TableDataCollection )
        {
            for( TableColumn column : ( (TableDataCollection)model ).getColumnModel() )
            {
                if( column.getValueClass() == DescribedString.class )
                    return true;
            }
        }
        return false;
    }

    @Override
    public Action[] getActions()
    {
        return new Action[0];
    }

    @Override
    public void valueChanged(ListSelectionEvent arg0)
    {
        updateView();
    }

    protected void updateView()
    {
        List<DataElement> selectedDe = ( (TableDocument)document ).getSelectedItems();
        if( selectedDe.size() > 0 )
        {
            StringBuilder html = new StringBuilder("<html>");

            for( ru.biosoft.access.core.DataElement de : selectedDe )
            {
                html.append("<h3>");
                html.append( ( (RowDataElement)de ).getName());
                html.append("</h3>");
                for( TableColumn column : ( (TableDataCollection)model ).getColumnModel() )
                {
                    if( column.getValueClass() == DescribedString.class )
                    {
                        Object value = ( (RowDataElement)de ).getValue(column.getName());
                        if( value instanceof DescribedString )
                        {
                            html.append("<h4>");
                            html.append(column.getName());
                            html.append(" (");
                            html.append( ( (DescribedString)value ).getTitle());
                            html.append(")</h4>");
                            html.append("<p>");
                            html.append( ( (DescribedString)value ).getHtml());
                            html.append("</p>");
                        }
                    }
                }
            }
            mainPane.setText(html.toString());
        }
    }
}
