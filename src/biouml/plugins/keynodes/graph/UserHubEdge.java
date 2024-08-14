package biouml.plugins.keynodes.graph;

import ru.biosoft.access.biohub.Element;
import ru.biosoft.access.biohub.RelationType;

import biouml.model.Node;
import biouml.plugins.keynodes.biohub.KeyNodesHub;
import biouml.standard.diagram.Util;
import biouml.standard.type.DatabaseReference;

public class UserHubEdge implements HubEdge
{
    public static final String USER_REACTION_NAME_PREFIX = "User_reaction_";
    public static final String USER_REACTION_PREFIX = "User reaction ";
    public static final String LINKED_NODE_SEPARATOR = ",";

    public static int cnt = 1;
    private final String name;
    private final String title;

    private final String from;
    private final String to;


    public UserHubEdge(String title)
    {
        this.name = generateCorrectName( title );
        this.title = title;
        from = null;
        to = null;
    }

    public UserHubEdge(String title, String from, String to)
    {
        this.name = generateCorrectName( title );
        this.title = title;
        this.from = from;
        this.to = to;

    }

    private String generateCorrectName(String title)
    {
        return USER_REACTION_NAME_PREFIX + cnt++;
    }

    @Override
    public String toString()
    {
        return title;
    }

    @Override
    public Element createElement(KeyNodesHub<?> hub)
    {
        Element element = new Element( DatabaseReference.STUB_PATH.getChildPath( "reaction", name ) );
        element.setValue( Element.USER_TITLE_PROPERTY, title );
        if( from != null && !from.isEmpty() )
            element.setValue( Element.USER_REACTANTS_PROPERTY, from );
        if( to != null && !to.isEmpty() )
            element.setValue( Element.USER_PRODUCTS_PROPERTY, to );
        return element;
    }

    @Override
    public String getRelationType(boolean upstream)
    {
        return upstream ? RelationType.REACTANT : RelationType.PRODUCT;
    }

    public static boolean isUserReaction(Node node)
    {
        return Util.isReaction( node ) && node.getKernel().getName().startsWith( USER_REACTION_NAME_PREFIX );
    }
}
