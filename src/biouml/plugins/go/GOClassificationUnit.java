package biouml.plugins.go;

import java.sql.Connection;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import javax.annotation.Nonnull;

import ru.biosoft.access.core.AbstractDataCollection;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.exception.BiosoftSQLException;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.access.sql.Query;
import ru.biosoft.access.sql.SqlConnectionHolder;
import ru.biosoft.access.sql.SqlConnectionPool;
import ru.biosoft.access.sql.SqlUtil;
import ru.biosoft.bsa.classification.ClassificationUnit;
import ru.biosoft.util.HtmlUtil;

import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.DynamicPropertySetAsMap;

/**
 * @author lan
 */
public class GOClassificationUnit extends AbstractDataCollection<ClassificationUnit> implements ClassificationUnit, SqlConnectionHolder
{
    private boolean init = false;
    private String description;
    private String title;
    private List<String> children;

    private synchronized void init()
    {
        if(init) return;
        init = true;
        try
        {
            log.info("Querying name: " + getName()); 
            Object[] row = SqlUtil
                    .queryRow(getConnection(), new Query(
                            "select name,term_definition,t.id from term t left join term_definition td on(t.id=term_id) where acc=$name$")
                            .str(getName()), String.class, String.class, String.class);
            if(row != null)
            {
                title = (String)row[0];
                description = (String)row[1];
                String id = (String)row[2];
                log.info("Got title: " + title + ", id: " + id + ", description: " + description);
                children = SqlUtil.queryStrings(getConnection(), new Query(
                        "select acc from term2term tt join term t on(term2_id=t.id) where is_obsolete=0 and term1_id=$id$ order by 1")
                        .str(id));
            }
        }
        catch( BiosoftSQLException e )
        {
            valid = false;
            throw e;
        }
    }

    public GOClassificationUnit(DataCollection<?> parent, Properties properties)
    {
        super(parent, properties);
    }

    @Override
    public String getClassName()
    {
        init();
        return title;
    }

    @Override
    public String getClassNumber()
    {
        return getName();
    }

    int level = -1;
    @Override
    public String getLevel()
    {
        if(level == -1)
        {
            for(DataCollection<?> origin = this; origin instanceof ClassificationUnit; origin = origin.getOrigin())
                level++;
        }
        return String.valueOf(level);
    }

    private DynamicPropertySet attributes;
    @Override
    public DynamicPropertySet getAttributes()
    {
        if(attributes == null)
        {
            attributes = new DynamicPropertySetAsMap();
        }
        return attributes;
    }

    @Override
    public String getDescription()
    {
        init();
        return description;
    }

    @Override
    public ClassificationUnit getParent()
    {
        DataCollection<?> parent = getOrigin();
        return ( parent instanceof ClassificationUnit ) ? (ClassificationUnit)parent : null;
    }

    @Override
    public ClassificationUnit getChild(int i)
    {
        try
        {
            if( i < getNameList().size() )
                return get(getNameList().get(i));
        }
        catch( Exception e )
        {
            throw ExceptionRegistry.translateException( e );
        }
        return null;
    }

    @Override
    public boolean isAncestor(ClassificationUnit unit)
    {
        return ( DataElementPath.create(this) ).isAncestorOf(DataElementPath.create(unit));
    }

    @Override
    public Connection getConnection() throws BiosoftSQLException
    {
        Connection connection = SqlConnectionPool.getConnection(this);
        SqlUtil.checkConnection(connection);
        return connection;
    }

    @Override
    public @Nonnull List<String> getNameList()
    {
        init();
        if(!isValid())
            return Collections.emptyList();
        return Collections.unmodifiableList(children);
    }

    @Override
    protected ClassificationUnit doGet(String name) throws Exception
    {
        init();
        if(!children.contains(name)) return null;
        Properties props = new Properties(getInfo().getProperties());
        props.put(DataCollectionConfigConstants.NAME_PROPERTY, name);
        return getClass().getConstructor(DataCollection.class, Properties.class).newInstance(this, props);
    }

    @Override
    public @Nonnull Class<? extends DataElement> getDataElementType()
    {
        return GOClassificationUnit.class;
    }

    public String getDisplayName()
    {
        return getName()+": "+HtmlUtil.stripHtml(getClassName());
    }
}
