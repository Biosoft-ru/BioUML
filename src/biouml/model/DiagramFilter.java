package biouml.model;

import java.awt.Component;

/**
 * Filter cab be applied to any diagram view.
 * Generally it should highlight (background node view can be used for this purpose)
 * or hide nodes according to some criteria.
 */
public interface DiagramFilter extends Cloneable
{
    /** Indicates whether a filter should be used. */
    public boolean isEnabled();

    /**
     * Hides or highlights diagram elements according to filter settings.
     * Prerequisite: diagram view should be generated by DiagramViewBuilder.
     */
    public void apply(Compartment diagram);

    /** Restores original diagram view. */
    public void restoreView(Compartment diagram);

    /** returns visual control to set up the filter options. */
    public Component getOptionsControl();
    
    /** Set target diagram. */
    public void setDiagram(Diagram diagram);
    
    /** Get filter properties bean. */
    public Object getProperties();
    
    /** Get filter name to display for user. */
    public String getName();
    
    /** Clone diagram filter */
    public DiagramFilter clone();

    /** Set loading flag for deserialization*/
    public void setLoading(boolean loading);
}
