package biouml.plugins.ensembl.access;

import java.sql.Connection;
import java.util.Properties;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;

import ru.biosoft.access.ClassLoading;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.ReadOnlyVectorCollection;
import ru.biosoft.access.SqlDataCollection;
import ru.biosoft.access.exception.BiosoftSQLException;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.access.sql.SqlConnectionHolder;
import ru.biosoft.access.sql.SqlConnectionPool;
import ru.biosoft.access.sql.SqlUtil;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.server.MapClientServerTransformer;
import ru.biosoft.util.TextUtil;
import biouml.model.Module;
import biouml.plugins.server.access.AccessProtocol;
import biouml.plugins.server.access.ClientDataCollection;

/**
 * @author lan
 *
 */
public class EnsemblSequenceSetsCollection extends ReadOnlyVectorCollection<SqlDataCollection<AnnotatedSequence>> implements SqlConnectionHolder
{
    private static final Predicate<String> IS_CHROMOSOME = Pattern.compile( "^(\\d+|X|Y)$" ).asPredicate();
    private static final String CHROMOSOMES_KEY = "chromosomes";
    private String[] chromosomes;

    public EnsemblSequenceSetsCollection(DataCollection<?> parent, Properties properties)
    {
        super(parent, properties);
        if(properties.containsKey(CHROMOSOMES_KEY))
            chromosomes = TextUtil.split( properties.getProperty(CHROMOSOMES_KEY), ',' );
    }

    @Override
    protected void doInit()
    {
        try
        {
            Connection connection = getConnection();
            Object[] row = SqlUtil.queryRow(connection, "SELECT coord_system_id,version FROM coord_system WHERE coord_system.name='chromosome' AND coord_system.rank=1", Integer.class, String.class);
            int coordSystemId = (Integer)row[0];
            String version = (String)row[1];

            if(chromosomes == null)
            {
                chromosomes = SqlUtil
                        .stringStream(
                                connection,
                                "SELECT DISTINCT sr.name FROM karyotype JOIN seq_region sr USING(seq_region_id) WHERE coord_system_id="
                                        + coordSystemId ).filter( IS_CHROMOSOME ).append( "MT" ).distinct().toArray( String[]::new );
            }

            Properties properties = new Properties();
            properties.setProperty(DataCollectionConfigConstants.NAME_PROPERTY, "chromosomes "+version);
            properties.setProperty( DataCollectionConfigConstants.CLASS_PROPERTY, EnsemblSequenceCollection.class.getName() );
            properties.setProperty(DataCollectionConfigConstants.DATA_ELEMENT_CLASS_PROPERTY, AnnotatedSequence.class.getName());
            properties.setProperty(SqlDataCollection.SQL_TRANSFORMER_CLASS, EnsemblSequenceTransformer.class.getName());
            String pluginId = ClassLoading.getPluginForClass( getClass() );
            if(pluginId != null)
                properties.setProperty( DataCollectionConfigConstants.PLUGINS_PROPERTY, pluginId );

            properties.setProperty(ClientDataCollection.CLIENT_DATA_ELEMENT_CLASS_PROPERTY, AnnotatedSequence.class.getName());
            properties.setProperty(AccessProtocol.TEXT_TRANSFORMER_NAME, MapClientServerTransformer.class.getName());

            properties.setProperty(EnsemblSequenceTransformer.COORD_SYSTEM_KEY, String.valueOf(coordSystemId));
            properties.setProperty(EnsemblSequenceTransformer.CHROMOSOMES_KEY, String.join(",", chromosomes));
            properties.setProperty(DataCollectionConfigConstants.CAN_OPEN_AS_TABLE, String.valueOf(true));

            Module module = Module.optModule(this);
            if( module != null )
            {
                String genomeId = module.getInfo().getProperty( "genomeBuild" );
                if( genomeId != null )
                    properties.setProperty( "genomeBuild", genomeId );
            }

            EnsemblSequenceCollection dc = new EnsemblSequenceCollection(this, properties);
            setPropagationEnabled(false);
            doPut(dc, true);
            setPropagationEnabled(true);
        }
        catch( RuntimeException e )
        {
            throw ExceptionRegistry.translateException(e);
        }
    }

    @Override
    public Connection getConnection() throws BiosoftSQLException
    {
        return SqlConnectionPool.getConnection(this);
    }

    @Override
    public @Nonnull Class<? extends DataElement> getDataElementType()
    {
        return DataCollection.class;
    }
}
