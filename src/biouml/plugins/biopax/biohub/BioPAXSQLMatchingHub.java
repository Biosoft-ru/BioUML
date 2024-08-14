
package biouml.plugins.biopax.biohub;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import one.util.streamex.StreamEx;

import org.apache.commons.lang.ArrayUtils;

import java.util.logging.Level;
import java.util.logging.Logger;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.biohub.BioHubSupport;
import ru.biosoft.access.biohub.Element;
import ru.biosoft.access.biohub.ReferenceType;
import ru.biosoft.access.biohub.ReferenceTypeRegistry;
import ru.biosoft.access.biohub.TargetOptions;
import ru.biosoft.access.sql.SqlConnectionPool;
import ru.biosoft.access.sql.SqlUtil;
import ru.biosoft.jobcontrol.FunctionJobControl;
import ru.biosoft.jobcontrol.JobControl;

/**
 * Hub can match BioPAX collection identifiers to external identifiers and vice versa
 * Collection internal type has class CollectionSpecificReferenceType, specified by ReferenceType.REFERENCE_TYPE_PROPERTY in data collection config
 * External types are specified by ReferenceType.MATCHING_TYPE_PROPERTY property in data collection config
 */
public class BioPAXSQLMatchingHub extends BioHubSupport
{
    private Logger log = Logger.getLogger(BioPAXSQLMatchingHub.class.getName());

    private ThreadLocal<Connection> connection = new ThreadLocal<>();
    private DataCollection<?> dc = null;
    private String tableName = null;
    private ReferenceType[] types = null;
    private ReferenceType[] matchingTypes = null;
    private boolean typesInitialized = false;
    private Map<String, Double> typeQual = new HashMap<>();

    private String getOuterIdentifier = "SELECT inner_acc FROM #HUB_TABLE# WHERE outer_acc=? AND typeName='#TYPE#' AND is_main";
    private String getInnerIdentifier = "SELECT outer_acc FROM #HUB_TABLE# WHERE inner_acc=? AND typeName='#TYPE#'";

    public BioPAXSQLMatchingHub(Properties properties)
    {
        super(properties);
        init();
    }

    private void init()
    {
        String property = properties.getProperty(BioPAXSQLHubBuilder.HUB_TABLE_PROPERTY);
        if( property == null )
        {
            log.log(Level.SEVERE, "No BioPAX hub table name specified");
            return;
        }
        tableName = property + "_matching";
    }

    private void initTypes()
    {
        if( typesInitialized )
            return;
        DataElementPath modulePath = getModulePath();
        if(modulePath != null)
            dc = modulePath.optDataCollection();
        if( dc != null && ReferenceTypeRegistry.getReferenceType(dc) != null)
        {
            types = new ReferenceType[] {ReferenceTypeRegistry.getReferenceType(dc)};
            String matchingTypesProperty = dc.getInfo().getProperty(ReferenceType.MATCHING_TYPE_PROPERTY);
            if( matchingTypesProperty != null )
            {
                matchingTypes = StreamEx.split(matchingTypesProperty, ';').map( ReferenceTypeRegistry::optReferenceType ).nonNull()
                        .toArray( ReferenceType[]::new );
            }
            Connection conn = getConnection();
            if(conn == null)
            {
                log.log(Level.SEVERE, "Unable to open SQL connection for "+getName()+": hub is disabled");
                types = new ReferenceType[0];
                matchingTypes = new ReferenceType[0];
                typesInitialized = true;
                return;
            }
            int count = SqlUtil.queryInt(conn, "select count(distinct inner_acc) from "+tableName);
            if(count > 0)
            {
                try(Statement statement = conn.createStatement();
                        ResultSet resultSet = statement.executeQuery("select count(distinct inner_acc),typeName from "+tableName+" group by typeName"))
                {
                    while(resultSet.next())
                    {
                        typeQual.put(resultSet.getString(2), ((double)resultSet.getInt(1))/count);
                    }
                }
                catch( SQLException e )
                {
                    log.log(Level.SEVERE,  "SQL error for "+getName()+": hub is disabled", e );
                    types = new ReferenceType[0];
                    matchingTypes = new ReferenceType[0];
                    typesInitialized = true;
                    return;
                }
            }
        }
        if( types == null )
            types = new ReferenceType[0];
        if( matchingTypes == null )
            matchingTypes = new ReferenceType[0];
        typesInitialized = true;
    }

