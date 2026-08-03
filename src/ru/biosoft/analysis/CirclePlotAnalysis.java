package ru.biosoft.analysis;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import one.util.streamex.IntStreamEx;
import one.util.streamex.StreamEx;
import ru.biosoft.access.DataElementExporterRegistry;
import ru.biosoft.access.ImageDataElement;
import ru.biosoft.access.ImageFileImporter;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.script.LogScriptEnvironment;
import ru.biosoft.access.script.ScriptTypeRegistry;
import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.analysis.CirclePlotAnalysis.Parameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.jobcontrol.FunctionJobControl;
import ru.biosoft.jobcontrol.SubFunctionJobControl;
import ru.biosoft.table.ColumnModel;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.columnbeans.Column;
import ru.biosoft.table.columnbeans.ColumnNameSelector;
import ru.biosoft.table.export.TableElementExporter;
import ru.biosoft.table.export.TableElementExporter.TableExporterProperties;
import ru.biosoft.util.TempFile;
import ru.biosoft.util.TempFiles;

@ClassIcon("resources/dotplot.png")
public class CirclePlotAnalysis extends AnalysisMethodSupport<Parameters>
{
    public CirclePlotAnalysis(DataCollection<?> origin, String name)
    {
        super( origin, name, new Parameters() );
    }

    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        final LogScriptEnvironment env = new LogScriptEnvironment( log );
        File imageFile = TempFiles.file( "dotplot_output.png" );
        imageFile.delete();
        Set<String> columns = new HashSet<>();
        columns.add( parameters.getValuesColumn() );
        columns.add( parameters.getCategoryColumn() );
        columns.add( parameters.getCircleSizeColumn() );
        TableDataCollection inputTable = parameters.getInputTable().getDataElement( TableDataCollection.class );
        ColumnModel cm = inputTable.getColumnModel();
        if( parameters.getCircleColorColumn() != null )
            columns.add( parameters.getCircleColorColumn() );
        if( parameters.getOrderColumn() != null )
            columns.add( parameters.getOrderColumn() );
        
        File inputFile = exportTable( parameters.getInputTable(), columns.toArray( new String[] {} ), new SubFunctionJobControl( jobControl ) );

