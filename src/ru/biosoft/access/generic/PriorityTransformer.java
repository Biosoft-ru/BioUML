package ru.biosoft.access.generic;

import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.Transformer;

/**
 * Adds getPriority functionality to {@link Transformer} implementations
 */
public interface PriorityTransformer
{
    public int getInputPriority(Class<? extends DataElement> inputClass, DataElement output);
    public int getOutputPriority(String name);
}
