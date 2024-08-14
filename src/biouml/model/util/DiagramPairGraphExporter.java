package biouml.model.util;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import one.util.streamex.StreamEx;

import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramExporter;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.standard.type.Base;
import biouml.standard.type.Reaction;
import biouml.standard.type.SemanticRelation;
import biouml.standard.type.SpecieReference;

/**
 * Export diagram like pair graph in text format
 */
public class DiagramPairGraphExporter extends DiagramExporter
{
    /**
    * Accept any diagrams
    */
    @Override
    public boolean accept(Diagram diagram)
    {
        return diagram != null;
    }

    /**
     * Export diagram2
     */
    @Override
    public void doExport(@Nonnull Diagram diagram, @Nonnull File file) throws Exception
    {
        try (PrintWriter pw = new PrintWriter( file, "UTF-8" ))
        {
            processReactions( diagram, pw );
            processRelations( diagram, pw );
        }
    }

    protected void processReactions(Compartment compartment, PrintWriter pw)
    {
        for(Node de : compartment.recursiveStream().select( Node.class ))
        {
            if( isReaction(de.getKernel()) )
            {
                List<Node> reactants = new ArrayList<>();
                List<Node> products = new ArrayList<>();
                List<Node> modifiers = new ArrayList<>();
                for( Edge edge : de.getEdges() )
                {
                    if( isReactant(edge.getKernel()) )
                    {
                        reactants.add(edge.getOtherEnd(de));
                    } else if( isProduct(edge.getKernel()))
                    {
                        products.add(edge.getOtherEnd(de));
                    } else if( isModifier(edge.getKernel()))
                    {
                        modifiers.add(edge.getOtherEnd(de));
                    }
                }

                pw.println("//" + getReactionComment(de.getName(), reactants, products, modifiers));
                for( Node node : reactants )
                {
                    pw.println(node.getName() + "\t" + de.getName());
                }
                for( Node node : products )
                {
                    pw.println(de.getName() + "\t" + node.getName());
                }
                for( Node node : modifiers )
                {
                    pw.println(node.getName() + "\t" + de.getName());
                }
                pw.println();
            }
        }
    }
    protected String getReactionComment(String name, List<Node> reactants, List<Node> products, List<Node> modifiers)
    {
        StringBuilder result = new StringBuilder();
        result.append(name);
        result.append(": ");
        result.append(StreamEx.of(reactants).map(Node::getTitle).joining(" + "));
        String modifiersString = StreamEx.of(modifiers).map( m -> " -"+m.getTitle() ).joining();
        result.append( modifiersString.isEmpty() ? " " : modifiersString );
        result.append("-> ");
        result.append(StreamEx.of(products).map(Node::getTitle).joining(" + "));
        return result.toString();
    }

    protected void processRelations(Compartment compartment, PrintWriter pw)
    {
        compartment.recursiveStream().select( Edge.class ).filter(edge -> isRelation(edge.getKernel()))
            .map( edge -> edge.getInput().getTitle() + "\t" + edge.getOutput().getTitle() )
            .forEach( pw::println );
    }

    protected boolean isReaction(Base kernel)
    {
        return kernel != null && ( ( kernel instanceof Reaction ) || kernel.getType().equals("reaction") );
    }

    protected boolean isRelation(Base kernel)
    {
        return kernel != null && ( ( kernel instanceof SemanticRelation ) || kernel.getType().equals("relation") );
    }

    protected boolean isReactant(Base kernel)
    {
        return kernel != null
                && ( ( kernel instanceof SpecieReference && ( (SpecieReference)kernel ).getRole().equals(SpecieReference.REACTANT) ) || kernel
                        .getType().equals("consumption") );
    }

    protected boolean isProduct(Base kernel)
    {
        return kernel != null
                && ( ( kernel instanceof SpecieReference && ( (SpecieReference)kernel ).getRole().equals(SpecieReference.PRODUCT) ) || kernel
                        .getType().equals("production") );
    }

    protected boolean isModifier(Base kernel)
    {
        return kernel != null
                && ( ( kernel instanceof SpecieReference && ( (SpecieReference)kernel ).getRole().equals(SpecieReference.MODIFIER) ) || ( kernel
                        .getType().equals("regulation") ) );
    }

    /**
     * Initialize exporter
     */
    @Override
    public boolean init(String format, String suffix)
    {
        return true;
    }

}
