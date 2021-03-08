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

public class ExcelFileReader {

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
		Vector w = new Vector();
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
		int rowCount = sheet.getLastRowNum()-sheet.getFirstRowNum();
		for (int i = 0; i < rowCount+1; i++) {
			StringBuffer buf = new StringBuffer();
			Row row = sheet.getRow(i);
			for (int j = 0; j < row.getLastCellNum(); j++) {
				try {
					String retstr = getCellValue(row.getCell(j));
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
		String excelfile = "PCDC_Terminology_2021-03-04.xls";
		String filePath = System.getProperty("user.dir");
		int sheet_number = 1;
		Vector w = test.readExcel(filePath, excelfile, sheet_number);
		String sheetName = test.getSheetName(filePath, excelfile, sheet_number);
		Utils.saveToFile(sheetName + ".txt", w);
    }
}