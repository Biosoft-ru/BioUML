package biouml.standard.type.access;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

import ru.biosoft.access.SqlDataCollection;
import ru.biosoft.access.SqlTransformerSupport;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.DiagramType;
import biouml.model.Module;
import biouml.model.util.DiagramXmlReader;
import biouml.model.util.DiagramXmlWriter;
import biouml.model.util.ImageGenerator;
import biouml.standard.type.DiagramInfo;

public class DiagramSqlTransformer extends SqlTransformerSupport<Diagram>
{
    private final ReferrerSqlTransformer<DiagramInfo> diagramInfoTransformer = new ReferrerSqlTransformer<DiagramInfo>()
    {
        @Override
        public boolean init(SqlDataCollection<DiagramInfo> owner)
        {
            table = "diagrams";
            this.owner = owner;
            checkAttributesColumn(owner);
            return true;
        }

        @Override
        public Class<DiagramInfo> getTemplateClass()
        {
            return DiagramInfo.class;
        }

        @Override
        protected DiagramInfo createElement(ResultSet resultSet, Connection connection) throws SQLException
        {
            return new DiagramInfo(owner, resultSet.getString(1));
        }

        @Override
        protected String getSpecificFields(DiagramInfo de)
        {
            return " ";
        }

        @Override
        protected String[] getSpecificValues(DiagramInfo de)
        {
            return null;
        }
    };

    protected Logger log = Logger.getLogger(DiagramSqlTransformer.class.getName());

    @Override
    public boolean init(SqlDataCollection<Diagram> owner)
    {
        table = "diagrams";
        this.owner = owner;
        checkAttributesColumn(owner);
        diagramInfoTransformer.init((SqlDataCollection)owner);
        return true;
    }

    @Override
    public Class<Diagram> getTemplateClass()
    {
        return Diagram.class;
    }

    @Override
    public String getSelectQuery()
    {
        return "SELECT id, type, title, description, comment, xml " + "FROM " + table;
    }

    @Override
    public Diagram create(ResultSet resultSet, Connection connection) throws Exception
    {
        DiagramInfo info = diagramInfoTransformer.create(resultSet, connection);

        String xml = resultSet.getString(6);
        String name = resultSet.getString(1);

        Diagram diagram = null;
        //find module
        Module module = Module.optModule(owner);

        if( xml != null )
        {
            //fix bug open Biopath/DRG0312 diagram
            //xml = xml.replace ( "&#1", "?" );
            //fix bug open Biopath/DRG0136b diagram
            //xml = xml.replace ( "&", "?" );

            //InputStream is = new StringBufferInputStream(dml);
            InputStream is = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
            diagram = DiagramXmlReader.readDiagram(info.getName(), is, info, owner, module);
        }
        if( diagram == null )
        {
            log.info("Create new diagram");

            DiagramType diagramType = module.getType().getDiagramTypeObjects().findFirst().orElse( null );

            if( diagramType != null )
            {
                diagram = ( diagramType.createDiagram(module.getDiagrams(), name, null) );
            }
            else
            {
                log.log(Level.SEVERE, "There is no available DiagramType for this database");
            }
        }

        return diagram;
    }
    

    ///////////////////////////////////////////////////////////////////////////
    // Write the diagram
    //

    protected void updateXmlValue(Diagram diagram) throws Exception
    {
        try
        {
            String sql = "UPDATE " + table + " SET " + " xml = ? , image = ?, map = ? WHERE ID=" + validateValue(diagram.getName());

            Connection connection = getConnection();
            try (PreparedStatement pstmt = connection.prepareStatement( sql ))
            {
                // write the xml
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                DiagramXmlWriter writer = new DiagramXmlWriter( out );
                writer.write( diagram );
                byte[] xmlBytes = out.toString().getBytes( StandardCharsets.UTF_8 );
                ByteArrayInputStream xmlStream = new ByteArrayInputStream( xmlBytes );

                pstmt.setBinaryStream( 1, xmlStream, xmlBytes.length );

                // write png
                out = new ByteArrayOutputStream();
                BufferedImage image = ImageGenerator.generateDiagramImage( diagram );
                ImageGenerator.encodeImage( image, "PNG", out );

                byte[] imageBytes = out.toByteArray();
                ByteArrayInputStream imageStream = new ByteArrayInputStream( imageBytes );

                pstmt.setBinaryStream( 2, imageStream, imageBytes.length );

                // write map
                String map = ImageGenerator.generateImageMap( diagram.getView(), new SqlReferenceGenerator( connection ) );
                byte[] mapBytes = map.getBytes( StandardCharsets.UTF_8 );
                pstmt.setBinaryStream( 3, new ByteArrayInputStream( mapBytes ), mapBytes.length );

                pstmt.executeUpdate();
            }
        }
        catch( Exception ex )
        {
            log.log(Level.SEVERE, "Can't save diagram '" + diagram.getName() + "'", ex);
        }
    }

