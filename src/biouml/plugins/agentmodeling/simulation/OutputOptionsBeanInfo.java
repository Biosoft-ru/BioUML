package biouml.plugins.agentmodeling.simulation;

import ru.biosoft.util.bean.BeanInfoEx2;

public class OutputOptionsBeanInfo extends BeanInfoEx2<OutputOptions>
{
    public OutputOptionsBeanInfo()
    {
        super( OutputOptions.class );
    }

    @Override
    public void initProperties() throws Exception
    {
        add("plotResults");
        add("saveResults");
        addHidden( "resultPath", "dontSaveResults");
        add("initialTime");
        add("timeIncrement");
        add("completionTime");
        addReadOnly( "includeEventPoints");
     }
}