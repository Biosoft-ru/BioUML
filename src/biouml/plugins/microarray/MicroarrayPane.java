package biouml.plugins.microarray;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import biouml.model.Diagram;
import biouml.workbench.diagram.DiagramDocument;

import com.developmentontheedge.application.action.ActionInitializer;

public class MicroarrayPane extends JPanel
{
    public static final String APPLY = "apply";
    public static final String EDIT_ACTION = "microarray edit action";
    public static final String REMOVE_ACTION = "remove action";
    public static final String NEW_FILTER = "new filter";
    public static final String SAVE_FILTER = "save filter";
    public static final String REMOVE_FILTER = "remove filter";

    protected Action applyAction = new ApplyAction(APPLY);
    protected Action removeActionAction = new RemoveActionAction(REMOVE_ACTION);
    protected Action newFilterAction = new NewFilterAction(NEW_FILTER);
    protected Action saveFilterAction = new SaveFilterAction(SAVE_FILTER);
    protected Action removeFilterAction = new RemoveFilterAction(REMOVE_FILTER);
    protected Action[] actions;

    public static final String BINDING = "Binding";
    public static final String FILTER = "Filter";
    public static final String FILTERS = "Filters";

    protected DiagramDocument document;

    protected JTabbedPane tabbedPane;
    protected BindingTab bindingTab;
    protected FilterTab filterTab;
    protected FilterListTab filterListTab;

    public MicroarrayPane()
    {
        setLayout(new BorderLayout());

        bindingTab = new BindingTab();
        filterTab = new FilterTab();
        filterListTab = new FilterListTab();

        tabbedPane = new JTabbedPane(JTabbedPane.LEFT);
        tabbedPane.add(BINDING, bindingTab);
        tabbedPane.add(FILTER, filterTab);
        tabbedPane.add(FILTERS, filterListTab);

        add(tabbedPane, BorderLayout.CENTER);

        tabbedPane.addChangeListener(e -> {
            int index = tabbedPane.getSelectedIndex();
            if( index == 0 )
            {
                removeActionAction.setEnabled(false);
                newFilterAction.setEnabled(false);
                saveFilterAction.setEnabled(false);
                removeFilterAction.setEnabled(false);
            }
            else if( index == 1 )
            {
                removeActionAction.setEnabled(true);
                newFilterAction.setEnabled(true);
                saveFilterAction.setEnabled(true);
                removeFilterAction.setEnabled(false);
                filterTab.refreshDiagram(document.getDiagram());
            }
            else if( index == 2 )
            {
                removeActionAction.setEnabled(false);
                newFilterAction.setEnabled(false);
                saveFilterAction.setEnabled(false);
                removeFilterAction.setEnabled(true);
                filterListTab.refreshDiagram(document.getDiagram());
            }
        });

        applyAction.setEnabled(true);
        removeActionAction.setEnabled(false);
        newFilterAction.setEnabled(false);
        saveFilterAction.setEnabled(false);
        removeFilterAction.setEnabled(false);
    }

    public void setDocument(DiagramDocument document)
    {
        this.document = document;
        bindingTab.refreshDiagram(document.getDiagram());
        filterTab.refreshDiagram(document.getDiagram());
        filterListTab.refreshDiagram(document.getDiagram());
    }

    public Action[] getActions()
    {
        if( actions == null )
        {
            ActionInitializer initializer = new ActionInitializer(MessageBundle.class);
            initializer.initAction(applyAction, MicroarrayPane.APPLY);
            initializer.initAction(removeActionAction, MicroarrayPane.REMOVE_ACTION);
            initializer.initAction(newFilterAction, MicroarrayPane.NEW_FILTER);
            initializer.initAction(saveFilterAction, MicroarrayPane.SAVE_FILTER);
            initializer.initAction(removeFilterAction, MicroarrayPane.REMOVE_FILTER);

            actions = new Action[] {applyAction, removeActionAction, newFilterAction, saveFilterAction,
                    removeFilterAction};
        }

        return actions;
    }

    public void applyMicroarray()
    {
        Diagram diagram = document.getDiagram();
        if( diagram != null )
        {
            MicroarrayFilter diagramFilter = null;
            int index = tabbedPane.getSelectedIndex();
            if( index == 1 )
            {
                diagramFilter = filterTab.getCurrentFilter();
            }
            else if( index == 2 )
            {
                diagramFilter = filterListTab.getCurrentFilter();
            }
            if( diagramFilter == null )
            {
                diagramFilter = new MicroarrayFilter("default", diagram);
            }
            diagramFilter.setEnabled(true);
            diagram.setDiagramFilter(diagramFilter);
            diagramFilter.apply(diagram);
        }

        document.update();
    }

    class ApplyAction extends AbstractAction
    {
        public ApplyAction(String name)
        {
            super(name);
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            applyMicroarray();
        }
    }

    class RemoveActionAction extends AbstractAction
    {
        public RemoveActionAction(String name)
        {
            super(name);
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            filterTab.removeAction();
        }
    }

    class NewFilterAction extends AbstractAction
    {
        public NewFilterAction(String name)
        {
            super(name);
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            filterTab.newFilterAction();
        }
    }

    class SaveFilterAction extends AbstractAction
    {
        public SaveFilterAction(String name)
        {
            super(name);
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            filterTab.saveFilterAction();
        }
    }

    class RemoveFilterAction extends AbstractAction
    {
        public RemoveFilterAction(String name)
        {
            super(name);
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            filterListTab.removeFilterAction();
        }
    }
}