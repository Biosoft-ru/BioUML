package biouml.plugins.proteinmodel;

import java.awt.Point;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import javax.annotation.Nonnull;

import org.apache.commons.lang.ArrayUtils;
import java.util.logging.Logger;

import ru.biosoft.access.core.AbstractDataCollection;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.exception.BiosoftSQLException;
import ru.biosoft.access.sql.SqlConnectionHolder;
import ru.biosoft.access.sql.SqlConnectionPool;
import biouml.model.Diagram;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.VariableRole;
import biouml.standard.diagram.DiagramUtility;
import biouml.standard.diagram.PathwaySimulationDiagramType;
import biouml.standard.type.Base;
import biouml.standard.type.DiagramInfo;
import biouml.standard.type.KineticLaw;
import biouml.standard.type.Reaction;
import biouml.standard.type.SpecieReference;

public class DiagramsCollection extends AbstractDataCollection<Diagram> implements SqlConnectionHolder
{
    private static final Logger log = Logger.getLogger(DiagramsCollection.class.getName());
    
    private static final String[] names = new String[] {"Simulation", "Simulation-10", "Simulation-100"};

    public DiagramsCollection(DataCollection<?> parent, Properties properties)
    {
        super(parent, properties);
    }

    private Diagram createDiagram(String name) throws Exception
    {
        double allTime = System.currentTimeMillis();
        int limit = Integer.MAX_VALUE;
        if(name.equals("Simulation-10")) limit = 10;
        else if(name.equals("Simulation-100")) limit = 100;
        Diagram diagram = new Diagram(this, new DiagramInfo(name), new PathwaySimulationDiagramType());
        diagram.setRole(new EModel(diagram));
        diagram.setPropagationEnabled(false);
        Statement st = getConnection().createStatement();
        ResultSet resultSet = null;
        try
        {
            resultSet = st.executeQuery("SELECT * FROM protein WHERE " + ProteinModelUtils.getRNAExistsCondition());
            int i = 0;
            List<SpecieReference> speciesReferences;
            int numberSkip = 0;
            double timeForProtein = 0, timeForRNA = 0, timeForTranslation = 0, timeForSR = 0, timeForDR = 0, timeForDP = 0, time;
            while( resultSet.next() )
            {
                String id = resultSet.getString("id");
                if(resultSet.getString("ksp_avg") == null)
                {
                    //log.warning(id+": ksp_avg is NA; skipped");
                    numberSkip++;
                    continue;
                }
                if(resultSet.getString("vsr_avg") == null)
                {
                    //log.warning(id+": vsr_avg is NA; skipped");
                    numberSkip++;
                    continue;
                }
                if(resultSet.getString("mrna_halflife_avg") == null)
                {
                    //log.warning(id+": mrna_halflife_avg is NA; skipped");
                    numberSkip++;
                    continue;
                }
                if(resultSet.getString("protein_halflife_avg") == null)
                {
                    //log.warning(id+": protein_halflife_avg is NA; skipped");
                    numberSkip++;
                    continue;
                }

                time = System.currentTimeMillis();
                diagramObject(diagram, resultSet, "../Data/protein/", 250, i, "protein");
                timeForProtein += ( System.currentTimeMillis() - time );

                time = System.currentTimeMillis();
                diagramObject(diagram, resultSet, "../Data/rna/", 50, i, "RNA");
                timeForRNA += ( System.currentTimeMillis() - time );

                //  RNA->Protein
                time = System.currentTimeMillis();
                speciesReferences = new ArrayList<>();
                speciesReferences.add(DiagramsCollection.createSpeciesReference(id, SpecieReference.PRODUCT));
                speciesReferences
                        .add(DiagramsCollection.createSpeciesReference(id + "_m", SpecieReference.MODIFIER));
                createReaction(diagram, speciesReferences, resultSet.getString("ksp_avg") + "*$" + id + "_m", 150,
                        80 * i, id+"_rps");
                timeForTranslation += ( System.currentTimeMillis() - time );

                // ->RNA
                time = System.currentTimeMillis();
                speciesReferences = new ArrayList<>();
                speciesReferences.add(DiagramsCollection.createSpeciesReference(id + "_m", SpecieReference.PRODUCT));
                createReaction(diagram, speciesReferences, resultSet.getString("vsr_avg"), 10, 80 * i, id+"_rrs");
                timeForSR += ( System.currentTimeMillis() - time );

                // RNA->
                time = System.currentTimeMillis();
                speciesReferences = new ArrayList<>();
                speciesReferences
                        .add(DiagramsCollection.createSpeciesReference(id + "_m", SpecieReference.REACTANT));
                createReaction(diagram, speciesReferences, String.valueOf(Math.log(2) / resultSet.getDouble("mrna_halflife_avg")) + "*$"
                        + id + "_m", 150, 80 * i - 40, id+"_rrd");
                timeForDR += ( System.currentTimeMillis() - time );

                // Protein->
                time = System.currentTimeMillis();
                speciesReferences = new ArrayList<>();
                speciesReferences.add(DiagramsCollection.createSpeciesReference(id, SpecieReference.REACTANT));
                createReaction(diagram, speciesReferences, String.valueOf(Math.log(2) / resultSet.getDouble("protein_halflife_avg")) + "*$"
                        + id, 350, 80 * i, id+"_rpd");
                timeForDP += ( System.currentTimeMillis() - time );

                i++;
                if( i % 1000 == 0 )
                    log.info(i + " mRNA's processed");
                if( i > limit )
                    break;
            }
            log.info("Time for all protein: " + timeForProtein);
            log.info("Time for all RNA: " + timeForRNA);
            log.info("Time for translation: " + timeForTranslation);
            log.info("Time for synthesis RNA: " + timeForSR);
            log.info("Time for degradation RNA: " + timeForDR);
            log.info("Time for degradation protein: " + timeForDP);
            log.info("All time: " + ( System.currentTimeMillis() - allTime ));
            log.info("Number of the skipped processes: " + numberSkip);
        }
        finally
        {
            try
            {
                if( resultSet != null )
                    resultSet.close();
            }
            catch( Exception e )
            {
            }
            try
            {
                if( st != null )
                    st.close();
            }
            catch( Exception e )
            {
            }
        }
        diagram.setPropagationEnabled(true);

        return diagram;
    }

