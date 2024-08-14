package biouml.plugins.gtrd.analysis.nosql;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

public class GenomeInfo {
    private String organism;
    private String chrSizesFileUCSC;
    private String genomeBuildUCSC;
    private String ensemblGTF;
    private String chrNameMappingUCSCToEnsembl;
    
    public String getOrganism()
    {
        return organism;
    }
    public String getChrSizesFileUCSC() {
        return chrSizesFileUCSC;
    }
    public String getGenomeBuildUCSC() {
        return genomeBuildUCSC;
    }
    public String getEnsemblGTF()
    {
        return ensemblGTF;
    }
    public String getChrNameMappingUCSCToEnsembl()
    {
        return chrNameMappingUCSCToEnsembl;
    }
    //JSON serialization
    public void fromJSON(JSONObject elem) {
        
        organism = getAndRemoveString(elem, "organism");
        chrSizesFileUCSC = getAndRemoveString(elem, "chr_sizes_file_ucsc");
        genomeBuildUCSC = getAndRemoveString(elem, "genome_build_ucsc");
        ensemblGTF = getAndRemoveString( elem, "ensembl_gtf" );
        chrNameMappingUCSCToEnsembl = getAndRemoveString( elem, "chr_name_mapping_ucsc_to_ensembl" );
        
        if(!elem.keySet().isEmpty())
            throw new RuntimeException("Unexpected keys: " + elem.keySet());
    }
    
    public JSONObject toJSON()
    {
        JSONObject elem = new JSONObject();
        elem.put("organism", organism);
        elem.put("chr_sizes_file_ucsc", getChrSizesFileUCSC());
        elem.put("genome_build_ucsc", getGenomeBuildUCSC());
        elem.put("ensembl_gtf", getGenomeBuildUCSC());
        return elem;
    }
    
    public static Map<String, GenomeInfo> loadAll(Path jsonFile) throws IOException
    {
        Map<String, GenomeInfo> result = new HashMap<>();
        byte[] content = Files.readAllBytes( jsonFile );
        String jsonStr = new String(content, StandardCharsets.UTF_8);
        JSONArray json = new JSONArray( jsonStr );
        for(int i = 0; i < json.length(); i++)
        {
            JSONObject elem = json.getJSONObject( i );
            GenomeInfo info = new GenomeInfo();
            info.fromJSON(elem);
            result.put(info.organism, info);
        }
        return result;
    }
    
    private String getAndRemoveString(JSONObject json, String key)
    {
        Object result = json.remove(key);
        if(result == null)
            throw new RuntimeException("Missing " + key);
        if(!(result instanceof String))
            throw new RuntimeException("Expecting String value for " + key + " key");
        return (String)result;
    }
}
