package ru.biosoft.bsa.view;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JComponent;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;

import ru.biosoft.bsa.project.Project;
import ru.biosoft.bsa.project.Region;
import ru.biosoft.gui.Document;
import ru.biosoft.gui.ViewPartSupport;

import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.swing.PropertyInspector;
import com.developmentontheedge.beans.swing.TabularPropertyInspector;

@SuppressWarnings ( "serial" )
public class RegionsViewPart extends ViewPartSupport implements PropertyChangeListener
{
    public static final String VIEW_OPTIONS = "View options";
    public static final String NOTES = "Notes";

    protected TabularPropertyInspector regionsTable;
    protected JTabbedPane tabbedPane;
    protected PropertyInspector propertiesEditor;
    protected JTextArea notes;

    public RegionsViewPart()
    {
        tabbedPane = new JTabbedPane(JTabbedPane.RIGHT);
        propertiesEditor = new PropertyInspector();
        notes = new JTextArea();
        notes.setEditable(false);

        tabbedPane.add(VIEW_OPTIONS, propertiesEditor);
        tabbedPane.add(NOTES, notes);

        add(tabbedPane);
    }

    protected void selectRegion(Region region)
    {
        if( model instanceof Project )
        {
            Option viewOptions = ( (Project)model ).getViewOptions().getRegionViewOptions();
            propertiesEditor.explore(viewOptions);

            notes.setText(region.getDescription());
            notes.setEditable(true);
        }
    }

    @Override
    public JComponent getView()
    {
        return this;
    }

    @Override
    public boolean canExplore(Object model)
    {
        if( model instanceof Project )
            return true;
        return false;
    }

    @Override
    public void explore(Object model, Document document)
    {
        if( this.model instanceof Project )
        {
            ((Project)this.model).removePropertyChangeListener(this);
        }
        super.explore(model, document);

        if( this.model instanceof Project )
        {
            ((Project)this.model).addPropertyChangeListener(this);
        }
        updatePanes();
    }

    protected void updatePanes()
    {
        if( model instanceof Project )
        {
            Project project = (Project)model;
            Region[] regions = project.getRegions();

            if( regions.length > 0 )
            {
                selectRegion(regions[0]);
            }
            else
            {
                propertiesEditor.explore(null);
                notes.setText("");
                notes.setEditable(false);
            }
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        updatePanes();
    }
}
