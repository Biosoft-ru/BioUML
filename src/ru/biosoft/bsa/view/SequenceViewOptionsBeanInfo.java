
/**
 * Author:  Igor V. Tyazhev  (champ@developmentontheedge.com)
 *
 * Created: 27.02.2001
 *
 * Description:
 *
 * Copyright (C) 2000, 2001 DevelopmentOnTheEdge.com. All rights reserved.
 */

package ru.biosoft.bsa.view;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;


public class SequenceViewOptionsBeanInfo extends BeanInfoEx
{
    public SequenceViewOptionsBeanInfo()
    {
        super(SequenceViewOptions.class, MessageBundle.class.getName() );
        beanDescriptor.setDisplayName(getResourceString("CN_SEQUENCE_VIEW_OPTIONS"));
        beanDescriptor.setShortDescription(getResourceString("CD_SEQUENCE_VIEW_OPTIONS"));
    }

    @Override
    public void initProperties() throws Exception
    {
        add(new PropertyDescriptorEx( "type", beanClass),
            SequenceViewTypeEditor.class,
            getResourceString("PN_SEQUENCE_VIEW_OPTIONS_TYPE"),           //  display_name ()
            getResourceString("PD_SEQUENCE_VIEW_OPTIONS_TYPE"));   //  description

        PropertyDescriptorEx pdDensity = new PropertyDescriptorEx( "density", beanClass);
        pdDensity.setReadOnly(beanClass.getMethod("isDensityReadOnly"));
        add(pdDensity,
            getResourceString("PN_SEQUENCE_VIEW_OPTIONS_DENSITY"),           //  display_name ()
            getResourceString("PD_SEQUENCE_VIEW_OPTIONS_DENSITY"));   //  description
        //changed by ela
        /*
       add(new PropertyDescriptorEx( "font", beanClass),
            FontEditor.class,
            getResourceString("FONT_NAME"),
            getResourceString("FONT_DESCRIPTION"));*/

        add(new PropertyDescriptorEx( "rulerOptions", beanClass),
            getResourceString("PN_SEQUENCE_VIEW_OPTIONS_RULEROPTIONS"),
            getResourceString("PN_SEQUENCE_VIEW_OPTIONS_RULEROPTIONS"));
    }
}