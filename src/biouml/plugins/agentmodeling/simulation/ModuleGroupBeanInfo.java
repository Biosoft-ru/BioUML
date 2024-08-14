package biouml.plugins.agentmodeling.simulation;

import ru.biosoft.util.bean.BeanInfoEx2;
import ru.biosoft.workbench.editors.GenericMultiSelectEditor;

public class ModuleGroupBeanInfo extends BeanInfoEx2<ModuleGroup>
{
    public ModuleGroupBeanInfo()
    {
        super( ModuleGroup.class );
    }

    @Override
    public void initProperties() throws Exception
    {
        add("name");
        property("subdiagrams").hideChildren().editor(  SubDiagramNamesEditor.class ).add();
        add( "simulateSeparately" );
        addWithTags( "engineName", bean -> bean.getAppropriateEngineNames() );
        add( "engine" );
    }
    
    public static class SubDiagramNamesEditor extends GenericMultiSelectEditor
    {
        @Override
        protected String[] getAvailableValues()
        {
           return ((ModuleGroup)getBean()).getAvailableSubDiagrams();
        }
    }
}