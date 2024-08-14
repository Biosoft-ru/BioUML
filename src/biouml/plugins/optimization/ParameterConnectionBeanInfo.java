package biouml.plugins.optimization;

import ru.biosoft.util.bean.BeanInfoEx2;
import biouml.plugins.optimization.document.editors.DiagramParametersEditor;
import biouml.plugins.optimization.document.editors.SubdiagramsEditor;
import biouml.plugins.optimization.document.editors.TimePointsEditor;

import com.developmentontheedge.beans.BeanInfoConstants;

public class ParameterConnectionBeanInfo extends BeanInfoEx2<ParameterConnection>
{
    public ParameterConnectionBeanInfo()
    {
        super(ParameterConnection.class, MessageBundle.class.getName());
    }

    @Override
    public void initProperties() throws Exception
    {
        property( "nameInFile" ).readOnly().add();
        property( "subdiagramPath" ).hidden( "isSubdiagramPathHidden" ).editor( SubdiagramsEditor.class ).add();
        property( "nameInDiagram" ).editor( DiagramParametersEditor.class ).add();
        property( "relativeTo" ).editor( TimePointsEditor.class ).hidden( "isSteadyState" ).add();
        property( "weight" ).numberFormat( BeanInfoConstants.NUMBER_FORMAT_NONE ).readOnly( "isWeightReadOnly" ).add();
    }
}
