package ru.biosoft.access;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.developmentontheedge.application.ApplicationUtils;
import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementImporter;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.access.core.MessageBundle;
import ru.biosoft.jobcontrol.FunctionJobControl;
import ru.biosoft.jobcontrol.JobControl;
import ru.biosoft.util.OptionEx;
import ru.biosoft.util.TempFiles;


/**
 * Install NCBI SRA Toolkit as described here
 * https://trace.ncbi.nlm.nih.gov/Traces/sra/sra.cgi?view=software
 * 
 * Default installation directory is /usr/local/ncbi/sra-tools
 * Configure sra-toolkit by running vdb-config --interactive
 * Sometimes it is required to go to sra-tools directory and call ./vdb-config --interactive 
 * 
 * Check that path to fasterq-dump tool is available for tomcat
 * 
 * @author anna
 *
 */

public class SRAFastqImporter implements DataElementImporter
{
    protected SRAImportProperties importerProperties;
    private int BASE_UPLOAD_PROGRESS = 5;
    @Override
    public boolean init(Properties properties)
    {
        return isProgExists( "fasterq-dump" );
    }


    private boolean isProgExists(String progName)
    {
        for( String path : System.getenv( "PATH" ).split( ":" ) )
        {
            File file = new File( path, progName );
            if( file.exists() && file.canExecute() )
                return true;
        }
        return false;
    }


    @Override
    public int accept(DataCollection parent, File file)
    {

        if( parent == null || !parent.isMutable() || !DataCollectionUtils.isAcceptable( parent, FileDataElement.class ) )
            return ACCEPT_UNSUPPORTED;
        if( FileImporter.isTextFile( file ) )
        {
            try (BufferedReader reader = new BufferedReader( new FileReader( file ) ))
            {
                String line = reader.readLine();
                if( line == null || !line.startsWith( "SRR" ) )
                {
                    reader.close();
                    return ACCEPT_UNSUPPORTED;
                }
                return ACCEPT_HIGH_PRIORITY;
            }
            catch( Exception e )
            {
                return ACCEPT_UNSUPPORTED;
            }
        }
        return ACCEPT_UNSUPPORTED;
    }


