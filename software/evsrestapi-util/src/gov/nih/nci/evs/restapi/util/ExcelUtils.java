package gov.nih.nci.evs.restapi.util;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;

import java.io.*;
import java.util.*;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class ExcelUtils {

    public ExcelUtils() {

    }

	public static void saveToFile(String outputfile, Vector v) {
		PrintWriter pw = null;
		try {
			pw = new PrintWriter(outputfile, "UTF-8");
			if (v != null && v.size() > 0) {
				for (int i=0; i<v.size(); i++) {
					String t = (String) v.elementAt(i);
					pw.println(t);
				}
		    }
		} catch (Exception ex) {

		} finally {
			try {
				pw.close();
				System.out.println("Output file " + outputfile + " generated.");
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}

    public static String getCellValue(Cell cell) {
		if (cell == null) {
			return "";
		}
        switch (cell.getCellType()) {
            case BOOLEAN:
                System.out.print(cell.getBooleanCellValue());
                Boolean bool_obj = cell.getBooleanCellValue();
                boolean bool = Boolean.valueOf(bool_obj);
                return "" + bool;

            case STRING:
                return (cell.getRichStringCellValue().getString());

            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return ("" + cell.getDateCellValue());
                } else {
                    return ("" + cell.getNumericCellValue());
                }

            case FORMULA:
                return(cell.getCellFormula().toString());

            case BLANK:
                return "";

            default:
                return "";
        }
    }

	public static Vector readFile(String filename)
	{
		Vector v = new Vector();
		try {
			BufferedReader in = new BufferedReader(
			   new InputStreamReader(
						  new FileInputStream(filename), "UTF8"));
			String str;
			while ((str = in.readLine()) != null) {
				v.add(str);
			}
            in.close();
		} catch (Exception ex) {
            ex.printStackTrace();
		}
		return v;
	}

    public static Vector parseData(String line, char delimiter) {
		if(line == null) return null;
		Vector w = new Vector();
		StringBuffer buf = new StringBuffer();
		for (int i=0; i<line.length(); i++) {
			char c = line.charAt(i);
			if (c == delimiter) {
				w.add(buf.toString());
				buf = new StringBuffer();
			} else {
				buf.append(c);
			}
		}
		w.add(buf.toString());
		return w;
	}

    public static Vector excel2Text(String excelfile, int sheetIndex) {
		return excel2Text(excelfile, sheetIndex, '\t');
	}

    public static String clone(String excelfile) {
		String target_file = "clone_" + excelfile;;
		FileUtils.copyfile(excelfile, target_file);
		return target_file;
	}

    public static Vector excel2Text(String excelfile, int sheetIndex, char delim) {
		int n = excelfile.lastIndexOf(".");
		String textfile = excelfile.substring(0, n) + ".txt";
		Vector w = new Vector();
		Workbook workbook = null;//
		try {
			workbook = ExcelReader.openWorkbook(excelfile);
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		Sheet sheet = null;
		try {
			sheet = workbook.getSheetAt(sheetIndex);
			textfile = sheet.getSheetName() + ".txt";

		} catch (Exception ex) {
			ex.printStackTrace();
		}

		int numberOfRows = -1;
		try {
			numberOfRows = getNumberOfRows(sheet);
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		for (int i=0; i<numberOfRows; i++) {
			Row row = sheet.getRow(i);
			StringBuffer buf = new StringBuffer();
			int numberOfCells = getNumberOfCells(row);
			for (int j=0; j<numberOfCells; j++) {
			    Cell cell = row.getCell(j);
			    String value = getCellValue(cell);
			    buf.append(value);
				if (j<numberOfCells-1) {
					buf.append(delim);
				}
			}
			w.add(buf.toString());
		}
		saveToFile(textfile, w);
		return w;
	}

    public static CellStyle getCellStype(String excelfile, int sheetNumber, int rowNumber) {
		try {
			Workbook workbook = ExcelReader.openWorkbook(excelfile);
			Sheet sheet = workbook.getSheetAt(sheetNumber);
			Row row = sheet.getRow(rowNumber);
			return row.getRowStyle();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return null;
	}


    public static int getNumberOfSheet(Workbook workbook) {
		return workbook.getNumberOfSheets();
	}

    public static int getNumberOfRows(Sheet sheet) {
		return sheet.getLastRowNum();
	}

    public static int getNumberOfRows(Workbook workbook, int sheetIndex) {
		return workbook.getSheetAt(sheetIndex).getLastRowNum();
	}

    public static int getNumberOfCells(Row row) {
		return row.getPhysicalNumberOfCells();
	}

    public static int getNumberOfCells(Workbook workbook, int sheetIndex, int rowIndex) {
		return getNumberOfCells(workbook.getSheetAt(sheetIndex).getRow(rowIndex));
	}

    public static void dumpMetadata(String excelfile) {
	    Workbook workbook = ExcelReader.openWorkbook(excelfile);
	    int numberOfSheets = workbook.getNumberOfSheets();
	    System.out.println("numberOfSheets: " + numberOfSheets);
	    for (int i=0; i<numberOfSheets; i++) {
		    Sheet sheet = workbook.getSheetAt(i);
		    String sheetName = sheet.getSheetName();
		    System.out.println("sheetName: " + sheetName);
		    int lastRowNum = getNumberOfRows(sheet);
		    int rowCount = lastRowNum + 1;
		    System.out.println("numberOfRows: " + rowCount);
		    for (int j=0; j<rowCount; j++) {
				Row row = sheet.getRow(j);
				short height = row.getHeight();
				int lastCellNum = row.getLastCellNum();
				System.out.println("numberOfCells: " + j + ": " +  lastCellNum + "; height: " + height);
			}
		}
	}

    public static void showHeadingCellStype(String excelfile) {
		int sheetNumber = 0;
		int rowNumber = 0;
	    CellStyle style = getCellStype(excelfile, 0, 0);
	    /*
	    short al = style.getAlignment();
	    short bb = style.getBorderBottom();
	    short bl = style.getBorderLeft();
	    */
	    short al = style.getAlignment().getCode();
	    short bb = style.getBorderBottom().getCode();
  	    short bl = style.getBorderLeft().getCode();

	    System.out.println("getAlignment: " + al);
	    System.out.println("getBorderBottom: " + bb);
	    System.out.println("getBorderLeft: " + bl);

        short fillForegroundColor = style.getFillForegroundColor();
        System.out.println("fillForegroundColor: " + fillForegroundColor);

        short fillBackgroundColor = style.getFillBackgroundColor();
        System.out.println("fillBackgroundColor: " + fillBackgroundColor);

        //int fps = style.getFillPattern();

        //int fps = style.getFillPattern().getCode();
        //System.out.println("getFillPattern: " + fps);
    }

    public static CellStyle createWrappedCellStyle(Sheet sheet, boolean wrapped) {
        CellStyle cellStyle = sheet.getWorkbook().createCellStyle();
		cellStyle.setWrapText(wrapped);
		return cellStyle;
	}

    public static CellStyle createCellStyle(Sheet sheet, int colorIndex, int patternIndex) {
		CellStyle cellStyle = sheet.getWorkbook().createCellStyle();
		if (colorIndex != -1) {
			IndexedColors indexedColors = IndexedColors.fromInt(colorIndex);
			cellStyle.setFillBackgroundColor(indexedColors.index);
		}
		if (patternIndex != -1) {
			FillPatternType fillPatternType = FillPatternType.forInt(patternIndex);
			cellStyle.setFillPattern(fillPatternType);
		}
		return cellStyle;
	}

	public static Sheet createRow(Sheet sheet, int rowNumber, Object[] objArr, int colorIndex, int patternIndex) {
		CellStyle cellStyle = createCellStyle(sheet, colorIndex, patternIndex);
		Row row = sheet.createRow(rowNumber);

		int cellnum = 0;
		for (Object obj : objArr) {
			Cell cell = row.createCell(cellnum++);
			cell.setCellStyle(cellStyle);
			if (obj instanceof String) {
				cell.setCellValue((String) obj);
			} else if (obj instanceof Integer) {
				cell.setCellValue((Integer) obj);
			}
		}
		return sheet;
	}

    public static void write(Workbook workbook, String excelfile) {
		try {
			FileOutputStream fileOut = new FileOutputStream(excelfile);
			workbook.write(fileOut);
			fileOut.close();
			workbook.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

    public static void generateTemplate(String excelfile, int sheetIndex) {
		String clonedfile = clone(excelfile);
		Workbook workbook = ExcelReader.openWorkbook(clonedfile);
		Sheet sheet = workbook.getSheetAt(sheetIndex);
		int numberOfRows = getNumberOfRows(sheet);
		for (int i=1; i<=numberOfRows; i++) {
			Row row = sheet.getRow(i);
			int cellCount = getNumberOfCells(row);
			for (int j=0; j<cellCount; j++) {
				Cell cell = row.getCell(j);
				if (cell != null) {
					row.removeCell(row.getCell(j));
				}
			}
		}
		write(workbook, "template_" + excelfile);
	}


    public static Object[] toObjectArray(Vector v) {
		Object[] objArr = new Object[v.size()];
		for (int i=0; i<v.size(); i++) {
			objArr[i] = v.elementAt(i);
		}
		return objArr;
	}

    public static void write(String template_excelfile, String outputexcel, int sheetIndex, Vector data, char delim) {
		Workbook workbook = ExcelReader.openWorkbook(template_excelfile);
		Sheet sheet = workbook.getSheetAt(sheetIndex);
		for (int i=1; i<data.size(); i++) {
			String line = (String) data.elementAt(i);
			Vector u = parseData(line, delim);
			sheet = createRow(sheet, i, toObjectArray(u), 0, 0);
		}
		write(workbook, outputexcel);
	}

    public static String getRowValues(String excelfile, int sheetIndex, int rowIndex, char delim) {
		Workbook workbook = ExcelReader.openWorkbook(excelfile);
		return getRowValues(workbook, sheetIndex, rowIndex, delim);
	}

    public static String getRowValues(Workbook workbook, int sheetIndex, int rowIndex, char delim) {
		Sheet sheet = workbook.getSheetAt(sheetIndex);
		Row row = sheet.getRow(rowIndex);
		StringBuffer buf = new StringBuffer();
		int numberOfCells = ExcelUtils.getNumberOfCells(row);
		for (int j=0; j<numberOfCells; j++) {
			Cell cell = row.getCell(j);
			String value = getCellValue(cell);
			buf.append(value);
			if (j<numberOfCells-1) {
				buf.append(delim);
			}
		}
		return buf.toString();
	}

    public static Workbook replaceRowValues(Workbook workbook, int sheetIndex, int rowIndex, Vector rowValues) {
		Sheet sheet = workbook.getSheetAt(sheetIndex);
		Row row = sheet.getRow(rowIndex);
		int numberOfCells = ExcelUtils.getNumberOfCells(row);
		for (int j=0; j<numberOfCells; j++) {
			Cell cell = row.getCell(j);
			String value = (String) rowValues.elementAt(j);
			cell.setCellValue(value);
		}
		return workbook;
	}


	public static Workbook deleteColumn(Workbook workbook, int sheetIndex, int columnToDelete ){
		Sheet sheet = workbook.getSheetAt(sheetIndex);
        int maxColumn = 0;
        for (int r=0; r < sheet.getLastRowNum()+1; r++){
            Row row = sheet.getRow(r);
            // if no row exists here; then nothing to do; next!
            if ( row == null )
                continue;

            // if the row doesn't have this many columns then we are good; next!
            int lastColumn = row.getLastCellNum();
            if (lastColumn > maxColumn )
                maxColumn = lastColumn;

            if (lastColumn < columnToDelete )
                continue;

            for (int x=columnToDelete+1; x < lastColumn + 1; x++){
                Cell oldCell    = row.getCell(x-1);
                if ( oldCell != null ) {
                    row.removeCell( oldCell );
				}
                Cell nextCell = row.getCell(x);
                if ( nextCell != null ){
                    Cell newCell    = row.createCell( x-1, nextCell.getCellType());
                    cloneCell(newCell, nextCell);
                }
            }
        }
        // Adjust the column widths
        for ( int c=0; c < maxColumn; c++ ){
            sheet.setColumnWidth( c, sheet.getColumnWidth(c+1) );
        }
        return workbook;
    }

/*
	private static void cloneCell( Cell cNew, Cell cOld ){
		cNew.setCellComment( cOld.getCellComment() );
		cNew.setCellStyle( cOld.getCellStyle() );
		switch ( cNew.getCellType() ){
			case Cell.CELL_TYPE_BOOLEAN:{
				cNew.setCellValue( cOld.getBooleanCellValue() );
				break;
			}
			case Cell.CELL_TYPE_NUMERIC:{
				cNew.setCellValue( cOld.getNumericCellValue() );
				break;
			}
			case Cell.CELL_TYPE_STRING:{
				cNew.setCellValue( cOld.getStringCellValue() );
				break;
			}
			case Cell.CELL_TYPE_ERROR:{
				cNew.setCellValue( cOld.getErrorCellValue() );
				break;
			}
			case Cell.CELL_TYPE_FORMULA:{
				cNew.setCellFormula( cOld.getCellFormula() );
			   break;
			}
		}

	}
*/

	private static void cloneCell( Cell cNew, Cell cOld ){
		cNew.setCellComment( cOld.getCellComment() );
		cNew.setCellStyle( cOld.getCellStyle() );
		switch ( cNew.getCellType() ){
			case BOOLEAN:{
				cNew.setCellValue( cOld.getBooleanCellValue() );
				break;
			}
			case NUMERIC:{
				cNew.setCellValue( cOld.getNumericCellValue() );
				break;
			}
			case STRING:{
				cNew.setCellValue( cOld.getStringCellValue() );
				break;
			}
			case ERROR:{
				cNew.setCellValue( cOld.getErrorCellValue() );
				break;
			}
			case FORMULA:{
				cNew.setCellFormula( cOld.getCellFormula() );
			   break;
			}
		}
	}

	public static boolean isInteger(String input) {
		try {
			Integer.parseInt( input );
			return true;
		}
			catch( Exception e ) {
			return false;
		}
	}

	public static Workbook addRow(Workbook workbook, int sheetIndex, Vector rowData){
		Sheet sheet = workbook.getSheetAt(sheetIndex);
		int lastRowNumber = sheet.getLastRowNum();
		Row row = sheet.createRow(lastRowNumber+1);
		for (int i=0; i<rowData.size(); i++) {
			String value = (String) rowData.elementAt(i);
			Cell cell = row.createCell(i);
			if (!isInteger(value)) {
				cell.setCellValue(value);
			} else {
				cell.setCellValue(new Integer(Integer.parseInt(value)));
			}
		}
		return workbook;
	}

    public static Workbook removeBlankCells(String excelfile, int sheetIndex) {
		Workbook workbook = ExcelReader.openWorkbook(excelfile);
        Sheet sheet = workbook.getSheetAt(sheetIndex);
        for (int i = sheet.getLastRowNum(); i >= 1; i--)
        {
			Row row = sheet.getRow(i);
            if (row != null)
            {
                sheet.removeRow(row);
            }
        }
        int numOfColumns = getNumberOfRows(sheet);
        for (int j = numOfColumns; j >= 1; j--)
        {
            workbook = deleteColumn(workbook, sheetIndex, j);
        }
        return workbook;
	}

	public static void main(String[] args) {
		String excelfile = args[0];
		String sheetNumStr = args[1];
	    int sheet_number = Integer.parseInt(sheetNumStr);
		System.out.println("excelfile: " + excelfile);
		Vector data = excel2Text(excelfile, sheet_number);
        //generateTemplate(excelfile, 0);
        //write("template_" + excelfile, "v2_" + excelfile, 0, data, '\t');
        //String target_file = clone(excelfile);

	}
}
