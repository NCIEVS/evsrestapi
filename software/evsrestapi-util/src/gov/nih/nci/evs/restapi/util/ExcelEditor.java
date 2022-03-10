package gov.nih.nci.evs.restapi.util;
import java.io.*;
import java.util.*;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.usermodel.CellStyle;

import org.apache.poi.util.StringUtil;

public class ExcelEditor {

	public static Vector readFile(String filename) {
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

	public static boolean isInteger(String str) {
		try {
			int i = Integer.parseInt(str);
		} catch (Exception ex) {

		}
		return false;
	}

    public static void run(String excelFilePath, int sheetIndex, String datafile) {
        Vector v = readFile(datafile);
        CellStyle old_style = null;
        try {
            FileInputStream inputStream = new FileInputStream(new File(excelFilePath));
            Workbook workbook = WorkbookFactory.create(inputStream);

            Sheet sheet = workbook.getSheetAt(sheetIndex);
            int rowCount = sheet.getLastRowNum();
            for (int i=0; i<v.size(); i++) {
				String line = (String) v.elementAt(i);
				Vector values = parseData(line, '\t');
				Row old_row = sheet.getRow(i);
				Row row = sheet.createRow(i);
				for(int k = 0; k < values.size(); k++) {
					Cell old_cell = old_row.getCell(k);
                    old_style = old_cell.getCellStyle();
					Cell cell = row.createCell(k);
					cell.setCellStyle(old_style);
					String value = (String) values.elementAt(k);
                    if (isInteger(value)) {
						Integer int_obj = new Integer(Integer.parseInt(value));
						cell.setCellValue(int_obj);
                    } else {
                        cell.setCellValue((String) value);
                    }
				}
            }

            inputStream.close();
            FileOutputStream outputStream = new FileOutputStream("new_" + excelFilePath);
            workbook.write(outputStream);
            workbook.close();
            outputStream.close();

        } catch (IOException | EncryptedDocumentException
                | InvalidFormatException ex) {
            ex.printStackTrace();
        }
    }

    public static void main(String[] args) {
        String excelFilePath = args[0];
        String sheetIndexStr = args[1];
        int sheetIndex = Integer.parseInt(sheetIndexStr);
        String datafile = args[2];
        run(excelFilePath, sheetIndex, datafile);
	}

}