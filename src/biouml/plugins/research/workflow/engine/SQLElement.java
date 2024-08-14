package biouml.plugins.research.workflow.engine;

import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.developmentontheedge.beans.DynamicProperty;

import biouml.plugins.server.SqlEditorClient;
import biouml.plugins.server.access.ServerRegistry;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.jobcontrol.JobControlListener;
import ru.biosoft.server.ClientConnection;
import ru.biosoft.server.ConnectionPool;
import ru.biosoft.server.Request;
import ru.biosoft.server.tomcat.TomcatConnection;
import ru.biosoft.table.TableDataCollection;

/**
 * SQL element implementation for {@link WorkflowElement}
 */
public class SQLElement extends WorkflowElement
{
    protected Logger log = Logger.getLogger(SQLElement.class.getName());

    public static final String SQL_SOURCE = "source";
    public static final String SQL_HOST = "host";

    protected String source;
    protected String host;
    protected String outputPath;
    protected boolean complete = false;

    public SQLElement(String source, String host, String outputPath, DynamicProperty statusProperty)
    {
        super(statusProperty);
        this.source = source;
        this.host = host;
        this.outputPath = outputPath;
    }

    @Override
    public boolean isComplete()
    {
        return complete;
    }

    @Override
    public void startElementExecution(JobControlListener listener)
    {
        setPreparedness(0);
        final JobControlListener jobListener = listener;
        DataElementPath dePath = DataElementPath.create(outputPath);
        DataCollection parent = dePath.optParentCollection();
        if( ( parent != null ) && ( TableDataCollection.class.isAssignableFrom(parent.getDataElementType()) ) )
        {
            String sessionId = ServerRegistry.getServerSession(host);
            if( sessionId != null )
            {
                try
                {
                    String name = dePath.getName();
                    parent.remove(name);
                    Properties properties = new Properties();
                    properties.put(DataCollectionConfigConstants.NAME_PROPERTY, name);
                    properties.put(DataCollectionConfigConstants.CLASS_PROPERTY, parent.getDataElementType().getName());
                    properties.put(DataCollectionConfigConstants.PLUGINS_PROPERTY, parent.getInfo().getProperty(DataCollectionConfigConstants.PLUGINS_PROPERTY));
                    TableDataCollection tdc = (TableDataCollection)CollectionFactory.createCollection(parent, properties);

                    ClientConnection conn = ConnectionPool.getConnection(TomcatConnection.class, host);
                    SqlEditorClient sqlClient = new SqlEditorClient(host, new Request(conn, log), log, sessionId);
                    sqlClient.fillResultTable(source, tdc);
                    sqlClient.close();

                    parent.put(tdc);
                }
                catch( Exception e )
                {
                    log.log(Level.SEVERE, "SQL error", e);
                }
            }
            complete = true;
            setPreparedness(100);
            jobListener.jobTerminated(null);
        }
        else
        {
            complete = true;
            setPreparedness(100);
            jobListener.jobTerminated(null);
        }
    }

    @Override
    public double getWeight()
    {
        return 1;
    }
}
