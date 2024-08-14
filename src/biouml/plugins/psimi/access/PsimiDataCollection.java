package biouml.plugins.psimi.access;

import java.io.File;
import java.io.FileInputStream;
import java.util.logging.Level;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import javax.annotation.Nonnull;

import java.util.logging.Logger;

import biouml.model.Module;
import biouml.model.util.UniversalXmlTransformer;
import biouml.plugins.psimi.PsimiModelConstants;
import biouml.plugins.psimi.PsimiModelReader;
import biouml.plugins.psimi.model.Experiment;
import biouml.plugins.psimi.model.Interaction;
import biouml.plugins.psimi.model.Interactor;
import biouml.standard.type.Concept;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.VectorDataCollection;


public class PsimiDataCollection extends VectorDataCollection<DataCollection<?>>
{
    protected static final Logger log = Logger.getLogger(PsimiDataCollection.class.getName());

    protected String filepath;

    protected DataCollection<DataCollection<?>> data;

    protected boolean isInit = false;

    protected IndexedDataCollection<Concept> availabilitiesDC;
    protected IndexedDataCollection<Experiment> experimentsDC;
    protected IndexedDataCollection<Interactor> interactorsDC;
    protected IndexedDataCollection<Interaction> interactionsDC;
    protected IndexedDataCollection<Concept> sourcesDC;

    public static final String TEXT_TRANSFORMER_NAME = "transformer.text";

    public PsimiDataCollection(DataCollection<?> parent, Properties properties) throws Exception
    {
        super(parent, properties);
        filepath = properties.getProperty(DataCollectionConfigConstants.FILE_PATH_PROPERTY);
    }

    /**
     * Get the {@link edu.caltech.sbw.Module} instance from SBW broker
     * and load its services.
     */
    public void init()
    {
        if( isInit )
            return;

        isInit = true;

        this.setNotificationEnabled(false);
        try
        {
            Properties props = new Properties();
            props.put(DataCollectionConfigConstants.DATA_ELEMENT_CLASS_PROPERTY, DataCollection.class.getName());
            data = new VectorDataCollection<>(Module.DATA, this, props);

            doPut(data, true);

            Properties props1 = new Properties();
            props1.put(DataCollectionConfigConstants.DATA_ELEMENT_CLASS_PROPERTY, Concept.class.getName());
            props1.put(TEXT_TRANSFORMER_NAME, UniversalXmlTransformer.class.getName());
            props1.put(DataCollectionConfigConstants.PLUGINS_PROPERTY, "biouml.plugins.psimi");
            availabilitiesDC = new IndexedDataCollection<>(PsimiModelConstants.AVAILABILITIES, Concept.class, data, props1, new File(filepath
                    + File.separator + PsimiModelConstants.AVAILABILITIES));
            Properties props2 = new Properties();
            props2.put(DataCollectionConfigConstants.DATA_ELEMENT_CLASS_PROPERTY, Experiment.class.getName());
            props2.put(TEXT_TRANSFORMER_NAME, UniversalXmlTransformer.class.getName());
            props2.put(DataCollectionConfigConstants.PLUGINS_PROPERTY, "biouml.plugins.psimi");
            experimentsDC = new IndexedDataCollection<>(PsimiModelConstants.EXPERIMENTS, Experiment.class, data, props2, new File(filepath
                    + File.separator + PsimiModelConstants.EXPERIMENTS));
            Properties props3 = new Properties();
            props3.put(DataCollectionConfigConstants.DATA_ELEMENT_CLASS_PROPERTY, Interactor.class.getName());
            props3.put(TEXT_TRANSFORMER_NAME, UniversalXmlTransformer.class.getName());
            props3.put(DataCollectionConfigConstants.PLUGINS_PROPERTY, "biouml.plugins.psimi");
            interactorsDC = new IndexedDataCollection<>(PsimiModelConstants.INTERACTORS, Interactor.class, data, props3, new File(filepath
                    + File.separator + PsimiModelConstants.INTERACTORS));
            Properties props4 = new Properties();
            props4.put(DataCollectionConfigConstants.DATA_ELEMENT_CLASS_PROPERTY, Interaction.class.getName());
            props4.put(TEXT_TRANSFORMER_NAME, UniversalXmlTransformer.class.getName());
            props4.put(DataCollectionConfigConstants.PLUGINS_PROPERTY, "biouml.plugins.psimi");
            interactionsDC = new IndexedDataCollection<>(PsimiModelConstants.INTERACTIONS, Interaction.class, data, props4, new File(filepath
                    + File.separator + PsimiModelConstants.INTERACTIONS));
            Properties props5 = new Properties();
            props5.put(DataCollectionConfigConstants.DATA_ELEMENT_CLASS_PROPERTY, Concept.class.getName());
            props5.put(TEXT_TRANSFORMER_NAME, UniversalXmlTransformer.class.getName());
            props5.put(DataCollectionConfigConstants.PLUGINS_PROPERTY, "biouml.plugins.psimi");
            sourcesDC = new IndexedDataCollection<>(PsimiModelConstants.SOURCES, Concept.class, data, props5, new File(filepath
                    + File.separator + PsimiModelConstants.SOURCES));

            data.put(availabilitiesDC);
            data.put(experimentsDC);
            data.put(interactorsDC);
            data.put(interactionsDC);
            data.put(sourcesDC);

            if( ! ( availabilitiesDC.hasIndex() && experimentsDC.hasIndex() && interactorsDC.hasIndex() && availabilitiesDC.hasIndex() && sourcesDC
                    .hasIndex() ) )
            {
                File root = new File(filepath);
                processDirectory(root);
            }

            availabilitiesDC.saveIndexes();
            experimentsDC.saveIndexes();
            interactorsDC.saveIndexes();
            interactionsDC.saveIndexes();
            sourcesDC.saveIndexes();
        }
        catch( Throwable t )
        {
            log.log(Level.SEVERE, "Init exception", t);
        }

        this.setNotificationEnabled(true);
    }
    protected void processDirectory(File file) throws Exception
    {
        if( file.isDirectory() )
        {
            for( File childFile : file.listFiles() )
            {
                processDirectory(childFile);
            }
        }
        else
        {
            if( file.getName().endsWith(".xml") )
            {
                processFile(file);
            }
        }
    }

