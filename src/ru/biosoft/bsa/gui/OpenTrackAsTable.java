package ru.biosoft.bsa.gui;

import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.repository.AbstractElementAction;
import ru.biosoft.bsa.SqlTrack;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.access.SitesTableCollection;
import ru.biosoft.gui.GUI;
import ru.biosoft.table.document.TableDocument;

/**
 * @author lan
 */
@SuppressWarnings ( "serial" )
public class OpenTrackAsTable extends AbstractElementAction
{
    @Override
    protected void performAction(DataElement de) throws Exception
    {
        Track track = (Track)de;
        GUI.getManager().addDocument( new TableDocument( new SitesTableCollection( track, track.getAllSites() ) ) );
    }

    @Override
    protected boolean isApplicable(DataElement de)
    {
        return de instanceof SqlTrack;
    }

}
