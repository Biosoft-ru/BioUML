package biouml.plugins.ensembl.tracks;


import ru.biosoft.access.core.DataCollection;

/**
 * Gene track
 */
public class ExtendedGeneTrack extends GeneTrack
{
    public ExtendedGeneTrack(DataCollection<?> origin)
    {
        super("ExtendedGeneTrack", origin);
        viewBuilder = new GeneTrackViewBuilder();
    }
}
