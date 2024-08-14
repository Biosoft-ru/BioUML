package biouml.plugins.gtrd.analysis.nosql;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

public class GeneAnnotation
{
    Map<String, GeneLocation> byEnsGeneId = new HashMap<>();
    Map<String, GeneLocation> byGeneSymbol = new HashMap<>();
    
    void loadFromGTF(Path gtfFile) throws IOException
    {
        InputStream is = new FileInputStream(gtfFile.toFile());
        if(gtfFile.toFile().getName().endsWith( ".gz" ))
            is = new GZIPInputStream( is );
        try (BufferedReader reader = new BufferedReader( new InputStreamReader( is ) ))
        {
            String line;
            while( ( line = reader.readLine() ) != null )
            {
                if( line.startsWith( "#" ) )
                    continue;
                String[] parts = line.split( "\t" );
                if( !parts[2].equals( "gene" ) )
                    continue;
                GeneLocation loc = new GeneLocation();
                loc.chr = parts[0];
                loc.from = Integer.parseInt(parts[3]);
                loc.to = Integer.parseInt(parts[4]);
                if(parts[8].endsWith( ";" ))
                    parts[8] = parts[8].substring( 0, parts[8].length()-1 );
                for( String prop : parts[8].split( "; " ) )
                {
                    prop = prop.trim();
                    if( prop.isEmpty() )
                        continue;
                    String[] kv = prop.split( " ", 2 );
                    kv[1] = kv[1].replace( "\"", "" );
                    if( kv[0].equals( "gene_id" ) )
                        loc.ensemblGeneId = kv[1];
                    else if(kv[0].equals( "gene_name" ))
                        loc.geneSymbol = kv[1];
                }
                byEnsGeneId.put( loc.ensemblGeneId, loc );
                if(loc.geneSymbol != null)
                    byGeneSymbol.put(loc.geneSymbol, loc);
            }
        }
    }
}
