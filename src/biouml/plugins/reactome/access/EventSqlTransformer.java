package biouml.plugins.reactome.access;

import java.beans.PropertyDescriptor;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import ru.biosoft.access.sql.SqlDynamicProperty;
import ru.biosoft.access.sql.SqlUtil;
import ru.biosoft.util.bean.StaticDescriptor;
import biouml.standard.type.Reaction;

import com.developmentontheedge.beans.DynamicPropertySet;

public abstract class EventSqlTransformer extends ReactomeObjectSqlTransformer<Reaction>
{
    private static final PropertyDescriptor KEYWORDS_PD = StaticDescriptor.create("keywords");

    @Override
    protected abstract Reaction createElement(ResultSet resultSet, Connection connection) throws SQLException;

    @Override
    public Reaction create(ResultSet resultSet, Connection connection) throws Exception
    {
        Reaction reaction = super.create(resultSet, connection);
        
        final String reactomeId = getReactomeId(reaction);
        
        reaction.setComment(getSummation(reactomeId, "Event_2_summation", connection));
        reaction.setLiteratureReferences(getLiteratureReferences(reactomeId, "Event_2_literatureReference", connection));

        DynamicPropertySet dps = reaction.getAttributes();
        dps.add(new SqlDynamicProperty(COMPARTMENT_PD, getConnectionHolder(), getCompartmentQuery(reactomeId, "Event_2_compartment"), true));
        dps.add(new SqlDynamicProperty(KEYWORDS_PD, getConnectionHolder(), "SELECT DISTINCT keywords FROM Event" + " WHERE DB_ID="
                + SqlUtil.quoteString(reactomeId), false));
        return reaction;
    }

    @Override
    public Class<Reaction> getTemplateClass()
    {
        return Reaction.class;
    }
}
