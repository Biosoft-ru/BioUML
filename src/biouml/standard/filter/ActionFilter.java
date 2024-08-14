package biouml.standard.filter;

import biouml.model.DiagramElement;

/**
 * This filter generates some action (currently hide or highlight) if the processed node
 * satisfies to the filter condition.
 */
public interface ActionFilter
{
    /** Indicates whether a filter should be used. */
    public boolean isEnabled();

    /**
     * Returns action, that should be applied to the specified diagram element,
     * if it satisfies to the filter condition or null otherwise.
     */
    public Action getAction(DiagramElement de);
}


