package gov.nih.nci.evs.restapi.util;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xddf.usermodel.chart.ChartTypes;
import org.apache.poi.xddf.usermodel.chart.LegendPosition;
import org.apache.poi.xddf.usermodel.chart.XDDFChartData;
import org.apache.poi.xddf.usermodel.chart.XDDFChartLegend;
import org.apache.poi.xddf.usermodel.chart.XDDFDataSource;
import org.apache.poi.xddf.usermodel.chart.XDDFDataSourcesFactory;
import org.apache.poi.xddf.usermodel.chart.XDDFNumericalDataSource;
import org.apache.poi.xssf.usermodel.XSSFChart;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.util.*;
import java.text.*;

public class ApachePoiPieChartCS {
	static HashSet retired_concepts = null;
//1,1-Dimethylhydrazine|C1072|Contributing_Source|FDA
	static {
		retired_concepts = new HashSet();
		Vector w = Utils.readFile("Retired_Concept.txt");
		for (int i=0; i<w.size(); i++) {
			String line = (String) w.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			String code = (String) u.elementAt(0);
			retired_concepts.add(code);
		}
	}

//1,1-Dimethylhydrazine|C1072|Contributing_Source|FDA
	public static Vector removedRetired(Vector w) {
		Vector v = new Vector();
		for (int i=0; i<w.size(); i++) {
			String line = (String) w.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			String code = (String) u.elementAt(1);
			if (!retired_concepts.contains(code)) {
				v.add(line);
			}
		}
		return v;
	}

	public static void generatePieChart(String version) {
		Vector w = Utils.readFile("cs_data.txt");
		generatePieChart(version, w);
	}

	public static void generatePieChart(String version, String datafile) {
		//String version = args[0];
		System.out.println("version: " + version);
		Vector w = Utils.readFile("cs_data.txt");
		generatePieChart(version, w);
	}

	public static void generatePieChart(String version, Vector w) {
		System.out.println("version: " + version);
		w = removedRetired(w);
		HashMap hmap = new HashMap();
		HashMap concept_hmap = new HashMap();
		HashMap concept_count_hmap = new HashMap();
		for (int i=0; i<w.size(); i++) {
		    String line = (String) w.elementAt(i);
		    Vector u = StringUtils.parseData(line, '|');
		    //1, 25-Dihydroxyvitamin D Measurement|C92267|Contributing_Source|CDISC
		    String src = (String) u.elementAt(3);
		    String code = (String) u.elementAt(1);
		    Integer int_obj = new Integer(0);
		    if (hmap.containsKey(src)) {
				int_obj = (Integer) hmap.get(src);
			}
			hmap.put(src, new Integer(Integer.valueOf(int_obj) + 1));
		}

		Iterator it = hmap.keySet().iterator();
		Vector src_vec = new Vector();
		Vector count_vec = new Vector();
		while (it.hasNext()) {
			String src = (String) it.next();
			src_vec.add(src);
		}
		src_vec = new SortUtils().quickSort(src_vec);
		for (int i=0; i<src_vec.size(); i++) {
			String src = (String) src_vec.elementAt(i);
			Integer int_obj = (Integer) hmap.get(src);
			count_vec.add(int_obj);
		}
		try {
			pieChart(version, src_vec, count_vec);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public static String formatVersion(String version) {
		String year = version.substring(0, 2);
		String month = version.substring(3, 5);
		year = "20" + year;
		if (month.charAt(0) == '0') {
			month = "" + month.charAt(1);
		}
		int month_number = Integer.parseInt(month);
		String month_name = new DateFormatSymbols().getMonths()[month_number-1];
		String s = month_name + " " + year;
		return s;
	}

	public static void pieChart(String version, Vector src_vec, Vector count_vec) throws FileNotFoundException, IOException {
		String month_year = formatVersion(version);
		System.out.println(month_year);
		try (XSSFWorkbook wb = new XSSFWorkbook()) {
			//XSSFSheet sheet = wb.createSheet("NCIt Concept Count By Contributing Sources");
			//XSSFSheet sheet = wb.createSheet("NCIt Concept Count By Contributing Sources");
			XSSFSheet sheet = wb.createSheet("NCIt Concept Count By Sources");

            Row row = sheet.createRow((short) 0);
            Cell cell = row.createCell((short) 0);
            cell.setCellValue("Contributing Source");
            cell = row.createCell((short) 1);
            cell.setCellValue("Concept Count");
			for (int i=0; i<src_vec.size(); i++) {
				int j = i+1;
				row = sheet.createRow((short) j);
				cell = row.createCell((short) 0);
				String src = (String) src_vec.elementAt(i);
				cell.setCellValue(src);
				cell = row.createCell((short) 1);
				int count = (Integer) count_vec.elementAt(i);
				cell.setCellValue(Integer.valueOf(count));
			}

			XSSFDrawing drawing = sheet.createDrawingPatriarch();

			XSSFClientAnchor anchor = drawing.createAnchor(0, 0, 0, 0, 0, src_vec.size()+1, 20, src_vec.size()+100);

			XSSFChart chart = drawing.createChart(anchor);
			chart.setTitleText("NCIt Concepts from various contributing sources (" + version + " " + month_year +")");
			chart.setTitleOverlay(false);

			XDDFChartLegend legend = chart.getOrAddLegend();
			//legend.setPosition(LegendPosition.TOP_RIGHT);
			legend.setPosition(LegendPosition.BOTTOM);

			XDDFDataSource<String> sources = XDDFDataSourcesFactory.fromStringCellRange(sheet,
					new CellRangeAddress(1, src_vec.size(), 0, 0));

			XDDFNumericalDataSource<Double> values = XDDFDataSourcesFactory.fromNumericCellRange(sheet,
					new CellRangeAddress(1, count_vec.size(), 1, 1));

			//XDDFChartData data = chart.createData(ChartTypes.PIE3D, null, null);
			XDDFChartData data = chart.createData(ChartTypes.PIE, null, null);

			data.setVaryColors(true);
			data.addSeries(sources, values);
			chart.plot(data);
            String exceloutputfile = "NCIT_Concept_Stats_By_Contributing_Source.xlsx";
			try (FileOutputStream fileOut = new FileOutputStream(exceloutputfile)) {
				wb.write(fileOut);
				fileOut.close();
				wb.close();
				System.out.println(exceloutputfile + " generated.");
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}


}

