package ru.biosoft.gui;

import java.beans.PropertyChangeListener;

import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.repository.RepositoryListener;
import ru.biosoft.access.repository.RepositoryPane;
import ru.biosoft.graphics.editor.ViewPane;
import ru.biosoft.graphics.editor.ViewPaneListener;

import com.developmentontheedge.beans.swing.PropertyInspector;
import com.developmentontheedge.beans.web.HtmlPropertyInspector;

/**
 * Panel to view/edit some bean or {@link ru.biosoft.access.core.DataElement}. This panel consists of: <ul>
 * <li><b>toolbar bar</b>. This toolbar includes <i>'backward'</i> and <i>'forward'</i> buttons
 * for navigation provided by {@link HtmlPropertyInspector}.
 * <li><b>view pane</b> explores the bean using {@link HtmlPropertyInspector}.</li>
 * <li><b>edit pane</b> allows user to edit bean properties using classic {@link PropertyInspector}.</li> </ul>
 * ExplorerPane implements {@link RepositoryListener} and {@link ViewPaneListener} interfaces.
 * When it is registered as a listener then it is automatically explores selectead ru.biosoft.access.core.DataElement
 * or bean in {@link RepositoryPane} or {@link ViewPane}. {@link ActionsProvider} is used to specify what particular actions
 * (for example open, delete) can be applied to selected {@link ru.biosoft.access.core.DataElement}
 */
public class ExplorerPane extends EditorsTabbedPane
{
    ////////////////////////////////////////////////////////////////////////////
    // Constructor
    //

    public ExplorerPane()
    {
        propertiesEditor = new PropertiesEditor();
        propertiesView = new PropertiesView();

        addTab(propertiesView);
        addTab(propertiesEditor);

        selectTab(0);
    }

    protected PropertiesView propertiesView;
    public PropertiesView getPropertiesView()
    {
        return propertiesView;
    }

    protected PropertiesEditor propertiesEditor;
    public PropertiesEditor getPropertiesEditor()
    {
        return propertiesEditor;
    }

    public void addBeanPropertyChangeListener(PropertyChangeListener pcl)
    {
        propertiesEditor.addPropertyChangeListener(null, pcl);
    }

    public void removeBeanPropertyChangeListener(PropertyChangeListener pcl)
    {
        propertiesEditor.removePropertyChangeListener(null, pcl);
    }

    @Override
    public void updateTab()
    {
        super.updateTab();
        if( tabPane.getSelectedIndex() == 0 )
            propertiesView.initTemplatesPanel(toolbar);
    }
}
