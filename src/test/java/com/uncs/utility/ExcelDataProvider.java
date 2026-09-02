package com.uncs.utility;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;

/**
 * Reads a row out of a workbook under TestData.
 *
 * Row 0 of every sheet is the header, so the first row of real data is row 1, which is
 * the number the feature file passes. Columns are addressed by index in the order the
 * header lists them.
 */
public class ExcelDataProvider {

	XSSFWorkbook wb;

	public ExcelDataProvider(String fileName, String sheetName) {
		this(fileName);
	}

	public ExcelDataProvider(String fileName) {

		if (!fileName.endsWith(".xlsx")) {
			fileName = fileName + ".xlsx";
		}
		File src = new File("./TestData/" + fileName);

		try {
			FileInputStream fis = new FileInputStream(src);

			wb = new XSSFWorkbook(fis);
		} catch (IOException e) {
			System.out.println("Error in reading excel file" + e.getMessage());
		}
	}

	public String getStringData(String sheetName, int row, int col) {

		Cell cell = wb.getSheet(sheetName).getRow(row).getCell(col);

		if (cell == null) {
			return "";
		}

		if (cell.getCellType() == CellType.NUMERIC) {

			double value = cell.getNumericCellValue();

			/*
			 * A phone number or an all digit password typed straight into a cell is stored
			 * as a number, and the plain toString of one arrives as 1.2345678E7. Whole
			 * numbers go through long, and anything with a fraction is printed in plain
			 * notation so a value never reaches the browser in scientific form.
			 */
			if (value == Math.rint(value) && !Double.isInfinite(value)) {
				return String.valueOf((long) value);
			}
			return new BigDecimal(Double.toString(value)).stripTrailingZeros().toPlainString();
		}
		return cell.getStringCellValue();
	}

	public double getNumericData(String sheetName, int row, int col) {
		return wb.getSheet(sheetName).getRow(row).getCell(col).getNumericCellValue();
	}
}
