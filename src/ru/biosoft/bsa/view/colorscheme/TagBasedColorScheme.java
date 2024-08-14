package ru.biosoft.bsa.view.colorscheme;

import java.awt.Graphics;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.json.JSONObject;

import com.developmentontheedge.beans.Option;

import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.view.ViewTagger;
import ru.biosoft.graphics.Brush;
import ru.biosoft.graphics.CompositeView;

/**
 * Decorate standard color scheme with the special colors for tags
 */
public class TagBasedColorScheme extends AbstractSiteColorScheme
{
    protected SiteColorScheme baseColorScheme;
    protected ViewTagger tagger;
    protected Map<String, Brush> tagColors;

    public TagBasedColorScheme(SiteColorScheme baseColorScheme, ViewTagger tagger, Map<String, Brush> tagColors)
    {
        super( ( (Option)baseColorScheme ).getParent(), baseColorScheme.getName());
        this.baseColorScheme = baseColorScheme;
        this.tagger = tagger;
        this.tagColors = tagColors;
    }

    public TagBasedColorScheme(SiteColorScheme baseColorScheme, ViewTagger tagger, String tagColorsStr)
    {
        super( ( (Option)baseColorScheme ).getParent(), baseColorScheme.getName());
        this.baseColorScheme = baseColorScheme;
        this.tagger = tagger;
        tagColors = new HashMap<>();
        try
        {
            JSONObject colors = new JSONObject(tagColorsStr);
            Iterator<String> iter = colors.keys();
            while( iter.hasNext() )
            {
                String tag = iter.next();
                tagColors.put(tag, new Brush(colors.getJSONObject(tag)));
            }
        }
        catch( Exception e )
        {

        }
    }

    @Override
    public boolean isSuitable(Site site)
    {
        return baseColorScheme.isSuitable(site);
    }

    @Override
    public Brush getBrush(Site site)
    {
        if( tagger != null )
        {
            String tag = tagger.getTag(site);
            if( tag != null && tagColors.containsKey(tag) )
            {
                return tagColors.get(tag);
            }
        }
        return baseColorScheme.getBrush(site);
    }

    @Override
    public CompositeView getLegend(Graphics graphics)
    {
        return baseColorScheme.getLegend(graphics);
    }
}
