package ru.biosoft.workbench.editors;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.SystemColor;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.border.BevelBorder;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import one.util.streamex.StreamEx;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ru.biosoft.access.repository.JSONSerializable;
import ru.biosoft.util.TextUtil2;

import com.developmentontheedge.beans.editors.CustomEditorSupport;

/**
 * MultiSelect which can contain different sets of values
 * @see ru.biosoft.analysis.CELNormalizationParameters for usage example
 */
public class GenericMultiSelectEditor extends CustomEditorSupport implements JSONSerializable
{
    private JButton button = null;
    private MultiSelectPopup popup;
    boolean refresh = false;
    
    /**
     * Creates Editor or Renderer component
     * 
     * @param parent
     * @return Editor or Renderer component
     */
    private Component createComponent(Component parent)
    {
        // As getting list of items may be somewhat slow and it's called really often,
        // we will try to refresh only once per dialog
        refresh = true;
        if(parent instanceof JComponent)
        {
            String editorKey = this.getClass().getName()+"."+getDescriptor().getName();
            Object editor = ((JComponent)parent).getClientProperty(editorKey);
            if( editor != null )
                refresh = false;
            ((JComponent)parent).putClientProperty(editorKey, this);
        }
        
        button = new JButton("");
        button.setHorizontalAlignment(SwingConstants.LEFT);

        popup = new MultiSelectPopup(button);
        
        GenericMultiSelectItem value = getValueItem();
        if(value == null) return button;
        
        final String key = value.getKey();
        if(refresh)
        {
            if(!value.getKey().equals(""))
                value.updateAvailableValues();
        }
        Object[] vals = value.getAvailableValues();
        if(vals == null) return button;
        popup.getList().setListData(vals);
        refresh = false;

        popup.getList().setSelectedIndices(value.getIndexes());
        updateButtonText();

        popup.getList().addListSelectionListener(event -> doSet(key));

        return button;
    }
    
    /**
     * Returns text describing current selection
     */
    protected String getText(Object[] vals)
    {
        StringBuffer result = new StringBuffer();
        if(vals.length == 0)
        {
            return "(no selection)";
        }
        if(vals.length>1)
        {
            result.append("["+(String.valueOf(vals.length))+"] ");
        }
        for(int i=0; i<vals.length; i++)
        {
            if(i>0) result.append(", ");
            result.append(vals[i].toString());
            if(result.length() > 100) break;
        }
        return result.toString();
    }
    
    private void updateButtonText()
    {
        button.setText(getText(popup.getList().getSelectedValues()));
    }

    private void doSet(String key)
    {
        if(popup.getList().getSelectedValues() != null)
        {
            if(getValue() instanceof GenericMultiSelectItem)
                setValue(new GenericMultiSelectItem(key, popup.getList().getSelectedValues(), getValueItem().getAvailableValues()));
            else
                setValue(new GenericMultiSelectItem(key, popup.getList().getSelectedValues(), getValueItem().getAvailableValues()).getValues());
        }
        updateButtonText();
    }
    
    public void setStringValue(String[] val)
    {
        GenericMultiSelectItem value = getValueItem();
        if(getValue() instanceof GenericMultiSelectItem)
            setValue(new GenericMultiSelectItem(value.getKey(), val, value.getAvailableValues()));
        else
            setValue(new GenericMultiSelectItem(value.getKey(), val, value.getAvailableValues()).getValues());
    }
    
    protected Object[] getAvailableValues()
    {
        return null;
    }
    
    private GenericMultiSelectItem getValueItem()
    {
        Object valueObj = getValue();
        if(valueObj == null) valueObj = new Object[]{};
        GenericMultiSelectItem value = null;
        if(valueObj instanceof GenericMultiSelectItem)
        {
            value = (GenericMultiSelectItem)valueObj;
        }
        if(valueObj instanceof Object[])
        {
            Object[] valuesArray = (Object[])valueObj;
            String key = TextUtil2.nullToEmpty( (String)getDescriptor().getValue("key") );
            
            Object[] availVals = getAvailableValues();
            if( availVals == null )
                value = new GenericMultiSelectItem(key, valuesArray);
            else
                value = new GenericMultiSelectItem(key, valuesArray, availVals);
        }
        return value;
    }

