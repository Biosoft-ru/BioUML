
package ru.biosoft.access.biohub;

import java.io.File;
import java.util.Map;
import java.util.regex.Pattern;

import ru.biosoft.access.ClassLoading;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElementPath;

/**
 * Reference type for DataCollection with unspecified element type
 * Required to match from external element identifiers to internal collection identifiers
 */

public class CollectionSpecificReferenceType extends ReferenceTypeSupport
{
    public static final String REFERENCE_TYPE_ICON_PROPERTY = "referenceTypeIcon";
    private final DataElementPath path;
    private String source = "Collection element";
    private Pattern pattern;

    public CollectionSpecificReferenceType(DataCollection<?> dc)
    {
        this.path = dc.getCompletePath();
        this.source = "Collection element";
        this.pattern = null;
        this.iconId = resolveIcon( dc, dc.getInfo().getProperties().getProperty(REFERENCE_TYPE_ICON_PROPERTY, ""));
    }

    private String resolveIcon(DataCollection<?> dc, String iconId)
    {
        if(iconId == null)
            iconId = ClassLoading.getResourceLocation( CollectionSpecificReferenceType.class, "resources/defaultTypeIcon.gif" );
        if(!iconId.contains(":"))
        {
            String path = dc.getInfo().getProperties().getProperty(DataCollectionConfigConstants.CONFIG_PATH_PROPERTY);
            if(path != null)
            {
                iconId = new File(path, iconId).getAbsolutePath();
            }
        }
        return iconId;
    }
    
    public CollectionSpecificReferenceType(DataCollection<?> dc, Map<String, String> props)
    {
        this.path = dc.getCompletePath();
        this.source  = props.get( "default" );
        this.iconId = resolveIcon( dc, props.get( "icon" ) );
        String pattern = props.get( "pattern" );
        if(pattern != null)
            this.pattern = Pattern.compile( pattern );
    }

    public DataElementPath getPath()
    {
        return path;
    }
    
    @Override
    public int getIdScore(String id)
    {
        if(pattern != null && pattern.matcher( id ).matches())
            return SCORE_MEDIUM_SPECIFIC;
        return SCORE_NOT_THIS_TYPE;
    }

    @Override
    public String getDisplayName()
    {
        return getObjectType()+": "+getSource();
    }

    @Override
    public String getObjectType()
    {
        return source;
    }

    @Override
    public String getStableName()
    {
        return getClass().getSimpleName() + getSource() + getObjectType();
    }

    @Override
    public String getSource()
    {
        return path != null ? path.getName() : "";
    }
}
