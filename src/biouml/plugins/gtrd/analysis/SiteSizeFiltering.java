package biouml.plugins.gtrd.analysis;

import java.util.ArrayList;
import java.util.List;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.task.TaskPool;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SqlTrack;
import ru.biosoft.bsa.TrackUtils;
import ru.biosoft.exception.LoggedException;
import ru.biosoft.jobcontrol.Iteration;
import ru.biosoft.util.bean.BeanInfoEx2;

public class SiteSizeFiltering extends AnalysisMethodSupport<SiteSizeFiltering.SiteSizeFilteringParameters>
{
    volatile SqlTrack outputTrack;
    
	public SiteSizeFiltering(DataCollection<?> origin, String name)
	{
		super(origin, name, new SiteSizeFilteringParameters());
	}
	
	@Override
    public DataCollection<?> justAnalyzeAndPut() throws LoggedException, Exception 
	{
		int minSize = parameters.getMinSize();
		int maxSize = parameters.getMaxSize();
		SqlTrack inputTrack = parameters.getInputTrack().getDataElement( SqlTrack.class );
		outputTrack = SqlTrack.createTrack(parameters.getOutputTrack(), inputTrack);
		ru.biosoft.access.core.DataElementPath genomePath = TrackUtils.getTrackSequencesPath( inputTrack );
		
		TaskPool.getInstance().iterate(genomePath.getChildren(), 
				new SiteSizeFilteringIteration(inputTrack, minSize, maxSize), parameters.getThreadsNumber());
		
		outputTrack.finalizeAddition();
		CollectionFactoryUtils.save(outputTrack);
		return outputTrack;
	}
	
	class SiteSizeFilteringIteration implements Iteration<ru.biosoft.access.core.DataElementPath>
	{
		SqlTrack inputTrack;
		int minSize, maxSize;
		
		SiteSizeFilteringIteration(SqlTrack inputTrack, int minSize, int maxSize)
		{
			this.inputTrack = inputTrack;
			this.minSize = minSize;
			this.maxSize = maxSize;
		}
		
		@Override
		public boolean run(DataElementPath element) 
		{
			try
			{
				Sequence chrSeq = element.getDataElement( AnnotatedSequence.class ).getSequence();
				DataCollection<Site> sites = inputTrack.getSites( element.toString(), 0, chrSeq.getLength() + chrSeq.getStart() );
				List<Site> results = new ArrayList<>();
				for( Site s : sites )
					if(s.getLength() >= minSize && s.getLength() <= maxSize)
						results.add( s );			
				
                synchronized(outputTrack)
                {
                    for(Site result : results)
                    	outputTrack.addSite( result );    
                }
			}
			catch(Exception e)
			{
				return false;
			}
			return true;
		}
	}
	
	public static class SiteSizeFilteringParameters extends AbstractAnalysisParameters
	{
		private DataElementPath inputTrack, outputTrack;
		private int minSize = 0, maxSize = 0;
		private int threadsNumber = 1;
		
		SiteSizeFilteringParameters()
		{}
		
		public DataElementPath getInputTrack() 
		{
			return inputTrack;
		}
		
		@PropertyName("Input SQL Track")
        @PropertyDescription("Input SQL Track")
		public void setInputTrack(DataElementPath inputTrack) 
		{
			this.inputTrack = inputTrack;
		}

		public DataElementPath getOutputTrack() 
		{
			return outputTrack;
		}

		@PropertyName("Output SQL Track")
        @PropertyDescription("Output SQL Track")
		public void setOutputTrack(DataElementPath outputTrack) 
		{
			this.outputTrack = outputTrack;
		}

		public int getThreadsNumber() 
		{
			return threadsNumber;
		}

		@PropertyName("Number of threads")
        @PropertyDescription("Number of threads")
		public void setThreadsNumber(int threadsNumber) 
		{
			this.threadsNumber = threadsNumber;
		}
		
		public int getMinSize() 
		{
			return minSize;
		}
		
		@PropertyName("The min size of sites")
        @PropertyDescription("The min size of sites")
		public void setMinSize(int minSize) 
		{
			this.minSize = minSize;
		}

		public int getMaxSize() 
		{
			return maxSize;
		}
		
		@PropertyName("The max size of sites")
        @PropertyDescription("The max size of sites")
		public void setMaxSize(int maxSize) 
		{
			this.maxSize = maxSize;
		}
	}
	
	public static class SiteSizeFilteringParametersBeanInfo extends BeanInfoEx2<SiteSizeFilteringParameters>
	{
		public SiteSizeFilteringParametersBeanInfo()
        {
            super(SiteSizeFilteringParameters.class);
        }
		
		@Override
		protected void initProperties() throws Exception
		{
			property("inputTrack").inputElement( SqlTrack.class ).add();
			property("minSize").add();
			property("maxSize").add();
			property("threadsNumber").add();
			property("outputTrack").outputElement( SqlTrack.class ).add();
		}
	}
}
