package biouml.plugins.gtrd;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import ru.biosoft.access.SqlDataCollection;
import ru.biosoft.access.SqlTransformerSupport;

public class TreatmentTransformer extends SqlTransformerSupport<Treatment>
{

    @Override
    public boolean init(SqlDataCollection<Treatment> owner)
    {
        this.table = "treatments";
        return super.init( owner );
    }
    @Override
    public Class<Treatment> getTemplateClass()
    {
        return Treatment.class;
    }

    @Override
    public Treatment create(ResultSet resultSet, Connection connection) throws Exception
    {
        return new Treatment( resultSet.getString( 1 ), owner );
    }

    @Override
    public void addInsertCommands(Statement statement, Treatment de) throws Exception
    {
    }

}
