package ru.biosoft.bsa.server;

import java.util.Properties;

import javax.annotation.Nonnull;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.bsa.Const;
import ru.biosoft.bsa.classification.ClassificationUnit;
import ru.biosoft.util.TextUtil;
import biouml.plugins.server.access.DataClientCollection;

import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.DynamicPropertySetAsMap;

/**
 * @author lan
 *
 */
public class ClientClassificationUnit extends DataClientCollection<ClassificationUnit> implements ClassificationUnit
{
    public ClientClassificationUnit(DataCollection<?> origin, Properties properties) throws Exception
    {
        super( origin,properties );
        classNumber  = properties.getProperty(Const.NUMBER_PROPERTY);
        className    = properties.getProperty(Const.CLASSNAME_PROPERTY);
        level        = properties.getProperty(Const.LEVEL_PROPERTY);
        description  = properties.getProperty(Const.DESCRIPTION_PROPERTY);

        getInfo().setDisplayName( getDisplayName() );
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

    @Override
    public ClassificationUnit put(ClassificationUnit obj)
    {
        throw new UnsupportedOperationException();
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

    private final String level;
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
    public @Nonnull Class<? extends DataElement> getDataElementType()
    {
        return ClassificationUnit.class;
    }
}
