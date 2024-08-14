package biouml.workbench.graphsearch.actions;

import java.awt.event.ActionEvent;
import java.util.logging.Level;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;

import java.util.logging.Logger;

import ru.biosoft.access.biohub.TargetOptions;
import ru.biosoft.access.biohub.TargetOptions.CollectionRecord;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.ApplicationFrame;
import ru.biosoft.jobcontrol.FunctionJobControl;

import biouml.workbench.graphsearch.GraphSearchOptions;
import biouml.workbench.graphsearch.GraphSearchViewPart;
import biouml.workbench.graphsearch.QueryEngine;
import biouml.workbench.graphsearch.QueryEngineRegistry;
import biouml.workbench.graphsearch.SearchElement;

/**
 * Core action in GraphSearch. Starts the search process.
 */
public class SearchAction extends AbstractAction
{
    protected Logger log = Logger.getLogger(SearchAction.class.getName());

    public static final String KEY = "Search action";

    public static final String SEARCH_PANE = "SearchPane";

    public SearchAction()
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

        String searchType = searchPane.getOptions().getSearchType();
        TargetOptions target = searchPane.getOptions().getTargetOptions();
        if( !target.collections().anyMatch( cr -> !QueryEngineRegistry.getQueryEngines(cr, searchType).isEmpty() ) )
        {
            JOptionPane.showMessageDialog(Application.getApplicationFrame(), "There is no available search engine for this options");
            log.log(Level.SEVERE, "Can not find QueryEngine for this options");
            return;
        }

        new SearchThread(searchPane, searchType).start();
    }

    public class SearchThread extends Thread
    {
        protected GraphSearchViewPart searchPane;
        protected String searchType;
        
        public SearchThread(GraphSearchViewPart searchPane, String searchType)
        {
            this.searchPane = searchPane;
            this.searchType = searchType;
        }

        @Override
        public void run()
        {
            try
            {
                FunctionJobControl jobControl = new FunctionJobControl(null);
                ApplicationFrame application = Application.getApplicationFrame();
                if( application != null )
                {
                    //client mode
                    jobControl.addListener(application.getStatusBar());
                }

                jobControl.functionStarted();
                
                TargetOptions target = searchPane.getOptions().getTargetOptions();
                for( CollectionRecord collection : target.collections() )
                {
                    List<QueryEngine> engines = QueryEngineRegistry.getQueryEngines(collection, searchType);
                    for(QueryEngine queryEngine: engines)
                    {
                        SearchElement[] result = null;
                        if( GraphSearchOptions.TYPE_NEIGHBOURS.equals(searchType) )
                        {
                            result = queryEngine.searchLinked(searchPane.getInputElements(), searchPane.getOptions().getQueryOptions(), searchPane
                                    .getOptions().getTargetOptions(), jobControl);
                        }
                        else if( GraphSearchOptions.TYPE_PATH.equals(searchType) )
                        {
                            result = queryEngine.searchPath(searchPane.getInputElements(), searchPane.getOptions().getQueryOptions(), searchPane
                                    .getOptions().getTargetOptions(), jobControl);
                        }
                        else if( GraphSearchOptions.TYPE_SET.equals(searchType) )
                        {
                            result = queryEngine.searchSet(searchPane.getInputElements(), searchPane.getOptions().getQueryOptions(), searchPane
                                    .getOptions().getTargetOptions(), jobControl);
                        }
                        searchPane.addOutputElements(result);
                    }
                }
                jobControl.functionFinished();
            }
            catch( Exception e )
            {
                log.log(Level.SEVERE, "Search error", e);
            }
        }
    }
}
