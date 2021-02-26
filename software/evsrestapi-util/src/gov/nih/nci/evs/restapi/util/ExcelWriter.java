package gov.nih.nci.evs.restapi.util;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFCell;

import java.io.*;
import java.util.*;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;


public class ExcelWriter {
	public static final String AQUA = "AQUA";
	public static final String AUTOMATIC = "AUTOMATIC";
	public static final String BLACK = "BLACK";
	public static final String BLACK1 = "BLACK1";
	public static final String BLUE = "BLUE";
	public static final String BLUE_GREY = "BLUE_GREY";
	public static final String BLUE1 = "BLUE1";
	public static final String BRIGHT_GREEN = "BRIGHT_GREEN";
	public static final String BRIGHT_GREEN1 = "BRIGHT_GREEN1";
	public static final String BROWN = "BROWN";
	public static final String CORAL = "CORAL";
	public static final String CORNFLOWER_BLUE = "CORNFLOWER_BLUE";
	public static final String DARK_BLUE = "DARK_BLUE";
	public static final String DARK_GREEN = "DARK_GREEN";
	public static final String DARK_RED = "DARK_RED";
	public static final String DARK_TEAL = "DARK_TEAL";
	public static final String DARK_YELLOW = "DARK_YELLOW";
	public static final String GOLD = "GOLD";
	public static final String GREEN = "GREEN";
	public static final String GREY_25_PERCENT = "GREY_25_PERCENT";
	public static final String GREY_40_PERCENT = "GREY_40_PERCENT";
	public static final String GREY_50_PERCENT = "GREY_50_PERCENT";
	public static final String GREY_80_PERCENT = "GREY_80_PERCENT";
	public static final String INDIGO = "INDIGO";
	public static final String LAVENDER = "LAVENDER";
	public static final String LEMON_CHIFFON = "LEMON_CHIFFON";
	public static final String LIGHT_BLUE = "LIGHT_BLUE";
	public static final String LIGHT_CORNFLOWER_BLUE = "LIGHT_CORNFLOWER_BLUE";
	public static final String LIGHT_GREEN = "LIGHT_GREEN";
	public static final String LIGHT_ORANGE = "LIGHT_ORANGE";
	public static final String LIGHT_TURQUOISE = "LIGHT_TURQUOISE";
	public static final String LIGHT_TURQUOISE1 = "LIGHT_TURQUOISE1";
	public static final String LIGHT_YELLOW = "LIGHT_YELLOW";
	public static final String LIME = "LIME";
	public static final String MAROON = "MAROON";
	public static final String OLIVE_GREEN = "OLIVE_GREEN";
	public static final String ORANGE = "ORANGE";
	public static final String ORCHID = "ORCHID";
	public static final String PALE_BLUE = "PALE_BLUE";
	public static final String PINK = "PINK";
	public static final String PINK1 = "PINK1";
	public static final String PLUM = "PLUM";
	public static final String RED = "RED";
	public static final String RED1 = "RED1";
	public static final String ROSE = "ROSE";
	public static final String ROYAL_BLUE = "ROYAL_BLUE";
	public static final String SEA_GREEN = "SEA_GREEN";
	public static final String SKY_BLUE = "SKY_BLUE";
	public static final String TAN = "TAN";
	public static final String TEAL = "TEAL";
	public static final String TURQUOISE = "TURQUOISE";
	public static final String TURQUOISE1 = "TURQUOISE1";
	public static final String VIOLET = "VIOLET";
	public static final String WHITE = "WHITE";
	public static final String WHITE1 = "WHITE1";
	public static final String YELLOW = "YELLOW";
	public static final String YELLOW1 = "YELLOW1";

	static int XLSX = 1;
	static int XLS = 2;

    public Workbook createWorkbook(int type) {
		if (type == XLSX) {
			return new XSSFWorkbook();
		}
		return null;//HSSFWorkbook();
	}

    public Sheet createSheet(int type, Workbook workbook, String label) {
		return workbook.createSheet(label);
	}

