package biouml.plugins.biopax;

import java.io.File;
import java.util.Properties;

import com.developmentontheedge.application.ApplicationUtils;

import biouml.model.DiagramType;
import biouml.model.Module;
import biouml.model.ModuleType;
import biouml.plugins.biopax.access.BioPaxOwlDataCollection;
import biouml.plugins.biopax.model.BioSource;
import biouml.plugins.biopax.model.OpenControlledVocabulary;
import biouml.plugins.sbgn.SbgnDiagramType;
import biouml.standard.type.Complex;
import biouml.standard.type.Concept;
import biouml.standard.type.DNA;
import biouml.standard.type.DatabaseInfo;
import biouml.standard.type.DiagramInfo;
import biouml.standard.type.Protein;
import biouml.standard.type.Publication;
import biouml.standard.type.RNA;
import biouml.standard.type.Reaction;
import biouml.standard.type.SemanticRelation;
import biouml.standard.type.SpecieReference;
import biouml.standard.type.Substance;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.LocalRepository;
import ru.biosoft.access.Repository;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementSupport;

public class BioPAXModuleType extends DataElementSupport implements ModuleType
{
    public static final String VERSION = "BioPAX";
    protected String[] filenames;

    public BioPAXModuleType()
    {
        super("BioPAX", null);
    }

    public void setFileNames(String[] filenames)
    {
        this.filenames = filenames;
    }

    @Override
    @SuppressWarnings ( "unchecked" )
    public Class<? extends DiagramType>[] getDiagramTypes()
    {
        return new Class[] {SbgnDiagramType.class};
    }
    
    @Override
    public String[] getXmlDiagramTypes()
    {
        return null;
    }

    @Override
    public String getVersion()
    {
        return VERSION;
    }

    @Override
    public boolean isCategorySupported()
    {
        return true;
    }

    @Override
    public String getCategory(Class<? extends DataElement> c)
    {
        if( Complex.class.isAssignableFrom(c) )
            return Module.DATA + "/Complex";
        if( SemanticRelation.class.isAssignableFrom(c) )
            return Module.DATA + "/Controls";
        if( Reaction.class.isAssignableFrom(c) )
            return Module.DATA + "/Conversion";
        if( DNA.class.isAssignableFrom(c) )
            return Module.DATA + "/DNA";
        if( Protein.class.isAssignableFrom(c) )
            return Module.DATA + "/Protein";
        if( RNA.class.isAssignableFrom(c) )
            return Module.DATA + "/RNA";
        if( Substance.class.isAssignableFrom(c) )
            return Module.DATA + "/SmallMolecule";
        if( SpecieReference.class.isAssignableFrom(c) )
            return Module.DATA + "/Participants";
        if( DiagramInfo.class.isAssignableFrom(c) )
            return Module.DATA + "/Pathway";
        if( Publication.class.isAssignableFrom(c) )
            return Module.DATA + "/Publications";
        if( DatabaseInfo.class.isAssignableFrom(c) )
            return Module.DATA + "/DataSources";
        if( OpenControlledVocabulary.class.isAssignableFrom(c) )
            return Module.DATA + "/CellularLocations";
        if( BioSource.class.isAssignableFrom(c) )
            return Module.DATA + "/Organisms";
        if( Concept.class.isAssignableFrom(c) )
            return Module.DATA + "/PhysicalEntities";

        throw new IllegalArgumentException("Unknown kernel class in BioPAX categoriser: " + c.getName());
    }

    @Override
    public boolean canCreateEmptyModule()
    {
        return true;
    }

    @Override
    public Module createModule(Repository parent, String name) throws Exception
    {
        String dataDirectory = ((LocalRepository)CollectionFactoryUtils.getDatabases()).getAbsolutePath()+System.getProperty("file.separator");
        File directory = new File(dataDirectory + name);
        if( directory.exists() || directory.mkdir() )
        {
            String newFilename = filenames[0].substring(filenames[0].lastIndexOf(System.getProperty("file.separator")) + 1);
            String newFilePath = dataDirectory + name + "/" + newFilename;
            ApplicationUtils.copyFile( new File(newFilePath), new File(filenames[0]) );
            filenames[0] = newFilename;
        }

        Properties primary = new Properties();
        primary.setProperty( DataCollectionConfigConstants.FILE_PROPERTY, filenames[0] );

        primary.setProperty(DataCollectionConfigConstants.NAME_PROPERTY, name);
        primary.setProperty(DataCollectionConfigConstants.CLASS_PROPERTY, BioPaxOwlDataCollection.class.getName());
        primary.setProperty(DataCollectionConfigConstants.PLUGINS_PROPERTY, "biouml.plugins.biopax");

        Properties transformed = new Properties();
        transformed.setProperty(DataCollectionConfigConstants.CLASS_PROPERTY, Module.class.getName());
        transformed.setProperty(Module.TYPE_PROPERTY, BioPAXModuleType.class.getName());
        transformed.setProperty(DataCollectionConfigConstants.PLUGINS_PROPERTY, "biouml.plugins.biopax");

        Module module = (Module)CollectionFactoryUtils.createDerivedCollection(parent, name, primary, transformed, name);

        return module;
    }
}
