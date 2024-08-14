
package ru.biosoft.bsa.view.colorscheme;

import java.awt.Graphics;
import java.util.Arrays;
import java.util.Comparator;
import java.util.StringTokenizer;

import ru.biosoft.graphics.Brush;
import ru.biosoft.graphics.CompositeView;

import com.developmentontheedge.beans.Option;

/**
 * Key color group - responding to tree structure of Key-Brush corresponding
 * of KeyBasedSiteColorScheme.
 */
public class KeyColorGroup extends Option
{
    private static final Comparator<KeyColorGroup> groupComparator = Comparator.comparing( KeyColorGroup::getName );

    ////////////////////////////////////////
    // Constructors
    //

    /**
     * Constructor
     * @param parentListener a parent listener
     * @param name a name of color scheme
     * @param brush a brush for this color scheme
     */
    public KeyColorGroup(Option parentListener, String name, Brush brush)
    {
        super(parentListener);
        this.name = name;
        this.brush = brush;
    }

    /**
     * Constructor
     * @param name a name of color scheme
     * @param brush a brush for this color scheme
     */
    public KeyColorGroup(String name, Brush brush)
    {
        super(null);
        this.name = name;
        this.brush = brush;
    }

    ////////////////////////////////////////
    // Properties
    //

    private final String name;

    /**
     * Get name of color scheme
     * @return name
     */
    public String getName()
    {
        return name;
    }

    private Brush brush;

    /**
     * Get color scheme brush
     * @return brush
     */
    public Brush getBrush()
    {
        return brush;
    }

    /**
     * Set brush for the group.
     * The brush will be applied for all childs.
     * @param brush a brush
     */
    public void setBrush(Brush brush)
    {
        Brush oldValue = this.brush;
        setBrushForAll(brush);
        firePropertyChange("brush", oldValue, brush);
    }

    private void setBrushForAll(Brush brush)
    {
        this.brush = brush;
        for( KeyColorGroup group : groups )
            group.setBrushForAll(brush);
    }

    ////////////////////////////////////////

    /**
     * Get color scheme view (this function is used for getLegend)
     * @param graphics a graphics
     * @return view
     */
    public CompositeView getView(Graphics graphics)
    {
        CompositeView legend = new CompositeView();

        if(!name.equals("default"))
        {
            legend.add(new BoxText(brush, name, graphics));
        }


        for( KeyColorGroup group : groups )
        {
            CompositeView view = group.getView(graphics);
            view.move(AbstractSiteColorScheme.INSETS);
            legend.add(view, CompositeView.Y_BT);
        }

        return legend;
    }

    ////////////////////////////////////////
    // Groups
    //

    private KeyColorGroup[] groups = new KeyColorGroup[0];

    /**
     * Get groups
     * @return groups
     */
    public KeyColorGroup[] getGroups()
    {
        return groups;
    }

    /**
     * Set groups
     * @param groups groups
     */
    public void setGroups(KeyColorGroup[] groups)
    {

        KeyColorGroup[] oldValue = this.groups;
        this.groups = groups;
        for( KeyColorGroup group : groups )
        {
            group.setParent(this);
        }
        firePropertyChange("groups", oldValue, groups);
    }

    /**
     * Get group at index i
     * @param i a index
     * @return group
     */
    public KeyColorGroup getGroups(int i)
    {
        return groups[i];
    }

    /**
     * Set group at index i
     * @param i a index
     * @param group a group
     */
    public void setGroups(int i, KeyColorGroup keyColorGroup)
    {
        KeyColorGroup oldValue = groups[i];
        groups[i] = keyColorGroup;
        groups[i].setParent(this);
        firePropertyChange("groups", oldValue, keyColorGroup);
    }

    /**
     * Gets goup recursively first token - is a name of
     * parent group and every next - children.
     */
    public KeyColorGroup getGroup(StringTokenizer tokens)
    {
        String key = tokens.nextToken();
        KeyColorGroup group = new KeyColorGroup(this, key, getBrush());
        int index = Arrays.binarySearch(groups, group, groupComparator);
        if (index < 0)
            return this;

        group = groups[index];
        return tokens.hasMoreTokens() ? group.getGroup(tokens) : group;
    }

    /**
     * Gets goup recursively first token - is a name of
     * parent group and every next - children.
     * if group is not exists, then this function creates it.
     */
    public KeyColorGroup provideGroup(StringTokenizer tokens)
    {
        String key = tokens.nextToken();
        KeyColorGroup group = new KeyColorGroup(this, key, getBrush());
        int index = Arrays.binarySearch(groups, group, groupComparator);
        if (index >= 0)
            group = groups[index];
        else
             add(-1 - index, group);

        return tokens.hasMoreTokens() ? group.provideGroup(tokens) : group;
    }

    /**
     * Add new group to index <CODE>index</CODE>.
     * @param index a index
     * @param group a group
     */
    public void add(int index, KeyColorGroup group)
    {
        KeyColorGroup[] newGroups = new KeyColorGroup[groups.length + 1];
        System.arraycopy(groups, 0, newGroups, 0, index);
        newGroups[index] = group;
        if (groups.length > index)
            System.arraycopy(groups, index, newGroups, index + 1, groups.length - index);
        groups = newGroups;
    }

    /** Method used for setting names of the color groups */
    public String calcKeyColorGroupName(Integer index, Object obj)
    {
        if (obj instanceof KeyColorGroup)
        {
            return ((KeyColorGroup)obj).getName();
        }
        else
            return "<" + index + ">";
    }

    public boolean isGroupArrayEmpty()
    {
        return !(groups != null && groups.length > 0);
    }
}
