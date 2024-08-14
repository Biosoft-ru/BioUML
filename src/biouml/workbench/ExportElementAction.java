package biouml.workbench;

import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.DataElementExporterRegistry;
import ru.biosoft.access.repository.AbstractElementAction;

import com.developmentontheedge.application.Application;

public class ExportElementAction extends AbstractElementAction
{
    @Override
    protected void performAction(DataElement de) throws Exception
    {
        ExportElementDialog dialog = new ExportElementDialog(Application.getApplicationFrame(), de);
        dialog.doModal();
    }

    @Override
    protected boolean isApplicable(DataElement de)
    {
        return DataElementExporterRegistry.hasExporter(de);
    }
}
