package ru.biosoft.table;

/* ====================================================================
   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  See the NOTICE file distributed with
   this work for additional information regarding copyright ownership.
   The ASF licenses this file to You under the Apache License, Version 2.0
   (the "License"); you may not use this file except in compliance with
   the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
==================================================================== */

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import one.util.streamex.StreamEx;

import org.apache.poi.hssf.eventusermodel.FormatTrackingHSSFListener;
import org.apache.poi.hssf.eventusermodel.HSSFEventFactory;
import org.apache.poi.hssf.eventusermodel.HSSFListener;
import org.apache.poi.hssf.eventusermodel.HSSFRequest;
import org.apache.poi.hssf.eventusermodel.MissingRecordAwareHSSFListener;
import org.apache.poi.hssf.eventusermodel.EventWorkbookBuilder.SheetRecordCollectingListener;
import org.apache.poi.hssf.eventusermodel.dummyrecord.LastCellOfRowDummyRecord;
import org.apache.poi.hssf.eventusermodel.dummyrecord.MissingCellDummyRecord;
import org.apache.poi.hssf.model.HSSFFormulaParser;
import org.apache.poi.hssf.record.BOFRecord;
import org.apache.poi.hssf.record.BlankRecord;
import org.apache.poi.hssf.record.BoolErrRecord;
import org.apache.poi.hssf.record.BoundSheetRecord;
import org.apache.poi.hssf.record.FormulaRecord;
import org.apache.poi.hssf.record.LabelRecord;
import org.apache.poi.hssf.record.LabelSSTRecord;
import org.apache.poi.hssf.record.NoteRecord;
import org.apache.poi.hssf.record.NumberRecord;
import org.apache.poi.hssf.record.RKRecord;
import org.apache.poi.hssf.record.Record;
import org.apache.poi.hssf.record.SSTRecord;
import org.apache.poi.hssf.record.StringRecord;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;

/**
 * A XLS -> CSV processor, that uses the MissingRecordAware
 *  EventModel code to ensure it outputs all columns and rows.
 * @author Nick Burch
 * Modified by lan
 */
public class XLSToTabConverter implements HSSFListener, XLSandXLSXConverters
{
    protected int minColumns;
    protected POIFSFileSystem fs;

    /** Should we output the formula, or the value it has? */
    protected boolean outputFormulaValues = true;

    /** For parsing Formulas */
    protected SheetRecordCollectingListener workbookBuildingListener;
    protected HSSFWorkbook stubWorkbook;

    protected int lastRowNumber = 0;
    protected int lastColumnNumber = 0;

    // For handling formulas with string results
    protected int nextRow = 0;
    protected int nextColumn = 0;
    boolean outputNextStringRecord;

    // Records we pick up as we process
    protected SSTRecord sstRecord;
    protected FormatTrackingHSSFListener formatListener;

    /** So we known which sheet we're on */
    protected int sheetIndex = -1;
    protected BoundSheetRecord[] orderedBSRs;
    protected List<BoundSheetRecord> boundSheetRecords = new ArrayList<>();
    List<StringBuilder> sheetData = new ArrayList<>();

    /**
     * Creates a new XLS -> CSV converter
     * @param fs The POIFSFileSystem to process
     * @param output The PrintStream to output the CSV to
     * @param minColumns The minimum number of columns to output, or -1 for no minimum
     */
    public XLSToTabConverter(POIFSFileSystem fs, int minColumns)
    {
        this.fs = fs;
        this.minColumns = minColumns;
    }

    /**
     * Creates a new XLS -> CSV converter
     * @param file The file to process
     * @param minColumns The minimum number of columns to output, or -1 for no minimum
     * @throws IOException
     * @throws FileNotFoundException
     */
    public XLSToTabConverter(File file) throws IOException, FileNotFoundException
    {
        this(new POIFSFileSystem(new FileInputStream(file)), -1);
    }

    /**
     * Initiates the processing of the XLS file to CSV
     */
    @Override
    public void process() throws IOException
    {
        MissingRecordAwareHSSFListener listener = new MissingRecordAwareHSSFListener(this);
        formatListener = new FormatTrackingHSSFListener(listener);

        HSSFEventFactory factory = new HSSFEventFactory();
        HSSFRequest request = new HSSFRequest();

        if( outputFormulaValues )
        {
            request.addListenerForAllRecords(formatListener);
        }
        else
        {
            workbookBuildingListener = new SheetRecordCollectingListener(formatListener);
            request.addListenerForAllRecords(workbookBuildingListener);
        }

        factory.processWorkbookEvents(request, fs);
    }

    @Override
    public String[] getSheetNames()
    {
        return StreamEx.of( orderedBSRs ).map( BoundSheetRecord::getSheetname ).toArray( String[]::new );
    }

    @Override
    public String getSheetData(int sheetIndex)
    {
        return sheetData.get(sheetIndex).toString();
    }