    @Override
    public DataElement doImport(DataCollection parent, File file, String elementName, FunctionJobControl jobControl, Logger log)
            throws Exception
    {
        if( jobControl != null )
        {
            jobControl.functionStarted();
        }
        Set<String> ids = new HashSet<>();
        try (FileInputStream is = new FileInputStream( file );
                BufferedReader input = new BufferedReader( new InputStreamReader( is, StandardCharsets.UTF_8 ) );
                FileChannel ch = is.getChannel())
        {
            String line;
            while( ( line = input.readLine() ) != null )
            {
                String idStr = line.trim();
                if( idStr.startsWith( "SRR" ) )
                    ids.add( idStr );
            }
        }
        catch( Exception e )
        {
            throw e;
        }

        if( jobControl != null && jobControl.getStatus() != JobControl.TERMINATED_BY_REQUEST
                && jobControl.getStatus() != JobControl.TERMINATED_BY_ERROR )
        {
            jobControl.setPreparedness( BASE_UPLOAD_PROGRESS );
        }

        if( ids.isEmpty() )
        {
            jobControl.setPreparedness( 100 );
            jobControl.functionFinished();
            return null;
        }
        int progressPerId = 90 / ids.size();
        int cnt = 0;
        boolean cleanup = importerProperties.isCleanExisted();
        Set<String> uploaded = new HashSet<>();
        for( String srrId : ids )
        {
            cnt++;
            int progressShift = BASE_UPLOAD_PROGRESS + ( cnt - 1 ) * progressPerId;

            File uploadDir = TempFiles.dir( "upload_" + srrId );
            uploadDir.mkdir();

            try
            {
                ProcessBuilder pb = new ProcessBuilder( "fasterq-dump", "-O", uploadDir.getAbsolutePath(), "-p", srrId );
                pb.directory( uploadDir );
                pb.redirectErrorStream( true );

                Process process = pb.start();

                InputStream stdout = process.getInputStream();
                StringBuilder message = new StringBuilder();
                Pattern percentPattern = Pattern.compile( "\\s([0-9\\.]+)%" );
                byte[] buf = new byte[2048];
                while( true )
                {
                    try
                    {
                        int len = stdout.read( buf );
                        if( len == -1 )
                        {
                            break;
                        }
                        String line = new String( buf, 0, len, Charset.defaultCharset() );
                        Matcher m = percentPattern.matcher( line );
                        String perc = "0.0";
                        while( m.find() )
                        {
                            perc = m.group( m.groupCount() );
                        }
                        if( !perc.equals( "0.0" ) )
                            jobControl.setPreparedness( progressShift + progressPerId * Double.valueOf( perc ).intValue() / 100 );
                    }
                    catch( IOException e )
                    {
                        log.log( Level.SEVERE, "Can not load ID " + srrId + "\n" + e.getMessage().toString() );
                        break;
                    }
                }

                //TODO: what if process stuck?

                int NORMAL_EXIT_VAL = 0;
                int exitVal = process.waitFor();
                if( exitVal != NORMAL_EXIT_VAL )
                {
                    log.log( Level.SEVERE, "Can not load ID " + srrId + "\n" + message.toString() );
                    continue;
                }

                if( uploadDir.isDirectory() && uploadDir.list().length == 0 )
                {
                    log.log( Level.INFO, "Uploaded ID " + srrId + "is empty" );
                    continue;
                }
                DataCollection<DataElement> collection = null;
                DataElementPath rootPath = DataElementPath.create( parent, srrId );
                if( parent.contains( srrId ) && !cleanup && parent.get( srrId ) instanceof FolderCollection )
                {
                    collection = (FolderCollection)parent.get( srrId );
                }
                else if( parent.contains( srrId ) && cleanup )
                {
                    parent.remove( srrId );
                    collection = DataCollectionUtils.createSubCollection( rootPath );
                }
                else
                {
                    collection = DataCollectionUtils.createSubCollection( rootPath );
                }
                if( collection == null )
                    throw new Exception( "Unable to create " + rootPath );

                for( String name : uploadDir.list() )
                {
                    DataElement de = null;
                    File result = null;
                    File fastqFile = new File( uploadDir, name );
                    try
                    {
                        result = DataCollectionUtils.getChildFile( collection, name );
                    }
                    catch( ClassCastException e )
                    {
                    }
                    if( result != null )
                        ApplicationUtils.linkOrCopyFile( result, fastqFile, jobControl );
                    else
                        result = fastqFile;
                    de = new FileDataElement( name, collection, result );
                    collection.put( de );
                }

                parent.put( collection );
                uploaded.add( srrId );

            }
            finally
            {
                ApplicationUtils.removeDir( uploadDir );
            }

            if( jobControl != null )
            {
                jobControl.setPreparedness( 5 + cnt * progressPerId );
                if( jobControl.getStatus() == JobControl.TERMINATED_BY_REQUEST || jobControl.getStatus() == JobControl.TERMINATED_BY_ERROR )
                {
                    cleanUploaded( uploaded, parent );
                    return null;
                }
            }


        }
        if( jobControl != null && jobControl.getStatus() != JobControl.TERMINATED_BY_REQUEST
                && jobControl.getStatus() != JobControl.TERMINATED_BY_ERROR )
        {
            jobControl.setPreparedness( 100 );
            jobControl.functionFinished();
        }
        if( uploaded.isEmpty() )
            return null;

        return parent.get( uploaded.iterator().next() );
    }

    private void cleanUploaded(Set<String> uploaded, DataCollection parent) throws Exception
    {
        for( String name : uploaded )
        {
            parent.remove( name );
        }
    }

    @Override
    public Class<? extends DataElement> getResultType()
    {
        return FolderCollection.class;
    }

    @Override
    public SRAImportProperties getProperties(DataCollection parent, File file, String elementName)
    {
        if( importerProperties == null )
            importerProperties = new SRAImportProperties();
        return importerProperties;
    }


    public static class SRAImportProperties extends OptionEx
    {
        private boolean cleanExisted = true;

        @PropertyName ( "Cleanup existing" )
        @PropertyDescription ( "Each SRR* id from your file will be imported into a separate folder. "
                + "If the folder with the same name already exists, its content will be removed." )
        public boolean isCleanExisted()
        {
            return cleanExisted;
        }

        public void setCleanExisted(boolean cleanExisted)
        {
            Object oldValue = this.cleanExisted;
            this.cleanExisted = cleanExisted;
            firePropertyChange( "cleanExisted", oldValue, cleanExisted );
        }

        
    }
    
    public static class SRAImportPropertiesBeanInfo extends BeanInfoEx
    {
        public SRAImportPropertiesBeanInfo()
        {
           super(SRAImportProperties.class, MessageBundle.class.getName());
           beanDescriptor.setDisplayName(getResourceString("PN_FILE_IMPORT_PROPERTIES"));
           beanDescriptor.setShortDescription(getResourceString("PD_FILE_IMPORT_PROPERTIES"));
        }
        
        @Override
        protected void initProperties() throws Exception
        {
            add( "cleanExisted" );
        }
    }


}
