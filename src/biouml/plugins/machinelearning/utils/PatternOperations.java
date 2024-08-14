package biouml.plugins.machinelearning.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import one.util.streamex.IntStreamEx;
import biouml.plugins.machinelearning.utils.UtilsGeneral.UtilsForArray;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.Track;

public class PatternOperations 
{
	public static class Pattern
	{
		private char[] pattern; /* pattern[i] = {'+' if i-th pattern component presents; '-' if i-th pattern component is not presented */ 
		private int frequency;
//		String[] namesOfPatternComponents; /* dim(pattern) = dim(namesOfPatternComponents) */
		
//		public Pattern(char[] pattern, int frequency, String[] namesOfPatternComponents)
		public Pattern(char[] pattern, int frequency)

		{
			this.pattern = pattern;
			this.frequency = frequency;
//			this.namesOfPatternComponents = namesOfPatternComponents;
		}
		
		public boolean isEqual(char[] pattern)
		{
			return UtilsForArray.equal(this.pattern, pattern);
		}
		
		public void addFrequency()
		{
			frequency++;
		}
		
		public String toString()
		{
			return IntStreamEx.of(pattern).charsToString() + '\t' + Integer.toString(frequency);
		}
	}
	
	/****************************/
	
	public static class Patterns
	{
		private Pattern[] patterns;
		private String[] namesOfPatternComponents;  /* dim(namesOfPatternComponents) = dim(pattern) where pattern is from class Pattern() */
		private int maxNumberOfPatterns;
		
		public Patterns(int maxNumberOfPatterns, DataElementPath pathToTrackWithGivenSequences, DataElementPath[]pathsTrackWithPatternComponents)
		{
			this.maxNumberOfPatterns = maxNumberOfPatterns;
			Object[] objects = getPatternsOfSitesInGivenSequences(pathToTrackWithGivenSequences, pathsTrackWithPatternComponents);
			namesOfPatternComponents = (String[])objects[0];
			patterns = (Pattern[])objects[1];
		}
		
		private Object[] getPatternsOfSitesInGivenSequences(DataElementPath pathToTrackWithGivenSequences, DataElementPath[] pathsTrackWithPatternComponents)
		{
			// 1. Initial.
			String[] namesOfPatternComponents = new String[pathsTrackWithPatternComponents.length];
			Track track = (Track)pathToTrackWithGivenSequences.getDataElement();
			Track[] tracks = new Track[pathsTrackWithPatternComponents.length];
			DataCollection<Site> dc = track.getAllSites();
			int n = dc.getSize();
        	log.info("Number of sites in given track, n = " + n);
			List<Pattern> patternList = new ArrayList<>();

			// 2. Create String[] namesOfPatternComponents and Track[] tracks.
			for( int i = 0; i < tracks.length; i++ )
			{
				namesOfPatternComponents[i] = pathsTrackWithPatternComponents[i].getName();
				tracks[i] = (Track)pathsTrackWithPatternComponents[i].getDataElement();
			}
			
			// 3. Construct patternList.
			int index = 0;
			for( Site site : dc )
	        {
				String chromosomeName = site.getSequence().getName();
	            Interval coordinates = site.getInterval();
	            int from = coordinates.getFrom(), to = coordinates.getTo();
				char[] patternNew = UtilsForArray.getConstantArray(tracks.length,  '-');
				for( int j = 0; j < tracks.length; j++ )
					if( ! tracks[j].getSites(chromosomeName, from, to).isEmpty() )
						patternNew[j] = '+';
				addPattern(patternList, patternNew);
	        	log.info("Site No = " + index++ + " size(patternList) = " + patternList.size());
	        	if( patternList.size() >= maxNumberOfPatterns ) break;
	        }
			return new Object[]{namesOfPatternComponents, patternList.toArray(new Pattern[0])};
		}
		
		public void writePatternsToFile(DataElementPath pathToOutputFolder, String fileName)
		{
	        StringBuilder builder = new StringBuilder();
	        builder.append(" namesOfPatternComponents :");
			for( int i = 0; i < namesOfPatternComponents.length; i++ )
				builder.append("\n").append(i).append(namesOfPatternComponents[i]);
	        builder.append("ID	pattern	frequency");
			for( int i = 0; i < patterns.length; i++ )
				builder.append("\n").append(Integer.toString(i)).append("\t").append(patterns[i].toString());
			String str = builder.toString();
			TableAndFileUtils.writeStringToFile(str, pathToOutputFolder, fileName, log);
		}
	}
	
	private static void addPattern(List<Pattern> patternList, char[] patternNew)
	{
		for( Pattern pattern : patternList )
			if( pattern.isEqual(patternNew) )
			{
				pattern.addFrequency();
				return;
			}
		patternList.add(new Pattern(patternNew, 1));
	}

    static Logger log = Logger.getLogger(PatternOperations.class.getName());
}