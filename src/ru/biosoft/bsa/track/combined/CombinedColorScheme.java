package ru.biosoft.bsa.track.combined;

import java.awt.Graphics;

import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.view.SiteViewOptions;
import ru.biosoft.bsa.view.colorscheme.AbstractSiteColorScheme;
import ru.biosoft.bsa.view.colorscheme.BoxText;
import ru.biosoft.graphics.Brush;
import ru.biosoft.graphics.CompositeView;

public class CombinedColorScheme extends AbstractSiteColorScheme
{

    private DataElementPath trackPath;
    private CombinedItem[] trackColorItems = null;

    protected CombinedColorScheme()
    {
        super( "Combined" );
    }

    @Override
    public boolean isSuitable(Site site)
    {
        return true;
    }

    public Brush getBrush(Site site)
    {
        String tag = site == null ? null : ( (SiteViewOptions)getParent() ).getViewTagger().getTag( site );
        if( tag == null || tag.isEmpty() )
            tag = "(default)";
        try
        {
            CombinedItem[] items = getTrackColorItems();
            for( int i = 0; i < items.length; i++ )
            {
                if( items[i].getPath().toString().equals( tag ) )
                {
                    return new Brush( items[i].getColor() );
                }
            }
        }
        catch( Exception e )
        {
            //TODO: null checks
        }
        return defaultBrush;
    }

    @Override
    public CompositeView getLegend(Graphics graphics)
    {
        CompositeView legend = new CompositeView();
        CombinedItem[] items = getTrackColorItems();
        for( int i = 0; i < items.length; i++ )
        {
            legend.add( new BoxText( new Brush( items[i].getColor() ), items[i].getPath().toString(), graphics ), CompositeView.Y_BT );
        }
        return legend;
    }

    public void setTrackPath(DataElementPath trackPath)
    {
        this.trackPath = trackPath;
    }

    @PropertyName ( "Tracks and colors" )
    public CombinedItem[] getTrackColorItems()
    {
        if( trackPath != null )
        {
            CombinedTrack trDe = trackPath.getDataElement( CombinedTrack.class );
            if( trDe != null )
                return trDe.getTrackColorItems();
        }
        return new CombinedItem[0];
    }

    public void setTrackColorItems(CombinedItem[] items)
    {
        if( trackPath != null )
        {
            CombinedTrack trDe = trackPath.getDataElement( CombinedTrack.class );
            if( trDe != null )
                trDe.setTrackColorItems( items );
        }
    }

}
