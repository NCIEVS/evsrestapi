package gov.nih.nci.evs.restapi.util;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.DataFormatter;

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

/*
    public static Vector toDelimited(String excelfile, int sheetNumber, char delim) {
		Vector w = new Vector();
		Workbook workbook = openWorkbook(excelfile);
        Sheet sheet = workbook.getSheetAt(sheetNumber);
        Iterator<Row> rowIterator = sheet.rowIterator();
        while (rowIterator.hasNext()) {
			StringBuffer buf = new StringBuffer();
            Row row = rowIterator.next();
            int lastCellNumber = row.getLastCellNum();
            for (int lcv=0; lcv<lastCellNumber; lcv++) {
				Cell cell = row.getCell(lcv);
				String cellValue = "";
				if (cell != null) {
					cellValue = getCellValue(cell);
				}
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
*/
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
/*
	public static List getRowData(Row row) {
		// to extract the exact numerical value either integer/double
		List allRows = new ArrayList();
		DataFormatter fmt = new DataFormatter();
		Iterator<Cell> cellIterator = row.cellIterator();
        List fetchedRow = new ArrayList();

		while (cellIterator.hasNext()) {
			Cell cell = cellIterator.next();
            boolean rowEmpty = true;
			switch (cell.getCellTypeEnum()) {
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
					fetchedRow.add(cell.toString());
					rowEmpty = false;
					break;
				case BOOLEAN:
					fetchedRow.add(cell.toString());
					rowEmpty = false;
					break;
				case FORMULA:
					fetchedRow.add(Double.toString(cell.getNumericCellValue()));
					rowEmpty = false;
					//fetchedRow.add("");
					break;
			}
			if (!rowEmpty) {
				allRows.add(fetchedRow.toArray(new String[0]));
			}
		}
		return allRows;
	}
*/

	public static List getRowData(Row row) {
		// to extract the exact numerical value either integer/double
		//List allRows = new ArrayList();
		DataFormatter fmt = new DataFormatter();
		Iterator<Cell> cellIterator = row.cellIterator();
        List fetchedRow = new ArrayList();

		while (cellIterator.hasNext()) {
			Cell cell = cellIterator.next();
            boolean rowEmpty = true;
			switch (cell.getCellTypeEnum()) {
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
					fetchedRow.add(cell.toString());
					rowEmpty = false;
					break;
				case BOOLEAN:
					fetchedRow.add(cell.toString());
					rowEmpty = false;
					break;
				case FORMULA:
					fetchedRow.add(Double.toString(cell.getNumericCellValue()));
					rowEmpty = false;
					//fetchedRow.add("");
					break;
			}
			/*
			if (!rowEmpty) {
				allRows.add(fetchedRow.toArray(new String[0]));
			}
			*/
		}
		return fetchedRow;
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


    public static void main(String[] args) throws IOException, InvalidFormatException {
        String excelfile = args[0];

        String textfile = null;
        if (args.length >= 2) {
			textfile = args[1];
		} else {
        	int n = excelfile.lastIndexOf(".");
        	textfile = excelfile.substring(0, n) + "_" + StringUtils.getToday() + ".txt";
		}
		int sheetNumber = 0;
		if (args.length >= 3) {
			sheetNumber = Integer.parseInt(args[2]);
		}

		boolean skip_first_row = true;
		if (args.length >= 4) {
			String s = args[3];
			if (s.compareTo("false") == 0) {
				skip_first_row = false;
			}
		}

        System.out.println(excelfile);
        Vector w = toDelimited(excelfile, sheetNumber, '\t');
        if (skip_first_row) {
			w.remove(0);
		}
        Utils.saveToFile(textfile, w);
    }


}
