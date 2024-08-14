package biouml.standard.type.access;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import ru.biosoft.access.core.DataCollection;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.standard.type.Base;

/**
 * Builds (fills) diagramComponents table.
 *
 * @pending - spike solution
 */
public class DiagramComponentsTable
{
    protected static final Logger log = Logger.getLogger(DiagramComponentsTable.class.getName());

    public static void fillTable(Connection connection, DataCollection<Diagram> diagrams)
    {
        log.info( "Filling diagram components table, diagrams db=" + diagrams.getCompletePath() + ", size=" + diagrams.getSize() );

        for(Diagram diagram : diagrams)
        {
            try
            {
                fillTable(connection, diagram);
            }
            catch(Exception e)
            {
                log.log(Level.SEVERE, "Error for diagram " + diagram.getName() + "(" + diagram.getTitle() + ")", e);
            }
        }


        log.info( "Filling diagram components table was successfully completed." );
    }


    public static void fillTable(Connection connection, Diagram diagram) throws SQLException
    {
        log.info("  diagram: " + diagram.getName() + "(" + diagram.getTitle() + ")");

        // remove all previous components
        try( Statement statement = connection.createStatement() )
        {
            statement.execute( "DELETE FROM diagramComponents WHERE diagramID='" + diagram.getName() + "'" );
        }

        // build hashmap for all components: <kernel name> - <kernel>
        Map<String, Base> components = diagram.recursiveStream().map( DiagramElement::getKernel ).nonNull()
                .toMap( Base::getName, Function.identity() );

        // create and execute batch statement
        try( Statement statement = connection.createStatement() )
        {
            for( Entry<String, Base> entry : components.entrySet() )
            {
                String name = entry.getKey();
                Base kernel = entry.getValue();

                statement.addBatch( "INSERT INTO diagramComponents(diagramID, entityID, type) " + "VALUES('" + diagram.getName() + "', '"
                        + name + "', '" + kernel.getType() + "')" );
            }

            statement.executeBatch();

            log.info( "    " + components.size() + " components were added." );
        }

    }
}