    public void processFile(File file) throws Exception
    {
        String collectionName = file.getName().replace('\\', '/');
        int ind = collectionName.lastIndexOf('/');
        if( ind != -1 )
            collectionName = collectionName.substring(ind);
        int lastSymbol = collectionName.indexOf(".xml");
        if( lastSymbol != -1 )
        {
            collectionName = collectionName.substring(0, lastSymbol);
        }

        FileInputStream fis = null;
        try
        {
            fis = new FileInputStream(file);
            PsimiModelReader reader = new PsimiModelReader(collectionName, fis);

            availabilitiesDC.setCurrentFileName(file.getPath());
            experimentsDC.setCurrentFileName(file.getPath());
            interactorsDC.setCurrentFileName(file.getPath());
            interactionsDC.setCurrentFileName(file.getPath());
            sourcesDC.setCurrentFileName(file.getPath());

            reader.read(data);

            availabilitiesDC.setCurrentFileName(null);
            experimentsDC.setCurrentFileName(null);
            interactorsDC.setCurrentFileName(null);
            interactionsDC.setCurrentFileName(null);
            sourcesDC.setCurrentFileName(null);
        }
        catch( Throwable t )
        {
            log.log(Level.SEVERE, "parse file error: ", t);
        }
        finally
        {
            try
            {
                if( fis != null )
                    fis.close();
            }
            catch( Exception e )
            {
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    // redefine DataCollection methods due to Module instance lazy initialisation
    //

    @Override
    public boolean isMutable()
    {
        return false;
    }

    @Override
    public @Nonnull Class<? extends DataCollection> getDataElementType()
    {
        return ru.biosoft.access.core.DataCollection.class;
    }

    @Override
    public int getSize()
    {
        init();
        return super.getSize();
    }

    @Override
    protected DataCollection<?> doGet(String name)
    {
        init();
        return super.doGet(name);
    }

    @Override
    public @Nonnull Iterator<DataCollection<?>> iterator()
    {
        init();
        return super.iterator();
    }

    @Override
    public @Nonnull List<String> getNameList()
    {
        init();
        return super.getNameList();
    }
}
