package biouml.plugins.proteinmodel;

import java.beans.PropertyDescriptor;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import java.util.logging.Logger;

import ru.biosoft.access.SqlDataCollection;
import ru.biosoft.access.SqlTransformerSupport;
import ru.biosoft.access.exception.BiosoftSQLException;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.access.sql.SqlConnectionHolder;
import ru.biosoft.access.sql.SqlUtil;
import ru.biosoft.util.bean.StaticDescriptor;
import biouml.standard.type.DatabaseReference;
import biouml.standard.type.Protein;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.DynamicPropertySetAsMap;

/**
 * @author lan
 *
 */
public class VirtualCellProteinTransformer extends SqlTransformerSupport<Protein>
{
    private static final Logger log = Logger.getLogger(VirtualCellProteinTransformer.class.getName());
    private static final PropertyDescriptor EXPERIMENT_NAMES_PD = StaticDescriptor.create("experimentNames", "Experiment names");
    Map<Integer, PropertyDescriptor> fields;
    Map<Integer, PropertyDescriptor> experiments;

    @Override
    public Class<Protein> getTemplateClass()
    {
        return Protein.class;
    }

    @Override
    public Protein create(ResultSet resultSet, Connection connection) throws Exception
    {
        Protein protein = new Protein(owner, resultSet.getString("id"));
        protein.setTitle(resultSet.getString("name"));
        protein.setSpecies(resultSet.getString("species"));
        protein.setDescription(resultSet.getString("title"));
        protein.setDatabaseReferences( new DatabaseReference[] {new DatabaseReference( "UniProt", protein.getName() )} );

        DynamicPropertySet attributes = protein.getAttributes();
        Set<String> experimentNames = new TreeSet<>();
        for(PropertyDescriptor experimentPD: experiments.values())
        {
            DynamicPropertySetAsMap fieldsMap = new DynamicPropertySetAsMap()
            {
                @Override
                public String toString()
                {
                    StringBuilder result = new StringBuilder();
                    for(DynamicProperty property: this)
                    {
                        Object value = property.getValue();
                        if(value != null)
                        {
                            if(result.length() > 0) result.append("; ");
                            result.append(property.getDisplayName()).append(" = ").append(value);
                        }
                    }
                    return result.toString();
                }
            };
            for(PropertyDescriptor fieldPD: fields.values())
            {
                fieldsMap.add(new DynamicProperty(fieldPD, Double.class, null));
            }
            attributes.add(new DynamicProperty(experimentPD, DynamicPropertySet.class, fieldsMap));
        }
        try (Statement st = connection.createStatement();
                ResultSet rs = st.executeQuery( "select experiment_id,field_id,value from experiment_data ed where protein_id="
                        + validateValue( protein.getName() ) ))
        {
            while(rs.next())
            {
                PropertyDescriptor experimentPd = experiments.get(rs.getInt(1));
                PropertyDescriptor fieldPd = fields.get(rs.getInt(2));
                ((DynamicPropertySet)attributes.getValue(experimentPd.getName())).setValue(fieldPd.getName(), rs.getDouble(3));
                experimentNames.add(experimentPd.getName());
            }
        }
        attributes.add(new DynamicProperty(EXPERIMENT_NAMES_PD, String[].class, experimentNames.toArray(new String[experimentNames.size()])));
        return protein;
    }

    @Override
    public void addInsertCommands(Statement statement, Protein de) throws Exception
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addUpdateCommands(Statement statement, Protein de) throws Exception
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addDeleteCommands(Statement statement, String name) throws Exception
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean init(SqlDataCollection<Protein> owner)
    {
        super.init(owner);
        table = "protein";
        idField = "id";

        fields = new HashMap<>();
        experiments = new HashMap<>();
        try
        {
            SqlUtil.iterate( ( (SqlConnectionHolder)owner ).getConnection(), "SELECT id,name,title FROM field",
                    rs -> fields.put( rs.getInt( 1 ), StaticDescriptor.create( rs.getString( 2 ), rs.getString( 3 ) ) ) );
            SqlUtil.iterate( ( (SqlConnectionHolder)owner ).getConnection(), "SELECT id,title FROM experiment",
                    rs -> experiments.put( rs.getInt( 1 ), StaticDescriptor.create( rs.getString( 2 ) ) ) );
        }
        catch( BiosoftSQLException e )
        {
            log.log(Level.SEVERE, "Unable to initialize fields: "+ExceptionRegistry.log(e));
            return false;
        }
        return true;
    }

    @Override
    public String getSelectQuery()
    {
        return "SELECT p.*,species.name species FROM "+table+" p JOIN species ON (species.id=p.species_id)";
    }

    @Override
    public String getElementQuery(String name)
    {
        return getSelectQuery()+" WHERE p.id="+validateValue(name);
    }
}
