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

public class ExcelEditor {

    public static BufferedReader getBufferReader(String filename) throws Exception {
        File file = new File(filename);
        FileInputStream fis = new FileInputStream(file);
        BufferedInputStream bis = new BufferedInputStream(fis);
        BufferedReader br = new BufferedReader(new InputStreamReader(bis));
        return br;
    }

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
			BufferedReader br = null;
			try {
				br = getBufferReader(datafile);
			} catch (Exception ex) {
				return;
			}

            FileInputStream inputStream = new FileInputStream(new File(excelFilePath));
            Workbook workbook = WorkbookFactory.create(inputStream);

            Sheet sheet = workbook.getSheetAt(sheetIndex);
            int rowCount = sheet.getLastRowNum();
            int i = 0;
            while (true) {
                String line = br.readLine();

				if (line == null)
					break;
				// line = line.trim(); Note: 090512 first value could be empty
				if (line.length() <= 0)
					continue;

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
                        cell.setCellValue(value);
                    }
				}
				i++;
            }

            inputStream.close();
            FileOutputStream outputStream = new FileOutputStream("new_" + excelFilePath);
            workbook.write(outputStream);
            workbook.close();
            outputStream.close();

        } catch (IOException | EncryptedDocumentException
                 ex) {
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