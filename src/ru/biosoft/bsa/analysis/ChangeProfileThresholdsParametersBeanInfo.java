package ru.biosoft.bsa.analysis;

import ru.biosoft.bsa.SiteModelCollection;
import ru.biosoft.bsa.SiteModelTransformedCollection;
import ru.biosoft.bsa.analysis.ChangeProfileThresholdsParameters.TemplateSelector;
import ru.biosoft.util.bean.BeanInfoEx2;

/**
 * @author lan
 *
 */
public class ChangeProfileThresholdsParametersBeanInfo extends BeanInfoEx2<ChangeProfileThresholdsParameters>
{
    public ChangeProfileThresholdsParametersBeanInfo()
    {
        super(ChangeProfileThresholdsParameters.class);
    }

    @Override
    protected void initProperties() throws Exception
    {
        property( "inputProfile" ).inputElement( SiteModelCollection.class ).structureChanging().add();
        add("template", TemplateSelector.class);
        addHidden("threshold", "isThresholdHidden");
        addHidden("profileProperties", "isProfilePropertiesHidden");
        property( "outputProfile" ).outputElement( SiteModelTransformedCollection.class ).auto( "$inputProfile$ $template$" ).add();
    }
}
