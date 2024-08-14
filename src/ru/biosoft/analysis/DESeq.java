package ru.biosoft.analysis;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;

import javax.annotation.Nonnull;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.DataElementExporterRegistry;
import ru.biosoft.access.core.DataElementImporter;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.biohub.ReferenceType;
import ru.biosoft.access.biohub.ReferenceTypeRegistry;
import ru.biosoft.access.exception.BiosoftFileNotFoundException;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.access.script.LogScriptEnvironment;
import ru.biosoft.access.script.ScriptTypeRegistry;
import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.jobcontrol.FunctionJobControl;
import ru.biosoft.jobcontrol.SubFunctionJobControl;
import ru.biosoft.table.TableCSVImporter;
import ru.biosoft.table.TableCSVImporter.NullImportProperties;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.columnbeans.Column;
import ru.biosoft.table.columnbeans.ColumnGroup;
import ru.biosoft.table.export.TableElementExporter;
import ru.biosoft.table.export.TableElementExporter.TableExporterProperties;
import ru.biosoft.util.TempFile;
import ru.biosoft.util.TempFiles;
import ru.biosoft.util.bean.BeanInfoEx2;
import ru.biosoft.workbench.editors.GenericComboBoxEditor;

@ClassIcon("resources/DESeq.gif")
public class DESeq extends AnalysisMethodSupport<DESeq.Parameters>
{
    public DESeq(DataCollection<?> origin, String name)
    {
        super( origin, name, new Parameters() );
    }

    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        jobControl.pushProgress( 0, 20 );
        log.info( "Preparing experiment table..." );
        File experimentFile = exportTable( parameters.getExperimentGroup().getTablePath(), parameters.getExperimentGroup().getColumns(), new SubFunctionJobControl( jobControl ) );
        log.info( "done" );
        jobControl.popProgress();
        if( jobControl.isStopped() )
            return null;

        jobControl.pushProgress( 20, 40 );
        log.info( "Preparing control table..." );
        File controlFile = exportTable( parameters.getControlGroup().getTablePath(), parameters.getControlGroup().getColumns(), new SubFunctionJobControl( jobControl ) );
        log.info( "done" );
        jobControl.popProgress();
        if( jobControl.isStopped() )
            return null;

        jobControl.pushProgress( 40, 70 );
        File testsFile = TempFiles.file( "deseq_output.txt" );
        testsFile.delete();
        final String rCommand = getRCommand(parameters.getDeseqVersion(), experimentFile, controlFile, testsFile);
        final LogScriptEnvironment env = new LogScriptEnvironment( log );
        log.log(Level.FINE,  rCommand );
        log.info( "Invoking R command (that will take some time)..." );
        SecurityManager.runPrivileged( () -> ScriptTypeRegistry.execute( "R", rCommand, env, false ) );
        experimentFile.delete();
        controlFile.delete();
        /*if( env.isFailed() )
        {
            testsFile.delete();
            throw new Exception( "R command failed." );
        }*/
        if( jobControl.isStopped() )
            return null;
        jobControl.popProgress();

        jobControl.pushProgress( 70, 100 );
        log.info( "Importing..." );
        TableDataCollection result = importTable( testsFile, parameters.getTestsTable(), new SubFunctionJobControl( jobControl ) );
        testsFile.delete();
        ReferenceType referenceType = ReferenceTypeRegistry.getElementReferenceType( parameters.getExperimentGroup().getTablePath() );
        ReferenceTypeRegistry.setCollectionReferenceType( result, referenceType );
        log.info( "done" );
        jobControl.popProgress();


