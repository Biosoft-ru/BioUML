package ru.biosoft.table;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public abstract class XLSandXLSXToTabConvertersNew implements XLSandXLSXConverters
{
    final Workbook workbook;
    private final ArrayList<String> sheetNames = new ArrayList<>();
    private List<StringBuilder> sheetData = new ArrayList<>();

    private XLSandXLSXToTabConvertersNew(Workbook workbook)
    {
        this.workbook = workbook;
    }

    public static XLSandXLSXConverters getConverter(File file)
    {
        try
        {
            return ( new XLSandXLSXToTabConvertersNew.XLSToTabConverterNew( file ) );
        }
        catch( Exception ex1 )
        {
            try
            {
                return ( new XLSandXLSXToTabConvertersNew.XLSXToTabConverterNew( file ) );
            }
            catch( Exception ex2 )
            {
                return null;
            }
        }
    }

    @Override
    public void process()
    {
        sheetData = new ArrayList<>();

        Iterator<Sheet> sheetIterator = workbook.sheetIterator();
        while( sheetIterator.hasNext() )
        {
            Sheet sheet = sheetIterator.next();
            sheetNames.add( sheet.getSheetName() );

            StringBuilder curData = new StringBuilder();
            //TODO: process first lines to obtain real column number
            int maxColumns = 0;
            for( Row row : sheet )
            {
                // This whole row is empty
                if( row == null )
                    continue;

                //we assume that row with column names does not contain empty lines
                int lastColumn = Math.max( getLastDataCellNum( row ), maxColumns );
                if( maxColumns < lastColumn )
                    maxColumns = lastColumn;

                for( int cn = 0; cn < lastColumn; cn++ )
                {
                    Cell cell = row.getCell( cn, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL );
                    if( cell != null )
                    {
                        switch( cell.getCellTypeEnum() )
                        {
                            case STRING:
                                String stringValue = cell.getStringCellValue();
                                if( shouldEscape( stringValue ) )
                                    curData.append( '"' ).append( stringValue ).append( '"' );
                                else
                                    curData.append( stringValue );
                                break;
                            case BOOLEAN:
                                curData.append( cell.getBooleanCellValue() );
                                break;
                            case NUMERIC:
                                curData.append( cell.getNumericCellValue() );
                                break;
                            case FORMULA:
                                //TODO: support formula
                                break;
                            default:
                                //we ignore other types
                                break;
                        }
                    }
                    if( cn < lastColumn - 1 )
                        curData.append( '\t' );
                }
                curData.append( '\n' );
            }
            sheetData.add( curData );
        }
    }

    public static int getLastDataCellNum(Row row)
    {
        if( row == null )
            return 0;

        short lastDefined = row.getLastCellNum();
        if( lastDefined <= 0 )
            return 0; // Row has no cells defined

        // Iterate backwards from the last defined cell (0-based index = lastDefined - 1)
        for ( int i = lastDefined - 1; i >= 0; i-- )
        {
            Cell cell = row.getCell( i, Row.MissingCellPolicy.RETURN_NULL_AND_BLANK );
            if( cell == null )
                continue;

            CellType type = cell.getCellTypeEnum();
            if( type == CellType.BLANK )
                continue;

            // Handle empty/whitespace strings
            if( type == CellType.STRING )
            {
                String val = cell.getStringCellValue();
                if( val == null || val.trim().isEmpty() )
                    continue;
            }

            if( type == CellType.FORMULA && cell.getCachedFormulaResultTypeEnum() == CellType.BLANK )
                continue;

            return i + 1; // Found last non-empty cell, return it's number (index+1)
        }
        return 0; // No actual data in this row
    }

    private boolean shouldEscape(String stringValue)
    {
        return ( stringValue.contains( "\t" ) || stringValue.contains( "\n" ) )
                && ! ( stringValue.startsWith( "\"" ) && stringValue.endsWith( "\"" ) );
    }

    @Override
    public String[] getSheetNames()
    {
        return sheetNames.toArray( new String[sheetNames.size()] );
    }

    @Override
    public String getSheetData(int numberOfSheet)
    {
        return sheetData.get( numberOfSheet ).toString();
    }

    public static class XLSToTabConverterNew extends XLSandXLSXToTabConvertersNew
    {
        public XLSToTabConverterNew(File file) throws IOException, FileNotFoundException
        {
            super( new HSSFWorkbook( new FileInputStream( new File( file.getPath() ) ) ) );
        }
    }

    public static class XLSXToTabConverterNew extends XLSandXLSXToTabConvertersNew
    {
        public XLSXToTabConverterNew(File file) throws IOException, FileNotFoundException
        {
            super( new XSSFWorkbook( new FileInputStream( new File( file.getPath() ) ) ) );
        }
    }
}
