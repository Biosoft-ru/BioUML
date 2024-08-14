package ru.biosoft.bsa;

import ru.biosoft.bsa.view.ViewTagger;

/**
 * @author lan
 *
 */
public class DefaultSiteViewTagger implements ViewTagger
{
    @Override
    public String getTag(Object model)
    {
        if(!(model instanceof Site)) return null;
        Site site = (Site)model;
        Object siteModelObj = site.getProperties().getValue("siteModel");
        if(siteModelObj instanceof SiteModel) return ((SiteModel)siteModelObj).getName();
        return site.getType();
    }
}
