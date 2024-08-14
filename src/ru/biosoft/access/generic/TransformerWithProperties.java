package ru.biosoft.access.generic;

import java.util.Properties;

import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.Transformer;

/**
 * Transformers may implement this interface to return additional properties of ru.biosoft.access.core.DataElement which must be stored
 * in collection descriptor.
 * @author lan
 */
public interface TransformerWithProperties<I extends DataElement, O extends DataElement> extends Transformer<I, O>
{
    public Properties getProperties(O outputElement);
}
