package ru.biosoft.analysis.diagram;

import one.util.streamex.StreamEx;

import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.plugins.graph.GraphPlugin;
import ru.biosoft.plugins.graph.LayouterDescriptor;
import ru.biosoft.util.OptionEx;
import ru.biosoft.util.bean.BeanInfoEx2;
import biouml.model.Diagram;

/**
 * @author anna
 *
 */
public class JoinDiagramParametersBeanInfo extends BeanInfoEx2<JoinDiagramParameters>
{
    public JoinDiagramParametersBeanInfo ()
    {
        super(JoinDiagramParameters.class, MessageBundle.class.getName());
        beanDescriptor.setDisplayName(MessageBundle.CN_JD_CLASS);
        beanDescriptor.setShortDescription(MessageBundle.CD_JD_CLASS);
    }
    
    @Override
    public void initProperties() throws Exception
    {
        super.initProperties();
        add(DataElementPathEditor.registerInputMulti("inputDiagrams", beanClass, Diagram.class), MessageBundle.PN_INPUT_DIAGRAMS, MessageBundle.PD_INPUT_DIAGRAMS);
        String[] tags = StreamEx.of( GraphPlugin.loadLayouters() ).filter( LayouterDescriptor::isPublic )
                .map( LayouterDescriptor::getTitle ).prepend( JoinDiagramParameters.NONE_LAYOUTER, JoinDiagramParameters.AUTO_LAYOUTER )
                .toArray( String[]::new );
        property("layouterName").tags(tags).title( "PN_LAYOUTER" ).description( "PD_LAYOUTER" ).add();
        add(OptionEx.makeAutoProperty(DataElementPathEditor.registerOutput("outputDiagramPath", beanClass, Diagram.class),
                "$inputDiagrams/path$/Joined diagram"), MessageBundle.PN_OUTPUT_DIAGRAM, MessageBundle.PD_OUTPUT_DIAGRAM);
    }
}
