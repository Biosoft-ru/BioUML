package biouml.plugins.seek;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.workbench.editors.GenericMultiSelectEditor;

public class DataFilesLister extends GenericMultiSelectEditor
{
    //TODO: get rid of logger in this class
    //    private final Logger log;
    //    public DataFilesLister()
    //    {
    //        log = ( (SeekSyncParameters)getBean() ).getLogger();
    //    }

    @Override
    protected String[] getAvailableValues()
    {
        String login = ( (SeekSyncParameters)getBean() ).getLogin();
        String password = ( (SeekSyncParameters)getBean() ).getPassword();
        String seekUrl = ( (SeekSyncParameters)getBean() ).getSeekUrl();
        DataElementPath outputFolder = ( (SeekSyncParameters)getBean() ).getOutputPath();
        try
        {
            String[] list = SeekSyncAnalysis.listAllDataFiles( login, password, seekUrl, outputFolder );
            return list == null ? new String[] {} : list;
        }
        catch( Exception e )
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return new String[] {};
        }
        //return browseDataFiles( seekUrl, login, password );

        //        return Optional.ofNullable( ( (RemoveUnobservableMoleculesParameters)getBean() ).getInputPath() )
        //                .map( path -> path.optDataElement( Diagram.class ) ).map( d -> d.getRole( EModel.class ).getEquations() )
        //                .map( this::getNames ).orElse( new String[] {} );
    }

    //    protected String[] browseDataFiles(String seekUrl, String login, String password)
    //    {
    //
    //        // TODO Auto-generated method stub
    //        return StreamEx.of( seekUrl, login, password ).nonNull().toArray( String[]::new );
    //    }

}
