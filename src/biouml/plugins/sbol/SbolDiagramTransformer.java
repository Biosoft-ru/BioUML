package biouml.plugins.sbol;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.sbolstandard.core2.Annotation;
import org.sbolstandard.core2.Component;
import org.sbolstandard.core2.ComponentDefinition;
import org.sbolstandard.core2.GenericTopLevel;
import org.sbolstandard.core2.RestrictionType;
import org.sbolstandard.core2.SBOLDocument;
import org.sbolstandard.core2.SBOLValidationException;
import org.sbolstandard.core2.SBOLWriter;
import org.sbolstandard.core2.SequenceAnnotation;
import org.sbolstandard.core2.SequenceConstraint;

import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.Edge;
import biouml.model.Node;
import ru.biosoft.access.AbstractFileTransformer;
import ru.biosoft.access.file.FileDataElement;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.file.FileTypePriority;
import ru.biosoft.access.generic.PriorityTransformer;
import ru.biosoft.graph.Path;

public class SbolDiagramTransformer extends AbstractFileTransformer<Diagram> implements PriorityTransformer
{

    @Override
    public int getInputPriority(Class<? extends DataElement> inputClass, DataElement de)
    {
        if ( FileDataElement.class.isAssignableFrom(inputClass) && (de instanceof Diagram) )
        {
            if ( ((Diagram) de).getType() instanceof SbolDiagramType )
                return 12;
        }
        return 0;
    }

    @Override
    public int getOutputPriority(String name)
    {
        if ( name.endsWith(".rdf") || name.endsWith(".ttl") || name.endsWith(".nt") || name.endsWith(".jsonld") || name.endsWith(".rj") )
            return 10;
        else if ( name.endsWith(".xml") )
            return 5;
        return 0;
    }

    @Override
    public Class<? extends Diagram> getOutputType()
    {
        return Diagram.class;
    }

    @Override
    public Diagram load(File input, String name, DataCollection<Diagram> origin) throws Exception
    {
        Diagram result = SbolDiagramReader.readDiagram(input, name, origin);
        return result;
    }

    @Override
    public void save(File output, Diagram diagram) throws Exception
    {
        if ( diagram.getType() instanceof SbolDiagramType )
        {
            saveLayout( diagram );
            Object doc = arrangeComponents(diagram);
            if ( doc instanceof SBOLDocument )
                SBOLWriter.write((SBOLDocument) doc, output);
        }

    }

