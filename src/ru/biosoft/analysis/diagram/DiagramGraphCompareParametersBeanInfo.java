package ru.biosoft.analysis.diagram;

import java.util.Optional;
import java.util.stream.Stream;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.util.bean.BeanInfoEx2;
import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.Node;
import biouml.model.Role;
import biouml.model.dynamics.VariableRole;
import biouml.model.xml.XmlDiagramTypeConstants;
import biouml.standard.type.Base;
import biouml.standard.type.Reaction;
import biouml.standard.type.Specie;
import biouml.standard.type.Stub;

import com.developmentontheedge.beans.editors.StringTagEditor;

public class DiagramGraphCompareParametersBeanInfo extends BeanInfoEx2<DiagramGraphCompareParameters>
{
    public DiagramGraphCompareParametersBeanInfo()
    {
        super(DiagramGraphCompareParameters.class);
    }

    @Override
    protected void initProperties() throws Exception
    {
        property( "inputPath1" ).inputElement( Diagram.class ).add();
        property( "inputPath2" ).inputElement( Diagram.class ).add();
        add( "startNodeName1", NodeNameEditor.class );
        add( "startNodeName2", NodeNameEditor2.class );
        add("depth");
        property("directionStr").tags( DiagramGraphCompareParameters.availableDirections ).add();
        property("outputPath").outputElement( Diagram.class ).add();
        addExpert("needLayout");
        addExpert("comparator");
    }

    public static class NodeNameEditor extends StringTagEditor
    {
        @Override
        public String[] getTags()
        {
            return Optional.ofNullable( getPath() )
                .map( path -> path.optDataElement( Diagram.class ) )
                .map( this::nodeNames )
                .map( names -> names.toArray( String[]::new ) )
                .orElse( new String[0] );
        }
        protected Stream<String> nodeNames(Compartment comp)
        {
            return comp.recursiveStream()
                    .select( Node.class )
                    .remove( this::shouldSkip )
                    .map( DiagramGraphCompareParameters::generateNodeName );
        }

        private boolean shouldSkip(Node node)
        {
            //skip elements with unsuitable role
            Role role = node.getRole();
            if( ! ( role instanceof VariableRole ) )
                return true;

            //skip reactions and unknown stubs
            Base kernel = node.getKernel();
            boolean isReaction = kernel == null || kernel instanceof Reaction
                    || ( kernel instanceof Stub && ( "reaction".equals(kernel.getType()) || "unknown".equals(kernel.getType()) ) );
            if( isReaction )
                return true;

            //do not skip old SBGN-xml entities and not compartment nodes
            if( ! ( node instanceof Compartment )
                    || "entity".equals( node.getAttributes().getValueAsString( XmlDiagramTypeConstants.XML_TYPE ) ) )
                return false;

            //do not skip current SBGN species (compartments with Specie kernel)
            return ! ( kernel instanceof Specie );
        }

        protected DataElementPath getPath()
        {
            return ( (DiagramGraphCompareParameters)getBean() ).getInputPath1();
        }
    }
    public static class NodeNameEditor2 extends NodeNameEditor
    {
        @Override
        protected DataElementPath getPath()
        {
            return ( (DiagramGraphCompareParameters)getBean() ).getInputPath2();
        }
    }
}
