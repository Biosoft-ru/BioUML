package biouml.standard.diagram;

import ru.biosoft.access.core.DataCollection;

public interface ElementChooserPaneListener
{
    public void moduleChanged(String moduleName, DataCollection targetDC);
}
