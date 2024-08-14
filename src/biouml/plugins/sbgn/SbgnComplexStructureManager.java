package biouml.plugins.sbgn;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import com.developmentontheedge.application.ApplicationUtils;
import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;

import biouml.model.Compartment;
import biouml.model.DefaultSemanticController;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.DiagramViewBuilder;
import biouml.model.DiagramViewOptions;
import biouml.model.Node;
import biouml.model.dynamics.VariableRole;
import biouml.plugins.sbgn.title.TitleElement;
import biouml.standard.diagram.Util;
import biouml.standard.type.Specie;
import biouml.standard.type.Stub;
import one.util.streamex.StreamEx;
import ru.biosoft.graphics.ComplexTextView;
import ru.biosoft.graphics.CompositeView;
import ru.biosoft.graphics.View;
import ru.biosoft.util.DPSUtils;

public class SbgnComplexStructureManager implements PropertyChangeListener
{
    private final String structureAttr = "attributes/" + Util.COMPLEX_STRUCTURE;
    private final String typeAttr = "type";
    private final String multimerAttr = "attributes/" + SBGNPropertyConstants.SBGN_MULTIMER;

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        String propertyName = evt.getPropertyName();
        Object source = evt.getSource();

        //        System.out.println( "Property: " + evt.getPropertyName() + " : from " + evt.getOldValue() + " to " + evt.getNewValue() );
        if( structureAttr.equals(propertyName) && source instanceof Compartment && ( (Node)source ).getRole() instanceof VariableRole )
        {
            annotateSpecies((Compartment)source);
        }
        else if( typeAttr.equals(propertyName) && source instanceof Specie && ( (Specie)source ).getParent() instanceof Compartment )
        {
            String val = evt.getNewValue().toString();
            if( !Type.TYPE_COMPLEX.equals(val) )
            {
                try
                {
                    Compartment c = (Compartment) ( (Specie)source ).getParent();
                    Node[] inner = c.stream().filter(n -> n.getKernel() instanceof Specie).toArray(Node[]::new);
                    for( Node n : inner )
                        c.remove(n.getName());
                    c.getAttributes().setValue(Util.COMPLEX_STRUCTURE, constructSBGNViewTitle(c));
                }
                catch( Exception e )
                {
                    Logger.getLogger(SbgnComplexStructureManager.class.getName()).log(Level.SEVERE, e.getMessage(), e);
                }
            }
        }
        else if( multimerAttr.equals(propertyName) && source instanceof Compartment && ( (Node)source ).getRole() instanceof VariableRole )
        {
            Compartment c = (Compartment)source;
            c.getAttributes().setValue(Util.COMPLEX_STRUCTURE, constructSBGNViewTitle(c));
        }
        else if( "title".equals(propertyName) && source instanceof Compartment && ( (Node)source ).getRole() instanceof VariableRole )
        {
            ( (Node)source ).setTitle(validateTitle(evt.getNewValue().toString()));
        }
    }

    public static String validateTitle(String title)
    {
        if( title.startsWith("'") && title.endsWith("'") )
            return title;

        if( SbgnUtil.containsAny(title, new String[] {"{", "}", "[", "]", ":"}) )
        {
            if( !title.startsWith("'") )
                title = "'" + title;
            if( !title.endsWith("'") )
                title = title + "'";
        }
        return title;
    }

    public static boolean equals(TitleElement t1, TitleElement t2)
    {
        if( t1.getTitle().equals(t2.getTitle()) ) //elements may be rearranged, string inequality is not a guarantee
            return true;

        if( !t1.getType().equals(t2.getType()) )
            return false;

        if( !t1.getModificators().containsAll(t2.getModificators()) || !t2.getModificators().containsAll(t1.getModificators()) )
            return false;

        if( !t1.getUnitOfInfo().containsAll(t2.getUnitOfInfo()) || !t2.getUnitOfInfo().containsAll(t1.getUnitOfInfo()) )
            return false;

        List<TitleElement> t1Subs = t1.getSubElements();
        List<TitleElement> t2Subs = t2.getSubElements();
        if( t1Subs.size() != t2Subs.size() )
            return false;

        Set<TitleElement> t2SubSet = StreamEx.of(t2).toSet();
        boolean found = false;
        TitleElement foundTitle = null;
        for( TitleElement innerT1 : t1Subs )
        {
            for( TitleElement innerT2 : t2SubSet )
            {
                if( equals(innerT1, innerT2) )
                {
                    foundTitle = innerT2;
                    found = true;
                }
            }
            if( !found )
                return false;
            t2SubSet.remove(foundTitle);
        }

        return true;
    }

    public static void annotateSpecies(Compartment species)
    {
        try
        {
            String structure = species.getAttributes().getValueAsString(Util.COMPLEX_STRUCTURE);
            if( structure == null || structure.isEmpty() )
                return;
            TitleElement tree = new TitleElement(structure);
            species.clear();
            if( tree.isComplex() )
                ( (Specie)species.getKernel() ).setType(Type.TYPE_COMPLEX);
            else if( SbgnUtil.isComplex(species) )
                ( (Specie)species.getKernel() ).setType(Type.TYPE_MACROMOLECULE);
            annotateNode(species, tree);
            arrangeComplexView(species, tree);
            species.setView(null);
        }
        catch( Exception e )
        {
            Logger.getLogger(SbgnComplexStructureManager.class.getName()).log(Level.SEVERE, e.getMessage(), e);
        }
    }

    private static void annotateNode(Compartment de, TitleElement tree) throws Exception
    {
        if( ! ( de.getRole() instanceof VariableRole ) )
            de.setTitle(tree.getName() == null ? tree.getTitleNoSpecies() : tree.getName());

        if( tree.isComplex() )
        {
            //            de.setShowTitle(false);
            //structure of inner complexes can not be handled manually
            de.getAttributes().add(DPSUtils.createTransient(Util.COMPLEX_STRUCTURE, String.class, tree.getTitle()));
            int sub = 1;
            for( TitleElement element : tree.getSubElements() )
            {
                //(leptin)2:(LEPR-B:Jak2{pY1007}{pY1008})2
                String childAcc = element.getName();//DefaultSemanticController.generateUniqueNodeName(de, "sub_" + sub++);
                if( childAcc == null || childAcc.isEmpty() )
                    childAcc = DefaultSemanticController.generateUniqueNodeName(de, "sub_" + sub++);
                String childType = element.isComplex() ? Type.TYPE_COMPLEX : getTypeByString(element.getType());
                Compartment child = (Compartment) ( SbgnSemanticController.createDiagramElement(childType, childAcc, de) );
                de.put(child);
                annotateNode(child, element);
            }
        }

        de.setTitle(tree.getName() != null ? tree.getName() : "");
        if( tree.isMultimer() )
        {
            if( tree.getMultimerCount() == TitleElement.ARBITRARY_MULTIMER_COUNT )
                de.getAttributes().add(new DynamicProperty(SBGNPropertyConstants.SBGN_MULTIMER_PD, String.class, "n"));
            else
                de.getAttributes().add(new DynamicProperty(SBGNPropertyConstants.SBGN_MULTIMER_PD, Integer.class, tree.getMultimerCount()));
        }
        else
            de.getAttributes().add(new DynamicProperty(SBGNPropertyConstants.SBGN_MULTIMER_PD, Integer.class, 0));

        for( String modificator : tree.getModificators() )
        {
            Node varNode = new Node(de, new Stub(null, modificator, Type.TYPE_VARIABLE));
            SbgnSemanticController.setNeccessaryAttributes(varNode);
            de.put(varNode);
        }
        for( String uoi : tree.getUnitOfInfo() )
        {
            de.put(new Node(de, new Stub(null, uoi, Type.TYPE_UNIT_OF_INFORMATION)));
        }
        SbgnSemanticController.setNeccessaryAttributes(de);
    }

    private static String getTypeByString(String t)
    {
        switch( t )
        {
            case "s":
                return Type.TYPE_SIMPLE_CHEMICAL;
            case "n":
                return Type.TYPE_NUCLEIC_ACID_FEATURE;
            case "c":
                return Type.TYPE_COMPLEX;
            case "p":
                return Type.TYPE_PERTURBING_AGENT;
            case "m":
                return Type.TYPE_MACROMOLECULE;
            case "u":
                return Type.TYPE_UNSPECIFIED;
            default:
                return Type.TYPE_MACROMOLECULE;
        }
    }

    public static void arrangeComplexView(Compartment complex, TitleElement tree)
    {
        Diagram diagram = Diagram.getDiagram(complex);
        DiagramViewBuilder viewBuilder = diagram.getType().getDiagramViewBuilder();
        DiagramViewOptions viewOptions = diagram.getViewOptions();
        Graphics graphics = ApplicationUtils.getGraphics();
        viewBuilder.createCompartmentView(complex, viewOptions, graphics);
        moveComplexElements(complex);
    }

    private static void moveComplexElements(Compartment complex)
    {
        if( complex.getKernel().getType().equals(Type.TYPE_COMPLEX) )
        {
            int multimerShift = 15;
            Point location = complex.getLocation();
            int curY = location.y + 10;

            if( SbgnDiagramViewBuilder.getMultimerString(complex) != null )
                curY += multimerShift;
            for( Node subNode : complex.getNodes() )
            {
                if( subNode instanceof Compartment )
                {
                    subNode.setLocation(location.x + 10, curY);
                    if( subNode.getKernel().getType().equals(Type.TYPE_COMPLEX) )
                        moveComplexElements((Compartment)subNode);
                    else
                        arrangeModifiers((Compartment)subNode, curY, multimerShift);
                    Dimension dim = calcNodeSize(subNode);
                    subNode.setShapeSize(dim);
                    curY += dim.height;
                }
            }
            complex.setShapeSize(calcNodeSize(complex));
        }
        else
        {
            arrangeModifiers(complex, complex.getLocation().y + 10, 15);
            complex.setShapeSize(calcNodeSize(complex));
        }
    }

    private static void arrangeModifiers(Compartment c, int curY, int multimerShift)
    {
        int modifierShift = curY;
        if( SbgnDiagramViewBuilder.getMultimerString(c) != null )
            modifierShift += multimerShift;
        for( Node varNode : c.getNodes() )
        {
            int x = c.getLocation().x - varNode.getView().getBounds().width / 2;
            varNode.setLocation(x, modifierShift);
            modifierShift += varNode.getView().getBounds().height;
        }
    }

    protected static Dimension calcNodeSize(Node node)
    {
        //kernel
        if( node.getKernel() == null || node.getKernel().getType() == null )
            return new Dimension();

        Object nodeType = node.getKernel().getType();
        if( nodeType.equals(Type.TYPE_COMPLEX) && node instanceof Compartment )
        {
            int maxWidth = 0;
            int totalHeight = 0;
            for( Object subNode : (Compartment)node )
            {
                if( subNode instanceof Compartment )
                {
                    Dimension dim = calcNodeSize((Compartment)subNode);
                    maxWidth = Math.max(dim.width, maxWidth);
                    totalHeight += dim.height;
                    if( SbgnDiagramViewBuilder.getMultimerString((Compartment)subNode) != null )
                    {
                        totalHeight += 15;
                        maxWidth += 5;
                    }
                }
            }
            if( SbgnDiagramViewBuilder.getMultimerString(node) != null )
            {
                maxWidth += 5;
                totalHeight += 15;
            }
            return new Dimension(maxWidth + 20, totalHeight + 20);
        }
        else if( SBGNPropertyConstants.entityTypes.contains(nodeType) && node instanceof Compartment )
        {
            CompositeView view = (CompositeView)node.getView();
            int size = view.size();
            int width = 0, height = 0;
            int maxModWidth = 0;
            int totalModHeight = 0;
            for( Object subNode : (Compartment)node )
            {
                if( subNode instanceof Node )
                {
                    Dimension dim = calcNodeSize((Node)subNode);
                    maxModWidth = Math.max(dim.width, maxModWidth);
                    totalModHeight += dim.height;
                }
            }
            for( int i = 0; i < size; i++ )
            {
                View childView = view.elementAt(i);
                if( childView instanceof ComplexTextView )
                {
                    Rectangle bounds = childView.getBounds();
                    height = bounds.height;
                    width = bounds.width;
                }
            }
            height = Math.max(totalModHeight, height);
            width += maxModWidth;
            if( SbgnDiagramViewBuilder.getMultimerString(node) != null )
            {
                width += 5;
                height += 10;
            }
            return new Dimension(Math.max(70, width + 20), Math.max(40, height + 20));
        }
        else if( nodeType.equals(Type.TYPE_UNIT_OF_INFORMATION) || nodeType.equals(Type.TYPE_VARIABLE) )
        {
            return node.getView().getBounds().getSize();
        }
        else
            return new Dimension();
    }

    public static TitleElement getTitleByComplex(Compartment complex)
    {
        List<TitleElement> subTitles = new ArrayList<>();
        TitleElement complexTitleElement = new TitleElement();
        for( Node subNode : complex.getNodes() )
        {
            if( subNode instanceof Compartment )
            {
                TitleElement subTitle;
                Object nodeType = subNode.getKernel().getType();
                if( nodeType.equals(Type.TYPE_COMPLEX) )
                {
                    subTitle = getTitleByComplex((Compartment)subNode);
                }
                else
                {
                    subTitle = new TitleElement(subNode.getTitle());
                    subTitle.setModifiers( ( (Compartment)subNode ).stream(Node.class).map(n -> n.getTitle()).toList());

                }
                subTitle.setMultimerCount(getMultimerCount(subNode));
                subTitles.add(subTitle);
            }
        }
        complexTitleElement.setMultimerCount(getMultimerCount(complex));
        complexTitleElement.setSubElements(subTitles);
        return complexTitleElement;
    }

    @Nonnull
    public static String constructSBGNViewTitle(@Nonnull
    Node node)
    {
        StringBuilder sb = new StringBuilder();
        DynamicPropertySet attributes = node.getAttributes();
        String multimerStr = attributes.getValueAsString(SBGNPropertyConstants.SBGN_MULTIMER);
        boolean isMultimer = multimerStr != null && !multimerStr.isEmpty() && !"0".equals(multimerStr) && !"1".equals(multimerStr);
        boolean hasUOI = ( (Compartment)node ).stream(Node.class).filter(SbgnUtil::isUnitOfInformation).count() > 0;
        boolean hasState = ( (Compartment)node ).stream(Node.class).filter(SbgnUtil::isVariableNode).count() > 0;

        if( SbgnUtil.isComplex(node) )
        {
            if( node.isShowTitle() )
                sb.append(node.getTitle());

            long numChildren = ( (Compartment)node ).stream(Compartment.class).count();
            if( numChildren > 1 )
            {
                sb.append("(");
                sb.append( ( (Compartment)node ).stream(Compartment.class).map(inNode -> constructSBGNViewTitle(inNode))
                        .remove(String::isEmpty).joining(":"));
                sb.append(")");
            }
            else if( numChildren == 1 )
                sb.append("(" + ( (Compartment)node ).stream(Compartment.class).map(inNode -> constructSBGNViewTitle(inNode))
                        .remove(String::isEmpty).joining("") + ":)");
            else
                sb.append("(:)");

        }
        else
            sb.append(node.getTitle());//.getName());

        if( hasState )
            sb.append( ( (Compartment)node ).stream(Node.class).filter(SbgnUtil::isVariableNode)
                    .map(inNode -> "{" + inNode.getTitle() + "}").joining());
        if( hasUOI )
            sb.append( ( (Compartment)node ).stream(Node.class).filter(SbgnUtil::isUnitOfInformation)
                    .map(inNode -> "[" + inNode.getTitle() + "]").joining());

        if( isMultimer )
        {
            if( SbgnUtil.isNotComplexEntity(node) || ( SbgnUtil.isComplex(node) && ( hasUOI || hasState ) ) )
            {
                sb.insert(0, "(");
                sb.append(")").append(multimerStr);
            }
            else
                sb.append(multimerStr);
        }
        return sb.toString();
    }

    private static int getMultimerCount(Node node)
    {
        Object multimerObj = node.getAttributes().getValue(SBGNPropertyConstants.SBGN_MULTIMER);
        if( multimerObj instanceof Integer && (int)multimerObj > 1 )
            return (int)multimerObj;
        else if( multimerObj instanceof String )
            return TitleElement.ARBITRARY_MULTIMER_COUNT;
        else
            return 1;
    }

    public static String generateStructureProperty(DiagramElement de)
    {
        if( de instanceof Compartment )
        {
            Object structureObj = de.getAttributes().getValue(Util.COMPLEX_STRUCTURE);
            if( structureObj == null || ( structureObj instanceof String && structureObj.equals("") ) )
                return getTitleByComplex((Compartment)de).getTitle();
        }
        return "";

    }
}
