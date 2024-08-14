package biouml.plugins.biopax.access;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import org.semanticweb.owl.model.OWLDataFactory;
import org.semanticweb.owl.model.OWLOntology;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.VectorDataCollection;
import ru.biosoft.exception.ExceptionRegistry;
import biouml.model.Diagram;
import biouml.model.Module;
import biouml.plugins.biopax.BioPAXQuerySystem;
import biouml.plugins.biopax.BioPAXSupport;
import biouml.plugins.biopax.biohub.BioHubBuilder;
import biouml.plugins.biopax.model.BioSource;
import biouml.plugins.biopax.model.OpenControlledVocabulary;
import biouml.plugins.biopax.reader.BioPAXReader;
import biouml.plugins.biopax.reader.BioPAXReaderFactory;
import biouml.standard.type.Complex;
import biouml.standard.type.Concept;
import biouml.standard.type.DNA;
import biouml.standard.type.DatabaseInfo;
import biouml.standard.type.Protein;
import biouml.standard.type.Publication;
import biouml.standard.type.RNA;
import biouml.standard.type.Reaction;
import biouml.standard.type.SemanticRelation;
import biouml.standard.type.SpecieReference;
import biouml.standard.type.Substance;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.ApplicationFrame;
import com.developmentontheedge.application.ApplicationStatusBar;
import ru.biosoft.jobcontrol.FunctionJobControl;
import ru.biosoft.jobcontrol.JobControlException;
import ru.biosoft.jobcontrol.StackProgressJobControl;

public class BioPaxOwlDataCollection extends VectorDataCollection<DataCollection<?>>
{
    protected OWLDataFactory factory;
    protected OWLOntology ontology;
    protected String biopaxPrefix;
    protected File file;
    protected DataCollection<DataCollection> data;
    protected DataCollection<Diagram> diagrams;
    protected DataCollection<DataCollection> dictionaries;

    protected DataCollection<Concept> physicalEntities;
    protected DataCollection<Complex> complexes;
    protected DataCollection<Protein> proteins;
    protected DataCollection<RNA> rnas;
    protected DataCollection<DNA> dnas;
    protected DataCollection<Substance> smallMolecules;
    protected DataCollection<Reaction> conversions;
    protected DataCollection<SemanticRelation> controls;
    protected DataCollection<SpecieReference> participants;
    protected DataCollection<Publication> publications;
    protected DataCollection<OpenControlledVocabulary> vocabulary;
    protected DataCollection<DatabaseInfo> dataSources;
    protected DataCollection<BioSource> organisms;

    protected FunctionJobControl jobControl;

    protected int filesCount = 1;
    protected int currentFileNumber = 1;
    protected String prefix = "";

    protected boolean isInit = false;
    protected boolean initAccess = true;

    protected BioHubBuilder bioHubBuilder;

    public BioPaxOwlDataCollection(DataCollection<?> parent, Properties properties) throws Exception
    {
        super(parent, properties);
        String fileName = properties.getProperty(DataCollectionConfigConstants.FILE_PROPERTY);
        if( fileName.indexOf(':') == -1 && properties.containsKey(DataCollectionConfigConstants.FILE_PATH_PROPERTY) )
            this.file = new File(properties.getProperty(DataCollectionConfigConstants.FILE_PATH_PROPERTY), fileName);
        else
            this.file = new File(fileName);
    }

