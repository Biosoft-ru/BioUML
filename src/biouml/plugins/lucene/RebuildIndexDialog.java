package biouml.plugins.lucene;

import java.util.logging.Level;
import java.util.List;

import javax.swing.JDialog;
import javax.swing.JFrame;

import java.util.logging.Logger;

import ru.biosoft.access.ClassLoading;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.server.ClientConnection;
import ru.biosoft.util.OkCancelDialog;
import biouml.workbench.module.StatusInfoDialog;

import com.developmentontheedge.application.Application;
import ru.biosoft.jobcontrol.FunctionJobControl;
import ru.biosoft.jobcontrol.JobControl;
import com.developmentontheedge.server.JobControlClient;

/**
 * Rebuild lucene indexes dialog
 */
public class RebuildIndexDialog extends OkCancelDialog
{
    protected Logger log = Logger.getLogger(RebuildIndexDialog.class.getName());

    protected MessageBundle messageBundle = new MessageBundle();

    private DataCollection<?> module = null;

    private LuceneQuerySystem luceneFacade;

    private RebuildIndexPane pane = null;

    public RebuildIndexDialog(JDialog dialog, String title, String[] dc, LuceneQuerySystem luceneFacade, DataCollection<?> module) throws Exception
    {
        super(dialog, title);

        init(dc, luceneFacade, module);
        setResizable(false);
    }

    public RebuildIndexDialog(JFrame frame, String title, String[] dc, LuceneQuerySystem luceneFacade, DataCollection<?> module) throws Exception
    {
        super(frame, title);

        init(dc, luceneFacade, module);
        setResizable(false);
    }
    
    public RebuildIndexDialog(JFrame frame, LuceneQuerySystem luceneFacade) throws Exception
    {
        this(frame, "", null, luceneFacade, luceneFacade.getCollection());
        setTitle(messageBundle.getResourceString("LUCENE_REBUILD_INDEX_TITLE"));
    }

    private void init(String[] dc, LuceneQuerySystem luceneFacade, DataCollection<?> module) throws Exception
    {
        this.module = module;
        this.luceneFacade = luceneFacade;

        pane = new RebuildIndexPane(dc, luceneFacade, false);

        setContent(pane);
    }

    private JobControl jobControl = null;

    @Override
    protected void okPressed()
    {
        super.okPressed();

        if( luceneFacade instanceof LuceneQuerySystemClient )
        {
            LuceneQuerySystemClient lc = (LuceneQuerySystemClient)luceneFacade;
            // System.out.println("Connect to job control with id \"" +
            // lc.serverDCname + "\"");
            String connectionClassName = module.getInfo().getProperty(ClientConnection.CONNECTION_TYPE);
            if( connectionClassName != null )
            {
                try
                {
                    String pluginNames = module.getInfo().getProperty(DataCollectionConfigConstants.PLUGINS_PROPERTY);
                    Class<? extends ClientConnection> connectionClass = ClassLoading.loadSubClass( connectionClassName, pluginNames, ClientConnection.class );
                    jobControl = new JobControlClient( Logger.getLogger( LuceneQuerySystem.class.getName() ), connectionClass, lc.serverDCname,
                            lc.getHost() );
                }
                catch( Exception e )
                {
                }
            }
        }
        if( jobControl == null )
            jobControl = new FunctionJobControl(null);

        final StatusInfoDialog infoDialog = new StatusInfoDialog(Application.getApplicationFrame(), messageBundle
                .getResourceString("LUCENE_REBUILD_INDEX_PROGRESS_TITLE"), log, jobControl);

        Thread thread = new Thread()
        {
            @Override
            public void run()
            {
                try
                {
                    infoDialog.setInfo(messageBundle.getResourceString("LUCENE_REBUILD_INDEX_PROGRESS_TITLE") + "...");

                    List<String> list = pane.getUserChoose();
                    if( list != null && !list.isEmpty() )
                    {
                        if( list.size() == luceneFacade.getCollectionsNamesWithIndexes().size() )
                        {
                            luceneFacade.createIndex(log, jobControl);
                        }
                        else
                        {
                            for( String indexName : list )
                            {
                                luceneFacade.addToIndex(indexName, true, log, jobControl);
                            }
                        }
                    }
                }
                catch( Throwable t )
                {
                    log.log(Level.SEVERE, t.getMessage());
                    infoDialog.fails();
                    return;
                }
                infoDialog.success();
            }
        };

        infoDialog.startProcess(thread);
    }

}
