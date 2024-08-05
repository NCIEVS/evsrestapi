package gov.nih.nci.evs.restapi.util;

import java.io.*;
import java.io.InputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Vector;
import org.apache.commons.codec.binary.Base64;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFClientAnchor;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFPalette;
import org.apache.poi.hssf.usermodel.HSSFPicture;
import org.apache.poi.hssf.usermodel.HSSFPictureData;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFShape;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.HSSFColor;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFPictureData;
import org.apache.poi.xssf.usermodel.XSSFShape;
import org.apache.poi.xssf.usermodel.XSSFPicture;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.hssf.usermodel.HSSFDataFormatter;


import org.apache.poi.ss.usermodel.HorizontalAlignment;


/**
 * <!-- LICENSE_TEXT_START -->
 * Copyright 2008,2009 NGIT. This software was developed in conjunction
 * with the National Cancer Institute, and so to the extent government
 * employees are co-authors, any rights in such works shall be subject
 * to Title 17 of the United States Code, section 105.
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *   1. Redistributions of source code must retain the above copyright
 *      notice, this list of conditions and the disclaimer of Article 3,
 *      below. Redistributions in binary form must reproduce the above
 *      copyright notice, this list of conditions and the following
 *      disclaimer in the documentation and/or other materials provided
 *      with the distribution.
 *   2. The end-user documentation included with the redistribution,
 *      if any, must include the following acknowledgment:
 *      "This product includes software developed by NGIT and the National
 *      Cancer Institute."   If no such end-user documentation is to be
 *      included, this acknowledgment shall appear in the software itself,
 *      wherever such third-party acknowledgments normally appear.
 *   3. The names "The National Cancer Institute", "NCI" and "NGIT" must
 *      not be used to endorse or promote products derived from this software.
 *   4. This license does not authorize the incorporation of this software
 *      into any third party proprietary programs. This license does not
 *      authorize the recipient to use any trademarks owned by either NCI
 *      or NGIT
 *   5. THIS SOFTWARE IS PROVIDED "AS IS," AND ANY EXPRESSED OR IMPLIED
 *      WARRANTIES, (INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 *      OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE) ARE
 *      DISCLAIMED. IN NO EVENT SHALL THE NATIONAL CANCER INSTITUTE,
 *      NGIT, OR THEIR AFFILIATES BE LIABLE FOR ANY DIRECT, INDIRECT,
 *      INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *      BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *      LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *      CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *      LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *      ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *      POSSIBILITY OF SUCH DAMAGE.
 * <!-- LICENSE_TEXT_END -->
 */

/**
 * @author EVS Team
 * @version 1.0
 *
 *          Modification history Initial implementation kim.ong@ngc.com
 *
 */


public class ResolvedValueSetIteratorHolder {
    public static final String[] FILE_TYPES = new String[] { "xls", "xlsx" };

    private StringBuffer out = new StringBuffer();
    private SimpleDateFormat sdf;
    //private HSSFWorkbook book;
    private Workbook book;
    private HSSFPalette palette;
    private FormulaEvaluator evaluator;
    private short colIndex;
    private int rowIndex, mergeStart, mergeEnd;

    private List resolvedValueSetList = null;
    private ListIterator resolvedValueSetIterator = null;
    private Map<Integer, Map<Short, List<HSSFPictureData>>> pix = new HashMap<Integer, Map<Short, List<HSSFPictureData>>>();

    private Vector rvs_content_vec = null;
    private static String YELLOW = "#FFFF00";
    private String excelfile = null;
    private int sheet = 0;

    public String URL = "https://nciterms.nci.nih.gov/ncitbrowser/ConceptReport.jsp?dictionary=NCI_Thesaurus&ns=ncit";

    public ResolvedValueSetIteratorHolder() {

	}

	public String getHeadingLine(Vector headings) {
		StringBuffer buf = new StringBuffer();
		buf.append("<tr style='height: 51px; '>");
		for (int i=0; i<headings.size(); i++) {
			buf.append("<td height=\"15\" style='text-align: center; font-size: 8.0pt; background-color: rgb(192,192,192);border-top-style: solid; border-right-style: solid; border-bottom-style: solid; border-left-style: solid; '>");
			String heading = (String) headings.elementAt(i);
			buf.append(heading);
			buf.append("</td>");
		}
		buf.append("</tr>");
		return buf.toString();
	}

    private String getFileType(File file) {
		String fileName = file.getName();
		String folderName = file.getParent();
		if (fileName.toLowerCase().endsWith(FILE_TYPES[0])) {
			return FILE_TYPES[0];
		}
		return FILE_TYPES[1];
	}

