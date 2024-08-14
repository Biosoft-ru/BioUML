package ru.biosoft.access.biohub;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import ru.biosoft.access.core.DataElementPath;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.DynamicPropertySetAsMap;

/**
 * Base class for searched element
 */
public class Element implements Comparable<Element>
{
    public static final String USER_TITLE_PROPERTY = "userTitle";

    public static final String USER_REACTANTS_PROPERTY = "userReactionReactants";
    public static final String USER_PRODUCTS_PROPERTY = "userReactionProducts";

    protected String path;
    protected String accession = null;
    protected String relationType;
    protected String linkedFromPath;
    protected float linkedLength;
    protected String linkedPath;
    protected int linkedDirection;
    protected DynamicPropertySet properties;

    public Element(String path)
    {
        this.path = path;
    }

    public Element(DataElementPath path)
    {
        this.path = path.toString();
    }

    @Override
    public String toString()
    {
        return this.path;
    }

    @Override
    /**
     * Check equivalent element
     */
    public boolean equals(Object o)
    {
        if( o == null || ! ( o instanceof Element ) )
            return false;

        return path.equals( ( (Element)o ).getPath());
    }

    @Override
    public int hashCode()
    {
        return path.hashCode();
    }

    //
    //Getters and setters
    //

    public String getPath()
    {
        return path;
    }

    public DataElementPath getElementPath()
    {
        return DataElementPath.create(path);
    }

    public String getRelationType()
    {
        return relationType;
    }

    public void setRelationType(String relationType)
    {
        this.relationType = relationType;
    }

    public String getLinkedFromPath()
    {
        return linkedFromPath;
    }

    public void setLinkedFromPath(String linkedFromPath)
    {
        this.linkedFromPath = linkedFromPath;
    }

    public float getLinkedLength()
    {
        return linkedLength;
    }

    public void setLinkedLength(float linkedLength)
    {
        this.linkedLength = linkedLength;
    }

    public String getLinkedPath()
    {
        return linkedPath;
    }

    public void setLinkedPath(String linkedPath)
    {
        this.linkedPath = linkedPath;
    }

    public int getLinkedDirection()
    {
        return linkedDirection;
    }

    public void setLinkedDirection(int linkedDirection)
    {
        this.linkedDirection = linkedDirection;
    }

    public String getAccession()
    {
        if(path == null) return null;
        if(accession == null)
        {
            accession = DataElementPath.create(path).getName();
        }
        return accession;
    }

    /**
     * Get property value associated with current element and given key
     */
    public Object getValue(String key)
    {
        if(properties == null) return null;
        DynamicProperty property = properties.getProperty(key);
        return property == null?null:property.getValue();
    }

    /**
     * Set property value associated with current element and given key
     */
    public void setValue(String key, Object value)
    {
        if(properties == null) properties = new DynamicPropertySetAsMap();
        if(properties.getProperty(key) != null)
            properties.remove(key);

        properties.add(new DynamicProperty(key, value==null?String.class:value.getClass(), value));
    }

    public void setValue(DynamicProperty property)
    {
        if(properties == null) properties = new DynamicPropertySetAsMap();
        if(properties.getProperty(property.getName()) != null)
            properties.remove(property.getName());
        properties.add(property);
    }

    /**
     * Get list of all property keys currently associated with current element
     */
    public String[] getKeys()
    {
        if(properties == null) return new String[]{};
        List<String> names = new ArrayList<>();
        Iterator<String> iterator = properties.nameIterator();
        while(iterator.hasNext())
            names.add(iterator.next());
        return names.toArray(new String[names.size()]);
    }

    @Override
    public int compareTo(Element e)
    {
        return path.compareTo(e.path);
    }
}