        String rScript = getRScript( inputFile, imageFile );
        log.log( Level.FINE, rScript );
        SecurityManager.runPrivileged( () -> ScriptTypeRegistry.execute( "R", rScript, env, false ) );
        ImageFileImporter importer = new ImageFileImporter();
        DataElementPath outPath = parameters.getOutputChart();
        ImageDataElement result = (ImageDataElement) importer.doImport( outPath.getParentCollection(), imageFile, outPath.getName(), null, log );
        imageFile.delete();
        return new Object[] { result };
    }

    private File exportTable(DataElementPath tablePath, String[] columnNames, FunctionJobControl jc) throws IOException
    {
        TableElementExporter exporter = new TableElementExporter();
        Properties properties = new Properties();
        properties.setProperty( DataElementExporterRegistry.SUFFIX, "txt" );
        exporter.init( properties );

        TempFile file = TempFiles.file( "dotplot_input.txt" );
        try
        {
            TableDataCollection table = tablePath.getDataElement( TableDataCollection.class );
            TableExporterProperties exportParameters = (TableExporterProperties) exporter.getProperties( table, file );
            Column[] columns = StreamEx.of( columnNames ).map( name -> new Column( null, name ) ).toArray( Column[]::new );
            exportParameters.setColumns( columns );
            exporter.doExport( table, file, jc );
        }
        catch (Exception e)
        {
            file.delete();
            throw ExceptionRegistry.translateException( e );
        }
        return file;
    }

    private String getRScript(File inputFile, File imageFile)
    {
        String sortStr = parameters.getOrderColumn();
        String descriptionColumn = parameters.getCategoryColumn();
        String title = "Dotplot for " + parameters.getInputTable().getName();
        String sortTitlesStr = null;
        if( sortStr != null )
        {
            sortTitlesStr = sortStr;
            if( parameters.isSortDescending() )
                sortStr = "desc(" + sortStr + ")";
        }
        else
            sortStr = descriptionColumn;
        String xColumn = parameters.getValuesColumn();
        String sizeColumn = parameters.getCircleSizeColumn();
        String colorColumn = parameters.getCircleColorColumn();
        String fillColor = colorColumn == null ? "#00abff":colorColumn;

        StringBuilder script = new StringBuilder();
        script.append( "suppressPackageStartupMessages({\n"
                + "  library(ggplot2)\n"
                + "  library(dplyr)\n"
                + "})\n"
                + "# ---- 1. Read the results file ----\n"
                + "gsea_res <- read.delim(\"" + inputFile.getAbsolutePath() + "\",\n"
                + "                       sep = \"\\t\",\n"
                + "                       stringsAsFactors = FALSE,\n"
                + "                       check.names = FALSE)" );
        
        script.append( "# ---- 2. Sort and limit to top categories ----\n"
                + "showCategory <- " +parameters.getNumTopCategories()+ "\n"
                + "gsea_res <- gsea_res %>%\n");

        script.append("  arrange("+ sortStr +") %>%\n"
                + "  head(showCategory)" );
        if( sortTitlesStr != null )
        {
            script.append( "%>%\n" );
            script.append( "mutate (`" + descriptionColumn + "` = reorder(`" + descriptionColumn + "`, `" + sortTitlesStr + "`))\n" );
        }
        else
            script.append( "\n" );
        
        
        script.append( "# ---- 3. Draw the plot and assign it to a variable 'p' ----\n" 
                + "p <- ggplot(gsea_res, aes(x = `" + xColumn + "`, y = `" + descriptionColumn + "`)) +\n" 
                + "  geom_point(aes(size = `" + sizeColumn + "`, fill = `" + fillColor + "`),shape=21,color=\"black\",stroke=0.5) +\n" );
        if(colorColumn != null)
        {
            script.append( "  scale_fill_gradient(\n" + "    name    = \"" + colorColumn + "\",\n"
                + "    low     = \"red\",\n" 
                + "    high    = \"blue\",\n" 
                + "    trans   = \""+ (parameters.isNeedLog() ? "log10" : "none")
                    + "\",\n    guide   = guide_colorbar(reverse = TRUE)) + \n" );
        }
        script.append( "scale_x_continuous(expand = expansion(mult = c(0.15, 0.15))) +\n" );
        script.append( " scale_size_continuous(name = \"" + sizeColumn + "\", range = c(2, 8)) +\n" + "  labs(\n" + "    title = \"" + title + "\",\n" + "    x     = \""
                + xColumn + "\",\n"
                + "    y     = NULL\n" + "  ) +\n" + "  theme_minimal(base_size = 10) +\n"
                + "  theme(\n"
                + "    plot.title    = element_text(face = \"bold\", hjust = 0.5),\n" 
                + "    axis.text.y   = element_text(size = 10),\n"
                + "    panel.grid.minor = element_blank()\n" + "  )" );

        int dpi = parameters.getDpi();
        script.append( "# ---- 5. Save the plot to a file as high-resolution PNG ----\n"
                + "ggsave(filename = \""+imageFile.getAbsolutePath()+"\", \n"
                + "       plot = p, \n"
                + "       width = " + parameters.getImageWidth() / dpi + ",      # Width in inches\n" + "       height = " + parameters.getImageHeight() / dpi
                + ",    # Height in inches \n" + "       units = \"in\", \n" + "       dpi = " + 150 + ") " );
        return script.toString();
    }

    public static class Parameters extends ChartAnalysisParameters
    {
        private String valuesColumn, categoryColumn, orderColumn, circleSizeColumn, circleColorColumn;
        private boolean needLog = true;
        private int imageWidth = 1200, imageHeight = 800, dpi = 150;
        private int numTopCategories = 10;
        private boolean sortDescending = false;

        @PropertyName("Values column")
        public String getValuesColumn()
        {
            return valuesColumn;
        }

        public void setValuesColumn(String valuesColumn)
        {
            String oldValue = this.valuesColumn;
            this.valuesColumn = valuesColumn;
            firePropertyChange( "valuesColumn", oldValue, this.valuesColumn );
        }

        @PropertyName("Need log10")
        @PropertyDescription("If checked, -log10 of values will be used for plotting")
        public boolean isNeedLog()
        {
            return needLog;
        }

        public void setNeedLog(boolean needLog)
        {
            boolean oldValue = this.needLog;
            this.needLog = needLog;
            firePropertyChange( "needLog", oldValue, this.needLog );
        }

        @PropertyName("Width")
        @PropertyDescription("Image width")
        public int getImageWidth()
        {
            return imageWidth;
        }

        public void setImageWidth(int imageWidth)
        {
            int oldValue = this.imageWidth;
            this.imageWidth = imageWidth;
            firePropertyChange( "imageWidth", oldValue, this.imageWidth );
        }

        @PropertyName("Height")
        @PropertyDescription("Image height")
        public int getImageHeight()
        {
            return imageHeight;
        }

        public void setImageHeight(int imageHeight)
        {
            int oldValue = this.imageHeight;
            this.imageHeight = imageHeight;
            firePropertyChange( "imageHeight", oldValue, this.imageHeight );
        }

        @PropertyName("Titles column")
        public String getCategoryColumn()
        {
            return categoryColumn;
        }

        public void setCategoryColumn(String categoryColumn)
        {
            String oldValue = this.categoryColumn;
            this.categoryColumn = categoryColumn;
            firePropertyChange( "categoryColumn", oldValue, this.categoryColumn );
        }

        @PropertyName("Number of top categories")
        public int getNumTopCategories()
        {
            return numTopCategories;
        }

        public void setNumTopCategories(int numTopCategories)
        {
            int oldValue = this.numTopCategories;
            this.numTopCategories = numTopCategories;
            firePropertyChange( "numTopCategories", oldValue, this.numTopCategories );
        }

        @PropertyName("Category order")
        @PropertyDescription("Categories will be sorted by the selected column, or alphabetically if none is selected.")
        public String getOrderColumn()
        {
            return orderColumn;
        }

        public void setOrderColumn(String orderColumn)
        {
            String oldValue = this.orderColumn;
            this.orderColumn = orderColumn;
            firePropertyChange( "orderColumn", oldValue, this.orderColumn );
        }

        @PropertyName("Dot size")
        @PropertyDescription("Value to be used as dot size. For example, number of hits in functional classification")
        public String getCircleSizeColumn()
        {
            return circleSizeColumn;
        }

        public void setCircleSizeColumn(String circleSizeColumn)
        {
            String oldValue = this.circleSizeColumn;
            this.circleSizeColumn = circleSizeColumn;
            firePropertyChange( "circleSizeColumn", oldValue, this.circleSizeColumn );
        }

        @PropertyName("Dot color")
        @PropertyDescription("Maximal value is red, minimal is blue")
        public String getCircleColorColumn()
        {
            return circleColorColumn;
        }

        public void setCircleColorColumn(String circleColorColumn)
        {
            String oldValue = this.circleColorColumn;
            this.circleColorColumn = circleColorColumn;
            firePropertyChange( "circleColorColumn", oldValue, this.circleColorColumn );
        }

        @PropertyName("Sort descending")
        public boolean isSortDescending()
        {
            return sortDescending;
        }

        public void setSortDescending(boolean sortDescending)
        {
            boolean oldValue = this.sortDescending;
            this.sortDescending = sortDescending;
            firePropertyChange( "sortDescending", oldValue, this.sortDescending );
        }

        public boolean hideSort()
        {
            return orderColumn == null;
        }

        public int getDpi()
        {
            return dpi;
        }

        public void setDpi(int dpi)
        {
            int oldValue = this.dpi;
            this.dpi = dpi;
            firePropertyChange( "dpi", oldValue, this.dpi );
        }

    }

    public static class ParametersBeanInfo extends ChartAnalysisParametersBeanInfo
    {
        public ParametersBeanInfo()
        {
            super( Parameters.class );
        }

        @Override
        protected void initProperties() throws Exception
        {
            property( "inputTable" ).inputElement( TableDataCollection.class ).add();
            add( ColumnNameSelector.registerNumericSelector( "valuesColumn", beanClass, "inputTable", false ) );
            add( "needLog" );
            add( ColumnNameSelector.registerSelector( "categoryColumn", beanClass, "inputTable", false ) );
            add( ColumnNameSelector.registerNumericSelector( "circleSizeColumn", beanClass, "inputTable", false ) );
            add( ColumnNameSelector.registerNumericSelector( "circleColorColumn", beanClass, "inputTable", true ) );
            add( ColumnNameSelector.registerSelector( "orderColumn", beanClass, "inputTable", true ) );
            property( "sortDescending" ).hidden( "hideSort" ).add();

            add( "numTopCategories" );
            addExpert( "imageWidth" );
            addExpert( "imageHeight" );
            property( "dpi" ).tags( bean -> IntStreamEx.of( 72, 150, 300 ).mapToObj( String::valueOf ) ).expert().add();
            property( "outputChart" ).outputElement( ImageDataElement.class ).auto( "$inputTable$ dot plot" ).add();
        }
    }
}
