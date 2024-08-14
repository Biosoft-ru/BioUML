package biouml.plugins.ensembl.homology;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;

import one.util.streamex.StreamEx;

import ru.biosoft.util.Maps;

/**
 * Represents homological tree; any node of homological tree and collection of tree nodes
 * @author lan
 */
public class TreeNode implements Iterable<TreeNode>
{
    private final List<TreeNode> children = new ArrayList<>();
    private TreeNode parent;
    private float distanceToParent;
    private String name;
    
    public TreeNode()
    {
    }

    /**
     * Parse tree in Newick-format
     * Example: (ENSACAP00000006360:0.0000,ENSXETP00000031758:0.0000):0.0000;
     * @param str tree
     */
    public TreeNode(CharSequence str)
    {
        int offset2, offset = 0;
        if(str.charAt(offset) != '(')
            throw new IllegalArgumentException("( expected");
        offset = parseTree(str, 1, this) + 1;
        offset2 = offset;
        while(offset2 < str.length() && str.charAt(offset2) != ';') offset2++;
        setDistanceToParent(Float.parseFloat(str.subSequence(offset, offset2).toString()));
    }
    
    protected static int parseTree(CharSequence str, int offset, TreeNode node)
    {
        do
        {
            TreeNode child = new TreeNode();
            node.addChild(child);
            int offset2;
            if(str.charAt(offset) == '(')
            {
                offset = parseTree(str, offset+1, child);
            } else
            {
                offset2 = offset;
                while(str.charAt(offset2) != ':') offset2++;
                child.setName(str.subSequence(offset, offset2).toString());
                offset = offset2;
            }
            offset++;
            offset2 = offset;
            while(str.charAt(offset2) != ',' && str.charAt(offset2) != ')') offset2++;
            child.setDistanceToParent(Float.parseFloat(str.subSequence(offset, offset2).toString()));
            offset = offset2+1;
        } while(str.charAt(offset-1) != ')');
        return offset;
    }
    
    public List<TreeNode> getChildren()
    {
        return Collections.unmodifiableList(children);
    }
    
    public TreeNode getChild(int n)
    {
        return children.get(n);
    }
    
    public int getChildrenCount()
    {
        return children.size();
    }
    
    public void addChild(TreeNode child)
    {
        this.children.add(child);
        child.setParent(this);
    }

    public TreeNode getParent()
    {
        return parent;
    }

    public void setParent(TreeNode parent)
    {
        this.parent = parent;
    }

    public float getDistanceToParent()
    {
        return distanceToParent;
    }

    public void setDistanceToParent(float distanceToParent)
    {
        this.distanceToParent = distanceToParent;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }
    
    /**
     * Returns tree in Newick-format (the same as passed to constructor)
     */
    @Override
    public String toString()
    {
        StringBuilder result = new StringBuilder();
        if(getName() != null)
            result.append(getName());
        else
        {
            result.append( StreamEx.of( children ).joining( ",", "(", ")" ) );
        }
        result.append(":").append(String.format(Locale.ENGLISH, "%.4f", getDistanceToParent()));
        if(getParent() == null) result.append(";");
        return result.toString();
    }

    @Override
    public Iterator<TreeNode> iterator()
    {
        return new Iterator<TreeNode>()
        {
            int state = -1;
            Iterator<TreeNode> subIterator;

            @Override
            public boolean hasNext()
            {
                if(state == -1) return true;
                if(state < getChildrenCount()) return subIterator.hasNext();
                return false;
            }

            @Override
            public TreeNode next()
            {
                TreeNode result = null;
                if(state == -1)
                {
                    result = TreeNode.this;
                    state++;
                    if(state < getChildrenCount()) subIterator = getChild(state).iterator();
                } else if(state < getChildrenCount())
                {
                    result = subIterator.next();
                    if(!subIterator.hasNext())
                    {
                        state++;
                        if(state < getChildrenCount()) subIterator = getChild(state).iterator();
                    }
                } else throw new NoSuchElementException();
                return result;
            }
        };
    }

    public Map<String, Float> getDistances(String start, String[] ends)
    {
        class NodeDistance
        {
            TreeNode node;
            float distance = Float.MAX_VALUE;
            
            NodeDistance(TreeNode node)
            {
                this.node = node;
            }
        }
        
        Map<TreeNode, NodeDistance> list = new HashMap<>();
        Map<String, NodeDistance> results = new HashMap<>();
        for(String end: ends) results.put(end, null);
        for(TreeNode subNode: this)
        {
            NodeDistance dist = new NodeDistance(subNode);
            if(subNode.getName() != null && results.containsKey(subNode.getName()))
            {
                results.put(subNode.getName(), dist);
            }
            if(subNode.getName() != null && subNode.getName().equals(start))
            {
                dist.distance = 0F;
            }
            list.put(subNode, dist);
        }
        boolean changed;
        while(true)
        {
            changed = false;
            for(NodeDistance dist: list.values())
            {
                if(dist.node.getParent() != null && list.get(dist.node.getParent()).distance + dist.node.getDistanceToParent() < dist.distance)
                {
                    dist.distance = list.get(dist.node.getParent()).distance + dist.node.getDistanceToParent();
                    changed = true;
                }
                for(TreeNode child: dist.node.getChildren())
                {
                    if(list.get(child).distance + child.getDistanceToParent() < dist.distance)
                    {
                        dist.distance = list.get(child).distance + child.getDistanceToParent();
                        changed = true;
                    }
                }
            }
            if(!changed) break;
        }
        return Maps.transformValues( results, v -> v.distance );
    }
}