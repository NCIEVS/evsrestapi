package gov.nih.nci.evs.restapi.util;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.*;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ExcelReader {
	public static DataFormatter dataFormatter = new DataFormatter();

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

    public static Vector toDelimited(String excelfile, int sheetNumber, char delim) {
		Vector w = new Vector();
		Workbook workbook = openWorkbook(excelfile);
        Sheet sheet = workbook.getSheetAt(sheetNumber);
        Iterator<Row> rowIterator = sheet.rowIterator();
        while (rowIterator.hasNext()) {
			StringBuffer buf = new StringBuffer();
            Row row = rowIterator.next();
            Iterator<Cell> cellIterator = row.cellIterator();
            while (cellIterator.hasNext()) {
                Cell cell = cellIterator.next();
                String cellValue = getCellValue(cell);
                buf.append(cellValue).append(delim);
            }
            String line = buf.toString();
            line = line.substring(0, line.length()-1);
            w.add(line);
        }
        try {
        	workbook.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
        return w;
	}

    private static void printCellValue(Cell cell) {
        switch (cell.getCellTypeEnum()) {
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

    private static String getCellValue(Cell cell) {
        switch (cell.getCellTypeEnum()) {
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

    public static void main(String[] args) throws IOException, InvalidFormatException {
        String excelfile = args[0];
        System.out.println(excelfile);
        Vector w = toDelimited(excelfile, '|');
        Utils.dumpVector(excelfile, w);
        int n = excelfile.lastIndexOf(".");
        String textfile = excelfile.substring(0, n) + "_" + StringUtils.getToday() + ".txt";
        Utils.saveToFile(textfile, w);
    }
}
