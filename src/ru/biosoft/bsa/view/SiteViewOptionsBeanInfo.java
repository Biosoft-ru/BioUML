package ru.biosoft.bsa.view;

import one.util.streamex.StreamEx;
import ru.biosoft.graphics.editor.FontEditor;
import ru.biosoft.util.bean.BeanInfoEx2;

import com.developmentontheedge.beans.ChoicePropertyDescriptorEx;

public class SiteViewOptionsBeanInfo extends BeanInfoEx2<SiteViewOptions>
{
    public SiteViewOptionsBeanInfo()
    {
        this(SiteViewOptions.class);
    }

    protected SiteViewOptionsBeanInfo(Class<? extends SiteViewOptions> beanClass)
    {
        super(beanClass, MessageBundle.class.getName());
    }

    @Override
    public void initProperties() throws Exception
    {
        property("trackDisplayMode").hidden().tags( SiteViewOptions.getTrackDisplayModes() ).add();
        add(new ChoicePropertyDescriptorEx("displayName", beanClass, SiteDisplayNameEditor.class));
        add("trackTitleFont", FontEditor.class);
        add("font", FontEditor.class);
        add("interval");
        add("boxHeight");
        add("showTitle");
        add("showPositions");
        add("showSequence");
        add("showStrand");
        add("showBox");
        add("showStructure");
        initColorSchemeProperties();
    }

    protected void initColorSchemeProperties()
    {
        property( "colorSchemeName" )
            .tags( bean -> StreamEx.ofKeys( bean.schemes ) )
            .hidden( "isColorSchemeSelectorHidden" )
            .add();
        add( "colorScheme" );

    }
}