    @Override
    public void addUpdateCommands(Statement statement, Diagram diagram) throws Exception
    {
        DiagramInfo kernel = (DiagramInfo)diagram.getKernel();

        String updateQuery = "UPDATE " + table + " SET type=" + validateValue(kernel.getType()) + ", " + "title="
                + validateValue(kernel.getTitle()) + ", " + "description=" + validateValue(kernel.getDescription()) + ", " + "comment="
                + validateValue(kernel.getComment()) + " WHERE id=" + validateValue(kernel.getName());

        statement.addBatch(updateQuery);

        statement.addBatch("DELETE FROM dbReferences WHERE entityId=" + validateValue(diagram.getName()));
        statement.addBatch("DELETE FROM publicationReferences WHERE entityId=" + validateValue(diagram.getName()));
        diagramInfoTransformer.addInsertDBRefAndPublicationsCommands(statement, kernel);

        statement.executeBatch();
        statement.clearBatch();

        updateXmlValue(diagram);
    }

    @Override
    public void addInsertCommands(Statement statement, Diagram diagram) throws SQLException, Exception
    {
        DiagramInfo kernel = (DiagramInfo)diagram.getKernel();
        if( kernel == null )
            kernel = new DiagramInfo(diagram.getName());
        diagramInfoTransformer.addInsertCommands(statement, kernel);
        statement.executeBatch();
        statement.clearBatch();

        updateXmlValue(diagram);
    }

    public static class SqlReferenceGenerator implements ImageGenerator.ReferenceGenerator
    {
        public SqlReferenceGenerator(Connection connection)
        {
        }

        @Override
        public String getReference(Object obj)
        {
            if( obj instanceof DiagramElement )
            {
                DiagramElement de = (DiagramElement)obj;
                if( de.getKernel() != null )
                {
                    String type = de.getKernel().getType();
                    if( type != null && !type.startsWith("note") && !type.startsWith("math") )
                        return "view?type=" + de.getKernel().getType() + "&id=" + de.getKernel().getName();
                }
            }
            return null;
        }

        @Override
        public String getTarget(Object obj)
        {
            return "entityView";
        }

        @Override
        public String getTitle(Object obj)
        {
            return null;
        }
    }

    @Override
    public String[] getUsedTables()
    {
        return new String[] {"dbReferences", "publicationReferences", "publications", table};
    }

    @Override
    public String getCreateTableQuery(String tableName)
    {
        if( tableName.equals(table) )
        {
            return "CREATE TABLE `diagrams` ("
                    + diagramInfoTransformer.getIDFieldFormat() + ","
                    + "  `type` enum('unknown','semantic-concept','semantic-concept-function','semantic-concept-process','semantic-concept-state','molecule','molecule-gene','molecule-RNA','molecule-protein','molecule-substance','compartment','compartment-cell','reaction','relation','relation-semantic','relation-chemical','info-database','info-diagram','info-relation-type','info-species','info-unit','constant') NOT NULL default 'info-diagram',"
                    + diagramInfoTransformer.getTitleFieldFormat()+ "," + "  `completeName` varchar(200) default NULL,"
                    + "  `description` text," + "  `comment` text," + "  `xml` mediumblob,"
                    + "  `mimeType` varchar(50) NOT NULL default 'image/png'," + "  `image` mediumblob," + "  `map` mediumblob,"
                    + "  `attributes` text,"
                    + "  UNIQUE KEY `IDX_UNIQUE_diagrams_ID` (`ID`)" + ") ENGINE=MyISAM CHARSET=utf8";
        }
        return super.getCreateTableQuery(tableName);
    }
}
