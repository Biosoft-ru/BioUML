package biouml.plugins.lucene;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

import org.apache.lucene.queryparser.classic.ParseException;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.model.Module;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.DataElementPathSet;
import ru.biosoft.access.core.QuerySystem;
import ru.biosoft.access.core.RepositoryException;
import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.plugins.Plugins;

public class RebuildIndexesAnalysis extends AnalysisMethodSupport<RebuildIndexesAnalysis.RebuildIndexesParameters>
{
    public RebuildIndexesAnalysis(DataCollection<?> origin, String name)
    {
        super( origin, name, new RebuildIndexesParameters() );
    }

    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        DataElementPathSet modules = new DataElementPathSet(parameters.getModules());
        if(parameters.isIndexAnalyses())
            modules.add( DataElementPath.create("analyses") );
        jobControl.forCollection( modules, modulePath -> {
            log.info( "Indexing " + modulePath );
            DataCollection<?> module;
            try
            {
                module = modulePath.getDataElement( ru.biosoft.access.core.DataCollection.class );
            }
            catch( RepositoryException e1 )
            {
                log.log(Level.SEVERE,  ExceptionRegistry.log( e1 ) );
                return true;
            }
            if( ! ( module instanceof Module || module instanceof Plugins ) )
            {
                log.log(Level.SEVERE,  "Skipping " + modulePath + ": only modules and top-level repositories are supported" );
                return true;
            }
            module.getInfo().getProperties().setProperty( QuerySystem.QUERY_SYSTEM_CLASS, LuceneQuerySystemImpl.class.getName() );
            module.getInfo().getProperties().setProperty( LuceneQuerySystem.LUCENE_INDEX_DIRECTORY, LuceneUtils.INDEX_FOLDER_NAME );
            if(module.getOrigin() != null)
                CollectionFactoryUtils.save( module );

            LuceneQuerySystem luceneFacade = LuceneUtils.getLuceneFacade(module);
            if( luceneFacade instanceof LuceneQuerySystemImpl )
            {
                File dir = new File(module.getInfo().getProperties().getProperty( DataCollectionConfigConstants.CONFIG_PATH_PROPERTY, "." ),
                        LuceneUtils.INDEX_FOLDER_NAME);
                log.info( "Lucene dir: "+dir );
                ( (LuceneQuerySystemImpl)luceneFacade ).setLuceneDir(dir.getPath());
            }
            if( luceneFacade == null || luceneFacade.getCollectionsNamesWithIndexes().isEmpty() )
            {
                log.warning( "Lucene is not configured for module; skipping");
                return true;
            }
            try
            {
                luceneFacade.createIndex(log, jobControl);
            }
            catch( IOException | ParseException e2 )
            {
                log.log(Level.SEVERE,  ExceptionRegistry.log( e2 ) );
            }
            return true;
        } );
        return null;
    }

    @SuppressWarnings ( "serial" )
    public static class RebuildIndexesParameters extends AbstractAnalysisParameters
    {
        private DataElementPathSet modules = new DataElementPathSet();
        private boolean indexAnalyses;

        @PropertyName("Index analyses")
        public boolean isIndexAnalyses()
        {
            return indexAnalyses;
        }

        public void setIndexAnalyses(boolean indexAnalyses)
        {
            Object oldValue = this.indexAnalyses;
            this.indexAnalyses = indexAnalyses;
            firePropertyChange( "indexAnalyses", oldValue, this.indexAnalyses );
        }

        @PropertyName("Modules")
        @PropertyDescription("List of modules or user projects for index rebuild")
        public DataElementPathSet getModules()
        {
            return modules;
        }

        public void setModules(DataElementPathSet modules)
        {
            Object oldValue = this.modules;
            this.modules = modules;
            firePropertyChange( "modules", oldValue, this.modules );
        }
    }

    public static class RebuildIndexesParametersBeanInfo extends BeanInfoEx
    {
        public RebuildIndexesParametersBeanInfo()
        {
            super( RebuildIndexesParameters.class, true );
        }

        @Override
        protected void initProperties() throws Exception
        {
            add(DataElementPathEditor.registerInputMulti( "modules", beanClass, Module.class ));
            add("indexAnalyses");
        }
    }
}
