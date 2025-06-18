package ru.biosoft.analysis._test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import ru.biosoft.jobcontrol.ClassJobControl;
import ru.biosoft.jobcontrol.JobControl;
import ru.biosoft.jobcontrol.JobControlEvent;
import ru.biosoft.jobcontrol.JobControlListenerAdapter;

import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.VectorDataCollection;
import ru.biosoft.analysiscore.AnalysisMethod;
import ru.biosoft.analysiscore.AnalysisParameters;
import ru.biosoft.table.ColumnModel;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.StandardTableDataCollection;
import ru.biosoft.table.TableColumn;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.table.datatype.DataType;
import ru.biosoft.util.BeanUtil;
import com.developmentontheedge.beans.util.Beans.ObjectPropertyAccessor;
import ru.biosoft.util.TextUtil2;

/**
 * @author lan
 *
 */
public class AnalysisTestExecutor
{
    private final static class JobListener extends JobControlListenerAdapter
    {
        private String error = null;
        
        public String getError()
        {
            return error;
        }
        
        @Override
        public void jobTerminated(JobControlEvent event)
        {
            if(event.getStatus() == JobControl.TERMINATED_BY_ERROR)
                error = event.getMessage();
        }
    }

    private static class Row
    {
        private final String name;
        private final String[] values;
        
        private Row(String name, String[] values)
        {
            this.name = name;
            this.values = values;
        }
        
        public String getName()
        {
            return name;
        }

        public String[] getValues()
        {
            return values;
        }
        
        @Override
        public String toString()
        {
            StringBuilder sb = new StringBuilder();
            sb.append(name).append(": ");
            if(values != null)
            {
                sb.append( String.join( ",", values ) );
            }
            return sb.toString();
        }
        
        public static Row readRow(BufferedReader is)
        {
            String row = null;
            try
            {
                row = is.readLine();
            }
            catch( IOException e )
            {
            }
            if(row == null) return null;
            String[] fields = row.split("\t");
            if(fields.length == 0) return new Row(null, null);
            String[] values = new String[fields.length-1];
            System.arraycopy(fields, 1, values, 0, values.length);
            return new Row(fields[0], values);
        }
    }
    
    private static class Section
    {
        private final String type;
        private final String name;
        private final List<Row> rows;
        private final int startPos, endPos;
        
        private Section(String type, String name, List<Row> rows, int startPos, int endPos)
        {
            this.type = type;
            this.name = name;
            this.rows = rows;
            this.startPos = startPos;
            this.endPos = endPos;
        }
        
        public int getStartPos()
        {
            return startPos;
        }

        public int getEndPos()
        {
            return endPos;
        }

        public String getType()
        {
            return type;
        }
        
        public String getName()
        {
            return name;
        }
        
        public Row getRow(int i)
        {
            return rows.get(i);
        }
        
        public int getRowCount()
        {
            return rows.size();
        }
        
        @Override
        public String toString()
        {
            return getName() == null ? getType() : getType()+": "+getName();
        }
        
        public static Section readSection(BufferedReader is, int startPos)
        {
            Row startRow = null;
            do
            {
                startRow = Row.readRow(is);
                startPos++;
                if(startRow == null) return null;
            } while(startRow.getName() == null || !startRow.getName().startsWith("="));
            int endPos = startPos;
            String type = startRow.getName().substring(1);
            String name = startRow.getValues() == null || startRow.getValues().length == 0 ? null : startRow.getValues()[0];
            List<Row> rows = new ArrayList<>();
            while(true)
            {
                Row row = Row.readRow(is);
                endPos++;
                if(row == null) return null;
                if(row.getName() == null || row.getName().trim().startsWith("#")) continue;
                if(row.getName().equals("=End")) break;
                rows.add(row);
            }
            return new Section(type, name, rows, startPos, endPos);
        }
    }
    
    List<Section> sections = new ArrayList<>();
    String name;
    private String repositoryPath = "../data";
    
    public AnalysisTestExecutor(URL url) throws IOException
    {
        this(url.toString(), url.openStream());
    }
    
