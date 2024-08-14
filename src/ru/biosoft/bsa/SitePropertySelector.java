package ru.biosoft.bsa;

import java.util.ArrayList;
import java.util.List;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.util.BeanUtil;

import com.developmentontheedge.beans.BeanInfoConstants;
import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.PropertyDescriptorEx;
import com.developmentontheedge.beans.editors.StringTagEditor;
import com.developmentontheedge.beans.model.ComponentFactory;

public class SitePropertySelector extends StringTagEditor
{
    public static final String TRACK_PROPERTY = "TrackProperty";
    public static final String NUMERIC_ONLY_PROPERTY = "NumericOnly";
    public static final String NONE_PROPERTY = "(none)";

    @Override
    public String[] getTags()
    {
        try
        {
            String propertyName = getDescriptor().getValue( TRACK_PROPERTY ).toString();
            boolean canBeNull = BeanUtil.getBooleanValue( this, BeanInfoConstants.CAN_BE_NULL );
            boolean numericOnly = BeanUtil.getBooleanValue( this, NUMERIC_ONLY_PROPERTY );
            DataElementPath path = (DataElementPath)ComponentFactory.getModel( getBean() ).findProperty( propertyName ).getValue();
            Track track = path.getDataElement( Track.class );
            Site site = track.getAllSites().iterator().next();

            List<String> values = new ArrayList<>();
            if( canBeNull )
                values.add( NONE_PROPERTY );
            for( DynamicProperty dp : site.getProperties() )
            {
                if( numericOnly && ! ( Number.class.isAssignableFrom( dp.getType() ) ) )
                    continue;
                values.add( dp.getName() );
            }
            return values.toArray( new String[values.size()] );
        }
        catch( Exception e )
        {
            return new String[] {NONE_PROPERTY};
        }
    }
    
    public static PropertyDescriptorEx registerSelector(PropertyDescriptorEx pde, String trackProperty, boolean canBeNull, boolean numericOnly)
    {
        pde.setPropertyEditorClass( SitePropertySelector.class );
        pde.setSimple( true );
        pde.setCanBeNull( canBeNull );
        pde.setValue( NUMERIC_ONLY_PROPERTY, numericOnly );
        pde.setValue( TRACK_PROPERTY, trackProperty );
        return pde;
    }
}
