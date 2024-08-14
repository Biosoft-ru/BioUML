package biouml.plugins.ensembl.access;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;

import biouml.model.Module;
import biouml.plugins.ensembl.EnsemblConstants;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.SqlDataCollection;
import ru.biosoft.access.SqlTransformerSupport;
import ru.biosoft.access.core.DataElementReadException;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.StrandType;
import ru.biosoft.bsa.TrackUtils;
import ru.biosoft.bsa.VariationElement;

public class VariationTransformer extends SqlTransformerSupport<VariationElement>
{
    private static final String QUERY = "SELECT v.name name,s.name chr,seq_region_start,seq_region_end,seq_region_strand,allele_string " +
            "FROM %VARIATION_DB%.variation v " +
            "JOIN %VARIATION_DB%.variation_feature using(variation_id) " +
            "JOIN %VARIATION_DB%.seq_region s using(seq_region_id)";
    private DataCollection<AnnotatedSequence> sequences;
    private DataElementPath sequencesPath;
    private String variationDb;

    @Override
    public void addInsertCommands(Statement statement, VariationElement de) throws Exception
    {
        throw new UnsupportedOperationException("Collection is readonly");
    }

    @Override
    public VariationElement create(ResultSet resultSet, Connection connection) throws Exception
    {
        Sequence sequence = null;
        try
        {
            if(sequences == null)
                sequences = sequencesPath.getDataCollection(AnnotatedSequence.class);
            sequence = sequences.get(resultSet.getString("chr")).getSequence();
        }
        catch( Exception e )
        {
        }
        return new VariationElement(owner, resultSet.getString("name"), sequence, resultSet.getInt("seq_region_start"), resultSet.getInt("seq_region_end"), resultSet.getInt("seq_region_strand")==1?StrandType.STRAND_PLUS:StrandType.STRAND_MINUS, resultSet.getString("allele_string"));
    }

    @Override
    public String getSelectQuery()
    {
        return QUERY.replace("%VARIATION_DB%", variationDb);
    }

    @Override
    public Class<VariationElement> getTemplateClass()
    {
        return VariationElement.class;
    }

    @Override
    public void addDeleteCommands(Statement statement, String name) throws Exception
    {
        throw new UnsupportedOperationException("Collection is readonly");
    }

    @Override
    public void addUpdateCommands(Statement statement, VariationElement de) throws Exception
    {
        throw new UnsupportedOperationException("Collection is readonly");
    }

    @Override
    public String getCountQuery()
    {
        return "SELECT COUNT(*) FROM "+variationDb+".variation";
    }

    @Override
    public String getCreateTableQuery(String tableName)
    {
        return null;
    }

    @Override
    public String getElementExistsQuery(String name)
    {
        return "SELECT 1 FROM "+variationDb+".variation WHERE name="+validateValue(name);
    }

    @Override
    public String getElementQuery(String name)
    {
        return getSelectQuery() + "WHERE v.name="+validateValue(name);
    }

    @Override
    public String getNameListQuery()
    {
        return "SELECT name FROM "+variationDb+".variation";
    }

    @Override
    public boolean init(SqlDataCollection<VariationElement> owner)
    {
        super.init(owner);
        try
        {
            Module module = Module.getModule(owner);
            Properties properties = module.getInfo().getProperties();
            Properties trackProperties = ((DataCollection<?>)module.get("Tracks")).getInfo().getProperties();
            variationDb = properties.getProperty(EnsemblConstants.VARIATION_DB_PROPERTY, trackProperties.getProperty(EnsemblConstants.VARIATION_DB_PROPERTY));
            sequencesPath = TrackUtils.getPrimarySequencesPath(module.getCompletePath());
            this.sequences = sequencesPath.getDataCollection(AnnotatedSequence.class);
            if(variationDb == null)
                throw new DataElementReadException( module, EnsemblConstants.VARIATION_DB_PROPERTY );
            return true;
        }
        catch( Exception e )
        {
            throw ExceptionRegistry.translateException( e );
        }
    }
}