    public AnalysisTestExecutor(String name, InputStream is) throws IOException
    {
        this.name = name;
        try(BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8)))
        {
            int pos = 0;
            while(true)
            {
                Section section = Section.readSection(br, pos);
                if(section == null) break;
                sections.add(section);
                pos = section.getEndPos();
            }
        }
    }
    
    public void setRepositoryPath(String path)
    {
        this.repositoryPath = path;
    }
    
    public void initEnvironment() throws Exception
    {
        CollectionFactory.createRepository( repositoryPath );
        // TODO: fixme: temporary solution
        DataCollection<TableDataCollection> root = new VectorDataCollection<>("test", StandardTableDataCollection.class, null);
        CollectionFactory.registerRoot(root);
    }
    
    public void closeEnvironment()
    {
        CollectionFactory.unregisterAllRoot();
    }
    
    public void execute() throws Exception
    {
        initEnvironment();
        try
        {
            for(Section section: sections)
            {
                executeSection(section);
            }
        }
        catch( Exception e )
        {
            throw new Exception(e.getMessage()+"\n\tin "+name, e);
        }
        finally
        {
            closeEnvironment();
        }
    }
    
    private void executeSection(Section section) throws Exception
    {
        try
        {
            if(section.getType().equals("LoadTable"))
                executeLoadTable(section);
            else if(section.getType().equals("Analysis"))
                executeAnalysis(section);
            else if(section.getType().equals("CompareTables"))
                executeCompareTables(section);
            else if(section.getType().equals("SetProperties"))
                executeSetProperties(section);
            else throw new IllegalArgumentException("Invalid section type");
        }
        catch( Exception e )
        {
            throw new Exception(e.getMessage()+"\n\tat row#"+section.getStartPos()+" ["+section.toString()+"]", e);
        }
    }

    private void executeSetProperties(Section section) throws Exception
    {
        DataCollection<?> dc = CollectionFactory.getDataCollection(section.getName());
        if(dc == null) throw new Exception("Cannot find collection "+section.getName());
        Properties properties = dc.getInfo().getProperties();
        for(int i=0; i<section.getRowCount(); i++)
        {
            Row row = section.getRow(i);
            properties.setProperty(row.getName(), row.getValues()[0]);
        }
        CollectionFactoryUtils.save(dc);
    }

    private void executeCompareTables(Section section) throws Exception
    {
        if(section.getRowCount() != 2)
            throw new IllegalArgumentException("Invalid section "+section.getName()+": must have 2 rows");
        TableDataCollection tdc1 = (TableDataCollection)CollectionFactory.getDataElement(section.getRow(0).getName());
        TableDataCollection tdc2 = (TableDataCollection)CollectionFactory.getDataElement(section.getRow(1).getName());
        if(tdc1 == null) throw new Exception("Table "+section.getRow(0).getName()+" doesn't exist");
        if(tdc2 == null) throw new Exception("Table "+section.getRow(1).getName()+" doesn't exist");
        try
        {
            ColumnModel model1 = tdc1.getColumnModel();
            ColumnModel model2 = tdc2.getColumnModel();
            if(model1.getColumnCount() != model2.getColumnCount()) throw new Exception("Number of columns differs: "+model1.getColumnCount()+" != "+model2.getColumnCount());
            for(int i=0; i<model1.getColumnCount(); i++)
            {
                TableColumn col1 = model1.getColumn(i);
                TableColumn col2 = model2.getColumn(i);
                if(!col1.getName().equals(col2.getName()))
                    throw new Exception("Name of column #"+i+" differs: "+col1.getName()+" != "+col2.getName());
                if(col1.getType() != col2.getType())
                    throw new Exception("Type of column #"+i+" differs: "+col1.getType()+" != "+col2.getType());
            }
            if(tdc1.getSize() != tdc2.getSize()) throw new Exception("Number of rows differs: "+tdc1.getSize()+" != "+tdc2.getSize());
            List<String> names1 = tdc1.getNameList();
            for(int i=0; i<names1.size(); i++)
            {
                RowDataElement rde1 = tdc1.get(names1.get(i));
                RowDataElement rde2 = tdc2.get(names1.get(i));
                if(rde2 == null) throw new Exception("Second table doesn't have row "+names1.get(i));
                if(rde1 == null) throw new Exception("Erroneos row in first table: "+names1.get(i));
                Object[] values1 = rde1.getValues();
                Object[] values2 = rde2.getValues();
                for(int j=0; j<model1.getColumnCount(); j++)
                {
                    Object value1 = values1[j];
                    Object value2 = values2[j];
                    if( value1 == null )
                    {
                        if( value2 == null )
                            continue;
                        throw new Exception("Row '" + names1.get(i) + "', col '" + model1.getColumn(j).getName()
                                + "': first value is null, second is not");
                    }
                    if( value2 == null )
                        throw new Exception("Row '" + names1.get(i) + "', col '" + model1.getColumn(j).getName()
                                + "': second value is null, first is not");
                    if(value1 instanceof Number && value2 instanceof Number)
                    {
                        if(Math.abs(((Number)value1).doubleValue()-((Number)value2).doubleValue()) > 0.00001)
                            throw new Exception("Row '"+names1.get(i)+"', col '"+model1.getColumn(j).getName()+"': "+value1.toString()+" != "+value2.toString());
                    } else if(!value1.equals(value2))
                        throw new Exception("Row '"+names1.get(i)+"', col '"+model1.getColumn(j).getName()+"': "+value1.toString()+" != "+value2.toString());
                }
            }
        }
        catch( Exception e )
        {
            throw new Exception("Tables "+tdc1.getCompletePath()+" and "+tdc2.getCompletePath()+" differ: "+e.getMessage());
        }
    }

    private void executeAnalysis(Section section) throws Exception
    {
        String analysisClassName = section.getName();
        Class<? extends AnalysisMethod> analysisClass = Class.forName(analysisClassName).asSubclass(AnalysisMethod.class);
        AnalysisMethod method;
        try
        {
            method = analysisClass.getConstructor(DataCollection.class, String.class).newInstance(null, analysisClassName);
        }
        catch( Exception e )
        {
            throw new Exception("Unable to instantiate analysis method "+analysisClassName);
        }
        AnalysisParameters parameters = method.getParameters();
        for(int i=0; i<section.getRowCount(); i++)
        {
            Row row = section.getRow(i);
            try
            {
                ObjectPropertyAccessor accessor = BeanUtil.getBeanPropertyAccessor( parameters, row.getName() );
                accessor.set( TextUtil2.fromString(accessor.getType(), row.getValues()[0]));
            }
            catch( Exception e )
            {
                throw new Exception("Unable to set value to property "+row.getName()+" (value: "+row.getValues()[0]+")", e);
            }
        }
        ClassJobControl jobControl = method.getJobControl();
        JobListener listener = new JobListener();
        jobControl.addListener(listener);
        method.getJobControl().run();
        if(listener.getError() != null) throw new Exception(listener.getError());
    }

    private void executeLoadTable(Section section) throws Exception
    {
        DataElementPath path = DataElementPath.create(section.getName());
        TableDataCollection tdc = TableDataCollectionUtils.createTableDataCollection(path);
        Row headers = section.getRow(0);
        ColumnModel columnModel = tdc.getColumnModel();
        for(String header: headers.getValues())
        {
            String name = header;
            DataType type = DataType.Text;
            int colonPos = header.indexOf(':');
            if(colonPos >= 0)
            {
                name = header.substring(0, colonPos);
                type = DataType.fromString( header.substring( colonPos + 1 ) );
            }
            columnModel.addColumn(name, type);
        }
        for(int i=1; i<section.getRowCount(); i++)
        {
            Row row = section.getRow(i);
            String[] strValues = row.getValues();
            Object[] values = new Object[columnModel.getColumnCount()];
            for(int j=0; j<values.length; j++)
            {
                if(strValues.length > j && !strValues[j].equals("(null)"))
                {
                    values[j] = columnModel.getColumn(j).getType().convertValue(strValues[j]);
                } else
                {
                    values[j] = null;
                }
            }
            TableDataCollectionUtils.addRow(tdc, row.getName(), values, true);
        }
        tdc.finalizeAddition();
        CollectionFactoryUtils.save(tdc);
    }
}