    public void diagramObject(Diagram diagram, ResultSet resultSet, String address, int Location, int i, String typeNode) throws SQLException, Exception
    {
        Node node = new Node(diagram, getCompletePath().getRelativePath(address + resultSet.getString("id")).getDataElement(Base.class));
        if (typeNode.equals("protein"))
            node.setRole(new VariableRole(null, node, resultSet.getDouble("protein_copies_avg")));
        if (typeNode.equals("RNA"))
            node.setRole(new VariableRole(null, node, resultSet.getDouble("mrna_copies_avg")));
        node.setLocation(Location, i * 80);
        diagram.put(node);
    }

    public static SpecieReference createSpeciesReference(String variable, String role)
    {
        SpecieReference specieReference = new SpecieReference(null, variable, role);
        specieReference.setSpecie(variable);
        return specieReference;
    }

    protected static void createReaction(Diagram diagram, List<SpecieReference> components, String formula, int firstCoordinate,
            int secondCoordinate, String reactionName) throws Exception
    {
        Reaction reaction = new Reaction(null, reactionName);
        reaction.setTitle(DiagramUtility.generateReactionTitle(components));
        
        KineticLaw kineticLaw = new KineticLaw();
        reaction.setKineticLaw(kineticLaw);
        
        Node reactionNode = new Node(diagram, reaction);
        reactionNode.setRelativeLocation(diagram, new Point(firstCoordinate, secondCoordinate));
        
        diagram.put(reactionNode);
        
        // add specie roles and edges
        for( SpecieReference prototype : components )
        {
        
            //It's necessary to find node by prototype name (not by prototype specie) for the case when we have two nodes with identifiers like this:
            //CMP0225.CMP0034.PRT003455 and CMP0225.PRT003455
            Node de = diagram.findNode(DiagramUtility.toDiagramPath(prototype.getName()));
        
            String id = reaction.getName() + ": " + de.getKernel().getName() + " as " + prototype.getRole();
            SpecieReference real = prototype.clone(reaction, id);
            real.setTitle(de.getKernel().getName() + " as " + prototype.getRole());
        
            real.setSpecie(de.getCompleteNameInDiagram());
            reaction.put(real);
        
            Edge edge = null;
            if( real.getRole().equals(SpecieReference.PRODUCT) )
                edge = new Edge(real, reactionNode, de);
            else
                edge = new Edge(real, de, reactionNode);

            edge.save();
        }
        DiagramUtility.generateRoles(diagram, reactionNode);
        diagram.setNotificationEnabled(false);
        kineticLaw.setFormula(formula);
        diagram.setNotificationEnabled(true);
    }


    @Override
    public Connection getConnection() throws BiosoftSQLException
    {
        return SqlConnectionPool.getConnection(this);
    }

    @Override
    public boolean contains(String name)
    {
        return ArrayUtils.contains(names, name);
    }

    @Override
    public @Nonnull List<String> getNameList()
    {
        return Arrays.asList(names);
    }

    @Override
    protected Diagram doGet(String name) throws Exception
    {
        return contains(name)?createDiagram(name):null;
    }
}
