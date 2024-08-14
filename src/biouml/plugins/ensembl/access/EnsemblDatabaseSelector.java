package biouml.plugins.ensembl.access;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import biouml.standard.type.Species;
import one.util.streamex.StreamEx;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.bsa.TrackUtils;
import ru.biosoft.util.DatabaseVersionComparator;
import ru.biosoft.util.LazyValue;
import ru.biosoft.workbench.editors.GenericComboBoxEditor;

public class EnsemblDatabaseSelector extends GenericComboBoxEditor
{
    private static final Logger log = Logger.getLogger( EnsemblDatabaseSelector.class.getName() );
    private static LazyValue<EnsemblDatabase[]> values = new LazyValue<EnsemblDatabase[]>("Ensembl databases")
    {

        @Override
        protected EnsemblDatabase[] doGet() throws Exception
        {
            List<EnsemblDatabase> result = new ArrayList<>();
            SecurityManager.runPrivileged( () -> {
                for( DataCollection<?> db : CollectionFactoryUtils.getDatabases() )
                    if( "Ensembl".equals( db.getInfo().getProperties().getProperty( "database" ) ) )
                    {
                        EnsemblDatabase info = null;
                        DataElementPath basePath = DataElementPath.create( db );
                        try
                        {
                            info = new EnsemblDatabase( basePath );
                        }
                        catch( Exception e )
                        {
                            log.log( Level.SEVERE, "Can not init Ensembl", e );
                        }
                        if( info != null )
                            result.add( info );
                    }

                Comparator<EnsemblDatabase> cmp = Comparator.comparing( db -> db.getSpecie().getCommonName() );
                cmp = cmp.thenComparing( (db1, db2) -> new DatabaseVersionComparator().compare( db1.getVersion(), db2.getVersion() ) )
                        .reversed();
                Collections.sort( result, cmp );
                return null;
            } );
            return result.toArray( new EnsemblDatabase[result.size()] );
        }
    };

    @Override
    protected Object[] getAvailableValues()
    {
        return getEnsemblDatabases();
    }

    public static EnsemblDatabase[] getEnsemblDatabases()
    {
        return StreamEx.of( values.get() ).filter( db -> db.getPath().exists() ).toArray( EnsemblDatabase[]::new );
    }

    public static EnsemblDatabase getDefaultEnsembl()
    {
        EnsemblDatabase[] ensemblDatabases = getEnsemblDatabases();
        if(ensemblDatabases.length == 0)
            return null;
        return ensemblDatabases[0];
    }

    public static EnsemblDatabase getDefaultEnsembl(Species species)
    {
        return new EnsemblDatabase( TrackUtils.getEnsemblPath( species ) );
    }
}