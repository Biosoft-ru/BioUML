package biouml.plugins.physicell;

import com.developmentontheedge.beans.annot.PropertyName;

import biouml.plugins.simulation.Options;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.physicell.core.CellContainer;
import ru.biosoft.physicell.core.CellContainerExperimental;
import ru.biosoft.physicell.core.CellContainerParallel;

public class PhysicellOptions extends Options
{
    public static String DEFAULT_MODEL = "Default";
    public static String COVID_MODEL = "Covid";
    private String modelType = DEFAULT_MODEL;
    
    private DataElementPath resultPath;
    private double finalTime = 100;
    private boolean saveReport = false;
    private boolean saveDensity = true;
    private double reportInterval = 10;
    private boolean savePlots = false;
    private boolean saveImage = false;
    private boolean saveCellsText = true;
    private boolean saveCellsTable = false;
    private boolean saveGIF = false;
    private boolean saveVideo = false;
    private boolean parallelDiffusion = false;

    private String cellUpdateType = CellContainerParallel.PARALLEL_CONTAINER_NAME;

    private double imageInterval = 10;
    private double seed;
    private boolean useManualSeed = false;
    private double diffusionDt = 0.01;
    private double mechanicsDt = 0.1;
    private double phenotypeDt = 6;

    private boolean calculateGradient = true;
    private boolean trackInnerSubstrates = true;
    
    @PropertyName ( "Use manual seed" )
    public boolean isUseManualSeed()
    {
        return useManualSeed;
    }
    public void setUseManualSeed(boolean useManualSeed)
    {
        this.useManualSeed = useManualSeed;
    }

    @PropertyName ( "Manual seed" )
    public double getSeed()
    {
        return seed;
    }
    public void setSeed(double seed)
    {
        this.seed = seed;
    }

    @PropertyName ( "Save report" )
    public boolean isSaveReport()
    {
        return saveReport;
    }
    public void setSaveReport(boolean saveReport)
    {
        this.saveReport = saveReport;
    }
    
    @PropertyName ( "Save plots" )
    public boolean isSavePlots()
    {
        return savePlots;
    }
    public void setSavePlots(boolean savePlots)
    {
        this.savePlots = savePlots;
    }
    
    
    @PropertyName ( "Save density" )
    public boolean isSaveDensity()
    {
        return saveDensity;
    }
    public void setSaveDensity(boolean saveDensity)
    {
        this.saveDensity = saveDensity;
    }

    @PropertyName ( "Report interval" )
    public double getReportInterval()
    {
        return reportInterval;
    }
    public void setReportInterval(double reportInterval)
    {
        this.reportInterval = reportInterval;
    }

    @PropertyName ( "Save images" )
    public boolean isSaveImage()
    {
        return saveImage;
    }
    public void setSaveImage(boolean saveImage)
    {
        this.saveImage = saveImage;
    }
    
    @PropertyName ( "Save cells data" )
    public boolean isSaveCellsText()
    {
        return saveCellsText;
    }
    public void setSaveCellsText(boolean saveCellsText)
    {
        this.saveCellsText = saveCellsText;
    }
    
    @PropertyName ( "Model type" )
    public String getModelType()
    {
        return modelType;
    }
    public void setModelType(String type)
    {
        this.modelType = type;
    }
    
    @PropertyName ( "Save cells data as table" )
    public boolean isSaveCellsTable()
    {
        return saveCellsTable;
    }
    public void setSaveCellsTable(boolean saveCellsTable)
    {
        this.saveCellsTable = saveCellsTable;
    }

    @PropertyName ( "Save GIF" )
    public boolean isSaveGIF()
    {
        return saveGIF;
    }
    public void setSaveGIF(boolean saveGIF)
    {
        this.saveGIF = saveGIF;
    }

    @PropertyName ( "Save Video" )
    public boolean isSaveVideo()
    {
        return saveVideo;
    }
    public void setSaveVideo(boolean saveVideo)
    {
        this.saveVideo = saveVideo;
    }

    @PropertyName ( "Image interval" )
    public double getImageInterval()
    {
        return imageInterval;
    }
    public void setImageInterval(double imageInterval)
    {
        this.imageInterval = imageInterval;
    }

    @PropertyName ( "Result path" )
    public DataElementPath getResultPath()
    {
        return resultPath;
    }
    public void setResultPath(DataElementPath resultPath)
    {
        this.resultPath = resultPath;
    }

    @PropertyName ( "Max Time" )
    public double getFinalTime()
    {
        return finalTime;
    }
    public void setFinalTime(double finalTime)
    {
        this.finalTime = finalTime;
    }

    @PropertyName ( "Diffusion dt" )
    public double getDiffusionDt()
    {
        return diffusionDt;
    }
    public void setDiffusionDt(double diffusionDt)
    {
        this.diffusionDt = diffusionDt;
    }

    @PropertyName ( "Mechanics dt" )
    public double getMechanicsDt()
    {
        return mechanicsDt;
    }
    public void setMechanicsDt(double mechanicsDt)
    {
        this.mechanicsDt = mechanicsDt;
    }

    @PropertyName ( "Phenotype dt" )
    public double getPhenotypeDt()
    {
        return phenotypeDt;
    }
    public void setPhenotypeDt(double phenotypeDt)
    {
        this.phenotypeDt = phenotypeDt;
    }

    @PropertyName ( "Parallel diffusion simulation" )
    public boolean isParallelDiffusion()
    {
        return parallelDiffusion;
    }
    public void setParallelDiffusion(boolean parallelDiffusion)
    {
        this.parallelDiffusion = parallelDiffusion;
    }

    @PropertyName ( "Cell update type" )
    public String getCellUpdateType()
    {
        return cellUpdateType;
    }
    public void setCellUpdateType(String cellUpdateType)
    {
        this.cellUpdateType = cellUpdateType;
    }

    public String[] getCellUpdateTypes()
    {
        return new String[] {CellContainer.DEFAULT_NAME, CellContainerParallel.PARALLEL_CONTAINER_NAME,
                CellContainerExperimental.EXPERIMENTAL_CONTAINER_NAME};
    }
    
    
    @PropertyName("Track inner substrates in cells")
    public boolean isTrackInnerSubstrates()
    {
        return trackInnerSubstrates;
    }
    public void setTrackInnerSubstrates(boolean trackInnerSubstrates)
    {
        this.trackInnerSubstrates = trackInnerSubstrates;
    }
    
    @PropertyName("Recalculate gradients")
    public boolean isCalculateGradient()
    {
        return calculateGradient;
    }
    public void setCalculateGradient(boolean calculateGradient)
    {
        this.calculateGradient = calculateGradient;
    }
}