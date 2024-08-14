package ru.biosoft.bsa.project;

import java.awt.event.ActionEvent;

@SuppressWarnings ( "serial" )
public class SemanticZoomInAction extends AbstractProjectAction
{
    public static final String KEY = "semantic zoom in";

    @Override
    public void actionPerformed(ActionEvent e)
    {
        Region region = getProject().getRegions()[0];
        region.setInterval(region.getInterval().zoom(0.5));
    }
}
