package biouml.workbench.graphsearch;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.Action;
import javax.swing.JPanel;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.PanelInfo;
import com.developmentontheedge.application.PanelManager;
import com.developmentontheedge.application.action.ActionInitializer;
import com.developmentontheedge.application.action.ActionManager;
import com.developmentontheedge.beans.swing.PropertyInspector;
import com.developmentontheedge.beans.swing.TabularPropertyInspector;

import biouml.workbench.diagram.DiagramDocument;
import biouml.workbench.graphsearch.actions.AddDiagramElementAction;
import biouml.workbench.graphsearch.actions.AddElementsToDiagramAction;
import biouml.workbench.graphsearch.actions.CleanAction;
import biouml.workbench.graphsearch.actions.CreateResultDiagramAction;
import biouml.workbench.graphsearch.actions.SearchAction;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.gui.Document;
import ru.biosoft.gui.ViewPartSupport;

/**
 * View part for graph search functionality
 */
public class GraphSearchViewPart extends ViewPartSupport
{
    protected PanelManager panels;

    protected TabularPropertyInspector elementsTable;
    protected List<SearchElement> elements;

    protected PropertyInspector propertiesPane;
    private GraphSearchOptions graphSearchOptions;

    //actions
    protected AddDiagramElementAction addDiagramElementAction = null;
    protected SearchAction searchAction = null;
    protected AddElementsToDiagramAction addToDiagramAction = null;
    protected CreateResultDiagramAction createResultDiagramAction = null;
    protected CleanAction cleanAction = null;

    /**
     * Target diagrams is the set of documents which used for graph search
     * result added without. If current document is in set elements will be
     * added without confirm request.
     */
    protected Set<DiagramDocument> targetDiagrams = new HashSet<>();

    public GraphSearchViewPart()
    {
        panels = new PanelManager();
        elementsTable = new TabularPropertyInspector();

        PanelInfo leftInfo = new PanelInfo("search elements", elementsTable, true, null);
        panels.addPanel(leftInfo, null, 0);
        JPanel rightPane = new JPanel(new BorderLayout());
        PanelInfo rightInfo = new PanelInfo("search properties", rightPane, true, null);
        panels.addPanel(rightInfo, "search elements", PanelInfo.RIGHT, 400);

        add(panels);

        elements = new ArrayList<>();

        propertiesPane = new PropertyInspector();
        
        rightPane.add(propertiesPane, BorderLayout.CENTER);
        propertiesPane.explore(getOptions());
    }

    @Override
    public boolean canExplore(Object model)
    {
        return true;
    }

    @Override
    public void explore(Object model, Document document)
    {
        super.explore(model, document);

        //initialize AddDiagramElementAction for opened document
        if( document instanceof DiagramDocument )
        {
            addDiagramElementAction.putValue(AddDiagramElementAction.DIAGRAM_DOCUMENT, document);
            addDiagramElementAction.setEnabled(true);
        }
        else
        {
            addDiagramElementAction.setEnabled(false);
        }
    }

    /**
     * Initialize view part actions
     */
    protected void initActions()
    {
        ActionManager actionManager = Application.getActionManager();
        if( addDiagramElementAction == null )
        {
            addDiagramElementAction = new AddDiagramElementAction();
            actionManager.addAction(AddDiagramElementAction.KEY, addDiagramElementAction);

            new ActionInitializer(MessageBundle.class).initAction(addDiagramElementAction, AddDiagramElementAction.KEY);

            addDiagramElementAction.putValue(AddDiagramElementAction.SEARCH_PANE, this);
            addDiagramElementAction.setEnabled(false);
        }

        if( searchAction == null )
        {
            searchAction = new SearchAction();
            actionManager.addAction(SearchAction.KEY, searchAction);

            new ActionInitializer(MessageBundle.class).initAction(searchAction, SearchAction.KEY);

            searchAction.putValue(AddDiagramElementAction.SEARCH_PANE, this);
            searchAction.setEnabled(false);
        }

        if( addToDiagramAction == null )
        {
            addToDiagramAction = new AddElementsToDiagramAction();
            actionManager.addAction(AddElementsToDiagramAction.KEY, addToDiagramAction);

            new ActionInitializer(MessageBundle.class).initAction(addToDiagramAction, AddElementsToDiagramAction.KEY);

            addToDiagramAction.putValue(AddElementsToDiagramAction.SEARCH_PANE, this);
            addToDiagramAction.setEnabled(false);
        }

        if( createResultDiagramAction == null )
        {
            createResultDiagramAction = new CreateResultDiagramAction();
            actionManager.addAction(CreateResultDiagramAction.KEY, createResultDiagramAction);

            new ActionInitializer(MessageBundle.class).initAction(createResultDiagramAction, CreateResultDiagramAction.KEY);

            createResultDiagramAction.putValue(CreateResultDiagramAction.SEARCH_PANE, this);
        }

        if( cleanAction == null )
        {
            cleanAction = new CleanAction();
            actionManager.addAction(CleanAction.KEY, cleanAction);

            new ActionInitializer(MessageBundle.class).initAction(cleanAction, CleanAction.KEY);

            cleanAction.putValue(CleanAction.SEARCH_PANE, this);
        }
    }

