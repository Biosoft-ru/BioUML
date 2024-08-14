package biouml.model;

import java.util.List;

import ru.biosoft.workbench.editors.GenericComboBoxEditor;

import com.developmentontheedge.beans.PropertyDescriptorEx;

public class SubDiagramBeanInfo extends CompartmentBeanInfo
{
    public SubDiagramBeanInfo()
    {
        super( SubDiagram.class );
    }

    @Override
    public void initProperties() throws Exception
    {
        super.initProperties();

        PropertyDescriptorEx pde = new PropertyDescriptorEx( "diagramPath", beanClass, "getDiagramPath", null );
        add( 0, pde, "Diagram path", "Diagram path." );
        pde = new PropertyDescriptorEx( "stateName", beanClass );
        pde.setPropertyEditorClass( StateNamesEditor.class );
        add( 1, pde, "State name", "State name." );
    }
    
    public static class StateNamesEditor extends GenericComboBoxEditor
    {
        @Override
        public String[]  getAvailableValues()
        {
            List<String> stateNames = ( (SubDiagram)this.getBean()).getDiagram().getStateNames();
            return stateNames.toArray( new String[stateNames.size()] );
        }
    }
}
