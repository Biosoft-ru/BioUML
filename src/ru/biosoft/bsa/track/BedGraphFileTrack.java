package ru.biosoft.bsa.track;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.DynamicPropertySetAsMap;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.bsa.Basis;
import ru.biosoft.bsa.FileTrack;
import ru.biosoft.bsa.Precision;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SiteImpl;
import ru.biosoft.bsa.StrandType;
import ru.biosoft.bsa.track.big.BigWigTrackViewBuilder;

public class BedGraphFileTrack extends FileTrack {

	public BedGraphFileTrack(DataCollection<?> parent, Properties properties) throws IOException {
		super(parent, properties);
		viewBuilder = new BigWigTrackViewBuilder();
	}
	 
	@Override
	protected void readFromFile(File file, DataCollection<Site> sites) throws Exception {

		
        try ( FileInputStream is = new FileInputStream( file );
                BufferedReader input = new BufferedReader( new InputStreamReader( is, StandardCharsets.UTF_8 ) ) )
        {
            String line;
            int id = 1;
            while ( (line = input.readLine()) != null )
            {
                if( isComment( line ) )
                    continue;
                Site site = parseLine( line, id );
                if( site == null )
                    continue;
                sites.put( site );
                id++;
            }
        }


	}
	

    private Site parseLine(String line, int id) {
            String[] fields = line.split("\\s");
            String chr = fields[0];
            int start, end;
            float score;
            try
            {
            	start = Integer.parseInt(fields[1]);
            	end = Integer.parseInt(fields[2]);
                score = Float.parseFloat(fields[3]);
            }
            catch( NumberFormatException e )
            {
                return null;
            }
            DynamicPropertySet properties = new DynamicPropertySetAsMap();
            properties.add(new DynamicProperty(Site.SCORE_PD, Float.class, score));
            Sequence seq = getSequence( chr );
            return new SiteImpl(null, String.valueOf(id), null, Basis.BASIS_USER, start, end - start + 1, Precision.PRECISION_EXACTLY,
                    StrandType.STRAND_NOT_APPLICABLE, seq, properties);
	}

	
	private boolean isComment(String line)
    {
        return line.startsWith( "track " ) || line.startsWith( "browser " ) || line.startsWith( "#" );
    }

}