    @Override
    public Action[] getActions()
    {
        initActions();
        return new Action[] {addDiagramElementAction, searchAction, addToDiagramAction, createResultDiagramAction, cleanAction};
    }

    /**
     * Get current search options
     */
    public GraphSearchOptions getOptions()
    {
        if(graphSearchOptions == null)
            graphSearchOptions = new GraphSearchOptions(CollectionFactoryUtils.getDatabases());
        return graphSearchOptions;
    }

    /**
     * Refresh options pane
     */
    public void invalidateOptions()
    {
        propertiesPane.explore(getOptions());
    }

    /**
     * Get elements for adding
     */
    public SearchElement[] getAddElements()
    {
        return elements.stream().filter( SearchElement::isAdd ).toArray( SearchElement[]::new );
    }

    /**
     * Get current input elements
     */
    public SearchElement[] getInputElements()
    {
        return elements.stream().filter( SearchElement::isUse ).toArray( SearchElement[]::new );
    }

    /**
     * Add element to input list
     */
    public void addInputElement(SearchElement element)
    {
        if( !elements.contains(element) )
        {
            element.setUse(true);
            elements.add(element);
            elementsTable.explore(elements.iterator());
        }
        setSearchEnabled();
    }

    /**
     * Set search action available if input element exists
     */
    protected void setSearchEnabled()
    {
        if( hasElements(true) )
        {
            searchAction.setEnabled(true);
        }
        else
        {
            searchAction.setEnabled(false);
        }
    }

    /**
     * Add elements to output list
     */
    public void addOutputElements(SearchElement[] newElements)
    {
        if( newElements != null )
        {
            for( SearchElement se : newElements )
            {
                int pos = elements.indexOf(se);
                if( pos > -1 )
                {
                    SearchElement e = elements.get(pos);
                    e.setUse(false);
                    if( e.getLinkedFromPath() == null )
                    {
                        e.setLinkedDirection(se.getLinkedDirection());
                        e.setLinkedFromPath(se.getLinkedFromPath());
                        e.setLinkedLength(se.getLinkedLength());
                        e.setLinkedPath(se.getLinkedPath());
                        e.setRelationType(se.getRelationType());
                    }
                }
                else
                {
                    se.setUse(false);
                    elements.add(se);
                }
            }
            elementsTable.explore(elements.iterator());
        }

        //set add elements to diagram action available if output element exists
        if( hasElements(false) )
        {
            addToDiagramAction.setEnabled(true);
        }
        else
        {
            addToDiagramAction.setEnabled(false);
        }
    }

    /**
     * Get current output elements
     */
    public SearchElement[] getOutputElements()
    {
        List<SearchElement> outputElements = new ArrayList<>();
        for( SearchElement se : elements )
        {
            if( !se.isUse() )
            {
                outputElements.add(se);
            }
        }
        return outputElements.toArray(new SearchElement[outputElements.size()]);
    }

    /**
     * Clear all elements
     */
    public void removeElements()
    {
        elements.clear();
        elementsTable.explore(elements.iterator());
        setSearchEnabled();
    }

    /**
     * Look for input(output) elements
     */
    protected boolean hasElements(boolean useStatus)
    {
        for( SearchElement se : elements )
        {
            if( se.isUse() == useStatus )
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if target document set contains document
     */
    public boolean isDocumentInTargetSet(DiagramDocument document)
    {
        return targetDiagrams.contains(document);
    }

    /**
     * Add document to target document set. Usually this method calls
     * after confirmation user request to allow other add requests
     * without confirmation.
     */
    public void addDocumentToTargetSet(DiagramDocument document)
    {
        targetDiagrams.add(document);
    }

    /**
     * Update properties pane
     */
    public void updateProperties()
    {
        propertiesPane.explore(getOptions());
    }
}