    public Font createFont(Workbook workbook, boolean bold, int size, String color) {
        Font font = workbook.createFont();
        font.setBold(bold);
        font.setFontHeightInPoints((short) size);
        switch (color) {
			case "AQUA":
			   font.setColor(IndexedColors.AQUA.getIndex());
			case "AUTOMATIC":
			   font.setColor(IndexedColors.AUTOMATIC.getIndex());
			case "BLACK":
			   font.setColor(IndexedColors.BLACK.getIndex());
			case "BLACK1":
			   font.setColor(IndexedColors.BLACK1.getIndex());
			case "BLUE":
			   font.setColor(IndexedColors.BLUE.getIndex());
			case "BLUE_GREY":
			   font.setColor(IndexedColors.BLUE_GREY.getIndex());
			case "BLUE1":
			   font.setColor(IndexedColors.BLUE1.getIndex());
			case "BRIGHT_GREEN":
			   font.setColor(IndexedColors.BRIGHT_GREEN.getIndex());
			case "BRIGHT_GREEN1":
			   font.setColor(IndexedColors.BRIGHT_GREEN1.getIndex());
			case "BROWN":
			   font.setColor(IndexedColors.BROWN.getIndex());
			case "CORAL":
			   font.setColor(IndexedColors.CORAL.getIndex());
			case "CORNFLOWER_BLUE":
			   font.setColor(IndexedColors.CORNFLOWER_BLUE.getIndex());
			case "DARK_BLUE":
			   font.setColor(IndexedColors.DARK_BLUE.getIndex());
			case "DARK_GREEN":
			   font.setColor(IndexedColors.DARK_GREEN.getIndex());
			case "DARK_RED":
			   font.setColor(IndexedColors.DARK_RED.getIndex());
			case "DARK_TEAL":
			   font.setColor(IndexedColors.DARK_TEAL.getIndex());
			case "DARK_YELLOW":
			   font.setColor(IndexedColors.DARK_YELLOW.getIndex());
			case "GOLD":
			   font.setColor(IndexedColors.GOLD.getIndex());
			case "GREEN":
			   font.setColor(IndexedColors.GREEN.getIndex());
			case "GREY_25_PERCENT":
			   font.setColor(IndexedColors.GREY_25_PERCENT.getIndex());
			case "GREY_40_PERCENT":
			   font.setColor(IndexedColors.GREY_40_PERCENT.getIndex());
			case "GREY_50_PERCENT":
			   font.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
			case "GREY_80_PERCENT":
			   font.setColor(IndexedColors.GREY_80_PERCENT.getIndex());
			case "INDIGO":
			   font.setColor(IndexedColors.INDIGO.getIndex());
			case "LAVENDER":
			   font.setColor(IndexedColors.LAVENDER.getIndex());
			case "LEMON_CHIFFON":
			   font.setColor(IndexedColors.LEMON_CHIFFON.getIndex());
			case "LIGHT_BLUE":
			   font.setColor(IndexedColors.LIGHT_BLUE.getIndex());
			case "LIGHT_CORNFLOWER_BLUE":
			   font.setColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
			case "LIGHT_GREEN":
			   font.setColor(IndexedColors.LIGHT_GREEN.getIndex());
			case "LIGHT_ORANGE":
			   font.setColor(IndexedColors.LIGHT_ORANGE.getIndex());
			case "LIGHT_TURQUOISE":
			   font.setColor(IndexedColors.LIGHT_TURQUOISE.getIndex());
			case "LIGHT_TURQUOISE1":
			   font.setColor(IndexedColors.LIGHT_TURQUOISE1.getIndex());
			case "LIGHT_YELLOW":
			   font.setColor(IndexedColors.LIGHT_YELLOW.getIndex());
			case "LIME":
			   font.setColor(IndexedColors.LIME.getIndex());
			case "MAROON":
			   font.setColor(IndexedColors.MAROON.getIndex());
			case "OLIVE_GREEN":
			   font.setColor(IndexedColors.OLIVE_GREEN.getIndex());
			case "ORANGE":
			   font.setColor(IndexedColors.ORANGE.getIndex());
			case "ORCHID":
			   font.setColor(IndexedColors.ORCHID.getIndex());
			case "PALE_BLUE":
			   font.setColor(IndexedColors.PALE_BLUE.getIndex());
			case "PINK":
			   font.setColor(IndexedColors.PINK.getIndex());
			case "PINK1":
			   font.setColor(IndexedColors.PINK1.getIndex());
			case "PLUM":
			   font.setColor(IndexedColors.PLUM.getIndex());
			case "RED":
			   font.setColor(IndexedColors.RED.getIndex());
			case "RED1":
			   font.setColor(IndexedColors.RED1.getIndex());
			case "ROSE":
			   font.setColor(IndexedColors.ROSE.getIndex());
			case "ROYAL_BLUE":
			   font.setColor(IndexedColors.ROYAL_BLUE.getIndex());
			case "SEA_GREEN":
			   font.setColor(IndexedColors.SEA_GREEN.getIndex());
			case "SKY_BLUE":
			   font.setColor(IndexedColors.SKY_BLUE.getIndex());
			case "TAN":
			   font.setColor(IndexedColors.TAN.getIndex());
			case "TEAL":
			   font.setColor(IndexedColors.TEAL.getIndex());
			case "TURQUOISE":
			   font.setColor(IndexedColors.TURQUOISE.getIndex());
			case "TURQUOISE1":
			   font.setColor(IndexedColors.TURQUOISE1.getIndex());
			case "VIOLET":
			   font.setColor(IndexedColors.VIOLET.getIndex());
			case "WHITE":
			   font.setColor(IndexedColors.WHITE.getIndex());
			case "WHITE1":
			   font.setColor(IndexedColors.WHITE1.getIndex());
			case "YELLOW":
			   font.setColor(IndexedColors.YELLOW.getIndex());
			case "YELLOW1":
			   font.setColor(IndexedColors.YELLOW1.getIndex());
			default:
			   font.setColor(IndexedColors.BLACK.getIndex());
		}
		return font;
	}


