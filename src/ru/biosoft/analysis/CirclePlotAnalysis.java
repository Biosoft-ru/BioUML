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
        if( parameters.getCircleColorColumn() != null )
            columns.add( parameters.getCircleColorColumn() );
        if( parameters.getOrderColumn() != null )
            columns.add( parameters.getOrderColumn() );
        if( parameters.getMarkColumn() != null )
            columns.add( parameters.getMarkColumn() );
        
        File inputFile = exportTable( parameters.getInputTable(), columns.toArray( new String[] {} ), new SubFunctionJobControl( jobControl ) );

        String rScript = getRScript( inputFile, imageFile );
        log.log( Level.FINE, rScript );
        SecurityManager.runPrivileged( () -> ScriptTypeRegistry.execute( "R", rScript, env, false ) );
        DataElementPath outPath = parameters.getOutputChart();
        ImageFileImporter importer = new ImageFileImporter();
        importer.getProperties( outPath.getParentCollection(), imageFile, outPath.getName() ).setPreserveExtension( false );
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
        String sortColName = parameters.getOrderColumn();
        String descriptionColumn = parameters.getCategoryColumn();
        String sortTitlesStr = null;
        if( sortColName != null )
        {

            if( parameters.isSortDescending() )
                sortColName = "desc(`" + sortColName + "`)";
            else
                sortColName = "`" + sortColName + "`";
            sortTitlesStr = sortColName;
        }
        else
            sortColName = "`" + descriptionColumn + "`";
        String xColumn = parameters.getValuesColumn();
        String sizeColumn = parameters.getCircleSizeColumn();
        String colorColumn = parameters.getCircleColorColumn();
        String fillColor = colorColumn == null ? "#00abff":colorColumn;

        StringBuilder script = new StringBuilder();
        script.append( "suppressPackageStartupMessages({\n"
                + "  library(ggplot2)\n"
                + "  library(dplyr)\n"
                + "  library(scales)\n"
                + "})\n"
                + "# ---- 1. Read the results file ----\n"
                + "gsea_res <- read.delim(\"" + inputFile.getAbsolutePath() + "\",\n"
                + "                       sep = \"\\t\",\n"
                + "                       stringsAsFactors = FALSE,\n"
                + "                       check.names = FALSE)\n"
                + "# The colorbar tick labels anchor to the data's own P-value extent (which equals the\n"
                + "# gradient's limits): the two ends are always labeled, and the middle is filled with evenly\n"
                + "# spaced, decade-aligned ticks. This keeps ticks spread across the whole bar so none land on\n"
                + "# the (clipped) tip and none overlap. A tight range (< ~2 decades) falls back to min/max in\n"
                + "# plain scientific notation.\n"
                + "pvalue_breaks <- function(pv, target = 3) {\n"
                + "  lo <- min(pv, na.rm = TRUE)\n"
                + "  hi <- max(pv, na.rm = TRUE)\n"
                + "  if (!is.finite(lo) || !is.finite(hi) || hi <= lo || lo <= 0)\n"
                + "    return(list(breaks = c(lo, hi), formatter = format))\n"
                + "  span <- log10(hi / lo)\n"
                + "  if (span < 2)\n"
                + "    return(list(breaks = c(lo, hi), formatter = function(x) format(x, scientific = TRUE, digits = 2)))\n"
                + "  step <- max(1, round(span / (target - 1)))\n"
                + "  lb <- log10(lo)\n"
                + "  ub <- log10(hi)\n"
                + "  mids <- seq(floor(lb) + step, ceiling(ub) - step + 1, by = step)\n"
                + "  mids <- mids[mids > lb & mids < ub]\n"
                + "  b <- sort(c(lo, 10 ^ mids, hi))\n"
                + "  fmt <- function(x) {\n"
                + "    vapply(x, function(v) {\n"
                + "      lx <- log10(v)\n"
                + "      if (abs(lx - round(lx)) < 1e-9) as.character(format_log(v)) else format(v, scientific = TRUE, digits = 1)\n"
                + "    }, character(1))\n"
                + "  }\n"
                + "  list(breaks = b, formatter = fmt)\n"
                + "}\n" );

        script.append( "# ---- 2. Sort and limit to top categories ----\n"
                + "showCategory <- " +parameters.getNumTopCategories()+ "\n"
                + "gsea_res <- gsea_res %>%\n");

        script.append( "  arrange(" + sortColName + ") %>%\n"
                + "  head(showCategory)" );
        if( sortTitlesStr != null )
        {
            script.append( "%>%\n" );
            script.append( "mutate (`" + descriptionColumn + "` = reorder(`" + descriptionColumn + "`, " + sortTitlesStr + "))\n" );
        }
        else
            script.append( "\n" );
        String xValue = "`" + xColumn + "`";
        String xLabel = xColumn;
        if( parameters.isNeedLog() )
        {
            xValue = "-log10(" + xValue + ")";
            xLabel = xColumn + " (-log10)";
        }
        int labelSize = Math.max( 6, Math.min( 10, 120 / parameters.getNumTopCategories() ) );
        script.append( "# ---- 3. Draw the plot and assign it to a variable 'p' ----\n" );
        String colorExpr = "\"black\"";
        String strokeExpr = "0.5";
        String colorInAes = "", strokeInAes = "";
        if( parameters.getMarkColumn() != null )
        {
            // Rows marked by the flag column get a bold green outline; the rest keep the default black.
            // The flag column itself is NOT reassigned: the ifelse below reads it in the same
            // mutate() call, which dplyr does not allow (a column is re-read as its original value).
            String flag = parameters.getMarkColumn();
            script.append( "gsea_res <- gsea_res %>%\n"
                    + "  mutate(mark_color = ifelse(`" + flag + "` %in% c(TRUE, \"TRUE\", \"true\"), \"green\", \"black\"),\n"
                    + "         mark_stroke = ifelse(`" + flag + "` %in% c(TRUE, \"TRUE\", \"true\"), 1.0, 0.5))\n" );
            // The mapping MUST go inside aes(): a bare "color=mark_color" outside aes() is
            // evaluated as a constant in the script environment, where no such object exists
            // (fails with "object 'mark_color' not found" on R 4.3 / ggplot2 4.0), and it would
            // also bypass the color scale so the legend would not match the drawn outlines.
            colorInAes = ", color = mark_color";
            strokeInAes = ", stroke = mark_stroke";
        }
        // Data-driven colorbar ticks (see the pvalue_breaks() helper emitted before the read step).
        // Must be a standalone statement BEFORE the "p <- ggplot(...) + ... + ..." chain starts:
        // an assignment cannot appear in the middle of a "+" chain (R would parse "p + pvBreaks <- ...").
        if(colorColumn != null)
            script.append( "pvBreaks <- pvalue_breaks(gsea_res$`" + colorColumn + "`)\n" );

        script.append( "p <- ggplot(gsea_res, aes(x = " + xValue + ", y = `" + descriptionColumn + "`)) +\n"
                + "  geom_point(aes(size = `" + sizeColumn + "`, fill = `" + fillColor + "`" + colorInAes + strokeInAes + "),shape=21"
                + ( parameters.getMarkColumn() == null ? ",color=" + colorExpr + ",stroke=" + strokeExpr : "" ) + ") +\n" );

        if(colorColumn != null)
        {
            // Log-scaled color gradient: the values (e.g. P-values) span many orders of magnitude,
            // so a linear gradient compresses most points into the same color end. Scaling the fill
            // by log10 spreads the colors across decades. limits == the data extent, matching the
            // anchored colorbar breaks from pvalue_breaks().
            script.append( "  scale_fill_gradientn(\n" + "    name    = \"" + colorColumn + "\",\n"
                + "    colours = c(\"red\", \"blue\"),\n"
                + "    trans   = \"log10\",\n"
                + "    limits  = c(min(gsea_res$`" + colorColumn + "`), max(gsea_res$`" + colorColumn + "`)),\n"
                + "    breaks  = pvBreaks$breaks,\n"
                + "    labels  = pvBreaks$formatter,\n"
                + "    guide   = guide_colorbar(reverse = TRUE)) + \n" );
        }
        script.append( "scale_x_continuous(expand = expansion(mult = c(0.15, 0.15))) +\n" );
        script.append( " scale_size_continuous(name = \"" + sizeColumn + "\", range = c(2, 8)) +\n"
        + "  labs(\n" + "    title = NULL,\n"
                + "    x     = \"" + xLabel + "\",\n"
                + "    y     = NULL\n" + "  ) +\n"
                + "  scale_y_discrete(labels = scales::wrap_format(width = 40)) +\n"
                + "  theme_minimal(base_size = " + labelSize + ") +\n"
                + "  theme(\n"
                + "    axis.text.y   = element_text(size = " + labelSize + ", hjust = 1, vjust = 0.5),\n"
                + "    legend.text   = element_text(size = " + Math.max( 6, labelSize - 1 ) + "),\n"
                + "    legend.title  = element_text(size = " + Math.max( 6, labelSize - 1 ) + "),\n"
                + "    panel.grid.minor = element_blank()\n" + "  )" );
        if( parameters.getMarkColumn() != null )
        {
            // Only the green (marked) entry is shown in the legend; the unmarked black outline
            // carries no information of its own, so the "black" break is dropped.
            script.append( "+  scale_color_manual(name = NULL,\n"
                    + "    values = c(green = \"green\", black = \"black\"),\n"
                    + "    breaks = \"green\",\n"
                    + "    labels = \"" + parameters.getMarkColumn() + "\",\n"
                    + "    guide = guide_legend(override.aes = list(fill = NA), nrow = 1, byrow = TRUE))\n" );
        }
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
        private String valuesColumn, categoryColumn, orderColumn, circleSizeColumn, circleColorColumn, markColumn;
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

        @PropertyName("Additional mark column")
        @PropertyDescription("Optional boolean column: rows marked TRUE get a bold green circle outline (e.g. categories found in the disease GO classification).")
        public String getMarkColumn()
        {
            return markColumn;
        }

        public void setMarkColumn(String markColumn)
        {
            String oldValue = this.markColumn;
            this.markColumn = markColumn;
            firePropertyChange( "markColumn", oldValue, markColumn );
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
            add( ColumnNameSelector.registerSelector( "markColumn", beanClass, "inputTable", true ) );
            property( "sortDescending" ).hidden( "hideSort" ).add();

            add( "numTopCategories" );
            addExpert( "imageWidth" );
            addExpert( "imageHeight" );
            property( "dpi" ).tags( bean -> IntStreamEx.of( 72, 150, 300 ).mapToObj( String::valueOf ) ).expert().add();
            property( "outputChart" ).outputElement( ImageDataElement.class ).auto( "$inputTable$ dot plot" ).add();
        }
    }
}