    @Override
    public String[] getTags()
    {
        GenericMultiSelectItem value = getValueItem();
        if(value != null)
        {
            return StreamEx.of( value.getAvailableValues() ).map( Object::toString ).toArray( String[]::new );
        }
        return null;
    }

    @Override
    public Component getCustomRenderer(Component parent, boolean isSelected, boolean hasFocus)
    {
        return createComponent(parent);
/*        JLabel label = new JLabel(getText(getValueItem().getValues()));
        return label;*/
    }

    @Override
    public Component getCustomEditor(Component parent, boolean isSelected)
    {
        return createComponent(parent);
    }
    
    @SuppressWarnings ( "serial" )
    private static class MultiSelectPopup extends JPopupMenu
    {
        private JList<Object> listBox;
        private JScrollPane scrollPane;
        private Insets insets;
        private JButton button;
        // Used to correctly handle click on button, when popup is opened
        // TODO: implement more clean solution
        private static long deactivationTime = 0;

        public MultiSelectPopup(JButton button)
        {
            this.button = button;
            listBox = new JList<>();
            listBox.setBackground(SystemColor.control);
            listBox.setVisibleRowCount(20);
            scrollPane = new JScrollPane(listBox);
            scrollPane.setBorder(null);
            scrollPane.setFocusable(false);
            scrollPane.getVerticalScrollBar().setFocusable(false);
            setOpaque(false);
            add(scrollPane);
            setFocusable(false);
            setBorder(new BevelBorder(BevelBorder.LOWERED));
            insets = getBorder().getBorderInsets(this);
            addPopupMenuListener(new PopupMenuListener()
            {
                @Override
                public void popupMenuWillBecomeVisible(PopupMenuEvent arg0)
                {
                }

                @Override
                public void popupMenuWillBecomeInvisible(PopupMenuEvent arg0)
                {
                    deactivationTime = System.currentTimeMillis();
                }

                @Override
                public void popupMenuCanceled(PopupMenuEvent arg0)
                {
                }
            });
            button.addActionListener(ae -> {
                if( isVisible() || System.currentTimeMillis() - deactivationTime < 200 )
                    return;
                show();
            });
        }

        public JList<Object> getList()
        {
            return listBox;
        }

        @Override
        public void show()
        {
            if( listBox.getPreferredSize() == null || System.currentTimeMillis() - deactivationTime < 200 )
                return;
            setPreferredSize(new Dimension(button.getWidth(), Math.min(listBox.getPreferredScrollableViewportSize().height,
                    listBox.getPreferredSize().height)
                    + insets.top + insets.bottom));
            show(button, 0, button.getHeight());
            listBox.requestFocus();
        }

        @Override
        public void hide()
        {
            setVisible(false);
        }
    }

    @Override
    public JSONObject toJSON() throws JSONException
    {
        JSONObject result = new JSONObject();
        GenericMultiSelectItem item = getValueItem();
        JSONArray value = new JSONArray();
        if( item != null && item.getValues() != null )
        {
            for( Object val : item.getValues() )
            {
                value.put(val.toString());
            }
        }
        result.put("value", value);
        JSONArray dictionary = new JSONArray();
        if( item != null )
        {
            Object[] values = getValueItem().getAvailableValues();
            for( Object value2 : values )
                dictionary.put(value2.toString());
        }
        result.put("dictionary", dictionary);
        result.put("type", "multi-select");
        return result;
    }

    @Override
    public void fromJSON(JSONObject input) throws JSONException
    {
        JSONArray jsonArray = input.getJSONArray("value");
        String[] val = new String[jsonArray.length()];
        for( int index = 0; index < val.length; index++ )
            val[index] = jsonArray.getString(index);
        setStringValue(val);
    }

    @Override
    public void setAsText(String text) throws IllegalArgumentException
    {
        try
        {
            JSONArray array = new JSONArray(text);
            JSONObject input = new JSONObject();
            input.put("value", array);
            fromJSON(input);
        }
        catch( JSONException ex )
        {
            setStringValue(new String[] {text});
        }
    }
}