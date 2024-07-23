package gov.nih.nci.evs.restapi.util;

import java.io.*;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.codec.binary.Base64;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFClientAnchor;
import org.apache.poi.hssf.usermodel.HSSFDataFormatter;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFPalette;
import org.apache.poi.hssf.usermodel.HSSFPicture;
import org.apache.poi.hssf.usermodel.HSSFPictureData;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFShape;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ooxml.POIXMLDocument;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class ExcelUtils {

    final private StringBuilder out = new StringBuilder(65536);

    private short colIndex;
    private int rowIndex, mergeStart, mergeEnd;

    final private static String XLSX_FORMAT = "xlsx";
    final private static String XLS_FORMAT = "xls";
    final private static String UNKNOWN_FORMAT = "unknown";

    public static final String[] FILE_TYPES = new String[] {XLS_FORMAT, XLSX_FORMAT};

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
				cell.setCellValue(Integer.valueOf(Integer.parseInt(value)));
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

    private String getFileType(File file) {
		String fileName = file.getName();
		String folderName = file.getParent();
		if (fileName.toLowerCase().endsWith(FILE_TYPES[0])) {
			return FILE_TYPES[0];
		}
		return FILE_TYPES[1];
	}


    public static String getExcelHeader(String filename, int sheet) {
		if (filename.toLowerCase().endsWith(FILE_TYPES[0])) {
			return getHSSFHeader(filename, sheet);
		} else {
			return getXSSFHeader(filename, sheet);
		}
	}

    public static String getHSSFHeader(String file, int sheet) {
		StringBuffer buf = new StringBuffer();
		try {
			FileInputStream fis = new FileInputStream(new File(file));
			//Get the workbook instance for XLS file
			HSSFWorkbook workbook = new HSSFWorkbook(fis);
			try {
				fis.close();
			} catch (Exception ex) {
                ex.printStackTrace();
			}

			//Get first sheet from the workbook
			HSSFSheet hSSFSheet = workbook.getSheetAt(sheet);
			HSSFRow row = hSSFSheet.getRow(0);

			int cells = row.getPhysicalNumberOfCells();
			for (int c = 0; c < cells; c++) {
				HSSFCell cell = row.getCell(c);
				String value = null;
				/*

				switch (cell.getCellType()) {
					case HSSFCell.CELL_TYPE_FORMULA:
						value = cell.getCellFormula();
						break;

					case HSSFCell.CELL_TYPE_NUMERIC:
						value = "" + cell.getNumericCellValue();
						break;

					case HSSFCell.CELL_TYPE_STRING:
						value = cell.getStringCellValue();
						break;

					default:
				}
				*/
				value = getCellData(cell);
				buf.append(value);
				if (c < cells-1) {
					buf.append("|");
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return buf.toString();
	}

    public static int getExcelStartRow(String filename, int sheet, int col, String code) {
		if (filename.toLowerCase().endsWith(FILE_TYPES[0])) {
			return getHSSFStartRow(filename, sheet, col, code);
		} else {
			return getXSSFStartRow(filename, sheet, col, code);
		}
	}

     public static int getHSSFStartRow(String file, int sheet, int col, String code) {
		try {
			FileInputStream fis = new FileInputStream(new File(file));
			//Get the workbook instance for XLS file
			HSSFWorkbook workbook = new HSSFWorkbook(fis);
			try {
				fis.close();
			} catch (Exception ex) {
                ex.printStackTrace();
			}

			//Get first sheet from the workbook
			HSSFSheet hSSFSheet = workbook.getSheetAt(sheet);

			if (col == -1) {
				return 1;
			}

			//Get iterator to all the rows in current sheet
			Iterator<Row> rowIterator = hSSFSheet.iterator();

			//Get iterator to all cells of current row
			int lcv = 0;
			while (rowIterator.hasNext()) {
				Row row = rowIterator.next();
				if (row == null) return -1;
				//if (row.getCell(0).getStringCellValue().compareTo(code) == 0 ||
				if (row.getCell(col).getStringCellValue().compareTo(code) == 0) {
					return lcv;
				}

				lcv++;
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return -1;
	 }

    public static int getExcelEndRow(String filename, int sheet, int col, String code) {
		if (filename.toLowerCase().endsWith(FILE_TYPES[0])) {
			return getHSSFEndRow(filename, sheet, col, code);
		} else {
			return getXSSFEndRow(filename, sheet, col, code);
		}
	}

     public static int getHSSFEndRow(String file, int sheet, int col, String code) {
		int num = -1;
		try {
			FileInputStream fis = new FileInputStream(new File(file));
			//Get the workbook instance for XLS file
			HSSFWorkbook workbook = new HSSFWorkbook(fis);
			try {
				fis.close();
			} catch (Exception ex) {
                ex.printStackTrace();
			}

			//Get first sheet from the workbook
			HSSFSheet hSSFSheet = workbook.getSheetAt(sheet);

			if (col == -1) {
				return hSSFSheet.getLastRowNum();
			}

			//Get iterator to all the rows in current sheet
			Iterator<Row> rowIterator = hSSFSheet.iterator();

			//Get iterator to all cells of current row
			int lcv = 0;

			while (rowIterator.hasNext()) {
				Row row = rowIterator.next();
				if (row == null) return -1;
				//if (row.getCell(0).getStringCellValue().compareTo(code) == 0 ||
				if (row.getCell(col).getStringCellValue().compareTo(code) == 0) {
					num = lcv;
				}
				lcv++;
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return num;
	}


	private static String getCellData(Cell cell) {
		String value = null;
		if (cell == null) {
			return null;
		}
		switch (cell.getCellType()) {
			case STRING:
				value = cell.getStringCellValue();
				break;
			case FORMULA:
				value = cell.getCellFormula();
				break;
			case NUMERIC:
				HSSFDataFormatter dataFormatter = new HSSFDataFormatter();
				value = dataFormatter.formatCellValue(cell);
				break;
			case BLANK:
				value = null;
				break;
			case ERROR:
				value = "#ERROR#";
				break;
		}
		return value;
	}


     public static String getXSSFHeader(String file, int sheet) {
		StringBuffer buf = new StringBuffer();
		try {
			FileInputStream fis = new FileInputStream(new File(file));
			//Get the workbook instance for XLS file
			XSSFWorkbook workbook = new XSSFWorkbook(fis);
			try {
				fis.close();
			} catch (Exception ex) {
                ex.printStackTrace();
			}

			//Get first sheet from the workbook
			XSSFSheet hSSFSheet = workbook.getSheetAt(sheet);
			XSSFRow row = hSSFSheet.getRow(0);

			int cells = row.getPhysicalNumberOfCells();
			for (int c = 0; c < cells; c++) {
				XSSFCell cell = row.getCell(c);

				/*
				String value = null;

				switch (cell.getCellType()) {

					case XSSFCell.CELL_TYPE_FORMULA:
						value = cell.getCellFormula();
						break;

					case XSSFCell.CELL_TYPE_NUMERIC:
						value = "" + cell.getNumericCellValue();
						break;

					case XSSFCell.CELL_TYPE_STRING:
						value = cell.getStringCellValue();
						break;

					default:
				}
				*/
				String value = getCellData(cell);
				buf.append(value);
				if (c < cells-1) {
					buf.append("|");
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return buf.toString();
	 }

     public static int getXSSFStartRow(String file, int sheet, int col, String code) {
		try {
			FileInputStream fis = new FileInputStream(new File(file));
			//Get the workbook instance for XLS file
			XSSFWorkbook workbook = new XSSFWorkbook(fis);
			try {
				fis.close();
			} catch (Exception ex) {
                ex.printStackTrace();
			}

			//Get first sheet from the workbook
			XSSFSheet hSSFSheet = workbook.getSheetAt(sheet);

			if (col == -1) {
				return 1;
			}

			//Get iterator to all the rows in current sheet
			Iterator<Row> rowIterator = hSSFSheet.iterator();

			//Get iterator to all cells of current row
			int lcv = 0;
			while (rowIterator.hasNext()) {
				Row row = rowIterator.next();
				if (row == null) return -1;
				//if (row.getCell(0).getStringCellValue().compareTo(code) == 0 ||
				if (row.getCell(col).getStringCellValue().compareTo(code) == 0) {
					return lcv;
				}

				lcv++;
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return -1;
	 }

     public static int getXSSFEndRow(String file, int sheet, int col, String code) {
		int num = -1;
		try {
			FileInputStream fis = new FileInputStream(new File(file));
			//Get the workbook instance for XLS file
			XSSFWorkbook workbook = new XSSFWorkbook(fis);
			try {
				fis.close();
			} catch (Exception ex) {
                ex.printStackTrace();
			}

			//Get first sheet from the workbook
			XSSFSheet hSSFSheet = workbook.getSheetAt(sheet);

			if (col == -1) {
				return hSSFSheet.getLastRowNum();
			}

			//Get iterator to all the rows in current sheet
			Iterator<Row> rowIterator = hSSFSheet.iterator();

			//Get iterator to all cells of current row
			int lcv = 0;

			while (rowIterator.hasNext()) {
				Row row = rowIterator.next();
				if (row == null) return -1;
				if (row.getCell(col).getStringCellValue().compareTo(code) == 0) {
					num = lcv;
				}
				lcv++;
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return num;
	}

    public static void test(String excelfile, int sheet, int col, String code) {
		System.out.println("excelfile: " + excelfile);
		System.out.println("sheet: " + sheet);
		System.out.println("col: " + col);
		System.out.println("code: " + code);

		String header = getExcelHeader(excelfile, sheet);
		System.out.println(header);

		int start_row = getExcelStartRow(excelfile, sheet, col, code);
		System.out.println("getExcelStartRow: " + start_row);

		int end_row = getExcelEndRow(excelfile, sheet, col, code);
		System.out.println("getExcelEndRow: " + end_row);
	}

	public static void csvToXLSX(String csvfile, String xlsxfile, String sheet_name) {
		try {
			String csvFileAddress = csvfile;
			String xlsxFileAddress = xlsxfile;
			XSSFWorkbook workBook = new XSSFWorkbook();
			XSSFSheet sheet = workBook.createSheet(sheet_name);
			String currentLine=null;
			int RowNum=0;
			BufferedReader br = new BufferedReader(new FileReader(csvFileAddress));
			while ((currentLine = br.readLine()) != null) {
				String str[] = currentLine.split(",");

				XSSFRow currentRow=sheet.createRow(RowNum);
				for(int i=0;i<str.length;i++){
					currentRow.createCell(i).setCellValue(str[i]);
				}
				RowNum++;
			}

			FileOutputStream fileOutputStream =  new FileOutputStream(xlsxFileAddress);
			workBook.write(fileOutputStream);
			fileOutputStream.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public static void csvToXLS(String csvfile, String xlsfile, String sheet_name) {
		try {
			String csvFileAddress = csvfile;
			String xlsFileAddress = xlsfile;
			HSSFWorkbook workBook = new HSSFWorkbook();
			HSSFSheet sheet = workBook.createSheet(sheet_name);
			String currentLine=null;
			int RowNum=0;
			BufferedReader br = new BufferedReader(new FileReader(csvFileAddress));
			while ((currentLine = br.readLine()) != null) {
				String str[] = currentLine.split(",");

				HSSFRow currentRow=sheet.createRow(RowNum);
				for(int i=0;i<str.length;i++){
					currentRow.createCell(i).setCellValue(str[i]);
				}
				RowNum++;
			}

			FileOutputStream fileOutputStream =  new FileOutputStream(xlsFileAddress);
			workBook.write(fileOutputStream);
			fileOutputStream.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
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