        return new Object[] { result };
    }

    private String getRCommand(String deseqVersion, File experimentFile, File controlFile, File testsFile)
    {
    	String result =
                "writeLines('Reading experiment data...')\n" + "experiment <- read.table( '" + experimentFile.getAbsolutePath()
                        + "', header=TRUE, row.names=1, sep='\t' )\n"
                        + "writeLines('Reading control data...')\n"
                        + "control <- read.table( '" + controlFile.getAbsolutePath() + "', header=TRUE, row.names=1, sep='\t' )\n"
                + "countTable <- cbind(experiment, control)\n"
                + "names <- data.frame(ID = rownames(countTable))\n"
                + "countTable <- sapply(countTable, round)\n"
                + "condition <- factor(c(rep('experiment', ncol(experiment)), rep('control', ncol(control))))\n";
        if (deseqVersion.equals("DESeq"))
        {
            return result + "library(DESeq)\n"
              + "cds <- newCountDataSet(countTable, condition)\n"
              + "cds <- estimateSizeFactors(cds)\n"
              + "cds <- estimateDispersions(cds)\n"
              + "res <- nbinomTest(cds, 'experiment', 'control')\n"
                    + "write.table(res, file='" + testsFile.getAbsolutePath() + "', sep='\t', row.names=F, col.names=T, na='')";
        }
        else
        {
        	return result + "library(DESeq2)\n"
        			+ "de.data <- DESeqDataSetFromMatrix(as.matrix(countTable),DataFrame(condition),~ condition)\n"
        			+ "res     <- results(DESeq(de.data))\n"
                    + "write.table(cbind(names,res), file='" + testsFile.getAbsolutePath()
                    + "', quote=F, sep='\t', row.names=F, col.names=T, na='')\n";
        }
    }

    private File exportTable(DataElementPath tablePath, Column[] columns, FunctionJobControl jc) throws IOException
    {
        TableElementExporter exporter = new TableElementExporter();
        Properties properties = new Properties();
        properties.setProperty( DataElementExporterRegistry.SUFFIX, "txt" );
        exporter.init( properties );

        TempFile file = TempFiles.file( "deseq_input.txt" );
        try
        {
            TableDataCollection table = tablePath.getDataElement( TableDataCollection.class );
            TableExporterProperties exportParameters = (TableExporterProperties)exporter.getProperties( table, file );
            exportParameters.setColumns( columns );
            exporter.doExport( table, file, jc );
        }
        catch( Exception e )
        {
            file.delete();
            throw ExceptionRegistry.translateException( e );
        }
        return file;
    }

    private TableDataCollection importTable(File file, DataElementPath path, FunctionJobControl jc) throws Exception
    {
        if( !file.exists() )
            throw new BiosoftFileNotFoundException( file );
        DataElementImporter importer = new TableCSVImporter();
        DataCollection<DataElement> outputDC = path.getParentCollection();
        if( importer.accept( outputDC, file ) == DataElementImporter.ACCEPT_UNSUPPORTED )
            throw new Exception( "Can not import into selected collection" );
        NullImportProperties importParameters = (NullImportProperties)importer.getProperties( outputDC, file, path.getName() );
        importParameters.setColumnForID( "id" ); // It seems that this is useless. From DESeq2 a column named "id" was not used as ID column upon import
        importParameters.setHeaderRow( 1 );
        importParameters.setDataRow( 2 );
        return (TableDataCollection)importer.doImport( outputDC, file, path.getName(), jc, log );
    }


    public static class Parameters extends AbstractAnalysisParameters
    {
        private ColumnGroup experimentGroup = new ColumnGroup( this );
        private ColumnGroup controlGroup = new ColumnGroup( this );
        private DataElementPath testsTable;
        private String deseqVersion = "DESeq2";

        public static class DESeqVersion extends GenericComboBoxEditor
        {
        	@Override
            protected Object[] getAvailableValues()
        	{
                return new String[]{"DESeq","DESeq2"};
            }
        }

        @PropertyName("DESeq version")
        @PropertyDescription("Set version of the algorithm")
        public String getDeseqVersion()
        {
            return deseqVersion;
        }

        public void setDeseqVersion(String version)
        {
            Object old = this.deseqVersion;
            this.deseqVersion = version;
            firePropertyChange("deseqVersion", old, deseqVersion);
        }

        @PropertyName("Experiment samples")
        public ColumnGroup getExperimentGroup()
        {
            return experimentGroup;
        }
        public void setExperimentGroup(ColumnGroup experimentGroup)
        {
            this.experimentGroup = experimentGroup;
            if( experimentGroup != null )
                experimentGroup.setParent( this );
            firePropertyChange( "*", null, null );
        }

        @PropertyName("Control samples")
        public ColumnGroup getControlGroup()
        {
            return controlGroup;
        }
        public void setControlGroup(ColumnGroup controlGroup)
        {
            this.controlGroup = controlGroup;
            if( controlGroup != null )
                controlGroup.setParent( this );
            firePropertyChange( "*", null, null );
        }

        @PropertyName("Output table")
        @PropertyDescription("Path for output table")
        public DataElementPath getTestsTable()
        {
            return testsTable;
        }
        public void setTestsTable(DataElementPath testsTable)
        {
            Object oldValue = this.testsTable;
            this.testsTable = testsTable;
            firePropertyChange( "testsTable", oldValue, testsTable );
        }

        @Override
        public @Nonnull String[] getInputNames()
        {
            return new String[] {"experimentGroup/tablePath", "controlGroup/tablePath"};
        }

        @Override
        public void read(Properties properties, String prefix)
        {
            super.read( properties, prefix );
            String experimentGroupStr = properties.getProperty( prefix + "experimentGroup" );
            if( experimentGroupStr != null )
            {
                experimentGroup = ColumnGroup.readObject( this, experimentGroupStr );
            }
            String controlGroupStr = properties.getProperty( prefix + "controlGroup" );
            if( controlGroupStr != null )
            {
                controlGroup = ColumnGroup.readObject( this, controlGroupStr );
            }
        }
    }

    public static class ParametersBeanInfo extends BeanInfoEx2<Parameters>
    {
        public ParametersBeanInfo()
        {
            super( Parameters.class );
        }

        @Override
        protected void initProperties() throws Exception
        {
        	add( "deseqVersion", Parameters.DESeqVersion.class);
            add( "experimentGroup" );
            add( "controlGroup" );
            property( "testsTable" ).outputElement( TableDataCollection.class ).add();
        }
    }
}
