package gov.nih.nci.evs.restapi.util;

import java.io.*;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import org.apache.poi.common.usermodel.Hyperlink;
import org.apache.poi.common.usermodel.HyperlinkType;
import org.apache.poi.hpsf.SummaryInformation;
import org.apache.poi.hssf.usermodel.*;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFClientAnchor;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFHyperlink;
import org.apache.poi.hssf.usermodel.HSSFPalette;
import org.apache.poi.hssf.usermodel.HSSFPicture;
import org.apache.poi.hssf.usermodel.HSSFPictureData;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFShape;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.POIXMLDocument;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFHyperlink;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import org.apache.poi.ss.usermodel.FillPatternType;


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

	static String CONFIGFILE = "config.txt";
	static String hypperlinkURL = "https://nciterms.nci.nih.gov/ncitbrowser/ConceptReport.jsp?dictionary=NCI_Thesaurus&ns=ncit&code=";

    public Workbook createWorkbook(int type) {
		if (type == XLSX) {
			return new XSSFWorkbook();
		}
		return null;//HSSFWorkbook();
	}

    public Sheet createSheet(int type, Workbook workbook, String label) {
		return workbook.createSheet(label);
	}

	public static void set_hypperlinkURL(String url) {
		hypperlinkURL = url;
	}

	public static XSSFCell createHyperlinkXSSFCell(CreationHelper createHelper, XSSFCellStyle hlinkstyle, XSSFCell cell, String code) {
		cell.setCellValue(code);
		XSSFHyperlink link = (XSSFHyperlink)createHelper.createHyperlink(HyperlinkType.URL);
		link.setAddress(hypperlinkURL + code);
		cell.setHyperlink((XSSFHyperlink) link);
		cell.setCellStyle(hlinkstyle);
		return cell;
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

    public static Boolean isEven (Integer i) {
        return (i % 2) == 0;
    }

    public static CellStyle createCellStyle(XSSFWorkbook workbook, XSSFSheet spreadsheet, short foregroundColorIndex, short textColorIndex, HorizontalAlignment alignment, int fontsize, boolean bold){
		CellStyle cellStyle = workbook.createCellStyle();
		cellStyle.setAlignment(alignment);
		Font font = workbook.createFont();
		font.setFontHeightInPoints((short) fontsize);
		font.setColor(textColorIndex);
		font.setBold(bold);
		cellStyle.setFont(font);
		cellStyle.setFillForegroundColor(foregroundColorIndex);
		cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return cellStyle;
	}


    public static void writeHeading(XSSFWorkbook workbook, XSSFSheet spreadsheet, String line, char delim){
        CellStyle cellStyle = createCellStyle(workbook, spreadsheet, IndexedColors.GREEN.getIndex(),
                                              HSSFColor.WHITE.index, HorizontalAlignment.LEFT, (short)14, true);
        Vector u = StringUtils.parseData(line, delim);
		XSSFRow row = spreadsheet.createRow(0);
		for (int i=0; i<u.size(); i++) {
			String value = (String) u.elementAt(i);
			XSSFCell cell0 = row.createCell((short) i);
			cell0.setCellValue(value);
    		cell0.setCellStyle(cellStyle);
		}
	}


    public void writeToXSSF(Vector datafile_vec, String excelfile, char delim, Vector sheetLabel_vec) {
		XSSFWorkbook workbook = new XSSFWorkbook();
		XSSFCell cell = null;
		CreationHelper createHelper = workbook.getCreationHelper();

		CellStyle dateCellStyle = workbook.createCellStyle();
		dateCellStyle.setDataFormat(createHelper.createDataFormat().getFormat("dd-MM-yyyy"));

		for (int lcv=0; lcv<datafile_vec.size(); lcv++) {
			String datafile = (String) datafile_vec.elementAt(lcv);
			System.out.println(datafile);
			try {
				XSSFSheet sheet = workbook.createSheet((String) sheetLabel_vec.elementAt(lcv));

				CellStyle cellStyle_odd = createCellStyle(workbook, sheet, IndexedColors.LIGHT_GREEN.getIndex(),
													  HSSFColor.BLACK.index, HorizontalAlignment.LEFT, 12, false);

				CellStyle cellStyle_even = createCellStyle(workbook, sheet, IndexedColors.WHITE.getIndex(),
													  HSSFColor.BLACK.index, HorizontalAlignment.LEFT, 12, false);


				Vector lines = Utils.readFile(datafile);
				String line = (String) lines.elementAt(0);

				writeHeading(workbook, sheet, line, delim);

				String heading = (String) lines.elementAt(0);
				String[] columns = getColumnHeadings(heading, delim);

				for (int i=1;i<lines.size(); i++) {
					Boolean is_even = isEven(new Integer(i));
					line = (String) lines.elementAt(i);
					Vector values = StringUtils.parseData(line, delim);
					XSSFRow row = sheet.createRow(i);
					for(int k = 0; k < values.size(); k++) {
						cell = row.createCell((short) k);
						String value = (String) values.elementAt(k);
						cell.setCellValue(value);

						if (isNCItCode(value)) {

							XSSFCellStyle hlinkstyle = workbook.createCellStyle();
							XSSFFont hlinkfont = workbook.createFont();
							hlinkfont.setUnderline(XSSFFont.U_SINGLE);
							hlinkfont.setColor(IndexedColors.BLUE.index);
							hlinkstyle.setFont(hlinkfont);

                            if (is_even.equals(Boolean.TRUE)) {
							    hlinkstyle.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
							} else {
                                hlinkstyle.setFillForegroundColor(IndexedColors.WHITE.getIndex());
							}
							hlinkstyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
							cell = createHyperlinkXSSFCell(createHelper, hlinkstyle, cell, value);
						} else {

							if (is_even.equals(Boolean.TRUE)) {
								cell.setCellStyle(cellStyle_odd);
							} else {
								cell.setCellStyle(cellStyle_even);
							}
						}
					}
				}
				for(int i = 0; i < columns.length; i++) {
					sheet.autoSizeColumn(i);
				}

			} catch (Exception ex) {
				ex.printStackTrace();
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

    public void writeToHSSF(Vector datafile_vec, String excelfile, char delim, Vector sheetLabel_vec) {
		Workbook workbook = new HSSFWorkbook();
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
			System.out.println(datafile);
			try {
				Sheet sheet = workbook.createSheet((String) sheetLabel_vec.elementAt(lcv));
				Vector lines = Utils.readFile(datafile);
				if (lines.size() > 0) {
					String heading = (String) lines.elementAt(0);
					String[] columns = getColumnHeadings(heading, delim);

					Row headerRow = sheet.createRow(0);

					for(int i = 0; i < columns.length; i++) {
						Cell cell = headerRow.createCell(i);
						cell.setCellValue(columns[i]);
						cell.setCellStyle(headerCellStyle);
					}
					if (lines.size() > 1) {
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
				}

			} catch (Exception ex) {
				ex.printStackTrace();
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

    public static boolean isNCItCode(String code) {
		char c = code.charAt(0);
		if (c != 'C') return false;
		try {
			int i = Integer.parseInt(code.substring(1, code.length()-1));
			return true;
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return false;
	}



    public static void main(String[] args) throws IOException, InvalidFormatException {
		ExcelWriter writer = new ExcelWriter();
		Vector v = readFile(CONFIGFILE);
		String t = (String) v.elementAt(0);
		Vector u = parseData(t, '|');
		String excelfile = (String) u.elementAt(0);
		char delim = '|';
		String s = (String) u.elementAt(1);

		if (s.compareTo("tab") == 0) {
			delim = '\t';
		}
        Vector datafile_vec = new Vector();
        Vector sheetLabel_vec = new Vector();
        for (int i=1; i<v.size(); i++) {
			t = (String) v.elementAt(i);
			u = parseData(t, '|');
			datafile_vec.add((String) u.elementAt(1));
			sheetLabel_vec.add((String) u.elementAt(0));
		}
		if (excelfile.endsWith(".xls")) {
			writer.writeToHSSF(datafile_vec, excelfile, delim, sheetLabel_vec);
		} else {
			writer.writeToXSSF(datafile_vec, excelfile, delim, sheetLabel_vec);
		}
	}
}

/*
config.txt:
CDISC ADaM Terminology.xls|tab
ReadMe|readme.txt
CDISC ADaM Terminology|CDISC ADaM Terminology.txt
*/