    private Workbook createWorkBook(File file) {
		Workbook workbook = null;
		String fileName = file.getAbsolutePath();
		this.excelfile = fileName;
		//String folderName = file.getParent();
        InputStream in = getInputStream(fileName);
		try {
			if (fileName.toLowerCase().endsWith(FILE_TYPES[0])) {
				workbook = new HSSFWorkbook(in);
			} else {
   			    workbook = new XSSFWorkbook(in);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
        try {
			in.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return workbook;
	}


    public ResolvedValueSetIteratorHolder(final String filename, int sheet, int startIndex, int endIndex) throws IOException {
		this.excelfile = filename;
		this.sheet = sheet;
		resolvedValueSetList = new ArrayList();
		book = createWorkBook(new File(filename));
        if (book == null) {
            palette = null;
            evaluator = null;
            return;
        }

		if (book instanceof HSSFWorkbook) {
			HSSFWorkbook hssfWorkbook = (HSSFWorkbook) book;
			palette = hssfWorkbook.getCustomPalette();
			evaluator = hssfWorkbook.getCreationHelper().createFormulaEvaluator();
			table(hssfWorkbook.getSheetAt(sheet), startIndex, endIndex);
		} else {
			XSSFWorkbook hssfWorkbook = (XSSFWorkbook) book;
			table(hssfWorkbook.getSheetAt(sheet), startIndex, endIndex);
		}

        rvs_content_vec = extractRawDataFromTableContent();
   }


    public ResolvedValueSetIteratorHolder(final String filename, int sheet, int startIndex, int col, String code, String url, boolean cdisc) throws IOException {
        if (url != null) this.URL = url;
		this.excelfile = filename;
		this.sheet = sheet;
        book = createWorkBook(new File(filename));
		if (book instanceof HSSFWorkbook) {
			HSSFWorkbook hssfWorkbook = (HSSFWorkbook) book;
			palette = hssfWorkbook.getCustomPalette();
			evaluator = hssfWorkbook.getCreationHelper().createFormulaEvaluator();
			table(hssfWorkbook.getSheetAt(sheet), startIndex, col, code, cdisc);
		} else {
			XSSFWorkbook hssfWorkbook = (XSSFWorkbook) book;
			table(hssfWorkbook.getSheetAt(sheet), startIndex, col, code, cdisc);
		}
        rvs_content_vec = extractRawDataFromTableContent();
    }


    // public ResolvedValueSetIteratorHolder(final InputStream in) throws IOException {
    public ResolvedValueSetIteratorHolder(String filename) throws IOException {
		this.excelfile = filename;
		resolvedValueSetList = new ArrayList();
		book = createWorkBook(new File(filename));
		if (book instanceof HSSFWorkbook) {
			HSSFWorkbook hssfWorkbook = (HSSFWorkbook) book;
			palette = hssfWorkbook.getCustomPalette();
			evaluator = hssfWorkbook.getCreationHelper().createFormulaEvaluator();
			for (int i = 0; i < hssfWorkbook.getNumberOfSheets(); ++i) {
				table(hssfWorkbook.getSheetAt(i));
			}
		} else {
			XSSFWorkbook hssfWorkbook = (XSSFWorkbook) book;
			for (int i = 0; i < hssfWorkbook.getNumberOfSheets(); ++i) {
				table(hssfWorkbook.getSheetAt(i));
			}
		}
    }

    public ResolvedValueSetIteratorHolder(final String filename, int sheet, int startIndex, int endIndex, String url) throws IOException {
		this.excelfile = filename;
		this.sheet = sheet;
		resolvedValueSetList = new ArrayList();
        book = createWorkBook(new File(filename));
		if (book instanceof HSSFWorkbook) {
			HSSFWorkbook hssfWorkbook = (HSSFWorkbook) book;
			palette = hssfWorkbook.getCustomPalette();
			evaluator = hssfWorkbook.getCreationHelper().createFormulaEvaluator();
			table(hssfWorkbook.getSheetAt(sheet), startIndex, endIndex);
		} else {
			XSSFWorkbook hssfWorkbook = (XSSFWorkbook) book;
			table(hssfWorkbook.getSheetAt(sheet), startIndex, endIndex);
		}
    }


    public ResolvedValueSetIteratorHolder(final InputStream in, String url, String type) throws IOException {
        sdf = new SimpleDateFormat("dd/MM/yyyy");
        if (in == null) {
            book = null;
            palette = null;
            evaluator = null;
            return;
        }
        if (url != null) this.URL = url;
        if (type.toLowerCase().compareTo(FILE_TYPES[0]) == 0) {
            book = new HSSFWorkbook(in);
		} else {
			book = new XSSFWorkbook(in);
		}
        try {
			in.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		if (book instanceof HSSFWorkbook) {
			HSSFWorkbook hssfWorkbook = (HSSFWorkbook) book;
			palette = hssfWorkbook.getCustomPalette();
			evaluator = hssfWorkbook.getCreationHelper().createFormulaEvaluator();
			for (int i = 0; i < hssfWorkbook.getNumberOfSheets(); ++i) {
				table(hssfWorkbook.getSheetAt(i));
			}
		} else {
			XSSFWorkbook hssfWorkbook = (XSSFWorkbook) book;
			for (int i = 0; i < hssfWorkbook.getNumberOfSheets(); ++i) {
				table(hssfWorkbook.getSheetAt(i));
			}
		}
    }

    private void table(Sheet workbook_sheet) {
        if (workbook_sheet == null) {
            return;
        }
        if (workbook_sheet instanceof HSSFSheet) {
        	HSSFSheet sheet = (HSSFSheet) workbook_sheet;
			if (sheet.getDrawingPatriarch() != null) {
				final List<HSSFShape> shapes = sheet.getDrawingPatriarch()
						.getChildren();
				for (int i = 0; i < shapes.size(); ++i) {
					if (shapes.get(i) instanceof HSSFPicture) {
						try {
							// Gain access to private field anchor.
							final HSSFShape pic = shapes.get(i);
							final Field f = HSSFShape.class
									.getDeclaredField("anchor");
							f.setAccessible(true);
							final HSSFClientAnchor anchor = (HSSFClientAnchor) f
									.get(pic);
							// Store picture cell row, column and picture data.
							if (!pix.containsKey(anchor.getRow1())) {
								pix.put(anchor.getRow1(),
										new HashMap<Short, List<HSSFPictureData>>());
							}
							if (!pix.get(anchor.getRow1()).containsKey(
									anchor.getCol1())) {
								pix.get(anchor.getRow1()).put(anchor.getCol1(),
										new ArrayList<HSSFPictureData>());
							}

							HSSFWorkbook hssfWorkbook = (HSSFWorkbook) book;

							pix.get(anchor.getRow1())
									.get(anchor.getCol1())
									.add(hssfWorkbook.getAllPictures().get(
											((HSSFPicture) pic).getPictureIndex()));
						} catch (final Exception e) {
							throw new RuntimeException(e);
						}
					}
				}
			}

			out.append("<table cellspacing='0' style='border-spacing:0; border-collapse:collapse;'>\n");
			for (rowIndex = 0; rowIndex < sheet.getPhysicalNumberOfRows(); ++rowIndex) {
				tr(sheet.getRow(rowIndex));
			}
			out.append("</table>\n");
		} else {
        	XSSFSheet sheet = (XSSFSheet) workbook_sheet;
			out.append("<table cellspacing='0' style='border-spacing:0; border-collapse:collapse;'>\n");
			for (rowIndex = 0; rowIndex < sheet.getPhysicalNumberOfRows(); ++rowIndex) {
				tr(sheet.getRow(rowIndex));
			}
			out.append("</table>\n");
		}
    }


    public static String getOpenTableTag(String table_name) {
		//return "<table cellspacing='0' style='border-spacing:0; border-collapse:collapse;'>";
        //return "<table class=\"datatable_960\" summary=\"Data Table\" cellpadding=\"3\" cellspacing=\"0\" border=\"0\" width=\"100%\">";
        return "<table id=\"" + table_name + "\" width=\"900\" class=\"mt\">";
	}


    public static String getCloseTableTag() {
		return "</table>";
	}

    private void table(final Sheet workbook_sheet, int startIndex, int endIndex) {
		if (workbook_sheet instanceof HSSFSheet) {
		    HSSFSheet sheet = (HSSFSheet) workbook_sheet;

			if (sheet == null) {
				return;
			}
			if (sheet.getDrawingPatriarch() != null) {
				final List<HSSFShape> shapes = sheet.getDrawingPatriarch()
						.getChildren();
				for (int i = 0; i < shapes.size(); ++i) {
					if (shapes.get(i) instanceof HSSFPicture) {
						try {
							// Gain access to private field anchor.
							final HSSFShape pic = shapes.get(i);
							final Field f = HSSFShape.class
									.getDeclaredField("anchor");
							f.setAccessible(true);
							final HSSFClientAnchor anchor = (HSSFClientAnchor) f
									.get(pic);
							// Store picture cell row, column and picture data.
							if (!pix.containsKey(anchor.getRow1())) {
								pix.put(anchor.getRow1(),
										new HashMap<Short, List<HSSFPictureData>>());
							}
							if (!pix.get(anchor.getRow1()).containsKey(
									anchor.getCol1())) {
								pix.get(anchor.getRow1()).put(anchor.getCol1(),
										new ArrayList<HSSFPictureData>());
							}
							HSSFWorkbook hssfWorkbook = (HSSFWorkbook) book;
							pix.get(anchor.getRow1())
									.get(anchor.getCol1())
									.add(hssfWorkbook.getAllPictures().get(
											((HSSFPicture) pic).getPictureIndex()));
						} catch (final Exception e) {
							throw new RuntimeException(e);
						}
					}
				}
			}

			out.append("<table id=\"" + "rvs_table" + "\" width=\"915\" class=\"mt\">\n");
			tr(sheet.getRow(0));
			StringBuffer buf = new StringBuffer();
			tr(sheet.getRow(0), buf);
			String t = buf.toString();
			resolvedValueSetList.add(t);

			for (int i=startIndex; i<=endIndex; i++) {
				tr(sheet.getRow(i));
				buf = new StringBuffer();
				tr(sheet.getRow(i), buf);
				t = buf.toString();
				resolvedValueSetList.add(t);

			}
			out.append("</table>\n");
            setHeadingBackGroundColor();
			resolvedValueSetIterator = resolvedValueSetList.listIterator();
		} else if (workbook_sheet instanceof XSSFSheet) {
		    XSSFSheet sheet = (XSSFSheet) workbook_sheet;
			if (sheet == null) {
				return;
			}

			out.append("<table id=\"" + "rvs_table" + "\" width=\"915\" class=\"mt\">\n");
			tr(sheet.getRow(0));
			StringBuffer buf = new StringBuffer();
			tr(sheet.getRow(0), buf);
			String t = buf.toString();
			resolvedValueSetList.add(t);

			for (int i=startIndex; i<=endIndex; i++) {
				tr(sheet.getRow(i));
				buf = new StringBuffer();
				tr(sheet.getRow(i), buf);
				t = buf.toString();
				resolvedValueSetList.add(t);
			}
			out.append("</table>\n");
            setHeadingBackGroundColor();
			resolvedValueSetIterator = resolvedValueSetList.listIterator();
		}
	}


    private void table(Sheet workbook_sheet, int startIndex, int col, String code, boolean cdisc) {
		HashSet hset = new HashSet();
		if (workbook_sheet == null) {
			return;
		}

		if (workbook_sheet instanceof HSSFSheet) {
			HSSFSheet sheet = (HSSFSheet) workbook_sheet;
			resolvedValueSetList = new ArrayList();
			if (sheet == null) {
				return;
			}
			if (sheet.getDrawingPatriarch() != null) {
				final List<HSSFShape> shapes = sheet.getDrawingPatriarch()
						.getChildren();
				for (int i = 0; i < shapes.size(); ++i) {
					if (shapes.get(i) instanceof HSSFPicture) {
						try {
							// Gain access to private field anchor.
							final HSSFShape pic = shapes.get(i);
							final Field f = HSSFShape.class
									.getDeclaredField("anchor");
							f.setAccessible(true);
							final HSSFClientAnchor anchor = (HSSFClientAnchor) f
									.get(pic);
							// Store picture cell row, column and picture data.
							if (!pix.containsKey(anchor.getRow1())) {
								pix.put(anchor.getRow1(),
										new HashMap<Short, List<HSSFPictureData>>());
							}
							if (!pix.get(anchor.getRow1()).containsKey(
									anchor.getCol1())) {
								pix.get(anchor.getRow1()).put(anchor.getCol1(),
										new ArrayList<HSSFPictureData>());
							}
							HSSFWorkbook hssfWorkbook = (HSSFWorkbook) book;
							pix.get(anchor.getRow1())
									.get(anchor.getCol1())
									.add(hssfWorkbook.getAllPictures().get(
											((HSSFPicture) pic).getPictureIndex()));
						} catch (final Exception e) {
							throw new RuntimeException(e);
						}
					}
				}
			}
			out.append("<table id=\"" + "rvs_table" + "\" width=\"915\" class=\"mt\">\n");
			StringBuffer buf = new StringBuffer();
			tr(sheet.getRow(0), buf);
			String t = buf.toString();
			resolvedValueSetList.add(t);
			int rows = sheet.getPhysicalNumberOfRows();
			if (cdisc) {
				buf = new StringBuffer();
				HSSFRow row = sheet.getRow(startIndex-1);
				tr(row, buf);
				t = buf.toString();
				resolvedValueSetList.add(t);
			}
			for (int i=startIndex; i<=rows; i++) {
				buf = new StringBuffer();
				HSSFRow row = sheet.getRow(i);
				if (row != null) {
					if (col != -1) {
						HSSFCell cell = row.getCell(col);
						if (cell != null) {
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

							if (value != null && value.compareTo(code) == 0) {
								buf = new StringBuffer();
								tr(row, buf);
								t = buf.toString();
								resolvedValueSetList.add(t);
							}
						}
					} else {
						buf = new StringBuffer();
						tr(row, buf);
						t = buf.toString();
						resolvedValueSetList.add(t);
					}
				}
			}
			out.append("</table>\n");
			setHeadingBackGroundColor();
			resolvedValueSetIterator = resolvedValueSetList.listIterator();
		} else {
			XSSFSheet sheet = (XSSFSheet) workbook_sheet;
			resolvedValueSetList = new ArrayList();
			if (sheet == null) {
				return;
			}
			out.append("<table id=\"" + "rvs_table" + "\" width=\"915\" class=\"mt\">\n");
			StringBuffer buf = new StringBuffer();
			tr(sheet.getRow(0), buf);
			String t = buf.toString();
			resolvedValueSetList.add(t);
			int rows = sheet.getPhysicalNumberOfRows();
            buf = new StringBuffer();
			if (cdisc) {
				XSSFRow row = sheet.getRow(startIndex-1);
				tr(row, buf);
				t = buf.toString();
				if (!hset.contains(t)) {
					hset.add(t);
					resolvedValueSetList.add(t);
				}
			}
			for (int i=startIndex; i<=rows; i++) {
				buf = new StringBuffer();
				XSSFRow row = sheet.getRow(i);
				if (row != null) {
					if (col != -1) {
						XSSFCell cell = row.getCell(col);
						if (cell != null) {
							String value = null;
							/*
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
							value = getCellData(cell);

							if (value != null && value.compareTo(code) == 0) {
								buf = new StringBuffer();
								tr(row, buf);
								t = buf.toString();
								resolvedValueSetList.add(t);
							}
						}
					} else {
						buf = new StringBuffer();
						tr(row, buf);
						t = buf.toString();
						resolvedValueSetList.add(t);
					}
				}
			}
			out.append("</table>\n");
			setHeadingBackGroundColor();
			resolvedValueSetIterator = resolvedValueSetList.listIterator();
		}
    }

    public void setHeadingBackGroundColor() {
		if (this.excelfile.toLowerCase().endsWith(FILE_TYPES[0])) {
			return;
		}
		String header = ExcelUtils.getExcelHeader(this.excelfile, this.sheet);
		Vector u = parseData(header, '|');
		String t = getHeadingLine(u);
		resolvedValueSetList.set(0, t);
	}

    public ListIterator getResolvedValueSetIterator() {
		return resolvedValueSetIterator;
	}

    public List getResolvedValueSetList() {
		return resolvedValueSetList;
	}


    /**
     * (Each Excel sheet row becomes an HTML table row) Generates an HTML table
     * row which has the same height as the Excel row.
     *
     * @param sheet_row
     *            The Excel row.
     */
//    private void tr(final HSSFRow row) {
	private void tr(Row sheet_row) {
        if (sheet_row == null) {
            return;
        }
        if (sheet_row instanceof HSSFRow) {
			HSSFRow row = (HSSFRow) sheet_row;
			out.append("<tr ");
			// Find merged cells in current row.
			for (int i = 0; i < row.getSheet().getNumMergedRegions(); ++i) {
				final CellRangeAddress merge = row.getSheet().getMergedRegion(i);
				if (merge == null) return;
				if (rowIndex >= merge.getFirstRow()
						&& rowIndex <= merge.getLastRow()) {
					mergeStart = merge.getFirstColumn();
					mergeEnd = merge.getLastColumn();
					break;
				}
			}
			out.append("style='");
			if (row.getHeight() != -1) {
				out.append("height: ")
						.append(Math.round(row.getHeight() / 20.0 * 1.33333))
						.append("px; ");
			}
			out.append("'>\n");
			for (colIndex = 0; colIndex < row.getLastCellNum(); ++colIndex) {
				td(row.getCell(colIndex));
			}
			out.append("</tr>\n");
		} else {
			XSSFRow row = (XSSFRow) sheet_row;
			out.append("<tr ");
			// Find merged cells in current row.
			for (int i = 0; i < row.getSheet().getNumMergedRegions(); ++i) {
				final CellRangeAddress merge = row.getSheet().getMergedRegion(i);
				if (merge == null) return;
				if (rowIndex >= merge.getFirstRow()
						&& rowIndex <= merge.getLastRow()) {
					mergeStart = merge.getFirstColumn();
					mergeEnd = merge.getLastColumn();
					break;
				}
			}
			out.append("style='");
			if (row.getHeight() != -1) {
				out.append("height: ")
						.append(Math.round(row.getHeight() / 20.0 * 1.33333))
						.append("px; ");
			}
			out.append("'>\n");
			for (colIndex = 0; colIndex < row.getLastCellNum(); ++colIndex) {
				td(row.getCell(colIndex));
			}
			out.append("</tr>\n");
		}
    }

    private void tr(Row sheet_row, StringBuffer buf) {
        if (sheet_row == null) {
            return;
        }
        if (sheet_row instanceof HSSFRow) {
			HSSFRow row = (HSSFRow) sheet_row;

			buf.append("<tr ");
			// Find merged cells in current row.
			for (int i = 0; i < row.getSheet().getNumMergedRegions(); ++i) {
				final CellRangeAddress merge = row.getSheet().getMergedRegion(i);
				if (merge == null) return;
				if (rowIndex >= merge.getFirstRow()
						&& rowIndex <= merge.getLastRow()) {
					mergeStart = merge.getFirstColumn();
					mergeEnd = merge.getLastColumn();
					break;
				}
			}
			buf.append("style='");
			if (row.getHeight() != -1) {
				buf.append("height: ")
						.append(Math.round(row.getHeight() / 20.0 * 1.33333))
						.append("px; ");

			}
			buf.append("'>");
			for (colIndex = 0; colIndex < row.getLastCellNum(); ++colIndex) {
				Cell cell = row.getCell(colIndex);
				if (cell == null) {
					//System.out.println("WARNING: column " + colIndex + " is null???");
				}
				td(row.getCell(colIndex), buf, colIndex);
			}
			buf.append("</tr>");

		} else {
			XSSFRow row = (XSSFRow) sheet_row;

			buf.append("<tr ");
			// Find merged cells in current row.
			for (int i = 0; i < row.getSheet().getNumMergedRegions(); ++i) {
				final CellRangeAddress merge = row.getSheet().getMergedRegion(i);
				if (merge == null) return;
				if (rowIndex >= merge.getFirstRow()
						&& rowIndex <= merge.getLastRow()) {
					mergeStart = merge.getFirstColumn();
					mergeEnd = merge.getLastColumn();
					break;
				}
			}
			buf.append("style='");
			if (row.getHeight() != -1) {
				buf.append("height: ")
						.append(Math.round(row.getHeight() / 20.0 * 1.33333))
						.append("px; ");

			}
			buf.append("'>");
			for (colIndex = 0; colIndex < row.getLastCellNum(); ++colIndex) {
				//td(row.getCell(colIndex));
				td(row.getCell(colIndex), buf);
			}
			buf.append("</tr>");
		}
    }

     public boolean isCode(String str) {
		 if (str == null) return false;
		 if (str.length() == 0) return false;
		 char c = str.charAt(0);
		 //String first_ch = str.substring(0, 1);
		 String first_ch = "" + c;
		 if (first_ch.compareTo("C") != 0) return false;
		 String substr = str.substring(1, str.length());
		 if (substr == null) return false;
		 for (int i=0; i<substr.length(); i++) {
			 c = substr.charAt(i);
			 if (!Character.isDigit(c)) return false;
		 }
         return true;
	 }

	 public String getHyperLink(String code) {
         StringBuffer buf = new StringBuffer();
         buf.append("<a href=\"");
         buf.append(URL);
		 buf.append("&code=" + code);
		 buf.append("\"");
		 buf.append(">");
		 buf.append(code);
		 buf.append("</a>");
		 return buf.toString();
	 }

    /**
     * (Each Excel sheet cell becomes an HTML table cell) Generates an HTML
     * table cell which has the same font styles, alignments, colours and
     * borders as the Excel cell.
     *
     * @param row_cell
     *            The Excel cell.
     */
    private void td(Cell row_cell) {
		if (row_cell == null) return;
		if (row_cell instanceof HSSFCell) {
			HSSFCell cell = (HSSFCell) row_cell;
			int colspan = 1;
			if (colIndex == mergeStart) {
				// First cell in the merging region - set colspan.
				colspan = mergeEnd - mergeStart + 1;
			} else if (colIndex == mergeEnd) {
				// Last cell in the merging region - no more skipped cells.
				mergeStart = -1;
				mergeEnd = -1;
				return;
			} else if (mergeStart != -1 && mergeEnd != -1 && colIndex > mergeStart
					&& colIndex < mergeEnd) {
				// Within the merging region - skip the cell.
				return;
			}

			out.append("<td height=\"15\" ");
			if (colspan > 1) {
				out.append("colspan='").append(colspan).append("' ");
			}
			if (cell == null) {
				out.append("/>\n");
				return;
			}

			out.append("style='");
			final HSSFCellStyle style = cell.getCellStyle();
			// Text alignment
			//switch (style.getAlignmentEnum()) {
			switch (style.getAlignment()) {
				case LEFT:
				out.append("text-align: left; ");
				break;
				case RIGHT:
				out.append("text-align: right; ");
				break;
				case CENTER:
				out.append("text-align: center; ");
				break;
				default:
				break;
			}

			// Font style, size and weight
			final HSSFFont font = style.getFont(book);
			if (font == null) return;
			if (font.getBold()) {
				out.append("font-weight: bold; ");
			}
			if (font.getItalic()) {
				out.append("font-style: italic; ");
			}
			if (font.getUnderline() != HSSFFont.U_NONE) {
				out.append("text-decoration: underline; ");
			}
			out.append("font-size: ")
					.append(Math.floor(font.getFontHeightInPoints() * 0.8))
					.append("pt; ");
			// Cell background and font colours
			final short[] backRGB = style.getFillForegroundColorColor()
					.getTriplet();
			final HSSFColor foreColor = palette.getColor(font.getColor());
			if (foreColor != null) {
				final short[] foreRGB = foreColor.getTriplet();
				if (foreRGB[0] != 0 || foreRGB[1] != 0 || foreRGB[2] != 0) {
					out.append("color: rgb(").append(foreRGB[0]).append(',')
							.append(foreRGB[1]).append(',').append(foreRGB[2])
							.append(");");
				}
			}
			if (backRGB[0] != 0 || backRGB[1] != 0 || backRGB[2] != 0) {
				out.append("background-color: rgb(").append(backRGB[0]).append(',')
						.append(backRGB[1]).append(',').append(backRGB[2])
						.append(");");
			}
			// Border
			/*
			if (style.getBorderTopEnum() != BorderStyle.NONE) {
				out.append("border-top-style: solid; ");
			}
			if (style.getBorderRightEnum() != BorderStyle.NONE) {
				out.append("border-right-style: solid; ");
			}
			if (style.getBorderBottomEnum() != BorderStyle.NONE) {
				out.append("border-bottom-style: solid; ");
			}
			if (style.getBorderLeftEnum() != BorderStyle.NONE) {
				out.append("border-left-style: solid; ");
			}
			*/
			if (style.getBorderTop() != BorderStyle.NONE) {
				out.append("border-top-style: solid; ");
			}
			if (style.getBorderRight() != BorderStyle.NONE) {
				out.append("border-right-style: solid; ");
			}
			if (style.getBorderBottom() != BorderStyle.NONE) {
				out.append("border-bottom-style: solid; ");
			}
			if (style.getBorderLeft() != BorderStyle.NONE) {
				out.append("border-left-style: solid; ");
			}
			out.append("'>");
			String val = "";
			/*
			try {
				switch (cell.getCellType()) {
				case HSSFCell.CELL_TYPE_STRING:
					val = cell.getStringCellValue();
					break;
				case HSSFCell.CELL_TYPE_NUMERIC:
					// POI does not distinguish between integer and double, thus:
					final double original = cell.getNumericCellValue(),
					rounded = Math.round(original);
					if (Math.abs(rounded - original) < 0.00000000000000001) {
						val = String.valueOf((int) rounded);
					} else {
						val = String.valueOf(original);
					}
					break;
				case HSSFCell.CELL_TYPE_FORMULA:
					final CellValue cv = evaluator.evaluate(cell);
					if (cv == null) return;
					switch (cv.getCellType()) {
					case Cell.CELL_TYPE_BOOLEAN:
						out.append(cv.getBooleanValue());
						break;
					case Cell.CELL_TYPE_NUMERIC:
						out.append(cv.getNumberValue());
						break;
					case Cell.CELL_TYPE_STRING:
						out.append(cv.getStringValue());
						break;
					case Cell.CELL_TYPE_BLANK:
						break;
					case Cell.CELL_TYPE_ERROR:
						break;
					default:
						break;
					}
					break;
				default:
					// Neither string or number? Could be a date.
					try {
						val = sdf.format(cell.getDateCellValue());
					} catch (final Exception e1) {
					}
				}
			} catch (final Exception e) {
				val = e.getMessage();
			}
			if ("null".equals(val)) {
				val = "";
			}
			*/
			val = getCellData(cell);
			if (pix.containsKey(rowIndex)) {
				if (pix.get(rowIndex).containsKey(colIndex)) {
					for (final HSSFPictureData pic : pix.get(rowIndex)
							.get(colIndex)) {
						out.append("<img alt='Image in Excel sheet' src='data:");
						out.append(pic.getMimeType());
						out.append(";base64,");
						try {
							out.append(new String(
									Base64.encodeBase64(pic.getData()), "US-ASCII"));
						} catch (final UnsupportedEncodingException e) {
							throw new RuntimeException(e);
						}
						out.append("'/>");
					}
				}
			}


			if (isCode(val) && this.URL != null) {
				val = getHyperLink(val);
			}
			out.append(val);
			out.append("</td>\n");
		} else {
			XSSFCell cell = (XSSFCell) row_cell;
			int colspan = 1;
			if (colIndex == mergeStart) {
				// First cell in the merging region - set colspan.
				colspan = mergeEnd - mergeStart + 1;
			} else if (colIndex == mergeEnd) {
				// Last cell in the merging region - no more skipped cells.
				mergeStart = -1;
				mergeEnd = -1;
				return;
			} else if (mergeStart != -1 && mergeEnd != -1 && colIndex > mergeStart
					&& colIndex < mergeEnd) {
				// Within the merging region - skip the cell.
				return;
			}

			out.append("<td height=\"15\" ");
			if (colspan > 1) {
				out.append("colspan='").append(colspan).append("' ");
			}
			if (cell == null) {
				out.append("/>\n");
				return;
			}

			out.append("style='");
			final XSSFCellStyle style = cell.getCellStyle();
			// Text alignment
			//switch (style.getAlignmentEnum()) {
			switch (style.getAlignment()) {
				case LEFT:
				out.append("text-align: left; ");
				break;
				case RIGHT:
				out.append("text-align: right; ");
				break;
				case CENTER:
				out.append("text-align: center; ");
				break;
			default:
				break;
			}

			// Font style, size and weight
			final XSSFFont font = style.getFont();
			if (font == null) return;
			if (font.getBold()) {
				out.append("font-weight: bold; ");
			}
			if (font.getItalic()) {
				out.append("font-style: italic; ");
			}
			if (font.getUnderline() != XSSFFont.U_NONE) {
				out.append("text-decoration: underline; ");
			}
			out.append("font-size: ")
					.append(Math.floor(font.getFontHeightInPoints() * 0.8))
					.append("pt; ");
			// Cell background and font colours

			final short[] foreRGB = new short[]{0, 0, 0};//style.getFillForegroundColorColor().getTriplet();
			//final short[] backRGB = new short[]{192, 192, 192};
			final short[] backRGB = new short[]{255, 255, 255};
			out.append("color: rgb(").append(foreRGB[0]).append(',')
					.append(foreRGB[1]).append(',').append(foreRGB[2])
					.append(");");
			out.append("background-color: rgb(").append(backRGB[0]).append(',')
					.append(backRGB[1]).append(',').append(backRGB[2])
					.append(");");
			/*
			//final XSSFColor foreColor = palette.getColor(font.getColor());
			//if (foreColor != null) {
				final short[] foreRGB = foreColor.getTriplet();
				if (foreRGB[0] != 0 || foreRGB[1] != 0 || foreRGB[2] != 0) {
					out.append("color: rgb(").append(foreRGB[0]).append(',')
							.append(foreRGB[1]).append(',').append(foreRGB[2])
							.append(");");
				}
			}
			if (backRGB[0] != 0 || backRGB[1] != 0 || backRGB[2] != 0) {
				out.append("background-color: rgb(").append(backRGB[0]).append(',')
						.append(backRGB[1]).append(',').append(backRGB[2])
						.append(");");
			}
			*/
			// Border
			/*
			if (style.getBorderTopEnum() != BorderStyle.NONE) {
				out.append("border-top-style: solid; ");
			}
			if (style.getBorderRightEnum() != BorderStyle.NONE) {
				out.append("border-right-style: solid; ");
			}
			if (style.getBorderBottomEnum() != BorderStyle.NONE) {
				out.append("border-bottom-style: solid; ");
			}
			if (style.getBorderLeftEnum() != BorderStyle.NONE) {
				out.append("border-left-style: solid; ");
			}
			*/
			if (style.getBorderTop() != BorderStyle.NONE) {
				out.append("border-top-style: solid; ");
			}
			//if (style.getBorderRightEnum() != BorderStyle.NONE) {
			if (style.getBorderRight() != BorderStyle.NONE) {
				out.append("border-right-style: solid; ");
			}
			//if (style.getBorderBottomEnum() != BorderStyle.NONE) {
			if (style.getBorderBottom() != BorderStyle.NONE) {
				out.append("border-bottom-style: solid; ");
			}
			//if (style.getBorderLeftEnum() != BorderStyle.NONE) {
			if (style.getBorderLeft() != BorderStyle.NONE) {
				out.append("border-left-style: solid; ");
			}
			out.append("'>");
			String val = "";
			/*
			try {
				switch (cell.getCellType()) {
				case XSSFCell.CELL_TYPE_STRING:
					val = cell.getStringCellValue();
					break;
				case XSSFCell.CELL_TYPE_NUMERIC:
					// POI does not distinguish between integer and double, thus:
					final double original = cell.getNumericCellValue(),
					rounded = Math.round(original);
					if (Math.abs(rounded - original) < 0.00000000000000001) {
						val = String.valueOf((int) rounded);
					} else {
						val = String.valueOf(original);
					}
					break;
				case XSSFCell.CELL_TYPE_FORMULA:
					final CellValue cv = evaluator.evaluate(cell);
					if (cv == null) return;
					switch (cv.getCellType()) {
					case Cell.CELL_TYPE_BOOLEAN:
						out.append(cv.getBooleanValue());
						break;
					case Cell.CELL_TYPE_NUMERIC:
						out.append(cv.getNumberValue());
						break;
					case Cell.CELL_TYPE_STRING:
						out.append(cv.getStringValue());
						break;
					case Cell.CELL_TYPE_BLANK:
						break;
					case Cell.CELL_TYPE_ERROR:
						break;
					default:
						break;
					}
					break;
				default:
					// Neither string or number? Could be a date.
					try {
						val = sdf.format(cell.getDateCellValue());
					} catch (final Exception e1) {
					}
				}
			} catch (final Exception e) {
				val = e.getMessage();
			}
			if ("null".equals(val)) {
				val = "";
			}
			*/
			/*
			if (pix.containsKey(rowIndex)) {
				if (pix.get(rowIndex).containsKey(colIndex)) {
					for (final HSSFPictureData pic : pix.get(rowIndex)
							.get(colIndex)) {
						out.append("<img alt='Image in Excel sheet' src='data:");
						out.append(pic.getMimeType());
						out.append(";base64,");
						try {
							out.append(new String(
									Base64.encodeBase64(pic.getData()), "US-ASCII"));
						} catch (final UnsupportedEncodingException e) {
							throw new RuntimeException(e);
						}
						out.append("'/>");
					}
				}
			}
			*/

			val = getCellData(cell);
			if (isCode(val) && this.URL != null) {
				val = getHyperLink(val);
			}
			out.append(val);
			out.append("</td>\n");
		}
	}

    private void td(Cell row_cell, StringBuffer buf) {
		td(row_cell, buf, -1);
	}

    private void td(Cell row_cell, StringBuffer buf, int columnIndex) {
		if (row_cell == null) {
			if (columnIndex != -1) {
				//System.out.println("WARNING: row_cell == null columnIndex: " + columnIndex);
			}
			//KLO
			//buf.append("/>");
			buf.append("<td></td>");
			return;
		}

		if (row_cell instanceof HSSFCell) {
			HSSFCell cell = (HSSFCell) row_cell;
			int colspan = 1;
			if (colIndex == mergeStart) {
				// First cell in the merging region - set colspan.
				colspan = mergeEnd - mergeStart + 1;
			} else if (colIndex == mergeEnd) {
				// Last cell in the merging region - no more skipped cells.
				mergeStart = -1;
				mergeEnd = -1;
				return;
			} else if (mergeStart != -1 && mergeEnd != -1 && colIndex > mergeStart
					&& colIndex < mergeEnd) {
				// Within the merging region - skip the cell.
				return;
			}

			buf.append("<td height=\"15\" ");

			if (colspan > 1) {
				buf.append("colspan='").append(colspan).append("' ");
			}
			if (cell == null) {
				buf.append("/>");
				return;
			}
			buf.append("style='");
			final HSSFCellStyle style = cell.getCellStyle();
			// Text alignment
			//switch (style.getAlignmentEnum()) {
			switch (style.getAlignment()) {
				case LEFT:
				buf.append("text-align: left; ");
				break;
				case RIGHT:
				buf.append("text-align: right; ");
				break;
			case CENTER:
				buf.append("text-align: center; ");
				break;
			default:
				break;
			}
			// Font style, size and weight
			final HSSFFont font = style.getFont(book);
			if (font == null) return;
			if (font.getBold()) {
				buf.append("font-weight: bold; ");
			}
			if (font.getItalic()) {
				buf.append("font-style: italic; ");
			}
			if (font.getUnderline() != HSSFFont.U_NONE) {
				buf.append("text-decoration: underline; ");
			}
			buf.append("font-size: ")
					.append(Math.floor(font.getFontHeightInPoints() * 0.8))
					.append("pt; ");

			// Cell background and font colours
			final short[] backRGB = style.getFillForegroundColorColor()
					.getTriplet();
			final HSSFColor foreColor = palette.getColor(font.getColor());
			if (foreColor != null) {
				final short[] foreRGB = foreColor.getTriplet();
				if (foreRGB[0] != 0 || foreRGB[1] != 0 || foreRGB[2] != 0) {
					buf.append("color: rgb(").append(foreRGB[0]).append(',')
							.append(foreRGB[1]).append(',').append(foreRGB[2])
							.append(");");

				}
			}
			if (backRGB[0] != 0 || backRGB[1] != 0 || backRGB[2] != 0) {
				buf.append("background-color: rgb(").append(backRGB[0]).append(',')
						.append(backRGB[1]).append(',').append(backRGB[2])
						.append(");");

			}
			// Border
			if (style.getBorderTop() != BorderStyle.NONE) {
				buf.append("border-top-style: solid; ");
			}
			//if (style.getBorderRightEnum() != BorderStyle.NONE) {
			if (style.getBorderRight() != BorderStyle.NONE) {
				buf.append("border-right-style: solid; ");
			}
			//if (style.getBorderBottomEnum() != BorderStyle.NONE) {
			if (style.getBorderBottom() != BorderStyle.NONE) {
				buf.append("border-bottom-style: solid; ");
			}
			//if (style.getBorderLeftEnum() != BorderStyle.NONE) {
			if (style.getBorderLeft() != BorderStyle.NONE) {
				buf.append("border-left-style: solid; ");
			}

			/*
			if (style.getBorderTopEnum() != BorderStyle.NONE) {
				buf.append("border-top-style: solid; ");
			}
			if (style.getBorderRightEnum() != BorderStyle.NONE) {
				buf.append("border-right-style: solid; ");
			}
			if (style.getBorderBottomEnum() != BorderStyle.NONE) {
				buf.append("border-bottom-style: solid; ");
			}
			if (style.getBorderLeftEnum() != BorderStyle.NONE) {
				buf.append("border-left-style: solid; ");
			}
			*/
			buf.append("'>");
			String val = "";

/*
			try {
				switch (cell.getCellType()) {
				case HSSFCell.CELL_TYPE_STRING:
					val = cell.getStringCellValue();
					break;
				case HSSFCell.CELL_TYPE_NUMERIC:
					// POI does not distinguish between integer and double, thus:
					final double original = cell.getNumericCellValue(),
					rounded = Math.round(original);
					if (Math.abs(rounded - original) < 0.00000000000000001) {
						val = String.valueOf((int) rounded);
					} else {
						val = String.valueOf(original);
					}
					break;
				case HSSFCell.CELL_TYPE_FORMULA:
					final CellValue cv = evaluator.evaluate(cell);
					if (cv == null) return;
					switch (cv.getCellType()) {
					case Cell.CELL_TYPE_BOOLEAN:
						buf.append(cv.getBooleanValue());
						break;
					case Cell.CELL_TYPE_NUMERIC:
						buf.append(cv.getNumberValue());
						break;
					case Cell.CELL_TYPE_STRING:
						buf.append(cv.getStringValue());
						break;
					case Cell.CELL_TYPE_BLANK:
						break;
					case Cell.CELL_TYPE_ERROR:
						break;
					default:
						break;
					}
					break;
				default:
					// Neither string or number? Could be a date.
					try {
						val = sdf.format(cell.getDateCellValue());
					} catch (final Exception e1) {
					}
				}
			} catch (final Exception e) {
				val = e.getMessage();
			}
			if ("null".equals(val)) {
				val = "";
			}
			*/
			val = getCellData(cell);


			if (pix.containsKey(rowIndex)) {
				if (pix.get(rowIndex).containsKey(colIndex)) {
					for (final HSSFPictureData pic : pix.get(rowIndex)
							.get(colIndex)) {
						buf.append("<img alt='Image in Excel sheet' src='data:");
						buf.append(pic.getMimeType());
						buf.append(";base64,");
						try {
							buf.append(new String(
									Base64.encodeBase64(pic.getData()), "US-ASCII"));


						} catch (final UnsupportedEncodingException e) {
							throw new RuntimeException(e);
						}
						buf.append("'/>");
					}
				}
			}
			if (isCode(val) && this.URL != null) {
				val = getHyperLink(val);
			}
			buf.append(val);
			buf.append("</td>");

		} else {
			XSSFCell cell = (XSSFCell) row_cell;
			int colspan = 1;
			if (colIndex == mergeStart) {
				// First cell in the merging region - set colspan.
				colspan = mergeEnd - mergeStart + 1;
			} else if (colIndex == mergeEnd) {
				// Last cell in the merging region - no more skipped cells.
				mergeStart = -1;
				mergeEnd = -1;
				return;
			} else if (mergeStart != -1 && mergeEnd != -1 && colIndex > mergeStart
					&& colIndex < mergeEnd) {
				// Within the merging region - skip the cell.
				return;
			}

			buf.append("<td height=\"15\" ");

			if (colspan > 1) {
				buf.append("colspan='").append(colspan).append("' ");
			}

			buf.append("style='");
			final XSSFCellStyle style = cell.getCellStyle();
			// Text alignment
			//switch (style.getAlignmentEnum()) {
			switch (style.getAlignment()) {
			case LEFT:
				buf.append("text-align: left; ");
				break;
			case RIGHT:
				buf.append("text-align: right; ");
				break;
			case CENTER:
				buf.append("text-align: center; ");
				break;
			default:
				break;
			}
			// Font style, size and weight
			final XSSFFont font = style.getFont();
			if (font == null) return;
			if (font.getBold()) {
				buf.append("font-weight: bold; ");
			}
			if (font.getItalic()) {
				buf.append("font-style: italic; ");
			}
			if (font.getUnderline() != XSSFFont.U_NONE) {
				buf.append("text-decoration: underline; ");
			}
			buf.append("font-size: ")
					.append(Math.floor(font.getFontHeightInPoints() * 0.8))
					.append("pt; ");

			final short[] foreRGB = new short[]{0, 0, 0};//style.getFillForegroundColorColor().getTriplet();
			//final short[] backRGB = new short[]{192, 192, 192};
			final short[] backRGB = new short[]{255, 255, 255};
			buf.append("color: rgb(").append(foreRGB[0]).append(',')
					.append(foreRGB[1]).append(',').append(foreRGB[2])
					.append(");");
			buf.append("background-color: rgb(").append(backRGB[0]).append(',')
					.append(backRGB[1]).append(',').append(backRGB[2])
					.append(");");


			// Border
			//if (style.getBorderTopEnum() != BorderStyle.NONE) {
			if (style.getBorderTop() != BorderStyle.NONE) {
				buf.append("border-top-style: solid; ");
			}
			//if (style.getBorderRightEnum() != BorderStyle.NONE) {
			if (style.getBorderRight() != BorderStyle.NONE) {
				buf.append("border-right-style: solid; ");
			}
			//if (style.getBorderBottomEnum() != BorderStyle.NONE) {
			if (style.getBorderBottom() != BorderStyle.NONE) {
				buf.append("border-bottom-style: solid; ");
			}
			//if (style.getBorderLeftEnum() != BorderStyle.NONE) {
			if (style.getBorderLeft() != BorderStyle.NONE) {
				buf.append("border-left-style: solid; ");
			}
			buf.append("'>");
			String val = "";
			/*
			try {
				switch (cell.getCellType()) {
				case XSSFCell.CELL_TYPE_STRING:
					val = cell.getStringCellValue();
					break;
				case XSSFCell.CELL_TYPE_NUMERIC:
					// POI does not distinguish between integer and double, thus:
					final double original = cell.getNumericCellValue(),
					rounded = Math.round(original);
					if (Math.abs(rounded - original) < 0.00000000000000001) {
						val = String.valueOf((int) rounded);
					} else {
						val = String.valueOf(original);
					}
					break;
				case XSSFCell.CELL_TYPE_FORMULA:
					final CellValue cv = evaluator.evaluate(cell);
					if (cv == null) return;
					switch (cv.getCellType()) {
					case Cell.CELL_TYPE_BOOLEAN:
						buf.append(cv.getBooleanValue());
						break;
					case Cell.CELL_TYPE_NUMERIC:
						buf.append(cv.getNumberValue());
						break;
					case Cell.CELL_TYPE_STRING:
						buf.append(cv.getStringValue());
						break;
					case Cell.CELL_TYPE_BLANK:
						break;
					case Cell.CELL_TYPE_ERROR:
						break;
					default:
						break;
					}
					break;
				default:
					// Neither string or number? Could be a date.
					try {
						val = sdf.format(cell.getDateCellValue());
					} catch (final Exception e1) {
					}
				}
			} catch (final Exception e) {
				val = e.getMessage();
			}
			if ("null".equals(val)) {
				val = "";
			}
			*/
			val = getCellData(cell);
			if (pix.containsKey(rowIndex)) {
				if (pix.get(rowIndex).containsKey(colIndex)) {
					for (final HSSFPictureData pic : pix.get(rowIndex)
							.get(colIndex)) {
						buf.append("<img alt='Image in Excel sheet' src='data:");
						buf.append(pic.getMimeType());
						buf.append(";base64,");


						try {
							buf.append(new String(
									Base64.encodeBase64(pic.getData()), "US-ASCII"));


						} catch (final UnsupportedEncodingException e) {
							throw new RuntimeException(e);
						}
						buf.append("'/>");
					}
				}
			}


			if (isCode(val) && this.URL != null) {
				val = getHyperLink(val);
			}
			buf.append(val);
			buf.append("</td>");
		}

    }


    public InputStream getInputStream(String filename) {
		try {
			return new FileInputStream(new File(filename));
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return null;
	}


    public String getHTML() {
        return out.toString();
    }


    public Vector getTableContent() {
       return rvs_content_vec;
    }


    public Vector getTableContent(int sheet_number, int startIndex, int endIndex) {
		if (book instanceof HSSFWorkbook) {
			HSSFWorkbook hssfWorkbook = (HSSFWorkbook) book;
			HSSFSheet sheet = hssfWorkbook.getSheetAt(sheet_number);
			Vector v = new Vector();
			getRowData(v, sheet.getRow(0));
			for (int i=startIndex; i<=endIndex; i++) {
				getRowData(v, sheet.getRow(i));
			}
			return v;
		} else {
			XSSFWorkbook hssfWorkbook = (XSSFWorkbook) book;
			XSSFSheet sheet = hssfWorkbook.getSheetAt(sheet_number);
			Vector v = new Vector();
			getRowData(v, sheet.getRow(0));
			for (int i=startIndex; i<=endIndex; i++) {
				getRowData(v, sheet.getRow(i));
			}
			return v;
		}
	}

    //private void getRowData(Vector v, final HSSFRow row) {
	private void getRowData(Vector v, Row row_in_sheet) {
		if (row_in_sheet instanceof HSSFRow) {
			HSSFRow row = (HSSFRow) row_in_sheet;

			if (row == null) {
				return;
			}

			for (int i = 0; i < row.getSheet().getNumMergedRegions(); ++i) {
				final CellRangeAddress merge = row.getSheet().getMergedRegion(i);
				if (merge == null) return;
				if (rowIndex >= merge.getFirstRow()
						&& rowIndex <= merge.getLastRow()) {
					mergeStart = merge.getFirstColumn();
					mergeEnd = merge.getLastColumn();
					break;
				}
			}

			StringBuffer buf = new StringBuffer();
			for (colIndex = 0; colIndex < row.getLastCellNum(); ++colIndex) {
				if (colIndex > 0) {
					buf.append("|");
				}
				String val = getCellData(row.getCell(colIndex));
				if (val != null) {
					buf.append(val);
				}
			}
			v.add(buf.toString());
		} else {
			XSSFRow row = (XSSFRow) row_in_sheet;
			if (row == null) {
				return;
			}

			for (int i = 0; i < row.getSheet().getNumMergedRegions(); ++i) {
				final CellRangeAddress merge = row.getSheet().getMergedRegion(i);
				if (merge == null) return;
				if (rowIndex >= merge.getFirstRow()
						&& rowIndex <= merge.getLastRow()) {
					mergeStart = merge.getFirstColumn();
					mergeEnd = merge.getLastColumn();
					break;
				}
			}

			StringBuffer buf = new StringBuffer();
			for (colIndex = 0; colIndex < row.getLastCellNum(); ++colIndex) {
				if (colIndex > 0) {
					buf.append("|");
				}
				String val = getCellData(row.getCell(colIndex));
				if (val != null) {
					buf.append(val);
				}
			}
			v.add(buf.toString());
		}
    }

    //private String getCellData(final HSSFCell cell) {
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


/*
	private String getCellData(Cell row_cell) {
		if (row_cell instanceof HSSFCell) {
			HSSFCell cell = (HSSFCell) row_cell;
			if (cell == null) return null;
			int colspan = 1;
			if (colIndex == mergeStart) {
				// First cell in the merging region - set colspan.
				colspan = mergeEnd - mergeStart + 1;
			} else if (colIndex == mergeEnd) {
				// Last cell in the merging region - no more skipped cells.
				mergeStart = -1;
				mergeEnd = -1;
				return null;
			} else if (mergeStart != -1 && mergeEnd != -1 && colIndex > mergeStart
					&& colIndex < mergeEnd) {
				// Within the merging region - skip the cell.
				return null;
			}
			String val = "";
			try {
				switch (cell.getCellType()) {
				case HSSFCell.CELL_TYPE_STRING:
					val = cell.getStringCellValue();
					break;
				case HSSFCell.CELL_TYPE_NUMERIC:
					// POI does not distinguish between integer and double, thus:
					final double original = cell.getNumericCellValue(),
					rounded = Math.round(original);
					if (Math.abs(rounded - original) < 0.00000000000000001) {
						val = String.valueOf((int) rounded);
					} else {
						val = String.valueOf(original);
					}
					break;
				case HSSFCell.CELL_TYPE_FORMULA:
					final CellValue cv = evaluator.evaluate(cell);
					if (cv == null) return null;
					switch (cv.getCellType()) {
					case Cell.CELL_TYPE_BOOLEAN:
						out.append(cv.getBooleanValue());
						break;
					case Cell.CELL_TYPE_NUMERIC:
						out.append(cv.getNumberValue());
						break;
					case Cell.CELL_TYPE_STRING:
						out.append(cv.getStringValue());
						break;
					case Cell.CELL_TYPE_BLANK:
						break;
					case Cell.CELL_TYPE_ERROR:
						break;
					default:
						break;
					}
					break;
				default:
					// Neither string or number? Could be a date.
					try {
						val = sdf.format(cell.getDateCellValue());
					} catch (final Exception e1) {
					}
				}
			} catch (final Exception e) {
				val = e.getMessage();
			}
			if ("null".equals(val)) {
				val = "";
			}
			return val;
		} else {

			XSSFCell cell = (XSSFCell) row_cell;
			if (cell == null) return null;
			int colspan = 1;
			if (colIndex == mergeStart) {
				// First cell in the merging region - set colspan.
				colspan = mergeEnd - mergeStart + 1;
			} else if (colIndex == mergeEnd) {
				// Last cell in the merging region - no more skipped cells.
				mergeStart = -1;
				mergeEnd = -1;
				return null;
			} else if (mergeStart != -1 && mergeEnd != -1 && colIndex > mergeStart
					&& colIndex < mergeEnd) {
				// Within the merging region - skip the cell.
				return null;
			}
			String val = "";
			try {
				switch (cell.getCellType()) {
				case XSSFCell.CELL_TYPE_STRING:
					val = cell.getStringCellValue();
					break;
				case XSSFCell.CELL_TYPE_NUMERIC:
					// POI does not distinguish between integer and double, thus:
					final double original = cell.getNumericCellValue(),
					rounded = Math.round(original);
					if (Math.abs(rounded - original) < 0.00000000000000001) {
						val = String.valueOf((int) rounded);
					} else {
						val = String.valueOf(original);
					}
					break;
				case XSSFCell.CELL_TYPE_FORMULA:
					final CellValue cv = evaluator.evaluate(cell);
					if (cv == null) return null;
					switch (cv.getCellType()) {
					case Cell.CELL_TYPE_BOOLEAN:
						out.append(cv.getBooleanValue());
						break;
					case Cell.CELL_TYPE_NUMERIC:
						out.append(cv.getNumberValue());
						break;
					case Cell.CELL_TYPE_STRING:
						out.append(cv.getStringValue());
						break;
					case Cell.CELL_TYPE_BLANK:
						break;
					case Cell.CELL_TYPE_ERROR:
						break;
					default:
						break;
					}
					break;
				default:
					// Neither string or number? Could be a date.
					try {
						val = sdf.format(cell.getDateCellValue());
					} catch (final Exception e1) {
					}
				}
			} catch (final Exception e) {
				val = e.getMessage();
			}
			if ("null".equals(val)) {
				val = "";
			}
			return val;
		}
    }
*/

    public Vector tableContent2CSV(Vector tableContent) {

		if (tableContent == null) return null;
		Vector v = new Vector();

		String headings = (String) tableContent.elementAt(0);
		Vector w = parseData(headings, '|');
		StringBuffer buf = new StringBuffer();
		for (int i=0; i<w.size(); i++) {
			String t = (String) w.elementAt(i);
			if (i > 0) {
				buf.append(",");
			}
			buf.append("\"");
			buf.append(t);
			buf.append("\"");
		}
		v.add(buf.toString());
		if (tableContent.size() == 1) return v;
		for (int i=1; i<tableContent.size(); i++) {
			String t = (String) tableContent.elementAt(i);
			w = parseData(t, '|');
			buf = new StringBuffer();
			for (int k=0; k<w.size(); k++) {
				String s = (String) w.elementAt(k);
				if (k > 0) {
					buf.append(",");
				}
				buf.append("\"");
				buf.append(s);
				buf.append("\"");
			}
			v.add(buf.toString());
		}
		return v;
	}

	public Vector extractRawDataFromTableContent() {
		if (resolvedValueSetList == null) {
			return null;
		}
		Vector w = new Vector();
		for (int i=0; i<resolvedValueSetList.size(); i++) {
            String line = (String) resolvedValueSetList.get(i);
            int k = i+1;
			line = line.replaceAll("</a>", "");
			line = line.replaceAll("</td>", "|");
			line = line.replaceAll("</th>", "|");
			Vector v = parseData(line, '|');

			StringBuffer buf = new StringBuffer();
			for (int j=0; j<v.size(); j++) {
				String t = (String) v.elementAt(j);
				int n = t.lastIndexOf(">");
				if (n != -1) {
					t = t.substring(n+1, t.length());
					if (j > 0) {
						buf.append("|");
					}
					buf.append(t);
			    }
			}
			w.add(buf.toString());
		}
		return w;
	}


    public static String removeHyperlinks(String t) {
		if (t == null) return t;
		int n = t.indexOf("<a href=");
		while (n != -1) {
			String t1 = t.substring(0, n);
			String t2 = t.substring(n, t.length());
			int m = t2.indexOf(">");
			String t3 = "";
			if (m != -1) {
			    t3 = t2.substring(m+1, t2.length());
				m = t3.indexOf("</a>");
				if (m != -1) {
					String t4 = t3.substring(0, m);
					t3 = t4 + t3.substring(m+4, t3.length());
				}
			}
			t = t1 + t3;
			n = t.indexOf("<a href=");
		}
		return t;
	}


    public static String addRowBackgroundColor(String tr) {
		return addRowBackgroundColor(YELLOW, tr);
	}


    public static String addRowBackgroundColor(String colorCode, String tr) {
		if (tr == null) return null;

		if (colorCode == null) {
			colorCode = YELLOW;
		}
		int n = tr.indexOf("<tr>");
		if (n != -1) {
			tr = tr.replace("<tr>", "<tr " + "bgcolor=" + "\"" + colorCode + "\">");
			return tr;
		}

		n = tr.indexOf("<tr ");
		if (n != -1) {
			tr = tr.replace("<tr ", "<tr " + "bgcolor=" + "\"" + colorCode + "\" ");
			return tr;
		}
		return tr;
	}

    public static String th(String filename, int sheet) {
		String header = ExcelUtils.getExcelHeader(filename, sheet);
		StringBuffer buf = new StringBuffer();
        buf.append("<tr style=\"height: 51px;\">").append("\n");
        Vector u = parseData(header, '|');
        for (int i=0; i<u.size(); i++) {
			String t = (String) u.elementAt(i);
            buf.append("<th style=\"text-align: center; font-size: 8.0pt; background-color: rgb(192,192,192);border-top-style: solid; border-right-style: solid; border-bottom-style: solid; border-left-style: solid; \" height=\"15\">" + t + "</th>").append("\n");
		}
        buf.append("</tr>").append("\n");
		return buf.toString();
	}

    public String getResolvedValueSetContent() {
		String table_content = "";
		StringBuffer table_content_buf = new StringBuffer();
		boolean bool_val;
		table_content_buf.append(getOpenTableTag("rvs_table")).append("\n");
		List list = getResolvedValueSetList();
		String first_line = (String) list.get(0);
		String first_line0 = first_line;

		first_line = first_line.replaceAll("td", "th");


		table_content_buf.append(first_line).append("\n");

		for (int k=1; k<list.size(); k++) {
			String line = (String) list.get(k);
			if (line.compareTo(first_line0) != 0) {
            	table_content_buf.append(line).append("\n");
			}
		}
		table_content_buf.append(getCloseTableTag()).append("\n");
		table_content = table_content_buf.toString();
		return table_content_buf.toString();
	}

    public HSSFWorkbook createHSSFWorkbook(String sheet_name) throws IOException {
		String delim = "|";
		return createHSSFWorkbook(sheet_name, this.getTableContent());
	}

    public HSSFWorkbook createHSSFWorkbook(String sheet_name, Vector delimitedData) throws IOException {
		String delim = "|";
		return createHSSFWorkbook(sheet_name, delimitedData, delim);
	}


    public HSSFWorkbook createHSSFWorkbook(String sheet_name, Vector delimitedData, String delim) throws IOException {
        HSSFWorkbook hwb = new HSSFWorkbook();
        char delimiter = delim.charAt(0);
        try {
            HSSFSheet sheet = hwb.createSheet(sheet_name);
            //for (int k = 0; k < arList.size(); k++) {
			for (int j = 0; j < delimitedData.size(); j++) {
				String line = (String) delimitedData.elementAt(j);
				Vector u = parseData(line, delimiter);
                //ArrayList<String> ardata = (ArrayList<String>) arList.get(k);
                HSSFRow row = sheet.createRow((short) 0 + j);
                //for (int p = 0; p < ardata.size(); p++) {
				for (int k = 0; k < u.size(); k++) {
					String cell_val = (String) u.elementAt(k);
                    //System.out.print(ardata.get(p));
                    HSSFCell cell = row.createCell((short) k);
                    cell.setCellValue(cell_val);
                    //cell.setCellValue(ardata.get(p).toString());
                }
            }
        } catch (Exception ex) {
        }

        return hwb;
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

    public static void saveToFile(String outputfile, Vector v) {
        try {
            FileOutputStream output = new FileOutputStream(outputfile);
            for (int i=0; i<v.size(); i++) {
				String data = (String) v.elementAt(i);
				if (i < v.size()) {
					data = data + "\n";
				}
				byte[] array = data.getBytes();
				output.write(array);
			}
            output.close();
        } catch(Exception e) {
            e.getStackTrace();
        }
    }

	public static void main(String [ ] args)
	{
		String excelfile = null;
		try {
			excelfile = "FDA-CDRH_NCIt_Subsets.xls";
			int sheet = 0;
			int col = 0;
			String code = "C91801";
			boolean cdisc = false;
			String url = null;
			String htmlfile = "test1.html";

			excelfile = "Mapped_ICDO3.2_Terminology_(20.05b)_05-16-2020.xlsx";
			sheet = 0;
			col = 0;
			code = "C168656";
			htmlfile = "test2.html";

            cdisc = true;
			excelfile = "CDASH_Terminology.xls";
			sheet = 1;
			col = 1;
			code = "C78418";
			htmlfile = "test3.html";

            cdisc = true;
			excelfile = "ADaM_Terminology.xls";
			sheet = 1;
			col = 0;
			code = "C81223";
			htmlfile = "test4.html";
			//5:1:C178127
            cdisc = false;
			excelfile = "PCDC_Terminology.xls";
			//2:1:C173217
			sheet = 4;
			col = 0;
			code = "C178127";
			htmlfile = "test7.html";

            String DEFAULT_URL = "https://nciterms65.nci.nih.gov/ncitbrowser/ConceptReport.jsp?dictionary=NCI_Thesaurus&ns=ncit";

			int startIndex = ExcelUtils.getExcelStartRow(excelfile, sheet, col, code);
            ResolvedValueSetIteratorHolder rvsi = new ResolvedValueSetIteratorHolder(excelfile, sheet, startIndex, col, code, DEFAULT_URL, cdisc);

			Vector w = new Vector();

			w.add("<html><head></head><body><table>");
			ListIterator iterator = rvsi.getResolvedValueSetIterator();
			int lcv = 0;
			while (iterator.hasNext()) {
				String t = (String) iterator.next();
				w.add(t);
				lcv++;
			}
			w.add("</table></body></html>");
			saveToFile(htmlfile, w);

			Vector content = rvsi.getTableContent();
			saveToFile("content.txt", content);

			String content_str = rvsi.getResolvedValueSetContent();
			w = new Vector();
			w.add(content_str);
			saveToFile("content_str.txt", content);
    	} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}

