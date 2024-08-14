package biouml.standard.filter;

import biouml.model.DiagramElement;

/**
 * Public interface that is used to specify what action should be applied to digarm element
 * if it satisfies to the filter condition.
 */
public interface Action
{
    /** Apllies the action to the specified diagram element. */
    public void apply(DiagramElement de);

    /** Should remove filter actions. */
    //public void cancel(DiagramElement de);
}


