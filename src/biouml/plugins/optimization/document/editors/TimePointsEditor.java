package biouml.plugins.optimization.document.editors;

import java.awt.Component;
import java.util.logging.Logger;

import javax.swing.JLabel;
import one.util.streamex.DoubleStreamEx;
import one.util.streamex.IntStreamEx;
import one.util.streamex.StreamEx;

import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import biouml.plugins.optimization.OptimizationExperiment;
import biouml.plugins.optimization.ParameterConnection;

import com.developmentontheedge.beans.editors.CustomEditorSupport;

public class TimePointsEditor extends CustomEditorSupport
{
    @Override
    public String getAsText()
    {
        Integer intValue = (Integer)getValue();
        if( intValue == null )
            return "";
        return getTags()[intValue + 1];
    }

    @Override
    public Component getCustomRenderer(Component parent, boolean isSelected, boolean hasFocus)
    {
        return new JLabel(getAsText());
    }

    @Override
    public void setAsText(String text)
    {
        IntStreamEx.ofIndices( getTags(), text::equals ).findAny().ifPresent( i -> setValue(i-1) );
    }

    @Override
    public String[] getTags()
    {
        StreamEx<String> result = StreamEx.of("unspecified");

        OptimizationExperiment experiment = ( (ParameterConnection)getBean() ).getExperiment();

        TableDataCollection tdc = experiment.getTableSupport().getTable();
        if( experiment.isTimeCourse() && tdc != null )
        {
        	if(tdc.getColumnModel().hasColumn("time"))
        	{
                String time = experiment.getVariableNameInFile("time");
                double[] times = TableDataCollectionUtils.getColumn(tdc, time);
                result = DoubleStreamEx.of(times).mapToObj( Double::toString ).prepend( result );
        	}
        	else
        	{
        		Logger.getLogger( TimePointsEditor.class.getName() ).warning("The experimental file does not contain the column \"time\"");
        	}
        }
        return result.toArray(String[]::new);
    }

    @Override
    public boolean supportsCustomEditor()
    {
        return false;
    }
}
