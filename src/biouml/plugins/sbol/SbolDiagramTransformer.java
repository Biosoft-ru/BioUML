package biouml.plugins.sbol;

import java.io.File;
import java.net.URI;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.sbolstandard.core2.Component;
import org.sbolstandard.core2.ComponentDefinition;
import org.sbolstandard.core2.RestrictionType;
import org.sbolstandard.core2.SBOLDocument;
import org.sbolstandard.core2.SBOLValidationException;
import org.sbolstandard.core2.SBOLWriter;
import org.sbolstandard.core2.SequenceAnnotation;
import org.sbolstandard.core2.SequenceConstraint;

import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.Node;
import ru.biosoft.access.AbstractFileTransformer;
import ru.biosoft.access.FileDataElement;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.generic.PriorityTransformer;

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

            Object doc = arrangeComponents(diagram);
            if ( doc != null && doc instanceof SBOLDocument )
            {
                SBOLWriter.write((SBOLDocument) doc, output);
            }
        }

    }

    private Object arrangeComponents(Diagram diagram)
    {
        Object doc = diagram.getAttributes().getValue(SbolUtil.SBOL_DOCUMENT_PROPERTY);
        if ( doc == null || !(doc instanceof SBOLDocument) )
            return doc;

        SBOLDocument document = (SBOLDocument) doc;
        Set<URI> allCDs = new HashSet<>();

        diagram.stream(Node.class).forEach(node -> {
            if ( node.getKernel() instanceof SbolBase )
            {
                allCDs.add(((SbolBase) node.getKernel()).getSbolObject().getIdentity());
            }
        });

        diagram.stream(Compartment.class).forEach(cmp -> {
            if ( cmp.getKernel() instanceof Backbone )
            {
                URI identity = ((Backbone) cmp.getKernel()).getSbolObject().getIdentity();
                ComponentDefinition cd = document.getComponentDefinition(identity);

                if ( cd == null )
                    return;

                Map<URI, Component> u2c = new HashMap<>();
                cd.getComponents().stream().forEach(c -> u2c.put(c.getDefinition().getIdentity(), c));

                List<Node> ordered = cmp.stream(Node.class).sorted(Comparator.comparing(node -> node.getLocation().getX())).filter(n -> {
                    if ( n.getKernel() instanceof SbolBase )
                    {
                        URI nodeURI = ((SbolBase) n.getKernel()).getSbolObject().getIdentity();
                        if ( u2c.containsKey(nodeURI) )
                        {
                            allCDs.add(nodeURI);
                            return true;
                        }
                    }
                    return false;
                }).toList();
                Set<URI> u2n = ordered.stream().map(n -> {
                    if ( n.getKernel() instanceof SbolBase )
                    {
                        return ((SbolBase) n.getKernel()).getSbolObject().getIdentity();
                    }
                    else
                    {
                        return null;
                    }
                }).filter(Objects::nonNull).collect(Collectors.toSet());

                Set<URI> toRemove = new HashSet<>(u2c.keySet());
                toRemove.removeAll(u2n);

                //remove all missed components (removed from diagram)
                if ( !toRemove.isEmpty() )
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
                        URI subject = ((SbolBase) ordered.get(i).getKernel()).getSbolObject().getIdentity();
                        URI object = ((SbolBase) ordered.get(i + 1).getKernel()).getSbolObject().getIdentity();
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


}
