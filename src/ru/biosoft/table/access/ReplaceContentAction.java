package ru.biosoft.table.access;

import java.util.Iterator;
import java.util.List;
import java.util.ListResourceBundle;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.subaction.BackgroundDynamicAction;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.TableColumn;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.datatype.DataType;
import ru.biosoft.util.TextUtil;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.PropertyDescriptorEx;
import ru.biosoft.jobcontrol.AbstractJobControl;
import ru.biosoft.jobcontrol.JobControl;
import ru.biosoft.jobcontrol.JobControlException;

public class ReplaceContentAction extends BackgroundDynamicAction
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
        return new AbstractJobControl(log){
            @Override
            protected void doRun() throws JobControlException
            {
                try
                {
                    ReplaceProperties parameters = (ReplaceProperties)properties;
                    if(parameters.getFrom().equals(""))
                        throw new IllegalArgumentException("Empty search string entered: cannot continue");
                    if(parameters.getFrom().equals(parameters.getTo()))
                        throw new IllegalArgumentException("Replacement equals to search string");
                    TableDataCollection tdc = (TableDataCollection)model;
                    List<DataType> types = tdc.columns().map( TableColumn::getType ).toList();
                    Iterator<? extends DataElement> itemsIterator = parameters.isSelectionOnly()?selectedItems.iterator():tdc.iterator();
                    int size = parameters.isSelectionOnly()?selectedItems.size():tdc.getSize();
                    int i=0;
                    while(itemsIterator.hasNext())
                    {
                        setPreparedness(100*(i++)/size);
                        DataElement de = itemsIterator.next();
                        if(!(de instanceof RowDataElement)) continue;
                        RowDataElement row = (RowDataElement)de;
                        Object[] values = row.getValues();
                        boolean rowChanged = false;
                        for(int j=0; j<values.length; j++)
                        {
                            String value = TextUtil.toString(values[j]);
                            if(parameters.isExactMatch())
                            {
                                if(value.equals(parameters.getFrom()))
                                {
                                    values[j] = TextUtil.fromString(types.get(j).getType(), parameters.getTo());
                                    rowChanged = true;
                                }
                            } else
                            {
                                if(value.contains(parameters.getFrom()))
                                {
                                    value = value.replace(parameters.getFrom(), parameters.getTo());
                                    values[j] = TextUtil.fromString(types.get(j).getType(), value);
                                    rowChanged = true;
                                }
                            }
                        }
                        if(rowChanged)
                        {
                            tdc.put(row);
                        }
                    }
                    if(parameters.isSelectionOnly())
                    setPreparedness(100);
                    resultsAreReady(new Object[]{model});
                }
                catch( Exception e )
                {
                    throw new JobControlException(e);
                }
            }
        };
    }

    @Override
    public Object getProperties(Object model, List<DataElement> selectedItems)
    {
        return new ReplaceProperties(selectedItems != null && !selectedItems.isEmpty());
    }
    
    public static class ReplaceProperties extends Option
    {
        private String from = "", to = "";
        private boolean exactMatch;
        private boolean selectionOnly;
        private boolean hasSelection;
        
        public ReplaceProperties(boolean hasSelection)
        {
            this.selectionOnly = this.hasSelection = hasSelection;
        }

        /**
         * @return the from
         */
        public String getFrom()
        {
            return from;
        }
        /**
         * @param from the from to set
         */
        public void setFrom(String from)
        {
            Object oldValue = this.from;
            this.from = from;
            firePropertyChange("from", oldValue, from);
        }
        /**
         * @return the to
         */
        public String getTo()
        {
            return to;
        }
        /**
         * @param to the to to set
         */
        public void setTo(String to)
        {
            Object oldValue = this.to;
            this.to = to;
            firePropertyChange("to", oldValue, to);
        }
        /**
         * @return the exactMatch
         */
        public boolean isExactMatch()
        {
            return exactMatch;
        }
        /**
         * @param exactMatch the exactMatch to set
         */
        public void setExactMatch(boolean exactMatch)
        {
            Object oldValue = this.exactMatch;
            this.exactMatch = exactMatch;
            firePropertyChange("exactMatch", oldValue, exactMatch);
        }
        /**
         * @return the selectionOnly
         */
        public boolean isSelectionOnly()
        {
            return selectionOnly;
        }
        /**
         * @param selectionOnly the selectionOnly to set
         */
        public void setSelectionOnly(boolean selectionOnly)
        {
            Object oldValue = this.selectionOnly;
            this.selectionOnly = selectionOnly;
            firePropertyChange("selectionOnly", oldValue, selectionOnly);
        }
        
        public boolean isSelectionOnlyHidden()
        {
            return !hasSelection;
        }
    }
    
    public static class ReplacePropertiesBeanInfo extends BeanInfoEx
    {
        public ReplacePropertiesBeanInfo()
        {
            super(ReplaceProperties.class, ReplacePropertiesMessageBundle.class.getName());
        }

        @Override
        protected void initProperties() throws Exception
        {
            add(new PropertyDescriptorEx("from", beanClass), getResourceString("PN_FROM"), getResourceString("PD_FROM"));
            add(new PropertyDescriptorEx("to", beanClass), getResourceString("PN_TO"), getResourceString("PD_TO"));
            add(new PropertyDescriptorEx("exactMatch", beanClass), getResourceString("PN_EXACT_MATCH"), getResourceString("PD_EXACT_MATCH"));
            PropertyDescriptorEx pde = new PropertyDescriptorEx("selectionOnly", beanClass);
            pde.setHidden(beanClass.getMethod("isSelectionOnlyHidden"));
            add(pde, getResourceString("PN_SELECTION_ONLY"), getResourceString("PD_SELECTION_ONLY"));
        }
    }
    
    public static class ReplacePropertiesMessageBundle extends ListResourceBundle
    {
        @Override
        protected Object[][] getContents()
        {
            return new Object[][] {
                {"PN_FROM", "Search string"},
                {"PD_FROM", "Type text you want to replace"},
                {"PN_TO", "Replacement"},
                {"PD_TO", "Type new text you want to add instead of old one"},
                {"PN_EXACT_MATCH", "Exact match"},
                {"PD_EXACT_MATCH", "Check if you want to replace only cells with exact match"},
                {"PN_SELECTION_ONLY", "Selection only"},
                {"PD_SELECTION_ONLY", "Check if you want to replace only in selected rows"},
            };
        }
    }
}
