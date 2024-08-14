package biouml.plugins.gtrd.access;

import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.finder.TrackSummary;

public class GTRDTrackSummary extends TrackSummary
{

	public GTRDTrackSummary(Track track) 
	{
		super(track);
	}
	public GTRDTrackSummary(Track track, String name ) 
	{
		super(track, name);
	}

	private boolean showCellName, showTarget, showTreatment;
	public GTRDTrackSummary(Track track, boolean showTarget, boolean showTreatment, boolean showCellName ) 
	{
		super(track);
		this.showCellName = showCellName;
		this.showTarget = showTarget;
		this.showTreatment = showTreatment;
	}
	
	public GTRDTrackSummary(Track track, String name, boolean showTarget, boolean showTreatment, boolean showCellName ) 
	{
		super(track, name);
		this.showCellName = showCellName;
		this.showTarget = showTarget;
		this.showTreatment = showTreatment;
	}
	
	public boolean isCellNameHidden()
	{
		return !showCellName;
	}
	public boolean isTreatmentHidden()
	{
		return !showTreatment;
	}
	public boolean isTargetHidden()
	{
		return !showTarget;
	}
	
	private String treatment;
    @PropertyName("Treatment")
    public String getTreatment()
    {
        return treatment;
    }
    public void setTreatment(String treatment)
    {
        this.treatment = treatment;
    }
    
    private String target;
    @PropertyName("Target")
    public String getTarget()
    {
        return target;
    }
    public void setTarget(String target)
    {
        this.target = target;
    }
    
    private String cellName;
    @PropertyName("Cell name")
    public String getCellName()
    {
        return cellName;
    }
    public void setCellName(String cellName)
    {
        this.cellName = cellName;
    }
}