	public void write() {
		Vector colors = Utils.readFile("colors.txt");
		for (int i=0; i<colors.size(); i++) {
			String color = (String) colors.elementAt(i);
			System.out.println("public static final String " + color + " = " + "\"" + color + "\";");
	    }
	}

	public void writeColor() {
		System.out.println("    public Font createFont(Workbook workbook, boolean bold, int size, String color) {");
		System.out.println("        Font font = workbook.createFont();");
		System.out.println("        font.setBold(bold);");
		System.out.println("        font.setFontHeightInPoints((short) size);");
		System.out.println("        switch (color) {");

		Vector colors = Utils.readFile("colors.txt");
		for (int i=0; i<colors.size(); i++) {
			String color = (String) colors.elementAt(i);
			System.out.println("			case \"" + color + "\":");
			System.out.println("			   font.setColor(IndexedColors." + color + ".getIndex());");
	    }
		System.out.println("			default:");
		String color = (String) colors.elementAt(0);
		System.out.println("			   font.setColor(IndexedColors." + color + ".getIndex());");
		System.out.println("		}");
		System.out.println("		return font;");
		System.out.println("	}");
	}


    public CellStyle createCellStyle(Workbook workbook, Font font) {
        CellStyle cellStyle = workbook.createCellStyle();
        cellStyle.setFont(font);
        return cellStyle;
	}

    public Row createRow(Sheet sheet, int rowNumber) {
		return sheet.createRow(rowNumber);
	}

    public void autoSizeColumn(Sheet sheet, int numberOfColumn) {
        // Resize all columns to fit the content size
        for(int i = 0; i < numberOfColumn; i++) {
            sheet.autoSizeColumn(i);
        }
	}


    public String[] getColumnHeadings(String heading_line, char delim) {
		Vector v = StringUtils.parseData(heading_line, delim);
		String[] headings = new String[v.size()];
		for (int i=0; i<v.size(); i++) {
			String heading = (String) v.elementAt(i);
			headings[i] = heading;
		}
		return headings;

	}

