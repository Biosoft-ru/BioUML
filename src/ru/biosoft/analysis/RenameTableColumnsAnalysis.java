package ru.biosoft.analysis;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.table.ColumnModel;
import ru.biosoft.table.TableColumn;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.exception.TableNoColumnException;
import ru.biosoft.util.bean.BeanInfoEx2;

public class RenameTableColumnsAnalysis extends AnalysisMethodSupport<RenameTableColumnsAnalysis.RenameTableColumnsParameters>
{

    public RenameTableColumnsAnalysis(DataCollection<?> origin, String name)
    {
        super(origin, name, new RenameTableColumnsParameters());
    }

    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        TableDataCollection table = getParameters().getInputPath().getDataElement(TableDataCollection.class);
        TableDataCollection result;
        if ( parameters.isUseSameTable() )
            result = table;
        else
        {
            DataElementPath outputPath = parameters.getOutputPath();
            result = table.clone(outputPath.getParentCollection(), outputPath.getName());
            outputPath.getParentCollection().put(result);
        }
        ColumnModel cm = result.getColumnModel();
        String[] oldNames = getParameters().getOldNames().split(";");
        String[] newNames = getParameters().getNewNames().split(";");
        if ( oldNames.length != newNames.length )
            log.info("Column counts don't match, not all will be renamed");
        int num = Math.min(oldNames.length, newNames.length);
        StringBuilder sb = new StringBuilder();
        for ( int i = 0; i < num; i++ )
        {
            String oldName = oldNames[i];
            try
            {
                String newName = newNames[i].trim();

                TableColumn col = cm.getColumn(oldName);
                if ( newName.isEmpty() )
                {
                    log.info("Empty new column name is not allowed, skip renaming for column " + newName);
                    continue;
                }
                if ( cm.hasColumn(newName) )
                {
                    log.info("Column with name " + newName + " already exist, skip renaming");
                    continue;
                }
                col.setName(newName);
                sb.append(oldName + " => " + newName + "\n");
            }
            catch (TableNoColumnException e)
            {
                log.info("Column " + oldName + " does not found");
            }
        }
        if ( sb.length() > 0 )
            log.info("Renamed\n" + sb.toString());
        CollectionFactoryUtils.save(result);
        return result;
    }


    public static class RenameTableColumnsParameters extends AbstractAnalysisParameters
    {
        private DataElementPath inputPath, outputPath;
        private String oldNames, newNames;
        private boolean useSameTable = false;

        @PropertyName("Input table") 
        @PropertyDescription("Table where to rename columns")
        public DataElementPath getInputPath()
        {
            return inputPath;
        }

        public void setInputPath(DataElementPath table)
        {
            DataElementPath oldValue = this.inputPath;
            this.inputPath = table;
            firePropertyChange("table", oldValue, this.inputPath);
        }

        @PropertyName("Old names") 
        @PropertyDescription("Old column names separated by semicolon (;)")
        public String getOldNames()
        {
            return oldNames;
        }

        public void setOldNames(String oldNames)
        {
            String oldValue = this.oldNames;
            this.oldNames = oldNames;
            firePropertyChange("oldNames", oldValue, this.oldNames);
        }

        @PropertyName("New names")
        @PropertyDescription("New column names separated by semicolon (;) in the same order as old names")
        public String getNewNames()
        {
            return newNames;
        }

        public void setNewNames(String newNames)
        {
            String oldValue = this.newNames;
            this.newNames = newNames;
            firePropertyChange("newNames", oldValue, this.newNames);
        }

        @PropertyName("Use same table")
        @PropertyDescription("If true, columns will be renamed in input table.")
        public boolean isUseSameTable()
        {
            return useSameTable;
        }

        public void setUseSameTable(boolean useSameTable)
        {
            boolean oldValue = this.useSameTable;
            this.useSameTable = useSameTable;
            firePropertyChange("useSameTable", oldValue, useSameTable);
        }

        @PropertyName("Output table")
        @PropertyDescription("Path to newly created table with renamed column(s)")
        public DataElementPath getOutputPath()
        {
            return outputPath;
        }

        public void setOutputPath(DataElementPath outputPath)
        {
            Object oldValue = this.outputPath;
            this.outputPath = outputPath;
            firePropertyChange("outputPath", oldValue, outputPath);
        }

    }

    public static class RenameTableColumnsParametersBeanInfo extends BeanInfoEx2<RenameTableColumnsParameters>
    {
        public RenameTableColumnsParametersBeanInfo()
        {
            super(RenameTableColumnsParameters.class);
        }

        @Override
        public void initProperties() throws Exception
        {
            add(DataElementPathEditor.registerInput("inputPath", beanClass, TableDataCollection.class));
            add("oldNames");
            add("newNames");
            add("useSameTable");
            property("outputPath").outputElement(TableDataCollection.class).hidden("isUseSameTable").auto("$inputPath$ renamed").add();
        }
    }

}
