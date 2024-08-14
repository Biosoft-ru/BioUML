package biouml.plugins.gtrd.utils;

// 09.06.22
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import org.apache.commons.lang.ArrayUtils;

import biouml.plugins.machinelearning.utils.DataMatrix;
import biouml.plugins.machinelearning.utils.DataMatrixString;
import biouml.plugins.machinelearning.utils.MatrixUtils;
import biouml.plugins.machinelearning.utils.PrimitiveOperations;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.sql.Connectors;
import ru.biosoft.bsa.Alphabet;
import ru.biosoft.bsa.BindingElement;
import ru.biosoft.bsa.Nucleotide15LetterAlphabet;
import ru.biosoft.bsa.analysis.FrequencyMatrix;

public class DataBaseUtils
{
	public static class CisBpUtils
	{
		private static Connection connection;
        
    	private static Connection getConnection()
        {
    		try
    		{
				if( connection == null || connection.isClosed() )
				{
					connection = Connectors.getConnection( "gtrd_v1903" );
				}
			}
    		catch (SQLException e)
    		{
				e.printStackTrace();
			}
    		return connection;
        }

        // 14.04.22
    	// It is modified
        /************************************* From Mike ***********************/
        public static String getCisBpID(String uniprotID)
        {
        	String result = null;
    		String query = "select cis_bp from tf_links where gtrd_uniprot_id=?";
    		Connection con = null;
			con = getConnection();
    		try(PreparedStatement ps = con.prepareStatement(query))
    		{
    			ps.setString(1, uniprotID);
    			try (ResultSet rs = ps.executeQuery();)
    			{
    				while( rs.next() )
    				{
    					result = rs.getString(1);
    				}
    			}
    		}
    		catch (SQLException e)
    		{
    			e.printStackTrace();
    		}
    		return result;
        }

        // TODO: under construction
        // Read matrix in text file
//        public static FrequencyMatrix getFrequencyMatrixFromTextFile(DataCollection<?> origin, DataElementPath pathToFolder, String matrixName)
//        {
//        	Alphabet alphabet = Nucleotide15LetterAlphabet.getInstance();
//        	DataElementPath pathToMatrix = pathToFolder.getChildPath(matrixName);
//        	DataMatrix dataMatrix = new DataMatrix(pathToMatrix, new String[]{"A", "C", "G", "T"});
//        	//dataMatrix.transpose();
//
//        	
////TODO
//    		log.info("--- Input dataMatrix:--------------------- matrixName = " + matrixName);
//        	DataMatrix.printDataMatrix(dataMatrix);
//        	
//        	String[] rowNames = dataMatrix.getRowNames();
//        	byte[] consensusBytes = new byte[rowNames.length];
//            for( int i = 0; i < consensusBytes.length; i++ )
//            	consensusBytes[i] = (byte)rowNames[i].getBytes()[0];
//        	
//            double[][] matrix = new double[consensusBytes.length - alphabet.codeLength() + 1][alphabet.size()];
//            String name = ! matrixName.contains(".txt" ) ? matrixName : matrixName.replace(".txt", "_4");
//
//            
///// TODO temporary --------------------------------
///////////////////////////////////
//    		log.info("--- matrixName = " + matrixName + " name = " + name);
//    		log.info("--- Output matrix:");
//            MatrixUtils.printMatrix(matrix);
//            for( int i = 0; i < matrix.length; i++ )
//            {
//                byte code = alphabet.lettersToCode(consensusBytes, i);
//        		log.info("--- i = " + i + " code = " + code);
//            	if( code == Alphabet.ERROR_CHAR || code == Alphabet.IGNORED_CHAR )
//            		log.info("--- error!!! ");
//            }
/////////////////////////////////////////                
//
//            
//            for( int i = 0; i < matrix.length; i++ )
//            {
//                byte code = alphabet.lettersToCode(consensusBytes, i);
//                if( code == Alphabet.ERROR_CHAR || code == Alphabet.IGNORED_CHAR ) return null;
//                for( byte basicCode : alphabet.basicCodes(code) )
//                	matrix[i][basicCode] = 1;
//            }
//        	return new FrequencyMatrix(origin, name, alphabet, null, matrix, false);
//        }
        
        // 27.05.22
        public static DataMatrix getDataMatrix(DataElementPath pathToTextFileWithCisBpInformation, DataElementPath pathToFolderWithTextMatricesFiles, String uniprotId)
        {
        	DataMatrixString dms = new DataMatrixString(pathToTextFileWithCisBpInformation, new String[]{"Motif_ID"});
        	String[] cisBpIds = dms.getRowNames(), cisBpMatrixNames = dms.getColumn("Motif_ID");
        	String cisBpId = getCisBpID(uniprotId);
        	int index = ArrayUtils.indexOf(cisBpIds, cisBpId);
            DataElementPath pathToMatrix = pathToFolderWithTextMatricesFiles.getChildPath(cisBpMatrixNames[index] + ".txt");
            if( ! pathToMatrix.exists() ) return null;
        	DataMatrix dataMatrix = new DataMatrix(pathToMatrix, new String[]{"A", "C", "G", "T"});
        	dataMatrix.transpose();
        	return dataMatrix;
        }
        
        public static String getConsensusFromMatrix(DataElementPath pathToTextFileWithCisBpInformation, DataElementPath pathToFolderWithTextMatricesFiles, String uniprotId)
        {
        	DataMatrixString dms = new DataMatrixString(pathToTextFileWithCisBpInformation, new String[]{"Motif_ID"});
        	String[] cisBpIds = dms.getRowNames(), cisBpMatrixNames = dms.getColumn("Motif_ID");
        	String cisBpId = getCisBpID(uniprotId);
        	int index = ArrayUtils.indexOf(cisBpIds, cisBpId);
        	return index < 0 ? null : getConsensusFromMatrix(pathToFolderWithTextMatricesFiles, cisBpMatrixNames[index] + ".txt");
        }


        private static String getConsensusFromMatrix(DataElementPath pathToFolderWithTextMatricesFiles, String fileName)
        {
        	String consensus = "";
            DataElementPath pathToMatrix = pathToFolderWithTextMatricesFiles.getChildPath(fileName);
            if( ! pathToMatrix.exists() ) return null;
        	DataMatrix dataMatrix = new DataMatrix(pathToMatrix, new String[]{"A", "C", "G", "T"});
        	double [][] matrix = dataMatrix.getMatrix();
        	if( matrix == null ) return null;
        	dataMatrix.transpose();
        	matrix = dataMatrix.getMatrix();
        	String[] rowNames = dataMatrix.getRowNames();
        	
        	// 21.04.22
//            for( int i = 0; i < dataMatrix.getColumnNames().length; i ++ )
//            	consensus += rowNames[(int)PrimitiveOperations.getMax(MatrixUtils.getColumn(matrix, i))[0]];
            for( int i = 0; i < dataMatrix.getColumnNames().length; i ++ )
            {
            	double[] column = MatrixUtils.getColumn(matrix, i);
            	PrimitiveOperations.getMax(column);
            	
            	Object[] objects = PrimitiveOperations.getMax(column);
            	int index = (int) objects[0];
            	double max = (double) objects[1];
            	consensus += max > 0.6 ? rowNames[index] : "N";
            }
            return consensus;
        }
	}

	static Logger log = Logger.getLogger(DataBaseUtils.class.getName());
}