    public void writeToXSSF(Vector datafile_vec, String excelfile, char delim, Vector sheetLabel_vec, String headerColor) {
		Workbook workbook = new XSSFWorkbook();
		CreationHelper createHelper = workbook.getCreationHelper();

		boolean bold = true;
		int size = 14;
		String color = RED;
		Font headerFont = createFont(workbook, bold, size, color);

		CellStyle headerCellStyle = workbook.createCellStyle();
		headerCellStyle.setFont(headerFont);

		CellStyle dateCellStyle = workbook.createCellStyle();
		dateCellStyle.setDataFormat(createHelper.createDataFormat().getFormat("dd-MM-yyyy"));

		for (int lcv=0; lcv<datafile_vec.size(); lcv++) {
			String datafile = (String) datafile_vec.elementAt(lcv);
            Vector lines = Utils.readFile(datafile);
			String heading = (String) lines.elementAt(0);
			String[] columns = getColumnHeadings(heading, delim);
			Sheet sheet = workbook.createSheet((String) sheetLabel_vec.elementAt(lcv));
			Row headerRow = sheet.createRow(0);
			for(int i = 0; i < columns.length; i++) {
				Cell cell = headerRow.createCell(i);
				cell.setCellValue(columns[i]);
				cell.setCellStyle(headerCellStyle);
			}
			for (int i=1;i<lines.size(); i++) {
				String line = (String) lines.elementAt(i);
				Vector values = StringUtils.parseData(line, delim);
				Row row = sheet.createRow(i);
				for(int k = 0; k < values.size(); k++) {
					row.createCell(k).setCellValue((String) values.elementAt(k));
				}
			}
			for(int i = 0; i < columns.length; i++) {
				sheet.autoSizeColumn(i);
			}
		}
        try {
			FileOutputStream fileOut = new FileOutputStream(excelfile);
			workbook.write(fileOut);
			fileOut.close();

			workbook.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
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


    public static CellStyle cloneStyleFrom(CellStyle style_clone, CellStyle style) {
		style_clone.cloneStyleFrom(style);

		int code = style.getAlignment();
		HorizontalAlignment ha = HorizontalAlignment.forInt(code);
		style_clone.setAlignment(ha);

		short style_code = style.getBorderBottom();
		style_clone.setBorderBottom(BorderStyle.valueOf(style_code));

		style_code = style.getFillBackgroundColor();
		style_clone.setFillBackgroundColor(style_code);

		style_code = style.getFillForegroundColor();
		style_clone.setFillForegroundColor(style_code);

		style_code = style.getFillPattern();
		style_clone.setFillPattern(FillPatternType.forInt(style_code));
        return style_clone;
	}

    public static String cloneWorkbook(String xlsxfile) {
		String outputfile = "cloned_" + xlsxfile;
		Workbook workbook = null;
		try {
			workbook = new XSSFWorkbook(new FileInputStream(xlsxfile));
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		Workbook workbook_clone = new XSSFWorkbook();
		CellStyle style_clone = workbook_clone.createCellStyle();
		Row row;
		//Cell cell;
		for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
			XSSFSheet sheet = (XSSFSheet) workbook.getSheetAt(i);
			XSSFSheet sheet_clone = (XSSFSheet) workbook_clone.createSheet(sheet.getSheetName());
			Cell c = null;
			Cell cell = null;
			System.out.println("Number of rows in " + sheet.getSheetName() + ": " + sheet.getPhysicalNumberOfRows());
			for (int rowIndex = 0; rowIndex < sheet.getPhysicalNumberOfRows(); rowIndex++) {
				row = sheet.getRow(rowIndex);
				if (row != null) {
					Row row_clone = sheet_clone.createRow(rowIndex);
					Iterator<Cell> cellIterator = row.cellIterator();
					int colIndex = 0;
					while (cellIterator.hasNext()) {
						c = cellIterator.next();
						if (c == null) {
							System.out.println("WARNING: cell(" + rowIndex + "," + colIndex + ") on " + sheet.getSheetName() +  " is null");
                            cell = row_clone.createCell(colIndex);
                            cell.setCellValue("");
						} else if (c != null) {
							CellStyle style = c.getCellStyle();
							cell = row_clone.createCell(colIndex);
							style_clone = cloneStyleFrom(style_clone, style);
							cell.setCellStyle(style_clone);
							switch (c.getCellTypeEnum()) {
								case STRING:
									cell.setCellValue(c.getRichStringCellValue().getString());
									break;
								case NUMERIC:
									if (DateUtil.isCellDateFormatted(cell)) {
										cell.setCellValue(c.getDateCellValue());
									} else {
										cell.setCellValue(c.getNumericCellValue());
									}
									break;
								case BOOLEAN:
									cell.setCellValue(c.getBooleanCellValue());
									break;
								case FORMULA:
									cell.setCellValue(c.getCellFormula());
									break;
								case BLANK:
									cell.setCellValue("");
									break;
								default:
									//cell.setCellValue("");
							}
						}
						colIndex++;
					}
				}
			}
		}
		FileOutputStream fileOut = null;
		try {
			fileOut = new FileOutputStream(outputfile);
			workbook_clone.write(fileOut);
			workbook.close();
			workbook_clone.close();
			fileOut.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return outputfile;
	}

    public static void main(String[] args) throws IOException, InvalidFormatException {
		ExcelWriter writer = new ExcelWriter();
		boolean test_mode = true;
        String datafile = "C168658.txt";
        char delim = '|';
        String sheetLabel = "test";
        String headerColor = RED;
        Vector datafile_vec = new Vector();
        datafile_vec.add("C168655.txt");
        datafile_vec.add("C168657.txt");
        datafile_vec.add("C168658.txt");
        Vector sheetLabel_vec = new Vector();
        sheetLabel_vec.add("Mapped ICDO3.1 Terminology");
        sheetLabel_vec.add("Mapped ICDO3.1 Morphology Terminology");
        sheetLabel_vec.add("Mapped ICDO3.1 Morphology PT Terminology");
        writer.writeToXSSF(datafile_vec, "test1.xlsx", delim, sheetLabel_vec, headerColor);
	}
}
