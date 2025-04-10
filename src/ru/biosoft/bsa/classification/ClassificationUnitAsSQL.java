package ru.biosoft.bsa.classification;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.annotation.Nonnull;

import java.util.logging.Level;
import java.util.logging.Logger;

import ru.biosoft.access.core.AbstractDataCollection;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElementDescriptor;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.exception.BiosoftSQLException;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.access.sql.SqlConnectionPool;
import ru.biosoft.access.sql.SqlUtil;
import ru.biosoft.util.HtmlUtil;
import ru.biosoft.util.TextUtil2;

import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.DynamicPropertySetAsMap;

/**
 * Classification based on SQL table
 */
public class ClassificationUnitAsSQL extends AbstractDataCollection<ClassificationUnit> implements ClassificationUnit
{
    private static final DataElementDescriptor LEAF_DESCRIPTOR = new DataElementDescriptor(ClassificationUnitAsSQL.class, true);
    private static final DataElementDescriptor NON_LEAF_DESCRIPTOR = new DataElementDescriptor(ClassificationUnitAsSQL.class, false);

    protected static final Logger log = Logger.getLogger(ClassificationUnitAsSQL.class.getName());

    public static final String CLASSIFICATION_TABLE_NAME = "table-name";
    public static final String TF_ID = "tf-id";

    protected Properties properties;

    public ClassificationUnitAsSQL(DataCollection<?> parent, Properties properties) throws BiosoftSQLException
    {
        super(parent, properties);
        this.properties = properties;
        Connection connection = SqlConnectionPool.getConnection(this);
        Object[] row = SqlUtil.queryRow(connection, "SELECT name,title,description,level FROM " + properties.getProperty(CLASSIFICATION_TABLE_NAME) + " WHERE name='"
                        + properties.getProperty(TF_ID) + "'", String.class, String.class, String.class, String.class);
        if( row != null )
        {
            classNumber = (String)row[0];
            className = (String)row[1];
            level = (String)row[3];
            description = (String)row[2];
        }
    }

    protected List<String> nameList = null;
    @Override
    public @Nonnull List<String> getNameList()
    {
        if( nameList == null )
        {
            nameList = new ArrayList<>();
            try
            {
                Connection connection = SqlConnectionPool.getConnection(this);
                nameList.addAll(SqlUtil.queryStrings(connection, "SELECT name FROM " + properties.getProperty(CLASSIFICATION_TABLE_NAME)
                        + " WHERE parent='" + properties.getProperty(TF_ID) + "'"));
                if( !nameList.contains(properties.getProperty(TF_ID) + ".0") )
                {
                    nameList.addAll(SqlUtil.queryStrings(
                            connection,
                            "SELECT name FROM " + properties.getProperty(CLASSIFICATION_TABLE_NAME) + " WHERE parent='"
                                    + properties.getProperty(TF_ID) + ".0'"));
                }
            }
            catch( BiosoftSQLException e )
            {
                log.log(Level.SEVERE, getCompletePath()+": namelist failed: "+ExceptionRegistry.log(e));
            }
        }
        return nameList;
    }

    @Override
    protected ClassificationUnit doGet(String name) throws Exception
    {
        Properties props = new Properties(this.properties);
        props.put(DataCollectionConfigConstants.NAME_PROPERTY, name);
        props.put(TF_ID, name);
        ClassificationUnitAsSQL instance = getClass().getConstructor( ru.biosoft.access.core.DataCollection.class, Properties.class ).newInstance( this, props );
        if(instance.getClassNumber() == null)
            return null;
        return instance;
    }

    @Override
    public @Nonnull Class<? extends ClassificationUnit> getDataElementType()
    {
        return getClass();
    }

    private DynamicPropertySet attributes;
    @Override
    public DynamicPropertySet getAttributes()
    {
        if( attributes == null )
        {
            attributes = new DynamicPropertySetAsMap();
        }
        return attributes;
    }

    private String className;
    @Override
    public String getClassName()
    {
        return className;
    }

    private String classNumber;
    @Override
    public String getClassNumber()
    {
        return classNumber;
    }

    private String level;
    @Override
    public String getLevel()
    {
        return level;
    }

    private String description;
    @Override
    public String getDescription()
    {
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
            log.log(Level.SEVERE, "Cannot load children " + i + " for classification unit: " + getName(), e);
        }
        return null;
    }

    @Override
    public boolean isAncestor(ClassificationUnit unit)
    {
        return ( DataElementPath.create(this) ).isAncestorOf(DataElementPath.create(unit));
    }

    @Override
    public String toString()
    {
        return TextUtil2.isEmpty(getClassName()) ? getName() : getClassName()
                + ( TextUtil2.isEmpty(getDescription()) ? "" : "; " + getDescription() );
    }

    public String getDisplayName()
    {
        return getName()+": "+HtmlUtil.stripHtml(getClassName());
    }

    @Override
    public DataElementDescriptor getDescriptor(String name)
    {
        DataElementDescriptor result = LEAF_DESCRIPTOR;
        try
        {
            if(get(name).getSize() > 0) result = NON_LEAF_DESCRIPTOR;
        }
        catch( Exception e )
        {
        }
        return result;
    }
}
