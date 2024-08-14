package ru.biosoft.bsa.access;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;

import com.developmentontheedge.beans.BeanInfoConstants;

import biouml.standard.type.Species;
import one.util.streamex.StreamEx;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.util.BeanUtil;
import ru.biosoft.util.DatabaseVersionComparator;
import ru.biosoft.workbench.editors.GenericComboBoxEditor;

public class SequencesDatabaseInfoSelector extends GenericComboBoxEditor
{
    private static SequencesDatabaseInfo[] databases;
    private static SequencesDatabaseInfo[] databasesWithNull;
    private static SequencesDatabaseInfo[] databasesEnsemblHuman;

    public static final String ONLY_ENSEMBL_HUMAN = "onlyEnsemblHuman";

    public static SequencesDatabaseInfo getDefaultDatabase()
    {
        init();
        return getDefaultDatabaseForArray( databases, SequencesDatabaseInfo.CUSTOM_SEQUENCES );
    }

    public static SequencesDatabaseInfo getDefaultHumanDatabase()
    {
        init();
        return getDefaultDatabaseForArray( databasesEnsemblHuman, null );
    }

    private static SequencesDatabaseInfo getDefaultDatabaseForArray(SequencesDatabaseInfo[] dbArray, SequencesDatabaseInfo defaultDB)
    {
        if( dbArray == null )
            return defaultDB;

        SequencesDatabaseInfo[] userDatabases = filterDatabasesForUser( dbArray );
        if( userDatabases.length == 0 )
            return defaultDB;
        Arrays.sort( userDatabases, new Comparator<SequencesDatabaseInfo>()
        {
            public int compare(SequencesDatabaseInfo o1, SequencesDatabaseInfo o2)
            {
                return new DatabaseVersionComparator().compare( o1.getVersion(), o2.getVersion() );
            }
        } );
        SequencesDatabaseInfo userDefaultDB = null;
        for( int i = 0; i < userDatabases.length; i++ )
        {
            if( userDatabases[i] == SequencesDatabaseInfo.CUSTOM_SEQUENCES )
                continue;
            if( userDefaultDB == null )
            {
                userDefaultDB = userDatabases[i];
                continue;
            }
            Properties properties = userDatabases[i].getBasePath().getDataCollection().getInfo().getProperties();
            if( isEnsemblHG38( properties ) )
                userDefaultDB = userDatabases[i];
        }
        if( userDefaultDB == null )
            return defaultDB;
        else
            return userDefaultDB;
    }
    private static boolean isEnsemblHG38(Properties properties)
    {
        return "Ensembl".equals( properties.getProperty( "database", "" ) ) && "hg38".equals( properties.getProperty( "genomeBuild", "" ) );
    }
    
    private static SequencesDatabaseInfo[] filterDatabasesForUser(SequencesDatabaseInfo[] databases)
    {
        init();
        List<SequencesDatabaseInfo> result = new ArrayList<>();
        for( int i = 0; i < databases.length; i++ )
        {
            SequencesDatabaseInfo db = databases[i];
            DataCollection<?> dc = db.getBasePath().optDataCollection();
            if(dc == null && db != SequencesDatabaseInfo.CUSTOM_SEQUENCES && db != SequencesDatabaseInfo.NULL_SEQUENCES)
                continue;//database not available for current user
            result.add( db );
        }
        return result.toArray( new SequencesDatabaseInfo[0] );
    }

    public static SequencesDatabaseInfo getNullDatabase()
    {
        return SequencesDatabaseInfo.NULL_SEQUENCES;
    }

    private static synchronized void init()
    {
        if(databases != null) return;
        try
        {
            SecurityManager.runPrivileged(() -> {
                List<SequencesDatabaseInfo> databasesList = new ArrayList<>();
                DataElementPath dbPath = DataElementPath.create("databases");
                if(!dbPath.exists())
                    return null;
                for(DataElementPath databasePath: dbPath.getChildren())
                {
                    try
                    {
                        databasesList.add(new SequencesDatabaseInfo(databasePath));
                    }
                    catch( Exception e )
                    {
                    }
                }
                databasesList.add(SequencesDatabaseInfo.CUSTOM_SEQUENCES);
                databases = databasesList.toArray(new SequencesDatabaseInfo[databasesList.size()]);

                databasesWithNull = new SequencesDatabaseInfo[databases.length+1];
                System.arraycopy(databases, 0, databasesWithNull, 1, databases.length);
                databasesWithNull[0] = SequencesDatabaseInfo.NULL_SEQUENCES;

                databasesEnsemblHuman = databasesList.stream().filter( SequencesDatabaseInfoSelector::isEnsemblHuman )
                        .toArray( SequencesDatabaseInfo[]::new );
                return null;
            });
        }
        catch( Exception e1 )
        {
            ExceptionRegistry.log(e1);
        }
    }

    public static boolean isEnsemblHuman(SequencesDatabaseInfo sbi)
    {
        DataCollection<?> dc = sbi.getBasePath().optDataCollection();
        if( dc == null || SequencesDatabaseInfo.CUSTOM_SEQUENCES == sbi || SequencesDatabaseInfo.NULL_SEQUENCES == sbi )
            return false;
        Properties properties = dc.getInfo().getProperties();
        String species = properties.getProperty( "species", "" );
        if( species.isEmpty() )
            return false;
        species = Species.getSpecies( species ).getCommonName();
        return "Ensembl".equals( properties.getProperty( "database", "" ) ) && "Human".equals( species );
    }

    public static SequencesDatabaseInfo getDatabase(DataElementPath path)
    {
        init();
        return StreamEx.of( filterDatabasesForUser( databases ) ).findAny( db -> db.getChromosomePath().equals( path ) )
                .orElse( SequencesDatabaseInfo.CUSTOM_SEQUENCES );
    }

    protected static SequencesDatabaseInfo getDatabaseInfo(String title)
    {
        init();
        return StreamEx.of( filterDatabasesForUser( databasesWithNull ) ).findFirst( db -> db.getName().equals( title ) ).orElse( null );
    }

    public static List<SequencesDatabaseInfo> getDatabases()
    {
        init();
        return Collections.unmodifiableList( Arrays.asList( databases ) );
    }

    @Override
    protected Object[] getAvailableValues()
    {
        init();

        boolean onlyEnsebmlHuman = BeanUtil.getBooleanValue( this, ONLY_ENSEMBL_HUMAN );
        if( onlyEnsebmlHuman )
            return filterDatabasesForUser( databasesEnsemblHuman );

        boolean canBeNull = BeanUtil.getBooleanValue(this, BeanInfoConstants.CAN_BE_NULL);
        return filterDatabasesForUser( canBeNull?databasesWithNull:databases );
    }
}
