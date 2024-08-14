package ru.biosoft.access;

import javax.swing.ImageIcon;

import ru.biosoft.access.biohub.ReferenceType;
import ru.biosoft.access.biohub.ReferenceTypeRegistry;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElementDescriptor;
import ru.biosoft.access.core.IconManager;
import ru.biosoft.access.repository.IconFactory;
import ru.biosoft.util.ApplicationUtils;
import ru.biosoft.util.CustomImageLoader;

/**
 * Temporary interlace between ru.biosoft.access.core.IconManager used in ru.biosot.access.core.Environment 
 * and existing Icon-relates BioUML processing
 * @author anna
 *
 */

//TODO: refactor code, remove duplications
public class BiosoftIconManager implements IconManager
{
    public BiosoftIconManager()
    {
    }

    @Override
    public ImageIcon getImageIcon(String imagename)
    {
        return ApplicationUtils.getImageIcon( imagename );
    }

    @Override
    public ImageIcon getImageIcon(String path, String name)
    {
        return ApplicationUtils.getImageIcon( path, name );
    }

    @Override
    public String getClassIconId(Class<?> clazz)
    {
        return IconFactory.getClassIconId( clazz );
    }

    @Override
    public String getDescriptorIconId(DataElementDescriptor descr)
    {
        String value = descr.getValue( DataCollectionConfigConstants.NODE_IMAGE );
        if( value == null )
        {
            ReferenceType referenceType = ReferenceTypeRegistry
                    .optReferenceType( descr.getValue( ReferenceTypeRegistry.REFERENCE_TYPE_PROPERTY ) );
            if( referenceType == null )
                referenceType = ReferenceTypeRegistry.getDefaultReferenceType();
            if( referenceType != null )
                value = referenceType.getIconId();
        }
        if( value == null )
            value = getClassIconId( descr.getType() );
        String customImageLoaderClass = descr.getValue( CustomImageLoader.DATA_COLLECTION_PROPERTY );
        if( customImageLoaderClass != null && value != null )
        {
            int colonIdx = value.indexOf( ':' );
            if( colonIdx > -1 )
            {
                String prefix = value.substring( 0, colonIdx );
                String suffix = value.substring( colonIdx + 1 );
                value = prefix + ":" + customImageLoaderClass + "?" + suffix;
            }
            else
            {
                value = customImageLoaderClass + "?" + value;
            }
        }
        return value;
    }

    @Override
    public ImageIcon getIconById(String imagename)
    {
        // TODO Auto-generated method stub
        // TODO: from IconFactory
        return null;
    }
}
