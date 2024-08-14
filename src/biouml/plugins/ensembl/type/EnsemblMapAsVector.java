package biouml.plugins.ensembl.type;

import java.util.Properties;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.bsa.MapAsVector;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.project.Project;
import ru.biosoft.bsa.project.ProjectAsLists;
import ru.biosoft.bsa.project.Region;
import ru.biosoft.bsa.project.TrackInfo;

/**
 * @author lan
 *
 */
public class EnsemblMapAsVector extends MapAsVector
{
    public EnsemblMapAsVector(String name, DataCollection<?> parent, EnsemblSequence sequence, Properties properties)
    {
        super(name, parent, sequence, properties);
    }
    
    public int getLength()
    {
        return sequence.getLength();
    }
    
    public Project getKaryotype()
    {
        Project result = new ProjectAsLists(getName(), null);
        result.addTrack(new TrackInfo(DataElementPath.create(getOrigin()).getRelativePath("../../Tracks/Karyotype").getDataElement(Track.class)));
        result.addRegion(new Region(this));
        return result;
    }
}
