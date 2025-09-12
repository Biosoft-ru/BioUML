package ru.biosoft.access.biohub;

import java.lang.annotation.Annotation;
import java.util.Deque;
import java.util.LinkedList;

import org.apache.commons.text.StringEscapeUtils;

import ru.biosoft.access.ClassLoading;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.ClassIcon;
import biouml.standard.type.DatabaseInfo;

import com.developmentontheedge.beans.DynamicPropertySet;

public abstract class ReferenceTypeSupport implements ReferenceType
{
    private String urlTemplate = null;
    private static final String MIRIAM_PATH = "databases/Utils/MIRIAM";
    private String sampleId = null;
    private String description = null;
    private final String stableName = getClass().getSimpleName();
    private final String displayName = getObjectType()+(getSource().equals("")?"":": "+getSource());
    private String miriamId = null;

    protected void setUrlTemplate(String urlTemplate)
    {
        if( urlTemplate != null && urlTemplate.startsWith( "MIR:" ) )
        {
            miriamId = urlTemplate;
            DataElementPath path = DataElementPath.create( MIRIAM_PATH ).getChildPath( urlTemplate );
            try
            {
                DataElement de = path.optDataElement();
                if( de instanceof DatabaseInfo )
                {
                    DatabaseInfo info = (DatabaseInfo)de;
                    this.urlTemplate = info.getQueryById();
                    if( info.getDescription() != null && !info.getDescription().isEmpty() )
                    {
                        description = "From <a href=\"http://www.ebi.ac.uk/miriam/\">MIRIAM</a>: "
                                + StringEscapeUtils.escapeHtml4( info.getDescription() );
                    }
                    sampleId = ( (DynamicPropertySet[])info.getAttributes().getValue( "resources" ) )[0]
                            .getValueAsString( "dataEntityExample" );
                }
            }
            catch( Exception e )
            {
            }
        }
        else
        {
            this.urlTemplate = urlTemplate;
        }
    }

    @Override
    public String getDisplayName()
    {
        return displayName;
    }

    @Override
    public String getSource()
    {
        return "";
    }

    @Override
    public String getURL(String id)
    {
        if(urlTemplate != null)
            return urlTemplate.replace("$id$", id);
        DataElementPath path = getPath( id );
        if(path != null)
            return "de:" + path;
        return null;
    }

    protected String iconId;

    /**
     * Default implementation will try to extract class from @ClassIcon annotation
     */
    @Override
    public String getIconId()
    {
        if(iconId != null)
            return iconId.equals("")?null:iconId;
        Deque<Class<? extends ReferenceType>> classes = new LinkedList<>();
        classes.add(getClass());
        // We cannot simply use clazz.getAnnotation(ClassIcon.class),
        // because we need to know exactly where that annotation is defined
        // as specified icon path is relative to class location
        while(!classes.isEmpty())
        {
            Class<? extends ReferenceType> curClass = classes.pop();
            for(Annotation annotation: curClass.getDeclaredAnnotations())
            {
                if(annotation instanceof ClassIcon)
                {
                    String resource = ClassLoading.getResourceLocation( curClass, ( (ClassIcon)annotation ).value() );
                    iconId = resource;
                    return resource;
                }
            }
            if(curClass.getSuperclass() != null && ReferenceType.class.isAssignableFrom(curClass.getSuperclass()))
            {
                classes.add((Class<? extends ReferenceType>)curClass.getSuperclass());
            }
            for(Class<?> iface: curClass.getInterfaces())
            {
                if(ReferenceType.class.isAssignableFrom(iface))
                    classes.add((Class<? extends ReferenceType>)iface);
            }
        }
        iconId = "";
        return null;
    }
    
    @Override
    public String toString()
    {
        return getDisplayName();
    }

    @Override
    public int hashCode()
    {
        return getDisplayName().hashCode();
    }

    @Override
    public boolean equals(Object obj)
    {
        return (obj instanceof ReferenceType) && ((ReferenceType)obj).getDisplayName().equals(getDisplayName());
    }

    @Override
    public String getStableName()
    {
        return stableName;
    }

    @Override
    public String getDescriptionHTML()
    {
        return description;
    }

    @Override
    public String getSampleID()
    {
        return sampleId;
    }

    @Override
    public String getMiriamId()
    {
        return miriamId;
    }
}
