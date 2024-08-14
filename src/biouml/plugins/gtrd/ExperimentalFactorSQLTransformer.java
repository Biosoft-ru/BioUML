package biouml.plugins.gtrd;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import ru.biosoft.access.SqlDataCollection;
import ru.biosoft.access.SqlTransformerSupport;

public class ExperimentalFactorSQLTransformer extends SqlTransformerSupport<ExperimentalFactor>
{

	@Override
    public boolean init(SqlDataCollection<ExperimentalFactor> owner)
    {
        this.table = "exp_factors_classification";
        this.idField = "name";
        return super.init( owner );
    }
	
	@Override
	public Class<ExperimentalFactor> getTemplateClass() 
	{
		
		return ExperimentalFactor.class;
	}
	private static final String COLUMNS = "name,title,parent,description";
	
	@Override
	public String getSelectQuery()
    {
        return "SELECT " + COLUMNS + " FROM exp_factors_classification";
    }
	
	@Override
    public String getElementQuery(String name)
    {
        
		
		return "SELECT " + COLUMNS + " FROM exp_factors_classification WHERE name='" + name + "'";
    }

	@Override
	public ExperimentalFactor create(ResultSet resultSet, Connection connection) throws Exception 
	{
		String factorId = resultSet.getString( 1 );
		String title = resultSet.getString( 2 );
		String parent = resultSet.getString( 3 );
        ExperimentalFactor result = new ExperimentalFactor( factorId,factorId , title, parent, owner );
        //result.setInfo(resultSet.getString( 5 ));
        result.setExRefs(resultSet.getString( 4 ) == null ? null : resultSet.getString( 4 ).split(";"));
        return result;
	}

	@Override
	public void addInsertCommands(Statement statement, ExperimentalFactor de) throws Exception {
		// TODO Auto-generated method stub
		
	}

}
