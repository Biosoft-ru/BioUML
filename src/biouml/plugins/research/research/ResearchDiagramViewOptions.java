package biouml.plugins.research.research;

import java.awt.Color;

import ru.biosoft.graphics.Brush;

import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.model.DiagramViewOptions;

/**
 * View options for research diagrams
 */
@PropertyName("View options")
public class ResearchDiagramViewOptions extends DiagramViewOptions
{
    public ResearchDiagramViewOptions(Option parent)
    {
        super(parent);
        diagramTitleVisible = false;
    }

    protected Brush deBrush = new Brush(new Color(176, 196, 222));
    public Brush getDeBrush()
    {
        return deBrush;
    }
    public void setDeBrush(Brush deBrush)
    {
        Brush oldValue = this.deBrush;
        this.deBrush = deBrush;
        firePropertyChange("deBrush", oldValue, deBrush);
    }

    protected Brush experimentBrush = new Brush(new Color(255, 200, 255));
    public Brush getExperimentBrush()
    {
        return experimentBrush;
    }
    public void setExperimentBrush(Brush experimentBrush)
    {
        Brush oldValue = this.experimentBrush;
        this.experimentBrush = experimentBrush;
        firePropertyChange("experimentBrush", oldValue, experimentBrush);
    }

    protected Brush resultBrush = new Brush(new Color(255, 255, 200));
    public Brush getResultBrush()
    {
        return resultBrush;
    }
    public void setResultBrush(Brush resultBrush)
    {
        Brush oldValue = this.resultBrush;
        this.resultBrush = resultBrush;
        firePropertyChange("resultBrush", oldValue, resultBrush);
    }

    protected Brush analysisTableBrush = new Brush(new Color(255, 228, 196));
    public Brush getAnalysisTableBrush()
    {
        return analysisTableBrush;
    }
    public void setAnalysisTableBrush(Brush analysisTableBrush)
    {
        Brush oldValue = this.analysisTableBrush;
        this.analysisTableBrush = analysisTableBrush;
        firePropertyChange("analysisTableBrush", oldValue, analysisTableBrush);
    }

    protected Brush analysisBrush = new Brush(new Color(238, 162, 173));
    public Brush getAnalysisBrush()
    {
        return analysisBrush;
    }
    public void setAnalysisBrush(Brush analysisBrush)
    {
        Brush oldValue = this.analysisBrush;
        this.analysisBrush = analysisBrush;
        firePropertyChange("analysisBrush", oldValue, analysisBrush);
    }

    protected Brush importBrush = new Brush(new Color(235,245,33));
    public Brush getImportBrush()
    {
        return importBrush;
    }
    public void setImportBrush(Brush importBrush)
    {
        Object oldValue = this.importBrush;
        this.importBrush = importBrush;
        firePropertyChange("importBrush", oldValue, this.importBrush);
    }

    protected Brush scriptBrush = new Brush(new Color(255, 255, 200));
    public Brush getScriptBrush()
    {
        return scriptBrush;
    }
    public void setScriptBrush(Brush scriptBrush)
    {
        Brush oldValue = this.scriptBrush;
        this.scriptBrush = scriptBrush;
        firePropertyChange("scriptBrush", oldValue, scriptBrush);
    }

    protected Brush sqlBrush = new Brush(new Color(200, 255, 200));
    public Brush getSqlBrush()
    {
        return sqlBrush;
    }
    public void setSqlBrush(Brush sqlBrush)
    {
        Brush oldValue = this.sqlBrush;
        this.sqlBrush = sqlBrush;
        firePropertyChange("sqlBrush", oldValue, sqlBrush);
    }
    
    protected Brush diagramBrush = new Brush(new Color(205, 241, 253));
    public Brush getDiagramBrush()
    {
        return diagramBrush;
    }
    public void setDiagramBrush(Brush diagBrush)
    {
        Brush oldValue = this.diagramBrush;
        this.diagramBrush = diagBrush;
        firePropertyChange("diagramBrush", oldValue, diagBrush);
    }
}
