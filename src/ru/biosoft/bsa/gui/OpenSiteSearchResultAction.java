package ru.biosoft.bsa.gui;

import ru.biosoft.access.core.DataElement;
import ru.biosoft.bsa.analysis.SiteSearchResult;
import ru.biosoft.table.TableDataCollection;
import biouml.workbench.OpenDocumentAction;

/**
 * @author lan
 *
 */
@SuppressWarnings ( "serial" )
public class OpenSiteSearchResultAction extends OpenDocumentAction
{
    private static DataElement getActualElement(DataElement de)
    {
        if(!(de instanceof SiteSearchResult)) return null;
        try
        {
            de = ((SiteSearchResult)de).get(SiteSearchResult.SUMMARY);
            if(de instanceof TableDataCollection) return de;
        }
        catch( Exception e )
        {
        }
        return null;
    }

    @Override
    protected void performAction(DataElement de) throws Exception
    {
        super.performAction(getActualElement(de));
    }

    @Override
    protected boolean isApplicable(DataElement de)
    {
        return getActualElement(de) != null;
    }
}