    private Object arrangeComponents(Diagram diagram)
    {
        SBOLDocument document = SbolUtil.getDocument( diagram ); 
        if ( document == null )
            return null;

        Set<URI> allCDs = new HashSet<>();

        allCDs.addAll( diagram.stream(Node.class).filter(SbolUtil::isSbol ).map(SbolUtil::getIdentity).toSet());

        diagram.stream(Compartment.class).forEach(cmp -> {
            if ( cmp.getKernel() instanceof Backbone )
            {
                URI identity = SbolUtil.getIdentity(cmp);
                ComponentDefinition cd = document.getComponentDefinition(identity);

                if ( cd == null )
                    return;

                Map<URI, Component> u2c = new HashMap<>();
                cd.getComponents().stream().forEach(c -> u2c.put(c.getDefinition().getIdentity(), c));

                List<Node> ordered = cmp.stream(Node.class).sorted(Comparator.comparing(node -> node.getLocation().getX())).filter(n -> {
                    if ( SbolUtil.isSbol( n ) )
                    {
                        URI nodeURI = SbolUtil.getIdentity( n );
                        if ( u2c.containsKey(nodeURI) )
                        {
                            allCDs.add(nodeURI);
                            return true;
                        }
                    }
                    return false;
                } ).toList();
                Set<URI> u2n = ordered.stream().map( n -> SbolUtil.isSbol( n ) ? SbolUtil.getIdentity( n ) : null )
                        .filter( Objects::nonNull ).collect( Collectors.toSet() );

                Set<URI> toRemove = new HashSet<>( u2c.keySet() );
                toRemove.removeAll( u2n );

                //remove all missed components (removed from diagram)
                if( !toRemove.isEmpty() )
                {
                    for ( URI uriRemove : toRemove )
                    {
                        Component component = u2c.get(uriRemove);
                        for ( SequenceAnnotation sa : cd.getSequenceAnnotations() )
                        {
                            if ( sa.isSetComponent() && sa.getComponentURI().equals(component.getIdentity()) )
                            {
                                cd.removeSequenceAnnotation(sa);
                            }
                        }
                        for ( SequenceConstraint sc : cd.getSequenceConstraints() )
                        {
                            if ( sc.getSubjectURI().equals(component.getIdentity()) )
                            {
                                cd.removeSequenceConstraint(sc);
                            }
                            if ( sc.getObjectURI().equals(component.getIdentity()) )
                            {
                                cd.removeSequenceConstraint(sc);
                            }
                        }
                        try
                        {
                            cd.removeComponent(component);
                            u2c.remove(uriRemove);
                        }
                        catch (SBOLValidationException e)
                        {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                }

                //remove all old order constraints
                for ( SequenceConstraint sc : cd.getSequenceConstraints() )
                {
                    if ( RestrictionType.PRECEDES.equals(sc.getRestriction()) )
                        cd.removeSequenceConstraint(sc);
                }

                if ( ordered.size() > 1 )
                {
                    //add new order constraints for all components
                    int i = 0;
                    while ( i < ordered.size() - 1 )
                    {
                        URI subject = SbolUtil.getIdentity(ordered.get(i));
                        URI object = SbolUtil.getIdentity(ordered.get(i+1));
                        try
                        {
                            cd.createSequenceConstraint(cmp.getName() + "_constaint_" + i, RestrictionType.PRECEDES, u2c.get(subject).getIdentity(), u2c.get(object).getIdentity());
                        }
                        catch (SBOLValidationException e)
                        {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                        i++;
                    }
                }
            }
        });

        for ( ComponentDefinition cd : document.getComponentDefinitions() )
        {
            if ( !allCDs.contains(cd.getIdentity()) )
                try
                {
                    document.removeComponentDefinition(cd);
                }
                catch (SBOLValidationException e)
                {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
        }
        return document;
    }

    public static void saveLayout(Diagram diagram)
    {
        try
        {
            String NAME_SPACE = "http://biouml.org/sbol/";
            String PREFIX = "biouml";

            SBOLDocument doc = SbolUtil.getDocument( diagram );
            GenericTopLevel level = doc.getGenericTopLevel( "Layout", "1" );
            if( level != null )
                doc.removeGenericTopLevel( level );
            level = SbolUtil.createTopLevel( doc, NAME_SPACE, "Layout", "biouml" );

            for( Node node : diagram.recursiveStream().select( Node.class ) )
            {
                if( node instanceof Diagram )
                    continue;
                List<Annotation> annotations = new ArrayList<>();
                annotations.add( SbolUtil.createAnnotation( NAME_SPACE, "refId", PREFIX, node.getKernel().getName() ) );
                annotations.add( SbolUtil.createAnnotation( NAME_SPACE, "title", PREFIX, node.getTitle() ) );
                annotations.add( SbolUtil.createAnnotation( NAME_SPACE, "x", PREFIX, String.valueOf( node.getLocation().x ) ) );
                annotations.add( SbolUtil.createAnnotation( NAME_SPACE, "y", PREFIX, String.valueOf( node.getLocation().y ) ) );
                annotations.add( SbolUtil.createAnnotation( NAME_SPACE, "width", PREFIX, String.valueOf( node.getShapeSize().width ) ) );
                annotations.add( SbolUtil.createAnnotation( NAME_SPACE, "height", PREFIX, String.valueOf( node.getShapeSize().height ) ) );
                SbolUtil.createAnnotation( level, NAME_SPACE, "NodeGlyph", "nodeGlyph", PREFIX, node.getKernel().getName(), annotations );
            }
            for( Edge edge : diagram.recursiveStream().select( Edge.class ) )
            {
                List<Annotation> annotations = new ArrayList<>();
                annotations.add( SbolUtil.createAnnotation( NAME_SPACE, "refId", PREFIX, edge.getKernel().getName() ) );
                Path path = edge.getPath();
                if( edge.getPath() != null )
                {
                    for( int i = 0; i < path.npoints; i++ )
                    {
                        annotations.add( SbolUtil.createAnnotation( NAME_SPACE, "segment", PREFIX,
                                String.valueOf( path.xpoints[i] ) + ";" + String.valueOf( path.ypoints[i] ) ) );
                    }
                }
                else
                {
                    annotations.add( SbolUtil.createAnnotation( NAME_SPACE, "inPort", PREFIX,
                            String.valueOf( edge.getInPort().x ) + ";" + String.valueOf( edge.getInPort().y ) ) );
                    annotations.add( SbolUtil.createAnnotation( NAME_SPACE, "outPort", PREFIX,
                            String.valueOf( edge.getOutPort().x ) + ";" + String.valueOf( edge.getOutPort().y ) ) );
                }
                SbolUtil.createAnnotation( level, NAME_SPACE, "Edge", "edge", PREFIX, edge.getName(), annotations );
            }
        }
        catch( Exception ex )
        {
            ex.printStackTrace();
        }
    }

    @Override
    public Map<String, FileTypePriority> getExtensionPriority()
    {
        return Collections.emptyMap();
    }
}