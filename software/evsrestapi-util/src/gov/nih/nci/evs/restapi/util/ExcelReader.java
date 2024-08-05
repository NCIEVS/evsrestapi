package gov.nih.nci.evs.restapi.util;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row.MissingCellPolicy;

import java.io.File;
import java.io.*;
import java.util.*;
import java.text.*;

public class ExcelReader {
	public static DataFormatter dataFormatter = new DataFormatter();

    public static Vector csv2Text(String xlsxfile, String sheetName) throws Exception {
		DataFormatter dataFormatter  = new DataFormatter();
		File file = new File(xlsxfile);
        FileInputStream ip = new FileInputStream(file);
        Workbook wb = WorkbookFactory.create(ip);
        Sheet sheet = wb.getSheet(sheetName);
		Vector w = new Vector();
        Iterator<Row> rowIterator = sheet.rowIterator();
        while (rowIterator.hasNext()) {
            Row row = rowIterator.next();
            Iterator<Cell> cellIterator = row.cellIterator();
            StringBuffer buf = new StringBuffer();
            while (cellIterator.hasNext()) {
                Cell cell = cellIterator.next();
                String cellValue = dataFormatter.formatCellValue(cell);
                buf.append(cellValue + "\t");
            }
            String t = buf.toString();
            t = t.substring(0, t.length()-1);
            w.add(t);
        }
        ip.close();
        return w;
    }

    public static int getNumberOfSheets(String excelfile) {
		try {
			Workbook workbook = WorkbookFactory.create(new File(excelfile));
			return workbook.getNumberOfSheets();
		} catch (Exception ex) {
			//return -1;
		}
		return -1;
	}

    public static Workbook openWorkbook(String excelfile) {
		try {
			Workbook workbook = WorkbookFactory.create(new File(excelfile));
			return workbook;
		} catch (Exception ex) {
			//return -1;
		}
		return null;
	}

    public static int getNumberOfSheets(Workbook workbook) {
        return workbook.getNumberOfSheets();
	}

    public static Vector getSheetNames(String excelfile) {
		try {
			Workbook workbook = WorkbookFactory.create(new File(excelfile));
			return getSheetNames(workbook);
		} catch (Exception ex) {
			//return -1;
		}
		return null;
	}


    public static Vector getSheetNames(Workbook workbook) {
		Vector<String> v = new Vector();
        Iterator<Sheet> sheetIterator = workbook.sheetIterator();
        System.out.println("Retrieving Sheets using Iterator");
        while (sheetIterator.hasNext()) {
            Sheet sheet = sheetIterator.next();
            v.add(sheet.getSheetName());
        }
        return v;
	}

    public Sheet getSheet(Workbook workbook, int sheetNumber) {
		return workbook.getSheetAt(sheetNumber);
	}


    public Sheet getSheet(String excelfile, int sheetNumber) {
		try {
		     Workbook workbook = openWorkbook(excelfile);
 		     return getSheet(workbook, sheetNumber);
		} catch (Exception e) {

		}
		return null;
	}

    public static Vector toDelimited(String excelfile, char delim) {
		return toDelimited(excelfile, 0, delim);
	}


    private static void printCellValue(Cell cell) {
        //switch (cell.getCellTypeEnum()) {
		switch (cell.getCellType()) {
            case BOOLEAN:
                System.out.print(cell.getBooleanCellValue());
                break;
            case STRING:
                System.out.print(cell.getRichStringCellValue().getString());
                break;
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    System.out.print(cell.getDateCellValue());
                } else {
                    System.out.print(cell.getNumericCellValue());
                }
                break;
            case FORMULA:
                System.out.print(cell.getCellFormula());
                break;
            case BLANK:
                System.out.print("");
                break;
            default:
                System.out.print("");
        }
        System.out.print("\t");
    }


    public static Vector toDelimited(String excelfile, int sheetNumber, char delim) {
		Vector w = new Vector();
		Workbook workbook = openWorkbook(excelfile);
        Sheet sheet = workbook.getSheetAt(sheetNumber);
        Iterator<Row> rowIterator = sheet.rowIterator();
        while (rowIterator.hasNext()) {
			StringBuffer buf = new StringBuffer();
			Row row = rowIterator.next();
            List list = getRowData(row);
            for (int i=0; i<list.size(); i++) {
				String t = (String) list.get(i);
				buf.append(t).append(delim);
			}
			String s = buf.toString();
			s = s.substring(0, s.length()-1);
			w.add(s);
		}
        try {
        	workbook.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
        return w;
	}

/*
    public static List getRowData(Row row) {
		List list = new ArrayList();
		for (int i=0; i<row.getPhysicalNumberOfCells(); i++) {
			Cell cell=row.getCell(i, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK );
			cell.setCellType(Cell.CELL_TYPE_STRING);
			list.add(cell.getStringCellValue());
		}
		return list;
	}
*/

    public static List getRowData(Row row) {
		DataFormatter fmt = new DataFormatter();
		List fetchedRow = new ArrayList();
		//List list = new ArrayList();
		for (int i=0; i<row.getPhysicalNumberOfCells(); i++) {
			Cell cell=row.getCell(i, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK );
            boolean rowEmpty = true;
			switch (cell.getCellType()) {
				case NUMERIC:
					if (DateUtil.isCellDateFormatted(cell)) {
						Date date = cell.getDateCellValue();
						DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
						//DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
						fetchedRow.add(dateFormat.format(date));
					} else {
						fetchedRow.add(fmt.formatCellValue(cell));
					}
					rowEmpty = false;
					break;
				case STRING:
					//fetchedRow.add(cell.toString());
                    fetchedRow.add(fmt.formatCellValue(cell));
					rowEmpty = false;
					break;
				case BOOLEAN:
					fetchedRow.add(cell.toString());
					rowEmpty = false;
					break;
				case FORMULA:
					fetchedRow.add(Double.toString(cell.getNumericCellValue()));
					rowEmpty = false;
					break;
				case BLANK:
					//fetchedRow.add("");
					fetchedRow.add(fmt.formatCellValue(cell));
					rowEmpty = false;
					break;
				case ERROR:
					//fetchedRow.add("");
					fetchedRow.add(fmt.formatCellValue(cell));
					rowEmpty = false;
					break;
				default:
					//fetchedRow.add("");
					fetchedRow.add(fmt.formatCellValue(cell));
					rowEmpty = false;
					break;
			}
		}
		return fetchedRow;
	}

}