    /**
     * Get the {@link edu.caltech.sbw.Module} instance from SBW broker
     * and load its services.
     */
    public void initCollection(FunctionJobControl fjc)
    {
        if( isInit || !initAccess )
            return;

        isInit = true;

        this.setNotificationEnabled(false);
        try
        {
            Properties props = new Properties();
            props.put(DataCollectionConfigConstants.DATA_ELEMENT_CLASS_PROPERTY, DataCollection.class.getName());
            data = new VectorDataCollection<>(Module.DATA, this, props);
            diagrams = new VectorDataCollection<>(Module.DIAGRAM, this, props);
            dictionaries = new VectorDataCollection<>(Module.METADATA, this, props);
            dictionaries.getInfo().setQuerySystem(new BioPAXQuerySystem(dictionaries));
            physicalEntities = new VectorDataCollection<>(BioPAXSupport.PHYSICAL_ENTITY, data, null);
            physicalEntities.getInfo().setQuerySystem(new BioPAXQuerySystem(physicalEntities));
            complexes = new VectorDataCollection<>(BioPAXSupport.COMPLEX, data, null);
            complexes.getInfo().setQuerySystem(new BioPAXQuerySystem(complexes));
            proteins = new VectorDataCollection<>(BioPAXSupport.PROTEIN, data, null);
            proteins.getInfo().setQuerySystem(new BioPAXQuerySystem(proteins));
            rnas = new VectorDataCollection<>(BioPAXSupport.RNA, data, null);
            rnas.getInfo().setQuerySystem(new BioPAXQuerySystem(rnas));
            dnas = new VectorDataCollection<>(BioPAXSupport.DNA, data, null);
            dnas.getInfo().setQuerySystem(new BioPAXQuerySystem(dnas));
            smallMolecules = new VectorDataCollection<>(BioPAXSupport.SMALL_MOLECULE, data, null);
            smallMolecules.getInfo().setQuerySystem(new BioPAXQuerySystem(smallMolecules));
            conversions = new VectorDataCollection<>(BioPAXSupport.CONVERSION, data, null);
            conversions.getInfo().setQuerySystem(new BioPAXQuerySystem(conversions));
            controls = new VectorDataCollection<>(BioPAXSupport.CONTROL, data, null);
            controls.getInfo().setQuerySystem(new BioPAXQuerySystem(controls));
            participants = new VectorDataCollection<>(BioPAXSupport.PARTICIPANT, data, null);
            participants.getInfo().setQuerySystem(new BioPAXQuerySystem(participants));
            publications = new VectorDataCollection<>(BioPAXSupport.PUBLICATION, data, null);
            publications.getInfo().setQuerySystem(new BioPAXQuerySystem(publications));
            vocabulary = new VectorDataCollection<>(BioPAXSupport.VOCABULARY, dictionaries, null);
            vocabulary.getInfo().setQuerySystem(new BioPAXQuerySystem(vocabulary));
            dataSources = new VectorDataCollection<>(BioPAXSupport.DATA_SOURCE, dictionaries, null);
            dataSources.getInfo().setQuerySystem(new BioPAXQuerySystem(dataSources));
            organisms = new VectorDataCollection<>(BioPAXSupport.ORGANISM, dictionaries, null);
            organisms.getInfo().setQuerySystem(new BioPAXQuerySystem(organisms));

            ApplicationFrame frame = Application.getApplicationFrame();
            final ApplicationStatusBar sb = frame == null ? null : frame.getStatusBar();
            jobControl = fjc == null ? new FunctionJobControl(null) : fjc;
            if(sb != null)
                jobControl.addListener(sb);
            try
            {
                startWork();
            }
            catch( Throwable t )
            {
                ExceptionRegistry.log(t);
                jobControl.functionTerminatedByError(t);
            }
            finally
            {
                if(sb != null)
                    jobControl.removeListener(sb);
            }
        }
        catch( Throwable t )
        {
            log.log(Level.SEVERE, "Import exception", t);
        }

        this.setNotificationEnabled(true);
    }

    @SuppressWarnings ( "unchecked" )
    public boolean initWithCollections(Map<String, DataCollection<?>> collections, FunctionJobControl jobControl, int filesCount,
            int currentFileNumber)
    {
        this.initAccess = false;
        this.filesCount = filesCount;
        this.currentFileNumber = currentFileNumber;
        if( filesCount > 1 )
            prefix = String.valueOf(currentFileNumber);

        data = (DataCollection<DataCollection>)collections.get( Module.DATA );
        diagrams = (DataCollection<Diagram>)collections.get(Module.DIAGRAM);
        dictionaries = (DataCollection<DataCollection>)collections.get( Module.METADATA );
        physicalEntities = (DataCollection<Concept>)collections.get(BioPAXSupport.PHYSICAL_ENTITY);
        complexes = (DataCollection<Complex>)collections.get(BioPAXSupport.COMPLEX);
        proteins = (DataCollection<Protein>)collections.get(BioPAXSupport.PROTEIN);
        rnas = (DataCollection<RNA>)collections.get(BioPAXSupport.RNA);
        dnas = (DataCollection<DNA>)collections.get(BioPAXSupport.DNA);
        smallMolecules = (DataCollection<Substance>)collections.get(BioPAXSupport.SMALL_MOLECULE);
        conversions = (DataCollection<Reaction>)collections.get(BioPAXSupport.CONVERSION);
        controls = (DataCollection<SemanticRelation>)collections.get(BioPAXSupport.CONTROL);
        participants = (DataCollection<SpecieReference>)collections.get(BioPAXSupport.PARTICIPANT);
        publications = (DataCollection<Publication>)collections.get(BioPAXSupport.PUBLICATION);
        vocabulary = (DataCollection<OpenControlledVocabulary>)collections.get(BioPAXSupport.VOCABULARY);
        dataSources = (DataCollection<DatabaseInfo>)collections.get(BioPAXSupport.DATA_SOURCE);
        organisms = (DataCollection<BioSource>)collections.get(BioPAXSupport.ORGANISM);

        this.jobControl = jobControl;
        try
        {
            return startWork();
        }
        catch( Throwable t )
        {
            log.log(Level.SEVERE, "Import exception", t);
            if(jobControl != null)
                jobControl.functionTerminatedByError(t);
        }
        return true;
    }

