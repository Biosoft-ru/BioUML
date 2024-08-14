package ru.biosoft.analysis.diagram;

import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.util.bean.BeanInfoEx2;
import ru.biosoft.workbench.editors.GenericMultiSelectEditor;
import biouml.model.Diagram;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.VariableRole;

public class ShareComplexMoleculeParametersBeanInfo extends BeanInfoEx2<ShareComplexMoleculeParameters>
{
    public ShareComplexMoleculeParametersBeanInfo()
    {
        super(ShareComplexMoleculeParameters.class);
    }

    @Override
    protected void initProperties() throws Exception
    {
        property( "diagramPath" ).inputElement( Diagram.class ).add();
        property( "elementNames" ).canBeNull().editor( VariableEditor.class ).simple().hideChildren().add();
        property( "outputPath" ).outputElement( Diagram.class ).auto( "$diagramPath$ changed" ).add();
    }

    public static class VariableEditor extends GenericMultiSelectEditor
    {
        @Override
        protected Object[] getAvailableValues()
        {
            DataElementPath path = ( (ShareComplexMoleculeParameters)getBean() ).getDiagramPath();
            DataElement de;
            if( path == null || ! ( ( de = path.optDataElement() ) instanceof Diagram ) )
                return new String[] {};
            EModel emodel = ( (Diagram)de ).getRole(EModel.class);
            return emodel.getVariableRoles().stream().map( VariableRole::getDiagramElement )
                    .filter( node -> ! ( node.getKernel() instanceof biouml.standard.type.Compartment ) )
                .map( node -> node.getTitle() + " (id:" + node.getCompleteNameInDiagram() + ")" )
                .toArray( String[]::new );
        }
    }
}
