package ru.biosoft.table.access;

import java.util.List;
import java.util.ListResourceBundle;
import java.util.logging.Logger;

import com.developmentontheedge.beans.Option;

import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.subaction.BackgroundDynamicAction;
import ru.biosoft.jobcontrol.AbstractJobControl;
import ru.biosoft.jobcontrol.JobControl;
import ru.biosoft.jobcontrol.JobControlException;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.util.bean.BeanInfoEx2;

@SuppressWarnings ( "serial" )
public class AddRowTableAction extends BackgroundDynamicAction
{
    @Override
    public boolean isApplicable(Object object)
    {
        if( object instanceof TableDataCollection )
        {
            return true;
        }
        return false;
    }

    @Override
    public JobControl getJobControl(final Object model, final List<DataElement> selectedItems, final Object properties) throws Exception
    {
        return new AddRowJob( log, model, properties );
    }

    @Override
    public AddRowProperties getProperties(Object model, List<DataElement> selectedItems)
    {
        return new AddRowProperties((TableDataCollection)model);
    }

    private static class AddRowJob extends AbstractJobControl
    {
        private final Object model;
        private final Object properties;
        private AddRowJob(Logger log, Object model, Object properties)
        {
            super( log );
            this.model = model;
            this.properties = properties;
        }
        @Override
        protected void doRun() throws JobControlException
        {
            try
            {
                AddRowProperties parameters = (AddRowProperties)properties;

                String[] rowNames = parameters.getRowNames();

                TableDataCollection table = (TableDataCollection)model;

                int size = rowNames.length;
                for (int i=0; i<size; i++)
                {
                    setPreparedness(100*i/size);

                    TableDataCollectionUtils.addRow( table, rowNames[i], table.columns().map( c -> c.getType().getDefaultValue() )
                            .toArray(), true );
                }

                table.finalizeAddition();
                CollectionFactoryUtils.save(table);
                setPreparedness(100);
                resultsAreReady(new Object[]{table});

            }
            catch( Exception e )
            {
                throw new JobControlException(e);
            }
        }
    }

    public static class AddRowProperties extends Option
    {
        public AddRowProperties(TableDataCollection tdc)
        {
            startIndex = 0;
            this.tdc = tdc;
            rowNames = new String[]{getUniqueName("New row ")};
        }

        private int startIndex = 0;
        private final TableDataCollection tdc;
        private String[] rowNames;

        /**
         * @return the from
         */
        public String[] getRowNames()
        {
            return rowNames;
        }
        /**
         * @param from the from to set
         */
        public void setRowNames(String[] rowNames)
        {
            validateRowNames(rowNames);
            Object oldValue = this.rowNames;
            this.rowNames = rowNames;
            firePropertyChange("rowNames", oldValue, rowNames);
        }

        private void validateRowNames(String[] rowNames)
        {
            for (int i=0; i<rowNames.length; i++)
            {
                if (rowNames[i].isEmpty() || tdc.contains( rowNames[i] ))
                {
                    rowNames[i] =  getUniqueName("New row ");
                }
            }
        }

        private String getUniqueName(String baseName)
        {
            String result = baseName + startIndex;
            while( tdc.contains( result ) )
            {
                startIndex++;
                result = baseName + startIndex;
            }
            startIndex++;
            return result;
        }
    }

    public static class AddRowPropertiesBeanInfo extends BeanInfoEx2<AddRowProperties>
    {
        public AddRowPropertiesBeanInfo()
        {
            super(AddRowProperties.class, AddRowPropertiesMessageBundle.class.getName());
        }

        @Override
        protected void initProperties() throws Exception
        {
            property("rowNames").title( "PN_ROW_NAMES" ).description( "PD_ROW_NAMES" ).add();
        }
    }

    public static class AddRowPropertiesMessageBundle extends ListResourceBundle
    {
        @Override
        protected Object[][] getContents()
        {
            return new Object[][] {
                {"PN_ROW_NAMES", "Row names"},
                {"PD_ROW_NAMES", "Row names."},
            };
        }
    }
}
