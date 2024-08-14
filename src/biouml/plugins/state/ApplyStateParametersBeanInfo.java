package biouml.plugins.state;

import biouml.model.Diagram;
import biouml.standard.state.MessageBundle;
import biouml.standard.state.State;
import ru.biosoft.util.bean.BeanInfoEx2;

/**
 * @author anna
 *
 */
public class ApplyStateParametersBeanInfo extends BeanInfoEx2<ApplyStateParameters>
{
    public ApplyStateParametersBeanInfo()
    {
        super(ApplyStateParameters.class, MessageBundle.class.getName());
        beanDescriptor.setDisplayName(getResourceString("CN_APPLY_STATE"));
        beanDescriptor.setShortDescription(getResourceString("CD_APPLY_STATE"));
    }
    
    @Override
    protected void initProperties() throws Exception
    {
        property( "inputDiagramPath" ).inputElement( Diagram.class ).title( "PN_INPUT_DIAGRAM" ).description( "PD_INPUT_DIAGRAM" ).add();
        
        property( "statePath" ).inputElement( State.class ).title( "PN_INPUT_STATE" ).description( "PD_INPUT_STATE" ).add();

        add("newDiagram");
        addHidden("writeStateToDiagram", "applyToSameDiagram");

        property( "outputDiagramPath" ).outputElement( Diagram.class ).hidden( "applyToSameDiagram" ).canBeNull( "applyToSameDiagram" )
                .title( "PD_OUTPUT_DIAGRAM" ).description( "PD_OUTPUT_DIAGRAM" ).add();
        
        add("stateName");
    }
}