    /**
     * Main HSSFListener method, processes events, and outputs the
     *  CSV as the file is processed.
     */
    @Override
    public void processRecord(Record record)
    {
        int thisRow = -1;
        int thisColumn = -1;
        String thisStr = null;

        switch( record.getSid() )
        {
            case BoundSheetRecord.sid:
                boundSheetRecords.add((BoundSheetRecord)record);
                break;
            case BOFRecord.sid:
                BOFRecord br = (BOFRecord)record;
                if( br.getType() == BOFRecord.TYPE_WORKSHEET )
                {
                    // Create sub workbook if required
                    if( workbookBuildingListener != null && stubWorkbook == null )
                    {
                        stubWorkbook = workbookBuildingListener.getStubHSSFWorkbook();
                    }

                    // Output the worksheet name
                    // Works by ordering the BSRs by the location of
                    //  their BOFRecords, and then knowing that we
                    //  process BOFRecords in byte offset order
                    sheetIndex++;
                    if( orderedBSRs == null )
                    {
                        orderedBSRs = BoundSheetRecord.orderByBofPosition(boundSheetRecords);
                    }
                    sheetData.add(new StringBuilder());
                    /*output.println();
                    output.println(
                        orderedBSRs[sheetIndex].getSheetname() +
                        " [" + (sheetIndex+1) + "]:"
                    );*/
                }
                break;

            case SSTRecord.sid:
                sstRecord = (SSTRecord)record;
                break;

            case BlankRecord.sid:
                BlankRecord brec = (BlankRecord)record;

                thisRow = brec.getRow();
                thisColumn = brec.getColumn();
                thisStr = "";
                break;
            case BoolErrRecord.sid:
                BoolErrRecord berec = (BoolErrRecord)record;

                thisRow = berec.getRow();
                thisColumn = berec.getColumn();
                thisStr = "";
                break;

            case FormulaRecord.sid:
                FormulaRecord frec = (FormulaRecord)record;

                thisRow = frec.getRow();
                thisColumn = frec.getColumn();

                if( outputFormulaValues )
                {
                    if( Double.isNaN(frec.getValue()) )
                    {
                        // Formula result is a string
                        // This is stored in the next record
                        outputNextStringRecord = true;
                        nextRow = frec.getRow();
                        nextColumn = frec.getColumn();
                    }
                    else
                    {
                        thisStr = formatListener.formatNumberDateCell(frec);
                    }
                }
                else
                {
                    thisStr = HSSFFormulaParser.toFormulaString(stubWorkbook, frec.getParsedExpression());
                }
                break;
            case StringRecord.sid:
                if( outputNextStringRecord )
                {
                    // String for formula
                    StringRecord srec = (StringRecord)record;
                    thisStr = srec.getString();
                    thisRow = nextRow;
                    thisColumn = nextColumn;
                    outputNextStringRecord = false;
                }
                break;

            case LabelRecord.sid:
                LabelRecord lrec = (LabelRecord)record;

                thisRow = lrec.getRow();
                thisColumn = lrec.getColumn();
                thisStr = lrec.getValue();
                break;
            case LabelSSTRecord.sid:
                LabelSSTRecord lsrec = (LabelSSTRecord)record;

                thisRow = lsrec.getRow();
                thisColumn = lsrec.getColumn();
                if( sstRecord == null )
                {
                    thisStr = "(No SST Record, can't identify string)";
                }
                else
                {
                    thisStr = sstRecord.getString(lsrec.getSSTIndex()).toString();
                }
                break;
            case NoteRecord.sid:
                NoteRecord nrec = (NoteRecord)record;

                thisRow = nrec.getRow();
                thisColumn = nrec.getColumn();
                // TODO: Find object to match nrec.getShapeId()
                thisStr = "(TODO)";
                break;
            case NumberRecord.sid:
                NumberRecord numrec = (NumberRecord)record;

                thisRow = numrec.getRow();
                thisColumn = numrec.getColumn();

                // Format
                thisStr = String.valueOf( numrec.getValue() );
                break;
            case RKRecord.sid:
                RKRecord rkrec = (RKRecord)record;

                thisRow = rkrec.getRow();
                thisColumn = rkrec.getColumn();
                thisStr = "(TODO)";
                break;
            default:
                break;
        }

        // Handle new row
        if( thisRow != -1 && thisRow != lastRowNumber )
        {
            lastColumnNumber = -1;
        }

        // Handle missing column
        if( record instanceof MissingCellDummyRecord )
        {
            MissingCellDummyRecord mc = (MissingCellDummyRecord)record;
            thisRow = mc.getRow();
            thisColumn = mc.getColumn();
            thisStr = "";
        }

        // If we got something to print out, do so
        if( thisStr != null && sheetIndex >= 0 )
        {
            StringBuilder out = sheetData.get(sheetIndex);
            if( thisColumn > 0 )
            {
                //output.print(',');
                out.append("\t");
            }
            //output.print(thisStr);
            out.append(thisStr);
        }

        // Update column and row count
        if( thisRow > -1 )
            lastRowNumber = thisRow;
        if( thisColumn > -1 )
            lastColumnNumber = thisColumn;

        // Handle end of row
        if( record instanceof LastCellOfRowDummyRecord )
        {
            StringBuilder out = sheetData.get(sheetIndex);
            // Print out any missing commas if needed
            if( minColumns > 0 )
            {
                // Columns are 0 based
                if( lastColumnNumber == -1 )
                {
                    lastColumnNumber = 0;
                }
                for( int i = lastColumnNumber; i < ( minColumns ); i++ )
                {
                    out.append("\t");
                    //output.print(',');
                }
            }

            // We're onto a new row
            lastColumnNumber = -1;

            // End the row
            out.append("\n");
            //output.println();
        }
    }
}
