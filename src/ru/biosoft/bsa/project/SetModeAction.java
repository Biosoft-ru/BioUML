package ru.biosoft.bsa.project;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import ru.biosoft.bsa.project.ProjectDocument.ViewOptionsMode;

@SuppressWarnings ( "serial" )
public class SetModeAction extends AbstractAction
{
    public static final String KEY_OVERVIEW = "overview option mode";
    public static final String KEY_DEFAULT = "default option mode";
    public static final String KEY_DETAILED = "detailed option mode";
    public static final String DOCUMENT = "document";

    protected ViewOptionsMode mode;

    public SetModeAction(String name, ViewOptionsMode mode)
    {
        super(name);
        this.mode = mode;
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        ProjectDocument document = (ProjectDocument)getValue(DOCUMENT);
        Region region = document.getProject().getRegions()[0];
        int length = document.getExtent(mode);
        region.setInterval(region.getInterval().zoomToLength(length).fit(region.getSequence().getInterval()));
    }
}
