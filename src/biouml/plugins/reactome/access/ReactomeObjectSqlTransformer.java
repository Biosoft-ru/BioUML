package biouml.plugins.reactome.access;

import java.beans.PropertyDescriptor;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import biouml.plugins.reactome.ReactomeSqlUtils;
import biouml.standard.type.BaseSupport;
import biouml.standard.type.Referrer;
import ru.biosoft.access.SqlTransformerSupport;
import ru.biosoft.access.sql.Query;
import ru.biosoft.access.sql.SqlConnectionHolder;
import ru.biosoft.access.sql.SqlUtil;
import ru.biosoft.util.bean.StaticDescriptor;

public abstract class ReactomeObjectSqlTransformer<T extends Referrer> extends SqlTransformerSupport<T>
{
    protected static final String databaseObjectTable = "DatabaseObject";
    
    private static final Query STABLE_ID_QUERY = query( "SELECT identifier FROM $table$ dbt INNER JOIN StableIdentifier si"
            + " ON(dbt.stableIdentifier=si.DB_ID) WHERE dbt.DB_ID=$id$" );
    private static final Query CREATION_DATE_QUERY = query("SELECT _displayName FROM $table$ WHERE DB_ID=$id$");
    
    protected static final PropertyDescriptor INNER_ID_PD = StaticDescriptor.createHidden("InnerID");
    protected static final PropertyDescriptor CLASS_PD = StaticDescriptor.create("Class");
    protected static final PropertyDescriptor COMPARTMENT_PD = StaticDescriptor.create("compartment");
    protected static final PropertyDescriptor ORGANISM_PD = StaticDescriptor.create("Organism");

    protected abstract T createElement(ResultSet resultSet, Connection connection) throws SQLException;
    
    protected abstract String getReactomeObjectClass();
    
    protected SqlConnectionHolder getConnectionHolder()
    {
        return owner;
    }

    @Override
    public T create(ResultSet resultSet, Connection connection) throws Exception
    {
        T referrer = createElement(resultSet, connection);
        String fullTitle = resultSet.getString(3);
        String title = fullTitle.replace( " (name copied from entity in Homo sapiens)", "" )
                .replace( " (the coordinates are copied over from Homo sapiens)", "" );
        referrer.setTitle(title);
        referrer.setDate(getCreationDate(resultSet.getString(4), connection));

        DynamicPropertySet dps = referrer.getAttributes();
        dps.add(new DynamicProperty(INNER_ID_PD, String.class, resultSet.getString(5)));
        dps.add(new DynamicProperty(CLASS_PD, String.class, resultSet.getString(2)));

        return referrer;
    }

    protected String getStableIdentifier(String name, Connection connection)
    {
        return SqlUtil.queryString( connection, STABLE_ID_QUERY.str( name ) );
    }

    @Override
    public String getSelectQuery()
    {
        return "SELECT DISTINCT si.identifier, _class, _displayName, created, dbt.DB_ID FROM " + databaseObjectTable
                + " dbt INNER JOIN StableIdentifier si ON(dbt.stableIdentifier=si.DB_ID)";
    }

    protected String getCollectionNameByClass(String className)
    {
        return ReactomeSqlUtils.getCollectionNameByClass(className);
    }

    @Override
    public void addInsertCommands(Statement statement, T de) throws Exception
    {
    }

    @Override
    public void addDeleteCommands(Statement statement, String name) throws Exception
    {
    }

    protected String getCreationDate(String creationId, Connection connection)
    {
        return SqlUtil.queryString( connection, CREATION_DATE_QUERY.str( creationId ) );
    }

    protected String getCompartmentQuery(String name, String compTblName)
    {
        return "SELECT DISTINCT _displayName, compartment_rank FROM " + compTblName + " pe2c" + " INNER JOIN " + databaseObjectTable
                + " dbo ON (dbo.DB_ID = pe2c.compartment)" + " WHERE pe2c.DB_ID='" + name + "' ORDER BY compartment_rank";
    }


    protected String getSummation(String name, String sumTblName, Connection connection)
    {
        List<String> comments = SqlUtil.queryStrings(connection, "SELECT DISTINCT text FROM Summation s INNER JOIN " + sumTblName
                + " sumdb  ON (sumdb.summation = s.DB_ID)" + " WHERE sumdb.DB_ID='" + name + "'");
        if( comments.size() > 0 )
            return String.join("; ", comments);
        return null;
    }

    protected String[] getLiteratureReferences(String name, String refTblName, Connection connection) throws SQLException
    {
        try(Statement statement = connection.createStatement();
        ResultSet literRefsSet = statement
                .executeQuery("SELECT literatureReference, journal, year, surname, literatureReference_rank" + " FROM " + refTblName
                        + " refdb INNER JOIN LiteratureReference lr ON (refdb.literatureReference=lr.DB_ID)"
                        + " LEFT JOIN Publication_2_author lr2a ON (refdb.literatureReference=lr2a.DB_ID)"
                        + " INNER JOIN Person p ON (lr2a.author=p.DB_ID)" + " WHERE refdb.DB_ID = '" + name
                        + "' ORDER BY literatureReference_rank"))
        {
            String curref = null;
            List<String> liter = new ArrayList<>();
            List<String> authors = new ArrayList<>();
            String journal = "";
            String year = "";
            while( literRefsSet.next() )
            {
                String refId = literRefsSet.getString(1);
                if( curref != null && curref.equals(refId) )
                {
                    authors.add(literRefsSet.getString(4));
                }
                else
                {
                    if( curref != null )
                    {
                        String ref = "";
                        if( authors.size() > 2 )
                        {
                            ref = authors.get(0) + " et al.";
                        }
                        else
                        {
                            ref = String.join(" and ", authors);
                        }
                        liter.add(String.join(", ", ref, journal, year));
                        authors.clear();
                    }
                    curref = refId;
                    journal = literRefsSet.getString(2);
                    year = literRefsSet.getString(3);
                    authors.add(literRefsSet.getString(4));
                }
            }
            if( curref != null )
            {
                String ref = "";
                if( authors.size() > 2 )
                {
                    ref = authors.get(0) + " et al.";
                }
                else
                {
                    ref = String.join(" and ", authors);
                }
                liter.add(String.join(", ", ref, journal, year));
            }
            if( liter.size() > 0 )
                return liter.toArray(new String[liter.size()]);
            return null;
        }
    }
    
    protected static String getReactomeId(BaseSupport object)
    {
        DynamicPropertySet dps = object.getAttributes();
        Object id = dps.getValue("InnerID");
        if(id != null)
        {
            return id.toString();
        }
        return "";
    }

    protected static Query query(String str)
    {
        return new Query(str).raw("table", databaseObjectTable);
    }
}
