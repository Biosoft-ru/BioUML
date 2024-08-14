package biouml.plugins.gtrd.analysis;

import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.developmentontheedge.beans.DynamicProperty;
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
import ru.biosoft.bsa.SiteImpl;
import ru.biosoft.bsa.SqlTrack;
import ru.biosoft.bsa.TrackUtils;
import ru.biosoft.exception.LoggedException;
import ru.biosoft.jobcontrol.Iteration;
import ru.biosoft.util.bean.BeanInfoEx2;
import ru.biosoft.util.bean.StaticDescriptor;

public class CoverageProfilesCutoff extends AnalysisMethodSupport<CoverageProfilesCutoff.CoverageProfilesCutoffParameters>
{
	private static PropertyDescriptor PROFILE_PD = StaticDescriptor.create("profile");
    private static PropertyDescriptor MAX_PROFILE_HEIGHT_PD = StaticDescriptor.create( "maxProfileHeight" );
    volatile SqlTrack outputTrack;
    
	public CoverageProfilesCutoff(DataCollection<?> origin, String name)
	{
		super(origin, name, new CoverageProfilesCutoffParameters());
	}
	
	@Override
    public DataCollection<?> justAnalyzeAndPut() throws LoggedException, Exception
	{
		int cutoffThreshold = parameters.getCutoffThreshold();
		SqlTrack inputTrack = parameters.getInputTrack().getDataElement( SqlTrack.class );
		outputTrack = SqlTrack.createTrack(parameters.getOutputTrack(), inputTrack);
		ru.biosoft.access.core.DataElementPath genomePath = TrackUtils.getTrackSequencesPath( inputTrack );
		
		TaskPool.getInstance().iterate(genomePath.getChildren(),
				new SiteCoverageCutoffIteration(inputTrack, cutoffThreshold), parameters.getThreadsNumber());
		
		outputTrack.finalizeAddition();
		CollectionFactoryUtils.save(outputTrack);
		return outputTrack;
	}
	
	class SiteCoverageCutoffIteration implements Iteration<ru.biosoft.access.core.DataElementPath>
	{
		SqlTrack inputTrack;
		int cutoffThreshold;
		
		SiteCoverageCutoffIteration(SqlTrack inputTrack, int cutoffThreshold)
		{
			this.inputTrack = inputTrack;
			this.cutoffThreshold = cutoffThreshold;
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
					List<Site> resultSites = getCutoff(s, cutoffThreshold, chrSeq);
					if(!resultSites.isEmpty())
						for(Site site : resultSites)
							results.add( site );
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
	
	public static List<Site> getCutoff(Site s, int cutoffThreshold, Sequence chrSeq)
	{
        List<Site> resultSites = new ArrayList<>();
		double[] profile = (double[]) s.getProperties().getValue(PROFILE_PD.getName());
		float maxHeight = (float) s.getProperties().getValue(MAX_PROFILE_HEIGHT_PD.getName());
		
		if(cutoffThreshold > (int) maxHeight)
			return resultSites;
		
		int from = s.getFrom();
		int newFrom = from;
		int newTo = s.getTo();
		
		boolean insideTheRegion = false;
		for(int i = 0; i < profile.length; i++)
		{
			if(profile[i] < cutoffThreshold) profile[i] = 0;
			//trimming 0 height profile and split regions
			if( !insideTheRegion && profile[i] != 0 )
			{
				newFrom = from + i;
				insideTheRegion = true;
			}
			if( insideTheRegion && ( profile[i] == 0 || i == (profile.length - 1) ) )
			{
				newTo = from + i - 1;
				SiteImpl newSite = new SiteImpl(null, chrSeq.getName(), newFrom, newTo - newFrom + 1,
						Site.STRAND_NOT_APPLICABLE, chrSeq);
				double[] subProfile = Arrays.copyOfRange(profile, newFrom - from, i);
				newSite.getProperties().add( new DynamicProperty( PROFILE_PD, double[].class, subProfile ));
				newSite.getProperties().add( new DynamicProperty( MAX_PROFILE_HEIGHT_PD, Float.class, getMaxHeight(subProfile) ) );
				resultSites.add( newSite );
				insideTheRegion = false;
			}
		}
		return resultSites;
	}
	
	public static class CoverageProfilesCutoffParameters extends AbstractAnalysisParameters
	{
		private DataElementPath inputTrack, outputTrack;
		private int cutoffThreshold = 0;
		private int threadsNumber = 1;
		
		CoverageProfilesCutoffParameters()
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
		
		public int getCutoffThreshold()
		{
			return cutoffThreshold;
		}
		
		@PropertyName("Cutoff Threshold (including the value)")
        @PropertyDescription("Cutoff Threshold (including the value)")
		public void setCutoffThreshold(int cutoffThreshold)
		{
			this.cutoffThreshold = cutoffThreshold;
		}


	}
	public static class CoverageProfilesCutoffParametersBeanInfo extends BeanInfoEx2<CoverageProfilesCutoffParameters>
	{
		public CoverageProfilesCutoffParametersBeanInfo()
        {
            super(CoverageProfilesCutoffParameters.class);
        }
		
		@Override
		protected void initProperties() throws Exception
		{
			property("inputTrack").inputElement( SqlTrack.class ).add();
			property("cutoffThreshold").add();
			property("threadsNumber").add();
			property("outputTrack").outputElement( SqlTrack.class ).add();
		}
	}
	private static double getMaxHeight(double[] profile)
	{
		double max = profile[0];
		for(int i = 0; i < profile.length; i++)
		{
			if(profile[i] > max) max = profile[i];
		}
		return max;
	}
}
