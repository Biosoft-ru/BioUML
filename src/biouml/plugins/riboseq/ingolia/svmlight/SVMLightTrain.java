package biouml.plugins.riboseq.ingolia.svmlight;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import java.util.logging.Logger;

import biouml.plugins.riboseq.ingolia.ObservationList;
import biouml.plugins.riboseq.ingolia.svmlight.kernel.KernelOptions;
import biouml.plugins.riboseq.ingolia.svmlight.kernel.LinearKernel;
import ru.biosoft.access.security.CodePrivilege;
import ru.biosoft.access.security.CodePrivilegeType;
import ru.biosoft.util.TempFile;
import ru.biosoft.util.TempFiles;

@CodePrivilege(CodePrivilegeType.LAUNCH)
public class SVMLightTrain extends SVMLight implements Options
{
    private Integer verbosity;

    private LearningOptions learningOptions = new LearningOptions();

    private PerfomanceEstimationOptions perfomanceEstimationOptions = new PerfomanceEstimationOptions();
    
    private TransductionOptions transductionOptions = new TransductionOptions();
    
    private KernelOptions kernelOptions = new LinearKernel();
    
    private OptimizationOptions optimizationOptions = new OptimizationOptions();
    
    public Integer getVerbosity()
    {
        return verbosity;
    }

    public void setVerbosity(Integer verbosity)
    {
        this.verbosity = verbosity;
    }

    public LearningOptions getLearningOptions()
    {
        return learningOptions;
    }

    public void setLearningOptions(LearningOptions learningOptions)
    {
        this.learningOptions = learningOptions;
    }

    public PerfomanceEstimationOptions getPerfomanceEstimationOptions()
    {
        return perfomanceEstimationOptions;
    }

    public void setPerfomanceEstimationOptions(PerfomanceEstimationOptions perfomanceEstimationOptions)
    {
        this.perfomanceEstimationOptions = perfomanceEstimationOptions;
    }

    public TransductionOptions getTransductionOptions()
    {
        return transductionOptions;
    }

    public void setTransductionOptions(TransductionOptions transductionOptions)
    {
        this.transductionOptions = transductionOptions;
    }

    public KernelOptions getKernelOptions()
    {
        return kernelOptions;
    }

    public void setKernelOptions(KernelOptions kernelOptions)
    {
        this.kernelOptions = kernelOptions;
    }

    public OptimizationOptions getOptimizationOptions()
    {
        return optimizationOptions;
    }

    public void setOptimizationOptions(OptimizationOptions optimizationOptions)
    {
        this.optimizationOptions = optimizationOptions;
    }

    public void train(ObservationList observations, File modelFile, Logger log) throws Exception
    {
        try(TempFile observationsFile = TempFiles.file( ".txt" ))
        {
            writeObservations( observations, observationsFile );
            List<String> options = getOptions();
            options.add( observationsFile.getAbsolutePath() );
            options.add( modelFile.getAbsolutePath() );
            runCommand( "svm_learn", options, log );
        }
    }

    @Override
    public List<String> getOptions()
    {
        List<String> options = new ArrayList<>();
        if(verbosity != null)
        {
            options.add( "-v" );
            options.add( String.valueOf( verbosity ) );
        }
        if(learningOptions != null)
            options.addAll( learningOptions.getOptions() );
        if(perfomanceEstimationOptions != null)
            options.addAll( perfomanceEstimationOptions.getOptions() );
        if(transductionOptions != null)
            options.addAll( transductionOptions.getOptions() );
        if(kernelOptions != null)
            options.addAll( kernelOptions.getOptions() );
        if(optimizationOptions != null)
            options.addAll( optimizationOptions.getOptions() );
        return options;
    }
}
