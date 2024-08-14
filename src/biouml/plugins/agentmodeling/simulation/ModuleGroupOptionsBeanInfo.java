package biouml.plugins.agentmodeling.simulation;

import ru.biosoft.util.bean.BeanInfoEx2;
import ru.biosoft.workbench.editors.GenericMultiSelectEditor;

public class ModuleGroupOptionsBeanInfo extends BeanInfoEx2<ModuleGroupOptions>
{
    public ModuleGroupOptionsBeanInfo()
    {
        super( ModuleGroupOptions.class );
    }

    @Override
    public void initProperties() throws Exception
    {
        add( "name" );
        add( "subdiagrams", SubDiagramNamesEditor.class );
    }

    public static class SubDiagramNamesEditor extends GenericMultiSelectEditor
    {
        @Override
        protected String[] getAvailableValues()
        {
            return ( (ModuleGroupOptions)getBean() ).getAvailableSubDiagrams();
        }
    }
}