package gov.nih.nci.evs.restapi.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.DataFormatter;
import java.util.*;
import java.io.*;

public class ExcelFileReader {
     static String CONFIGFILE = "config.txt";
	 public static void saveToFile(String outputfile, String t) {
		 Vector v = new Vector();
		 v.add(t);
		 saveToFile(outputfile, v);
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

	public static void saveToFile(PrintWriter pw, Vector v) {
		if (v != null && v.size() > 0) {
			for (int i=0; i<v.size(); i++) {
				String t = (String) v.elementAt(i);
				pw.println(t);
			}
		}
	}

	public String getSheetName(String fileName, int sheet_number) throws IOException {
		File file = new File(fileName);
		FileInputStream inputStream = new FileInputStream(file);
		Workbook workbook = null;
		String fileExtensionName = fileName.substring(fileName.indexOf("."));
		if(fileExtensionName.equals(".xlsx")){
			workbook = new XSSFWorkbook(inputStream);
		} else if(fileExtensionName.equals(".xls")){
			workbook = new HSSFWorkbook(inputStream);
		}
		Sheet sheet = workbook.getSheetAt(sheet_number);
		return sheet.getSheetName();
	}

	public String getSheetName(String filePath, String fileName, int sheet_number) throws IOException {
		File file = new File(filePath+"\\"+fileName);
		FileInputStream inputStream = new FileInputStream(file);
		Workbook workbook = null;
		String fileExtensionName = fileName.substring(fileName.indexOf("."));
		if(fileExtensionName.equals(".xlsx")){
			workbook = new XSSFWorkbook(inputStream);
		} else if(fileExtensionName.equals(".xls")){
			workbook = new HSSFWorkbook(inputStream);
		}
		Sheet sheet = workbook.getSheetAt(sheet_number);
		return sheet.getSheetName();
	}

	public Vector readExcel(String filePath, String fileName, int sheet_number) throws IOException {
		String absolutePath = filePath +"\\" + fileName;
		return readExcel(absolutePath, sheet_number);
	}

	public Vector readExcel(String absolutePath, int sheet_number) throws IOException {
		Vector w = new Vector();
		File file = new File(absolutePath);

		FileInputStream inputStream = new FileInputStream(file);
		Workbook workbook = null;
		String fileExtensionName = absolutePath.substring(absolutePath.indexOf("."));
		if(fileExtensionName.equals(".xlsx")){
			workbook = new XSSFWorkbook(inputStream);
		} else if(fileExtensionName.equals(".xls")){
			workbook = new HSSFWorkbook(inputStream);
		}
		Sheet sheet = workbook.getSheetAt(sheet_number);
		int rowCount = sheet.getLastRowNum()-sheet.getFirstRowNum();
		for (int i = 0; i < rowCount+1; i++) {
			StringBuffer buf = new StringBuffer();
			Row row = sheet.getRow(i);
			if (row != null) {
				for (int j = 0; j < row.getLastCellNum(); j++) {
					try {
						String retstr = getCellValue(row.getCell(j));
						retstr = retstr.replace("\n", " ");
						buf.append(retstr);
						if (j < row.getLastCellNum()-1) {
							buf.append("\t");
						}
					} catch (Exception ex) {
						buf.append("\t");
					}
				}
				w.add(buf.toString());
			}
		}
		return w;
    }

    private static String getCellValue(Cell cell) {
		String retstr = null;
        switch (cell.getCellTypeEnum()) {
            case BOOLEAN:
                retstr = "" + cell.getBooleanCellValue();
                break;
            case STRING:
                retstr = "" + cell.getRichStringCellValue().getString();
                break;
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    retstr = "" + cell.getDateCellValue();
                } else {
                    retstr = "" + cell.getNumericCellValue();
                }
                break;
            case FORMULA:
                retstr = "" + cell.getCellFormula();
                break;
            case BLANK:
                retstr = "";
                break;
            default:
                retstr = "";
        }
        return retstr;
    }

    public static void main(String[] args) throws IOException{
		ExcelFileReader test = new ExcelFileReader();
		String configfile = CONFIGFILE;
		String excelfile = args[0];
		String sheetIndex_str = args[1];
		int sheet_number = Integer.parseInt(sheetIndex_str);
		String filePath = System.getProperty("user.dir");
		Vector w = test.readExcel(filePath, excelfile, sheet_number);
		String sheetName = test.getSheetName(filePath, excelfile, sheet_number);
		saveToFile(sheetName + ".txt", w);
    }
}