    private boolean startWork() throws Exception
    {

        doPut(data, true);
        doPut(diagrams, true);
        doPut(dictionaries, true);
        data.put(physicalEntities);
        data.put(complexes);
        data.put(proteins);
        data.put(rnas);
        data.put(dnas);
        data.put(smallMolecules);
        data.put(conversions);
        data.put(controls);
        data.put(participants);
        data.put(publications);
        dictionaries.put(vocabulary);
        dictionaries.put(dataSources);
        dictionaries.put(organisms);

        BioPAXReader reader = BioPAXReaderFactory.getReader(file);
        reader.setCollections(data, diagrams, dictionaries);
        reader.setBioHubBuilder(bioHubBuilder);
        BioPAXCollectionJobControl jc = new BioPAXCollectionJobControl(null);
        double oneFilePercent = 100.0 / filesCount;
        jc.pushProgress((int) ( ( currentFileNumber - 1 ) * oneFilePercent ), (int) ( currentFileNumber * oneFilePercent ));
        boolean result = reader.read(prefix, jc);
        if(physicalEntities.isEmpty()) data.remove(physicalEntities.getName());
        if(complexes.isEmpty()) data.remove(complexes.getName());
        if(proteins.isEmpty()) data.remove(proteins.getName());
        if(rnas.isEmpty()) data.remove(rnas.getName());
        if(dnas.isEmpty()) data.remove(dnas.getName());
        if(smallMolecules.isEmpty()) data.remove(smallMolecules.getName());
        if(conversions.isEmpty()) data.remove(conversions.getName());
        if(controls.isEmpty()) data.remove(controls.getName());
        if(participants.isEmpty()) data.remove(participants.getName());
        if(publications.isEmpty()) data.remove(publications.getName());
        jc.popProgress();
        return result;
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
    public @Nonnull Class<DataCollection> getDataElementType()
    {
        return ru.biosoft.access.core.DataCollection.class;
    }

    @Override
    public int getSize()
    {
        initCollection( null );
        return super.getSize();
    }

    @Override
    protected DataCollection<?> doGet(String name)
    {
        initCollection( null );
        return super.doGet(name);
    }

    @Override
    public @Nonnull Iterator<DataCollection<?>> iterator()
    {
        initCollection( null );
        return super.iterator();
    }

    @Override
    public @Nonnull List<String> getNameList()
    {
        initCollection( null );
        return super.getNameList();
    }

    public void setBioHubBuilder(BioHubBuilder bioHubBuilder)
    {
        this.bioHubBuilder = bioHubBuilder;
    }

    public class BioPAXCollectionJobControl extends StackProgressJobControl
    {
        public BioPAXCollectionJobControl(Logger l)
        {
            super( l );
        }
        @Override
        protected void doRun() throws JobControlException
        {
        }
        @Override
        public void setPreparedness(int percent)
        {
            super.setPreparedness(percent);
            if( jobControl != null )
                jobControl.setPreparedness(getPreparedness());
        }
        public void functionStarted()
        {
            if( jobControl != null )
                jobControl.functionStarted();
        }
        public void functionFinished(String message)
        {
            if( jobControl != null )
                jobControl.functionFinished(message);
        }
        @Override
        public int getStatus()
        {
            return jobControl.getStatus();
        }

    }
}