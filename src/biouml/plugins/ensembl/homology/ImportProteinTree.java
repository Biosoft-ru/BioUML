package biouml.plugins.ensembl.homology;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

import ru.biosoft.jobcontrol.JobControl;

import ru.biosoft.access.sql.BulkInsert;
import ru.biosoft.access.sql.FastBulkInsert;
import ru.biosoft.access.sql.SqlUtil;
import ru.biosoft.util.TextUtil;

/**
 * Allows you to import homology information from Compara.64.protein_trees.nh.emf into MySQL for consequent use
 * in homology hub
 * The file can be downloaded from
 * ftp://ftp.ensembl.org/pub/current_emf/ensembl-compara/homologies/Compara.64.protein_trees.nh.emf.gz
 * (version number '64' may change in future)
 * @author lan
 */
public class ImportProteinTree
{
    public static void doImport(File file, Connection connection) throws Exception
    {
        createSchema( connection );
        insert( file, connection, null );
    }

    public static void insert(File file, Connection connection, JobControl jobControl) throws IOException
    {
        Map<String, Integer> speciesMap = new HashMap<>();
        int lastSpeciesId = 0;
        BulkInsert inserter = new FastBulkInsert(connection, "homology");
        BulkInsert speciesInserter = new FastBulkInsert(connection, "homology_species");
        BulkInsert treeInserter = new FastBulkInsert(connection, "homology_trees");
        int groupId = 1;
        long length = file.length();
        try(FileInputStream fis = new FileInputStream(file);
                FileChannel fc = fis.getChannel();
                BufferedReader reader = new BufferedReader(new InputStreamReader( fis, StandardCharsets.ISO_8859_1 )))
        {
            while(reader.ready())
            {
                String line = reader.readLine();
                if(line == null)
                    break;
                if(line.startsWith("("))
                {
                    treeInserter.insert(new Object[] {groupId, line});
                }
                String[] fields = TextUtil.split( line, ' ' );
                if(fields[0].equals("//")) groupId++;
                if(!fields[0].equals("SEQ")) continue;
                String species = fields[1];
                Integer speciesId = speciesMap.get(species);
                if(speciesId == null)
                {
                    speciesId = ++lastSpeciesId;
                    speciesMap.put(species, speciesId);
                    speciesInserter.insert(new Object[] {speciesId, species});
                }
                inserter.insert(new Object[] {groupId, fields[7], fields[2], speciesId});
                if(jobControl != null && length > 0) {
                    jobControl.setPreparedness( (int) ( fc.position()*100/length ) );
                }
            }
        }
        treeInserter.flush();
        speciesInserter.flush();
        inserter.flush();
    }

    public static void createSchema(Connection connection)
    {
        SqlUtil.dropTable(connection, "homology_species");
        SqlUtil.execute(connection, "CREATE TABLE homology_species (" +
                "species_id int," +
                "species_name varchar(64)," +
                "primary key(species_id)," +
                "unique key(species_name)) ENGINE=MyISAM");
        SqlUtil.dropTable(connection, "homology");
        SqlUtil.execute(connection, "CREATE TABLE homology (" +
                "group_id int," +
                "gene_acc varchar(32)," +
                "protein_acc varchar(32)," +
                "species_id int," +
                "key(group_id,species_id)," +
                "key(gene_acc)," +
                "key(protein_acc)" +
                ") ENGINE=MyISAM");
        SqlUtil.dropTable(connection, "homology_trees");
        SqlUtil.execute(connection, "CREATE TABLE homology_trees (" +
                "group_id int," +
                "tree text," +
                "key(group_id)" +
                ") ENGINE=MyISAM");
    }
}
