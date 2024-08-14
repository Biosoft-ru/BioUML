package ru.biosoft.bsa;

import ru.biosoft.bsa.view.ViewTagger;

/**
 * @author lan
 *
 */
public class SitePropertyViewTagger implements ViewTagger
{
    private String tagProperty;

    public SitePropertyViewTagger(String tagProperty)
    {
        this.tagProperty = tagProperty;
    }
    public void setTagProperty(String tagProperty)
    {
        this.tagProperty = tagProperty;
    }

    @Override
    public String getTag(Object model)
    {
        if(!(model instanceof Site)) return null;
        if(tagProperty == null) return null;
        Site site = (Site)model;
        Object siteProperty = site.getProperties().getValue( tagProperty );
        if( siteProperty != null )
            return siteProperty.toString();
        return null;
    }
}
