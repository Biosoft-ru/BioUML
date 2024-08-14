package ru.biosoft.analysis.diagram;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.Node;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.util.OptionEx;
import ru.biosoft.util.bean.BeanInfoEx2;
import ru.biosoft.util.bean.JSONBean;

@SuppressWarnings ( "serial" )
public class DiagramGraphCompareParameters extends AbstractAnalysisParameters
{
    private static final String BOTH = "BOTH";
    private static final String UP = "UP";
    private static final String DOWN = "DOWN";
    private static final String COMPARATOR_DEFAULT = "By name";
    private static final String COMPARATOR_KERNEL = "By kernel";
    private static final String COMPARATOR_ATTRIBUTE = "By attribute";

    private DataElementPath inputPath1;
    private DataElementPath inputPath2;
    private DataElementPath outputPath;
    private String startNodeName1 = null;
    private String startNodeName2 = null;
    private int depth = 0;
    private int direction = DiagramGraphCompareAnalysis.DOWN;
    private String directionStr = DOWN;
    private boolean needLayout = true;
    private NodeComparator comparator = new NodeComparator();

    @PropertyName ( "Input path 1" )
    @PropertyDescription ( "Path to the first input diagram" )
    public DataElementPath getInputPath1()
    {
        return inputPath1;
    }
    public void setInputPath1(DataElementPath modelPath)
    {
        Object oldValue = this.inputPath1;
        this.inputPath1 = modelPath;
        firePropertyChange("inputPath1", oldValue, modelPath);
    }

    @PropertyName ( "Input path 2" )
    @PropertyDescription ( "Path to the second input diagram" )
    public DataElementPath getInputPath2()
    {
        return inputPath2;
    }
    public void setInputPath2(DataElementPath modelPath)
    {
        Object oldValue = this.inputPath2;
        this.inputPath2 = modelPath;
        firePropertyChange("inputPath2", oldValue, modelPath);
    }

    @PropertyName ( "Output path" )
    @PropertyDescription ( "Path to the output diagram" )
    public DataElementPath getOutputPath()
    {
        return outputPath;
    }
    public void setOutputPath(DataElementPath outputPath)
    {
        Object oldValue = this.outputPath;
        this.outputPath = outputPath;
        firePropertyChange("outputPath", oldValue, outputPath);
    }

    @PropertyName ( "Start node name 1" )
    @PropertyDescription ( "Name of the start node from the first input diagram" )
    public String getStartNodeName1()
    {
        return startNodeName1;
    }
    public void setStartNodeName1(String startNodeName1)
    {
        Object oldValue = this.startNodeName1;
        this.startNodeName1 = startNodeName1;
        firePropertyChange("startNodeName1", oldValue, startNodeName1);
    }
    public String getStartNodeId1()
    {
        return getNodeIdByGeneratedName( getStartNodeName1() );
    }

    @PropertyName ( "Start node name 2" )
    @PropertyDescription ( "Name of the start node from the second input diagram" )
    public String getStartNodeName2()
    {
        return startNodeName2;
    }
    public void setStartNodeName2(String startNodeName2)
    {
        Object oldValue = this.startNodeName2;
        this.startNodeName2 = startNodeName2;
        firePropertyChange("startNodeName2", oldValue, startNodeName2);
    }
    public String getStartNodeId2()
    {
        return getNodeIdByGeneratedName( getStartNodeName2() );
    }

    @PropertyName ( "Depth" )
    @PropertyDescription ( "Depth of the search" )
    public int getDepth()
    {
        return depth;
    }
    public void setDepth(int depth)
    {
        Object oldValue = this.depth;
        this.depth = depth;
        firePropertyChange("depth", oldValue, depth);
    }

    public int getDirection()
    {
        return direction;
    }
    public void setDirection(int direction)
    {
        Object oldValue = this.direction;
        this.direction = direction;
        firePropertyChange("direction", oldValue, direction);
    }

    @PropertyName ( "Need layout" )
    @PropertyDescription ( "shows if output diagram needs layout" )
    public boolean isNeedLayout()
    {
        return needLayout;
    }
    public void setNeedLayout(boolean needLayout)
    {
        Object oldValue = this.needLayout;
        this.needLayout = needLayout;
        firePropertyChange("direction", oldValue, needLayout);
    }

