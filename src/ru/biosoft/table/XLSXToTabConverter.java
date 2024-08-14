package ru.biosoft.table;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.poi.hssf.eventusermodel.HSSFListener;
import org.apache.poi.hssf.record.Record;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.eventusermodel.XSSFReader.SheetIterator;
import org.apache.poi.xssf.model.SharedStringsTable;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class XLSXToTabConverter implements HSSFListener, XLSandXLSXConverters
{
    OPCPackage pkg;
    XSSFReader r;
    SharedStringsTable sst;
    private int sheetIndex=0;
    private final ArrayList<String> sheetNames =new ArrayList<>();
    List<StringBuilder> sheetData = new ArrayList<>();

    public XLSXToTabConverter(File file) throws IOException, FileNotFoundException, OpenXML4JException
    {
        pkg = OPCPackage.open(file.getPath());
        r = new XSSFReader(pkg);
        sst = r.getSharedStringsTable();
    }

    /**
     * @throws InvalidFormatException
     * @throws SAXException
     * Initiates the processing of the XLS file to CSV
     * @throws
     */
    @Override
    public void process() throws IOException, SAXException, InvalidFormatException
    {
        XMLReader parser = fetchSheetParser(sst);
        sheetIndex=0;
        sheetData = new ArrayList<>();
        SheetIterator sheets = ((SheetIterator)r.getSheetsData());
//        SheetIterator sh= new SheetIterator();
        while( sheets.hasNext() )
        {
            sheetData.add(new StringBuilder());
            try (InputStream sheet = sheets.next())
            {
                InputSource sheetSource = new InputSource( sheet );
                sheetNames.add( sheets.getSheetName() );
                parser.parse( sheetSource );
            }
            System.out.println("");
            sheetIndex++;
        }
    }

    @Override
    public String[] getSheetNames()
    {
        return sheetNames.toArray( new String[sheetNames.size()] );
    }

    @Override
    public String getSheetData(int sheetIndex)
    {
        return sheetData.get(sheetIndex).toString();
    }

    public XMLReader fetchSheetParser(SharedStringsTable sst) throws SAXException
    {
        XMLReader parser = XMLReaderFactory.createXMLReader("com.sun.org.apache.xerces.internal.parsers.SAXParser"/*"org.apache.xerces.parsers.SAXParser"*/);
        ContentHandler handler = new SheetHandler(sst);
        parser.setContentHandler(handler);
        return parser;
    }


    private class SheetHandler extends DefaultHandler
    {
        private final SharedStringsTable sst;
        private String lastContents;
        private boolean nextIsString;

        private SheetHandler(SharedStringsTable sst)
        {
            this.sst = sst;
        }

        @Override
        public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException
        {
            // c => cell
            if( name.equals("c") )
            {
                // Print the cell reference
                Pattern p=Pattern.compile("A\\d");
                boolean matched = p.matcher(attributes.getValue("r").substring(0,2)).matches();  // true
                if (matched&&(!attributes.getValue("r").equalsIgnoreCase("A1")))
                {
                    sheetData.get( sheetIndex ).append( "\n" );
                }
                else if (!attributes.getValue("r").equalsIgnoreCase("A1"))
                {
                    sheetData.get(sheetIndex).append("\t");
                }
                // Figure out if the value is an index in the SST
                String cellType = attributes.getValue("t");
                if( cellType != null && cellType.equals("s") )
                {
                    nextIsString = true;
                }
                else
                {
                    nextIsString = false;
                }
            }
            // Clear contents cache
            lastContents = "";
        }

        @Override
        public void endElement(String uri, String localName, String name) throws SAXException
        {
            // Process the last contents as required.
            // Do now, as characters() may be called more than once
            if( nextIsString )
            {
                int idx = Integer.parseInt(lastContents);
                lastContents = new XSSFRichTextString(sst.getEntryAt(idx)).toString();
                nextIsString = false;
            }

            // v => contents of a cell
            // Output after we've seen the string contents
            if( name.equals("v") )
            {
                sheetData.get(sheetIndex).append(lastContents);
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException
        {
            lastContents += new String(ch, start, length);
        }
    }

    /**
     * Main HSSFListener method, processes events, and outputs the
     *  CSV as the file is processed.
     */
    @Override
    public void processRecord(Record record)
    {
        //int thisRow = -1;
        //int thisColumn = -1;
        //        String thisStr = null;
        //
        //        switch( record.getSid() )
        //        {
        //            case BoundSheetRecord.sid:
        //                boundSheetRecords.add(record);
        //                break;
        //            case BOFRecord.sid:
        //                BOFRecord br = (BOFRecord)record;
        //                if( br.getType() == BOFRecord.TYPE_WORKSHEET )
        //                {
        //                    // Create sub workbook if required
        //                    if( workbookBuildingListener != null && stubWorkbook == null )
        //                    {
        //                        stubWorkbook = workbookBuildingListener.getStubHSSFWorkbook();
        //                    }
        //
        //                    // Output the worksheet name
        //                    // Works by ordering the BSRs by the location of
        //                    //  their BOFRecords, and then knowing that we
        //                    //  process BOFRecords in byte offset order
        //                    sheetIndex++;
        //                    if( orderedBSRs == null )
        //                    {
        //                        orderedBSRs = BoundSheetRecord.orderByBofPosition(boundSheetRecords);
        //                    }
        //                    sheetData.add(new StringBuilder());
        //                    /*output.println();
        //                    output.println(
        //                            orderedBSRs[sheetIndex].getSheetname() +
        //                            " [" + (sheetIndex+1) + "]:"
        //                    );*/
        //                }
        //                break;
        //
        //            case SSTRecord.sid:
        //                sstRecord = (SSTRecord)record;
        //                break;
        //
        //            case BlankRecord.sid:
        //                BlankRecord brec = (BlankRecord)record;
        //
        //                thisRow = brec.getRow();
        //                thisColumn = brec.getColumn();
        //                thisStr = "";
        //                break;
        //            case BoolErrRecord.sid:
        //                BoolErrRecord berec = (BoolErrRecord)record;
        //
        //                thisRow = berec.getRow();
        //                thisColumn = berec.getColumn();
        //                thisStr = "";
        //                break;
        //
        //            case FormulaRecord.sid:
        //                FormulaRecord frec = (FormulaRecord)record;
        //
        //                thisRow = frec.getRow();
        //                thisColumn = frec.getColumn();
        //
        //                if( outputFormulaValues )
        //                {
        //                    if( Double.isNaN(frec.getValue()) )
        //                    {
        //                        // Formula result is a string
        //                        // This is stored in the next record
        //                        outputNextStringRecord = true;
        //                        nextRow = frec.getRow();
        //                        nextColumn = frec.getColumn();
        //                    }
        //                    else
        //                    {
        //                        thisStr = formatListener.formatNumberDateCell(frec);
        //                    }
        //                }
        //                else
        //                {
        ////                    thisStr = HSSFFormulaParser.toFormulaString(stubWorkbook, frec.getParsedExpression());
        //                }
        //                break;
        //            case StringRecord.sid:
        //                if( outputNextStringRecord )
        //                {
        //                    // String for formula
        //                    StringRecord srec = (StringRecord)record;
        //                    thisStr = srec.getString();
        //                    thisRow = nextRow;
        //                    thisColumn = nextColumn;
        //                    outputNextStringRecord = false;
        //                }
        //                break;
        //
        //            case LabelRecord.sid:
        //                LabelRecord lrec = (LabelRecord)record;
        //
        //                thisRow = lrec.getRow();
        //                thisColumn = lrec.getColumn();
        //                thisStr = lrec.getValue();
        //                break;
        //            case LabelSSTRecord.sid:
        //                LabelSSTRecord lsrec = (LabelSSTRecord)record;
        //
        //                thisRow = lsrec.getRow();
        //                thisColumn = lsrec.getColumn();
        //                if( sstRecord == null )
        //                {
        //                    thisStr = "(No SST Record, can't identify string)";
        //                }
        //                else
        //                {
        //                    thisStr = sstRecord.getString(lsrec.getSSTIndex()).toString();
        //                }
        //                break;
        //            case NoteRecord.sid:
        //                NoteRecord nrec = (NoteRecord)record;
        //
        //                thisRow = nrec.getRow();
        //                thisColumn = nrec.getColumn();
        //                // TODO: Find object to match nrec.getShapeId()
        //                thisStr = "(TODO)";
        //                break;
        //            case NumberRecord.sid:
        //                NumberRecord numrec = (NumberRecord)record;
        //
        //                thisRow = numrec.getRow();
        //                thisColumn = numrec.getColumn();
        //
        //                // Format
        //                thisStr = ((Double)numrec.getValue()).toString();
        //                break;
        //            case RKRecord.sid:
        //                RKRecord rkrec = (RKRecord)record;
        //
        //                thisRow = rkrec.getRow();
        //                thisColumn = rkrec.getColumn();
        //                thisStr = "(TODO)";
        //                break;
        //            default:
        //                break;
        //        }
        //
        //        // Handle new row
        //        if( thisRow != -1 && thisRow != lastRowNumber )
        //        {
        //            lastColumnNumber = -1;
        //        }
        //
        //        // Handle missing column
        //        if( record instanceof MissingCellDummyRecord )
        //        {
        //            MissingCellDummyRecord mc = (MissingCellDummyRecord)record;
        //            thisRow = mc.getRow();
        //            thisColumn = mc.getColumn();
        //            thisStr = "";
        //        }
        //
        //        // If we got something to print out, do so
        //        if( thisStr != null && sheetIndex >= 0)
        //        {
        //            StringBuilder out = sheetData.get(sheetIndex);
        //            if( thisColumn > 0 )
        //            {
        //                //output.print(',');
        //                out.append("\t");
        //            }
        //            //output.print(thisStr);
        //            out.append(thisStr);
        //        }
        //
        //        // Update column and row count
        //        if( thisRow > -1 )
        //            lastRowNumber = thisRow;
        //        if( thisColumn > -1 )
        //            lastColumnNumber = thisColumn;
        //
        //        // Handle end of row
        //        if( record instanceof LastCellOfRowDummyRecord )
        //        {
        //            StringBuilder out = sheetData.get(sheetIndex);
        //            // Print out any missing commas if needed
        //            if( minColumns > 0 )
        //            {
        //                // Columns are 0 based
        //                if( lastColumnNumber == -1 )
        //                {
        //                    lastColumnNumber = 0;
        //                }
        //                for( int i = lastColumnNumber; i < ( minColumns ); i++ )
        //                {
        //                    out.append("\t");
        //                    //output.print(',');
        //                }
        //            }
        //
        //            // We're onto a new row
        //            lastColumnNumber = -1;
        //
        //            // End the row
        //            out.append("\n");
        //            //output.println();
        //        }
    }
}
