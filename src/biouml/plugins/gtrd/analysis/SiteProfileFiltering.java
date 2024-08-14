package biouml.plugins.gtrd.analysis;

import java.beans.PropertyDescriptor;
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
import ru.biosoft.util.bean.StaticDescriptor;

public class SiteProfileFiltering extends AnalysisMethodSupport<SiteProfileFiltering.SiteProfileFilteringParameters>
{
    volatile SqlTrack outputTrack;
    private static PropertyDescriptor MAX_PROFILE_HEIGHT_PD = StaticDescriptor.create( "maxProfileHeight" );
    
	public SiteProfileFiltering(DataCollection<?> origin, String name)
	{
		super(origin, name, new SiteProfileFilteringParameters());
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
				new SiteProfileFilteringIteration(inputTrack, minSize, maxSize), parameters.getThreadsNumber());
		
		outputTrack.finalizeAddition();
		CollectionFactoryUtils.save(outputTrack);
		return outputTrack;
	}
	
	class SiteProfileFilteringIteration implements Iteration<ru.biosoft.access.core.DataElementPath>
	{
		SqlTrack inputTrack;
		int minSize, maxSize;
		
		SiteProfileFilteringIteration(SqlTrack inputTrack, int minSize, int maxSize)
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
				{
					float maxHeight = (float) s.getProperties().getValue(MAX_PROFILE_HEIGHT_PD.getName());
					if(maxHeight >= minSize && maxHeight <= maxSize)
						results.add( s );
				}
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
	
	public static class SiteProfileFilteringParameters extends AbstractAnalysisParameters
	{
		private DataElementPath inputTrack, outputTrack;
		private int minSize = 0, maxSize = 0;
		private int threadsNumber = 1;
		
		SiteProfileFilteringParameters()
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
		
		@PropertyName("The min profile height")
        @PropertyDescription("The min profile height")
		public void setMinSize(int minSize) 
		{
			this.minSize = minSize;
		}

		public int getMaxSize() 
		{
			return maxSize;
		}
		
		@PropertyName("The max profile height")
        @PropertyDescription("The max profile height")
		public void setMaxSize(int maxSize) 
		{
			this.maxSize = maxSize;
		}
	}
	
	public static class SiteProfileFilteringParametersBeanInfo extends BeanInfoEx2<SiteProfileFilteringParameters>
	{
		public SiteProfileFilteringParametersBeanInfo()
        {
            super(SiteProfileFilteringParameters.class);
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