    final static String[] availableDirections = new String[] {DOWN, UP, BOTH};

    @PropertyName ( "Direction" )
    @PropertyDescription ( "Direction of the search" )
    public String getDirectionStr()
    {
        return directionStr;
    }
    public void setDirectionStr(String directionStr)
    {
        Object oldValue = this.directionStr;
        if( DOWN.equals(directionStr) )
        {
            this.directionStr = DOWN;
            setDirection(DiagramGraphCompareAnalysis.DOWN);
        }
        else if( UP.equals(directionStr) )
        {
            this.directionStr = UP;
            setDirection(DiagramGraphCompareAnalysis.UP);
        }
        else
        {
            this.directionStr = BOTH;
            setDirection(DiagramGraphCompareAnalysis.BOTH);
        }
        firePropertyChange("directionStr", oldValue, this.directionStr);
    }

    @PropertyName ( "Node comparator" )
    @PropertyDescription ( "Comparator which will be used to compare nodes" )
    public NodeComparator getComparator()
    {
        return comparator;
    }
    public void setComparator(NodeComparator comparator)
    {
        Object oldValue = this.comparator;
        this.comparator = comparator;
        firePropertyChange("comparator", oldValue, comparator);
    }

    public static class NodeComparator extends OptionEx implements JSONBean
    {
        String type = COMPARATOR_DEFAULT;
        String attributeName = "";
        @PropertyName ( "Comparing type" )
        @PropertyDescription ( "Predefined type to determine if nodes are the same.<br>E.g. '" + COMPARATOR_DEFAULT
                + "' compares by name and title." )
        public String getType()
        {
            return type;
        }
        public void setType(String type)
        {
            Object oldValue = this.type;
            this.type = type;
            firePropertyChange("type", oldValue, type);
            firePropertyChange( "*", null, null );
        }
        @PropertyName ( "Name of attribute" )
        public String getAttributeName()
        {
            return attributeName;
        }
        public void setAttributeName(String attributeName)
        {
            Object oldValue = this.attributeName;
            this.attributeName = attributeName;
            firePropertyChange("attributeName", oldValue, attributeName);
        }
        public boolean hideAttrName()
        {
            return !COMPARATOR_ATTRIBUTE.equals( type );
        }
        public boolean areAnalogues(Node node1, Node node2)
        {
            switch( type )
            {
                case COMPARATOR_ATTRIBUTE:
                    Object attr1 = node1.getAttributes().getValue(attributeName);
                    Object attr2 = node2.getAttributes().getValue(attributeName);
                    if( attr1 == null || attr2 == null )
                        return false;
                    return attr1.equals(attr2);

                case COMPARATOR_KERNEL:
                    return node1.getKernel().equals(node2.getKernel());

                case COMPARATOR_DEFAULT:
                default:
                    return node1.getName().equals(node2.getName()) || node1.getTitle().equals(node2.getTitle());
            }
        }
        static final String[] availableComparators = new String[] {COMPARATOR_DEFAULT, COMPARATOR_KERNEL, COMPARATOR_ATTRIBUTE};
    }

    public static class NodeComparatorBeanInfo extends BeanInfoEx2<NodeComparator>
    {
        public NodeComparatorBeanInfo()
        {
            super(NodeComparator.class);
        }
        @Override
        protected void initProperties() throws Exception
        {
            property( "type" ).tags( NodeComparator.availableComparators ).structureChanging().add();
            addHidden( "attributeName", "hideAttrName" );
        }
    }

    private static String getNodeIdByGeneratedName(String nodeName)
    {
        if( nodeName == null )
            return nodeName;
        int index = nodeName.indexOf( " (id: " );
        if( index == -1 )
            return nodeName;
        return nodeName.substring( index + 6, nodeName.length() - 1 );
    }
    public static String generateNodeName(Node node)
    {
        Compartment comp = node.getCompartment();
        String id = comp instanceof Diagram ? node.getName() : comp.getName() + "." + node.getName();
        String title = node.getTitle();
        if( title == null )
            return id;
        return title + " (id: " + id + ")";
    }
}
