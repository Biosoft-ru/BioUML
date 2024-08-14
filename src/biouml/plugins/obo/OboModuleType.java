package biouml.plugins.obo;

import java.io.File;
import java.util.Properties;

import com.developmentontheedge.application.ApplicationUtils;

import biouml.model.DiagramType;
import biouml.model.Module;
import biouml.model.ModuleType;
import biouml.plugins.obo.access.OboDataCollection;
import biouml.standard.type.Concept;
import biouml.standard.type.Reaction;
import biouml.standard.type.SemanticRelation;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.LocalRepository;
import ru.biosoft.access.Repository;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementSupport;

public class OboModuleType extends DataElementSupport implements ModuleType
{
    String FILE_PROPERTY = "file";

    public static final String VERSION = "OBO";
    protected String filename;

    public OboModuleType()
    {
        super("OBO", null);
    }

    public void setFileName(String filename)
    {
        this.filename = filename;
    }

    @Override
    @SuppressWarnings ( "unchecked" )
    public Class<? extends DiagramType>[] getDiagramTypes()
    {
        return new Class[] {OboDiagramType.class};
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
        if( Concept.class.isAssignableFrom(c) )
            return Module.DATA + "/terms";
        if( SemanticRelation.class.isAssignableFrom(c) )
            return Module.DATA + "/relations";
        if( Reaction.class.isAssignableFrom(c) )
            return null;

        throw new IllegalArgumentException("Unknown kernel class in Obo categoriser: " + c.getName());
    }

    @Override
    public boolean canCreateEmptyModule()
    {
        return true;
    }

    @Override
    public Module createModule(Repository parent, String name) throws Exception
    {
        String dataDirectory = ( (LocalRepository)CollectionFactoryUtils.getDatabases() ).getAbsolutePath() + System.getProperty("file.separator");
        File directory = new File(dataDirectory + name);
        if( directory.exists() || directory.mkdir() )
        {
            String newFilename = filename.substring(filename.lastIndexOf(System.getProperty("file.separator")) + 1);
            String newFilePath = dataDirectory + name + "/" + newFilename;
            ApplicationUtils.copyFile( new File(newFilePath), new File(filename) );
            filename = newFilename;
        }

        Properties primary = new Properties();
        primary.setProperty( FILE_PROPERTY, filename );

        primary.setProperty(DataCollectionConfigConstants.NAME_PROPERTY, name);
        primary.setProperty(DataCollectionConfigConstants.CLASS_PROPERTY, OboDataCollection.class.getName());
        primary.setProperty(DataCollectionConfigConstants.PLUGINS_PROPERTY, "biouml.plugins.obo");

        Properties transformed = new Properties();
        transformed.setProperty(DataCollectionConfigConstants.CLASS_PROPERTY, Module.class.getName());
        transformed.setProperty(Module.TYPE_PROPERTY, OboModuleType.class.getName());
        transformed.setProperty(DataCollectionConfigConstants.PLUGINS_PROPERTY, "biouml.plugins.obo");

        Module module = (Module)CollectionFactoryUtils.createDerivedCollection(parent, name, primary, transformed, name);

        return module;
    }
}
