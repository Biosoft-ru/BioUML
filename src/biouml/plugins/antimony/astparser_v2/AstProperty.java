/* Generated By:JJTree: Do not edit this line. AstProperty.java Version 7.0 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=false,TRACK_TOKENS=true,NODE_PREFIX=Ast,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package biouml.plugins.antimony.astparser_v2;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import biouml.plugins.antimony.AntimonyAstCreator;

public class AstProperty extends SimpleNode
{
    public static final String NOTE = "Annotation";
    public static final String TABLE = "Table";

    public AstProperty(int id)
    {
        super(id);
    }

    public AstProperty(AntimonyNotationParser p, int id)
    {
        super(p, id);
    }

    /**
     * Used for storing subelements' names, e.g. <i> entity.sub_1.sub_2... <i>
     */
    private List<String> chainNames = new ArrayList<>();
    private String notationType;
    private String declarationType;

    public void setNotationType(String notationType)
    {
        this.notationType = notationType;
    }

    public String getNotationType()
    {
        return notationType;
    }

    public String getDeclarationType()
    {
        return declarationType;
    }

    public void setDeclarationType(String declarationType)
    {
        this.declarationType = declarationType;
    }


    /**
     * Adds a chain name (subelement's name)
     */
    public void addChainName(String symbolName)
    {
        this.chainNames.add(symbolName);
    }

    /**
     * Returns the list of subelements' names
     * @return
     */
    public List<String> getChainNames()
    {
        return chainNames;
    }

    /***
     * Check whether is node returned by locate(entity_name, port type)
     */
    public boolean hasImplicitName()
    {
        if( chainNames.isEmpty() && getChildren() != null )
            for( Node node : getChildren() )
                if( node instanceof AstLocateFunction )
                    return true;

        return false;
    }

    @Override
    public String toString()
    {
        StringBuilder str = new StringBuilder();
        str.append("@").append(notationType);

        if( declarationType != null )
            str.append(" ").append(declarationType);

        if( !chainNames.isEmpty() )
            str.append(" ").append(chainNames.stream().collect(Collectors.joining(".")));

        return str.toString();
    }

    private boolean hasDot = false;

    /**
     * Dot needed if there is only one single property:
     * <br>
     * <i>entity.propertyName = propertyValue; <i>
     * @param value
     */
    public void setDotNeeded(boolean value)
    {
        if( hasDot != value )
        {
            hasDot = value;
            for( Node astNode : this.getChildren() )
                if( astNode instanceof AstSingleProperty )
                    ( (AstSingleProperty)astNode ).setDotNeeded(value);
        }
    }

    /**
     * Checks whether it contains any single properties
     * @return
     */
    public boolean isEmpty()
    {
        if( countSingleProperties() > 0 )
            return false;

        return true;
    }

    private int countSingleProperties()
    {
        if( this.getChildren() == null )
            return 0;

        int counter = 0;
        for( Node astNode : this.getChildren() )
            if( astNode instanceof AstSingleProperty )
                counter++;

        return counter;
    }

    /**
     * Adds single property if property already contains one or more of them 
     * @param spropName
     * @param value
     */
    public void addSingleProperty(String spropName, Object value)
    {
        int spropQuantity = countSingleProperties();

        if( spropQuantity == 0 )
            return;

        AstSingleProperty sprop = new AstSingleProperty(AntimonyNotationParser.JJTSINGLEPROPERTY);
        sprop.setPropertyName(spropName);
        sprop.setPropertyValue(value);
        AntimonyAstCreator.createSpace(sprop);

        if( spropQuantity == 1 )
        {
            for( int i = 0; i < jjtGetNumChildren(); i++ )
            {
                if( jjtGetChild(i) instanceof AstSingleProperty )
                {
                    ( (AstSingleProperty)jjtGetChild(i) ).setDotNeeded(false);
                    AstEqual equal = new AstEqual(AntimonyNotationParser.JJTEQUAL);
                    AntimonyAstCreator.createSpace(equal);
                    addWithDisplacement(equal, i);
                    AstRegularFormulaElement lb = new AstRegularFormulaElement(AntimonyNotationParser.JJTREGULARFORMULAELEMENT);
                    lb.setElement("{");
                    AntimonyAstCreator.createSpace(lb);
                    addWithDisplacement(lb, i + 1);
                    addWithDisplacement(sprop, i + 2);
                    addWithDisplacement(new AstComma(AntimonyNotationParser.JJTCOMMA), i + 3);

                    AntimonyAstCreator.createSpace((SimpleNode)jjtGetChild(i + 4));
                    AstRegularFormulaElement rb = new AstRegularFormulaElement(AntimonyNotationParser.JJTREGULARFORMULAELEMENT);
                    rb.setElement("}");
                    addWithDisplacement(rb, jjtGetNumChildren() - 1);
                    break;
                }

            }
        }
        else
        {
            for( int i = jjtGetNumChildren() - 1; i >= 0; i-- )
            {
                if( jjtGetChild(i) instanceof AstSingleProperty )
                {
                    addWithDisplacement(new AstComma(AntimonyNotationParser.JJTCOMMA), i + 1);
                    addWithDisplacement(sprop, i + 2);
                    break;
                }
            }
        }

    }

    /***
     * Adds AstSingleProperty nodes to AstProperty from map
     * @param value - map of values
     */
    public void createValueNode(Map<String, Object> value)
    {
        if( value.size() == 1 )
        {
            for( Map.Entry<String, Object> val : value.entrySet() )
            {
                AstSingleProperty sprop = new AstSingleProperty(AntimonyNotationParser.JJTSINGLEPROPERTY);
                sprop.setPropertyName(val.getKey());
                sprop.setPropertyValue(val.getValue());

                addAsLast(sprop);
                setDotNeeded(true);
            }
        }
        else
        {
            AstEqual eq = new AstEqual(AntimonyNotationParser.JJTEQUAL);
            AntimonyAstCreator.createSpace(eq);
            addAsLast(eq);
            AstRegularFormulaElement lb = new AstRegularFormulaElement(AntimonyNotationParser.JJTREGULARFORMULAELEMENT);
            lb.setElement("{");
            AntimonyAstCreator.createSpace(lb);
            addAsLast(lb);

            Iterator<Map.Entry<String, Object>> it = value.entrySet().iterator();
            while( it.hasNext() )
            {
                Map.Entry<String, Object> val = it.next();
                AstSingleProperty sprop = new AstSingleProperty(AntimonyNotationParser.JJTSINGLEPROPERTY);
                sprop.setPropertyName(val.getKey());
                AntimonyAstCreator.createSpace(sprop);
                sprop.setPropertyValue(val.getValue());
                addAsLast(sprop);

                if( it.hasNext() )
                    addAsLast(new AstComma(AntimonyNotationParser.JJTCOMMA));
            }

            AstRegularFormulaElement rb = new AstRegularFormulaElement(AntimonyNotationParser.JJTREGULARFORMULAELEMENT);
            rb.setElement("}");
            addAsLast(rb);
            setDotNeeded(false);
        }
    }

    /**
     * 
     * @param propertyName
     * @return AstSingleProperty node with property name
     */
    public AstSingleProperty getSinglePropety(String propertyName)
    {
        for( Node astNode : getChildren() )
            if( astNode instanceof AstSingleProperty && ( (AstSingleProperty)astNode ).getPropertyName().equals(propertyName) )
                return (AstSingleProperty)astNode;

        return null;
    }

}
/* JavaCC - OriginalChecksum=cac56be90e24d0f2bae36cbc09fdbd30 (do not edit this line) */