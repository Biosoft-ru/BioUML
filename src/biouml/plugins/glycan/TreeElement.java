package biouml.plugins.glycan;

import java.util.ArrayList;
import java.util.List;

import biouml.plugins.glycan.parser.GlycanMolecule;
import biouml.plugins.glycan.parser.Node;

public class TreeElement
{
    private final GlycanMolecule content;
    private int xPoint = 0;
    private int yPoint = 0;
    private List<TreeElement> children = new ArrayList<>();

    public TreeElement(GlycanMolecule mol, int xPoint, int yPoint)
    {
        this.content = mol;
        this.xPoint = xPoint;
        this.yPoint = yPoint;
    }

    public String getName()
    {
        return content.getName();
    }
    public int getX()
    {
        return xPoint;
    }
    public int getY()
    {
        return yPoint;
    }
    public List<TreeElement> getChildren()
    {
        return children;
    }
    public void setChildren(List<TreeElement> children)
    {
        this.children = children;
    }
    public Node getParent()
    {
        return content.jjtGetParent();
    }
    public String getBind()
    {
        return content.getBind();
    }
    /**
     * Shifts element with all children to the right
     * using given shiftStep
     * @param shiftStep
     */
    public void shift(int shiftStep)
    {
        xPoint += shiftStep;
        for( TreeElement child : children )
            child.shift(shiftStep);
    }
    @Override
    public String toString()
    {
        return content.getFullName() + "[" + xPoint + "," + yPoint + "]";
    }
}
