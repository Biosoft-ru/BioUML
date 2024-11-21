package biouml.plugins.virtualcell.diagram;

import java.awt.Color;

import ru.biosoft.graphics.Brush;
import biouml.model.DiagramViewOptions;

import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.annot.PropertyName;

@SuppressWarnings ( "serial" )
@PropertyName ( "View options" )
public class VirtualCellDiagramViewOptions extends DiagramViewOptions
{
    public VirtualCellDiagramViewOptions(Option parent)
    {
        super( parent );
        setDiagramTitleVisible( false );
    }

    private Brush processBrush = new Brush( new Color(190, 190, 255) );
    private Brush datasetBrush = new Brush( new Color(190, 190, 190) );

    @PropertyName ( "Process brush" )
    public Brush getProcessBrush()
    {
        return processBrush;
    }
    public void setProcessBrush(Brush processBrush)
    {
        this.processBrush = processBrush;
    }

    @PropertyName ( "Dataset brush" )
    public Brush getDatasetBrush()
    {
        return datasetBrush;
    }
    public void setDatasetBrush(Brush datasetBrush)
    {
        this.datasetBrush = datasetBrush;
    }

}