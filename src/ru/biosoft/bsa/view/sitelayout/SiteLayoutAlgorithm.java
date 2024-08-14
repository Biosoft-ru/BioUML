package ru.biosoft.bsa.view.sitelayout;

import ru.biosoft.bsa.view.TrackView;

public interface SiteLayoutAlgorithm
{
    public static final int TOP    = -1;
    public static final int BOTTOM = 1;

    public void layout(TrackView view, int direction );
}
