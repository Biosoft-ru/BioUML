package ru.biosoft.bsa.access;

import java.util.Properties;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.exception.LoggedException;
import ru.biosoft.bsa.TrackUtils;
import biouml.standard.type.Species;

public class SequencesDatabaseInfo
{
    public static final SequencesDatabaseInfo CUSTOM_SEQUENCES = new SequencesDatabaseInfo("Custom...");
    public static final SequencesDatabaseInfo NULL_SEQUENCES = new SequencesDatabaseInfo("(none)");

    private String name;
    private DataElementPath basePath;
    private DataElementPath chromosomePath;
    private String genomeBuild;
    private String version;
    
    public SequencesDatabaseInfo(DataElementPath basePath) throws LoggedException
    {
        DataCollection<DataElement> db = basePath.getDataCollection();
        Properties properties = db.getInfo().getProperties();
        chromosomePath = TrackUtils.getPrimarySequencesPath(basePath);
        this.basePath = basePath;

        String databaseProp = properties.getProperty( "database", basePath.getName() );
	version = properties.getProperty("version");
        String versionProp = properties.getProperty( "version", "" );
        String speciesProp = properties.containsKey( "species" )
                ? " " + Species.getSpecies( properties.getProperty( "species" ) ).getCommonName() : "";
        genomeBuild = properties.getProperty( "genomeBuild" );
        String genomeBuildProp = properties.containsKey( "genomeBuild" ) ? " (" + genomeBuild + ")" : "";
        
        this.name = databaseProp + " " + versionProp + speciesProp + genomeBuildProp;
    }
    
    private SequencesDatabaseInfo(String name)
    {
        this.name = name;
        this.basePath = DataElementPath.EMPTY_PATH;
        this.chromosomePath = DataElementPath.EMPTY_PATH;
    }

    public String getName()
    {
        return name;
    }
    public String getVersion()
    {
	if(version == null)
            return "";
        return version;
    }
    public DataElementPath getBasePath()
    {
        return basePath;
    }

    public DataElementPath getChromosomePath()
    {
        return chromosomePath;
    }
    
    public String getGenomeBuild()
    {
        return genomeBuild;
    }
    
    @Override
    public String toString()
    {
        return getName();
    }
    
    public static SequencesDatabaseInfo createInstance(String from)
    {
        return SequencesDatabaseInfoSelector.getDatabaseInfo(from);
    }
}
