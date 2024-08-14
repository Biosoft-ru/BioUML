package ru.biosoft.bsa.view;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;

import ru.biosoft.graphics.editor.FontEditor;

public class MapViewOptionsBeanInfo extends BeanInfoEx
{
    protected PropertyDescriptorEx region,
                                   regionColor;

    public MapViewOptionsBeanInfo( Class<? extends MapViewOptions> clas, java.lang.String string )
    {
        super( clas, string );
    }

    protected void addDescriptions() throws Exception
    {
        add( new PropertyDescriptorEx( "font", beanClass ), FontEditor.class, getResourceString( "PN_MAP_VIEW_OPTIONS_FONT" ),
                getResourceString( "PD_MAP_VIEW_OPTIONS_FONT" ) );

        PropertyDescriptorEx pde = new PropertyDescriptorEx("maxWidth", beanClass);
        add(pde, getResourceString("PN_MAP_VIEW_OPTIONS_MAXWIDTH"), getResourceString("PD_MAP_VIEW_OPTIONS_MAXWIDTH"));
        pde.setHidden(true);

        add(new PropertyDescriptorEx("interval", beanClass), getResourceString("PN_MAP_VIEW_OPTIONS_INTERVAL"),
                getResourceString("PD_MAP_VIEW_OPTIONS_INTERVAL"));

        region = new PropertyDescriptorEx("region", beanClass);
        region.setHidden(beanClass.getMethod("getRegion"));
        add(region, getResourceString("PN_MAP_VIEW_OPTIONS_REGION"), getResourceString("PD_MAP_VIEW_OPTIONS_REGION"));


        regionColor = new PropertyDescriptorEx("regionColor", beanClass);
        regionColor.setHidden(beanClass.getMethod("getRegionColor"));
        add(regionColor, getResourceString("PN_MAP_VIEW_OPTIONS_REGIONCOLOR"), getResourceString("PD_MAP_VIEW_OPTIONS_REGIONCOLOR"));


        add(new PropertyDescriptorEx("sequenceViewOptions", beanClass), getResourceString("PN_MAP_VIEW_OPTIONS_SEQUENCEVIEWOPTIONS"),
                getResourceString("PD_MAP_VIEW_OPTIONS_SEQUENCEVIEWOPTIONS"));

        pde = new PropertyDescriptorEx("maxSequenceLength", beanClass);
        pde.setHidden(true);
        add(pde);
    }

    public MapViewOptionsBeanInfo()
    {
        super(MapViewOptions.class, MessageBundle.class.getName() );
        beanDescriptor.setDisplayName(getResourceString("CN_MAP_VIEW_OPTIONS"));
        beanDescriptor.setShortDescription(getResourceString("CD_MAP_VIEW_OPTIONS"));
    }

    @Override
    public void initProperties() throws Exception
    {
        addDescriptions();
    }
}