    @Override
    public int getPriority(TargetOptions dbOptions)
    {
        return 0;
    }

    @Override
    public Element[] getReference(Element startElement, TargetOptions dbOptions, String[] relationTypes, int maxLength, int direction)
    {
        return null;
    }

    @Override
    public double getMatchingQuality(ReferenceType inputType, ReferenceType outputType)
    {
        initTypes();
        if( inputType.equals(outputType) )
            return 0;
        boolean inputBiopax = false;
        boolean outputBiopax = false;
        for( ReferenceType type : types )
        {
            if( inputType.getStableName().equals(type.getStableName()) )
                inputBiopax = true;
            if( outputType.getStableName().equals(type.getStableName()) )
                outputBiopax = true;
        }
        if( ! ( inputBiopax || outputBiopax ) )
            return 0;

        for( ReferenceType type : matchingTypes )
        {
            if( outputBiopax && inputType.getStableName().equals(type.getStableName()) )
                return typeQual.containsKey(inputType.getSource())?typeQual.get(inputType.getSource()):0;
            if( inputBiopax && outputType.getStableName().equals(type.getStableName()) )
                return typeQual.containsKey(outputType.getSource())?typeQual.get(outputType.getSource()):0;
        }
        return 0;
    }

    @Override
    public ReferenceType[] getSupportedInputTypes()
    {
        initTypes();
        return (ReferenceType[])ArrayUtils.addAll(types, matchingTypes);
    }

    @Override
    public ReferenceType[] getSupportedMatching(ReferenceType inputType)
    {
        initTypes();
        for( ReferenceType type : types )
        {
            if( inputType.getStableName().equals(type.getStableName()) )
                return matchingTypes.clone();

        }
        for( ReferenceType type : matchingTypes )
        {
            if( inputType.getStableName().equals(type.getStableName()) )
                return types.clone();
        }
        return null;
    }

    @Override
    public Map<String, String[]> getReferences(String[] inputList, ReferenceType inputType, ReferenceType outputType,
            Properties properties, FunctionJobControl jobControl)
    {
        initTypes();
        if( dc == null )
            return null;
        try
        {
            Connection conn = getConnection();
            String query = getQuery(inputType, outputType, tableName);
            if( query == null )
                throw new Exception("Unsupported input/output type combination");
            Map<String, String[]> result = new HashMap<>();
            try(PreparedStatement ps = conn.prepareStatement(query))
            {
                List<String> curList = new ArrayList<>();
                for( int i = 0; i < inputList.length; i++ )
                {
                    curList.clear();
                    ps.setString(1, inputList[i]);
                    try(ResultSet rs = ps.executeQuery())
                    {
                        while( rs.next() )
                        {
                            curList.add(rs.getString(1));
                        }
                    }
                    result.put(inputList[i], curList.toArray(new String[curList.size()]));
                    if( jobControl != null )
                    {
                        jobControl.setPreparedness(i * 100 / inputList.length);
                        if( jobControl.getStatus() == JobControl.TERMINATED_BY_REQUEST )
                            return null;
                    }
                }
            }
            return result;
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, e.getMessage(), e);
            if( jobControl != null )
                jobControl.functionTerminatedByError(e);
            return null;
        }
    }

    private String getQuery(ReferenceType inputType, ReferenceType outputType, String refDBId)
    {
        for( ReferenceType type : types )
        {
            if( inputType.getStableName().equals(type.getStableName()) )
                return getInnerIdentifier.replaceAll("#HUB_TABLE#", refDBId).replaceAll("#TYPE#", outputType.getSource());
        }
        for( ReferenceType type : matchingTypes )
        {
            if( inputType.getStableName().equals(type.getStableName()) )
                return getOuterIdentifier.replaceAll("#HUB_TABLE#", refDBId).replaceAll("#TYPE#", inputType.getSource());
        }
        return null;
    }

    protected Connection getConnection()
    {
        try
        {
            if( connection.get() == null )
            {
                connection.set(SqlConnectionPool.getPersistentConnection(properties));
            }
        }
        catch( Exception e )
        {
            if( log != null )
                log.log(Level.SEVERE, "Connection error", e);
        }
        return connection.get();
    }
}
