package biouml.plugins.optimization.document;

import java.util.EventListener;
import java.util.EventObject;

public interface OptimizationDocumentListener extends EventListener
{
    void valueChanged(EventObject e);
}
