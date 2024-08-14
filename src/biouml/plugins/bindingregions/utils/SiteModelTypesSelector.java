
package biouml.plugins.bindingregions.utils;

import ru.biosoft.workbench.editors.GenericMultiSelectEditor;

/**
 * @author yura
 *
 */
public class SiteModelTypesSelector extends GenericMultiSelectEditor
{
    @Override
    protected Object[] getAvailableValues()
    {
        return SiteModelsComparison.getSiteModelTypes();
    }
}
