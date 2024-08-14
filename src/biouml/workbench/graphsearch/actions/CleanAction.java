package biouml.workbench.graphsearch.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import java.util.logging.Level;
import java.util.logging.Logger;

import biouml.workbench.graphsearch.GraphSearchViewPart;

/**
 * Remove all input elements and search results, update search options.
 */
public class CleanAction extends AbstractAction
{
    protected Logger log = Logger.getLogger(CleanAction.class.getName());

    public static final String KEY = "Clean action";

    public static final String SEARCH_PANE = "SearchPane";

    public CleanAction()
    {
        super(KEY);
    }

    @Override
    public void actionPerformed(ActionEvent event)
    {
        GraphSearchViewPart searchPane = (GraphSearchViewPart)getValue(SEARCH_PANE);
        if( searchPane == null )
        {
            log.log(Level.SEVERE, "Search view part is undefined");
            return;
        }

        searchPane.removeElements();

        searchPane.getOptions().getTargetOptions().setUseForAll(false);
        searchPane.updateProperties();
    }
}
