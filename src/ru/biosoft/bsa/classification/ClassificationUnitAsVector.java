package ru.biosoft.bsa.classification;

import java.util.Properties;
import java.util.logging.Level;

import javax.annotation.Nonnull;

import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.DynamicPropertySetAsMap;

import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.VectorDataCollection;
import ru.biosoft.bsa.Const;
import ru.biosoft.util.TextUtil;

public class ClassificationUnitAsVector extends VectorDataCollection<ClassificationUnit> implements ClassificationUnit
{
    /**
     * Constructor for {@link CollectionFactoryUtils}
     */
    public ClassificationUnitAsVector(DataCollection<?> origin, Properties properties)
    {
        super( origin,properties );
        classNumber  = properties.getProperty(Const.NUMBER_PROPERTY);
        className    = properties.getProperty(Const.CLASSNAME_PROPERTY);
        level        = properties.getProperty(Const.LEVEL_PROPERTY);
        description  = properties.getProperty(Const.DESCRIPTION_PROPERTY);

        getInfo().setDisplayName( getDisplayName() );
    }

    /**
     * This constructor is used by {@link CollectionAsVectorTransformer} to
     * optimise the creation process.
     */
    public ClassificationUnitAsVector(DataCollection<?> parent, String name,
                                      String classNumber, String className, String description)
    {
        super(name, parent, null);

        this.classNumber = classNumber;
        this.className   = className;
        this.description = description;
        getInfo().setDisplayName( getDisplayName() );

        try
        {
            if( parent instanceof ClassificationUnitAsVector)
                ((ClassificationUnitAsVector)parent).doPut(this, true);
        }
        catch(Throwable t)
        {
            log.log(Level.SEVERE, "Can not process classification unit " + getCompletePath(), t);
        }
    }

    /** Returns combination from class number and class name that can be used as display name. */
    public String getDisplayName()
    {
        String displayName = null;

        if( classNumber == null )
            displayName = className;
        else if( className == null )
            displayName = classNumber;
        else
            displayName = classNumber + ". " + className;

        if( displayName == null )
            displayName = getName();

        return displayName;
    }

    protected void setDisplayName()
    {
        getInfo().setDisplayName(className);
    }

    @Override
    public @Nonnull Class<ClassificationUnitAsVector> getDataElementType()
    {
        return ClassificationUnitAsVector.class;
    }

    ////////////////////////////////////////////////////////////////////////////
    // Properties
    //

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

    private final String className;
    @Override
    public String getClassName()
    {
        return className;
    }

    private final String classNumber;
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

    private final String description;
    @Override
    public String getDescription()
    {
        return description;
    }

    @Override
    public ClassificationUnit getParent()
    {
        DataCollection<?> parent = getOrigin();
        return (parent instanceof ClassificationUnit) ? (ClassificationUnit)parent : null;
    }

    @Override
    public ClassificationUnit getChild(int i)
    {
        try
        {
            return get(getNameList().get(i));
        }
        catch( Exception e )
        {
            return null;
        }
    }

    @Override
    public boolean isAncestor(ClassificationUnit unit)
    {
        return (DataElementPath.create(this)).isAncestorOf(DataElementPath.create(unit));
    }

    @Override
    public String toString()
    {
        return TextUtil.isEmpty(getClassName())?getName():getClassName()+(TextUtil.isEmpty(getDescription())?"":"\n"+getDescription().replace("; ", "\n"));
    }
}