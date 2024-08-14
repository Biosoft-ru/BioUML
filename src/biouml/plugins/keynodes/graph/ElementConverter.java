package biouml.plugins.keynodes.graph;

import java.util.function.Function;

import ru.biosoft.access.biohub.Element;

/**
 * This class is capable to convert {@link Element} objects to/from internal hub node
 *
 * @author lan
 *
 * @param <N> node type
 */
public interface ElementConverter<N>
{
    /**
     * Converts an {@link Element} to the node.
     *
     * @param e an input element
     * @return the created node. {@code toNode(e).toString()} must produce the same string as {@code e.getAccession()}
     * @throws ParameterException if the element is unsupported by this converter.
     */
    N toNode(Element e);

    /**
     * Converts a node to the {@link Element}
     *
     * @param n a node to convert
     * @return the created element. {@code fromNode(n).getAccession()} must produce the same string as {@code n.toString()}.
     * The path of the {@code Element} must be set, preferably linked to existing repository element.
     */
    Element fromNode(N n);

    public static <N> ElementConverter<N> of(Function<Element, N> toNode, Function<N, Element> fromNode)
    {
        return new ElementConverter<N>()
        {
            @Override
            public N toNode(Element e)
            {
                return toNode.apply( e );
            }

            @Override
            public Element fromNode(N n)
            {
                return fromNode.apply( n );
            }
        };
    }
}
