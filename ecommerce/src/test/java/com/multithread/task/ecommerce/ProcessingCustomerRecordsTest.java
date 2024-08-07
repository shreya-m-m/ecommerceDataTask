package com.multithread.task.ecommerce;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.multithread.task.ecommerceV01.ConfigSettings;
import com.multithread.task.ecommerceV01.ProcessingCustomerRecords;

class ProcessingCustomerRecordsTest {

	  @TempDir
	    Path tempDir;


	  @Test
	    public void testReadConfigSettings() {
	        String configFilePath = "config2.txt";
	        // Create a temporary config file for testing
	        try (PrintWriter writer = new PrintWriter(configFilePath)) {
	            writer.println("folderPath=./data");
	            writer.println("outputPath=./output.csv");
	            writer.println("excelFilePath=./output.xlsx");
	            writer.println("folderCount=2");
	        } catch (IOException e) {
	            fail("Failed to create temporary config file.");
	        }

	        ConfigSettings configSettings = ProcessingCustomerRecords.readConfigSettings(configFilePath);

	        assertNotNull(configSettings);
	        assertEquals("./data", configSettings.getFolderPath());
	        assertEquals("./output.csv", configSettings.getOutputPath());
	        assertEquals("./output.xlsx", configSettings.getExcelFilePath());
	        assertEquals(2, configSettings.getFolderCount());

	        // Delete the temporary config file
	        new File(configFilePath).delete();
	    }

	    @Test
	    public void testMultithreadingExecution() throws InterruptedException {
	        int numThreads = 4;
	        ExecutorService executor = Executors.newFixedThreadPool(numThreads);

	        for (int i = 0; i < 10; i++) {
	            executor.execute(() -> {
	                try {
	                    Thread.sleep(100); // Simulate some work
	                } catch (InterruptedException e) {
	                    Thread.currentThread().interrupt();
	                }
	            });
	        }

	        executor.shutdown();
	        boolean finished = executor.awaitTermination(1, TimeUnit.MINUTES);

	        assertTrue(finished, "All threads should finish within the timeout period");
	    }

  @Test
  public void testPatternMatching() {
      String customerRegex = "(\\w+) (?:bought a|purchased a|bought a pair of|purchased a pair of) (\\w+) with ID (\\w+), quantity (\\d+), price \\$([\\d.]+)( each)?, total \\$([\\d.]+)( each)?, delivered on (\\d{4}-\\d{2}-\\d{2})\\. Delivery Status: ([\\w, ]+)\\. .*?added (.+?) to (his|her) wishlist\\.";
      Pattern pattern = Pattern.compile(customerRegex);

      String sampleText = "Alice purchased a tablet with ID T456, quantity 2, price $499.99 each, total $999.98 each, delivered on 2024-07-12. Delivery Status: shipped. She also added a pair of headphones and a smartphone to her wishlist.";

      Matcher matcher = pattern.matcher(sampleText);
      assertTrue(matcher.find(), "Pattern should match the sample text");
      assertEquals("Alice", matcher.group(1), "Customer name should match");
      assertEquals("tablet", matcher.group(2), "Product name should match");
      assertEquals("T456", matcher.group(3), "Product ID should match");
      assertEquals("2", matcher.group(4), "Quantity should match");
      assertEquals("499.99", matcher.group(5), "Price should match");
      assertEquals("999.98", matcher.group(7), "Total amount should match");
      assertEquals("2024-07-12", matcher.group(9), "Delivery date should match");
      assertEquals("shipped", matcher.group(10), "Delivery status should match");
      assertEquals("a pair of headphones and a smartphone", matcher.group(11), "Wishlist items should match");
  }


  @Test
  public void testProcessCustomerCounts() {
      List<String[]> csvData = new ArrayList<>();
      csvData.add(new String[]{"folder1", "file1.txt", "Bob", "laptop", "L123", "1", "999.99", "999.99", "2024-07-10", "delivered", "smartwatch"});
      csvData.add(new String[]{"folder1", "file1.txt", "Alice", "tablet", "T456", "2", "499.99", "999.98", "2024-07-12", "shipped", "headphones and smartphone"});
      csvData.add(new String[]{"folder1", "file2.txt", "Charlie", "smartphone", "S789", "3", "299.99", "899.97", "2024-07-15", "delivered", "laptop, smartwatch"});

      ProcessingCustomerRecords.printCustomerCountSummary(csvData);

      assertEquals(2, ProcessingCustomerRecords.deliveredCount);
      assertEquals(1, ProcessingCustomerRecords.shippedCount);
      assertEquals(0, ProcessingCustomerRecords.cancelledCount);

      assertEquals(1, ProcessingCustomerRecords.deliveredProductCounts.get("laptop").intValue());
      assertEquals(3, ProcessingCustomerRecords.deliveredProductCounts.get("smartphone").intValue());

      assertEquals(2, ProcessingCustomerRecords.shippedProductCounts.get("tablet").intValue());
  }

  @Test
  void testCreateHeaderRow() throws IOException {
      Workbook workbook = new XSSFWorkbook();
      Sheet sheet = workbook.createSheet("Test Sheet");
      String[] headers = {"Header1", "Header2"};
      ProcessingCustomerRecords.createHeaderRow(sheet, 0, headers);

      Row headerRow = sheet.getRow(0);
      assertNotNull(headerRow);
      assertEquals(headers.length, headerRow.getLastCellNum());
      for (int i = 0; i < headers.length; i++) {
          assertEquals(headers[i], headerRow.getCell(i).getStringCellValue());
      }
      try (FileOutputStream fos = new FileOutputStream(tempDir.resolve("test.xlsx").toFile())) {
          workbook.write(fos);
      } finally {
          workbook.close();
      }
  }
  @Test
  void testCreateDataRow() throws IOException {
      Workbook workbook = new XSSFWorkbook();
      Sheet sheet = workbook.createSheet("Test Sheet");
      String[] rowData = {"Data1", "Data2"};
      ProcessingCustomerRecords.createDataRow(sheet, 0, rowData);

      Row row = sheet.getRow(0);
      assertNotNull(row);
      assertEquals(rowData.length, row.getLastCellNum());
      for (int i = 0; i < rowData.length; i++) {
          assertEquals(rowData[i], row.getCell(i).getStringCellValue());
      }

      try (FileOutputStream fos = new FileOutputStream(tempDir.resolve("test.xlsx").toFile())) {
          workbook.write(fos);
      } finally {
          workbook.close();
      }
  }

  @Test
  void testCreateProductSummaryRows() throws IOException {
      ProcessingCustomerRecords.productCounts.put("Tablet", 2);
      ProcessingCustomerRecords.deliveredProductCounts.put("tablet", 1);
      ProcessingCustomerRecords.cancelledProductCounts.put("tablet", 0);
      ProcessingCustomerRecords.shippedProductCounts.put("tablet", 1);

      Workbook workbook = new XSSFWorkbook();
      Sheet sheet = workbook.createSheet("Test Sheet");
      ProcessingCustomerRecords.createProductSummaryRows(sheet, 0);

      Row row = sheet.getRow(0);
      assertNotNull(row);
      assertEquals("tablet", row.getCell(0).getStringCellValue());
      assertEquals("1", row.getCell(1).getStringCellValue());
      assertEquals("0", row.getCell(2).getStringCellValue());
      assertEquals("1", row.getCell(3).getStringCellValue());
      assertEquals("2", row.getCell(4).getStringCellValue());

      try (FileOutputStream fos = new FileOutputStream(tempDir.resolve("test.xlsx").toFile())) {
          workbook.write(fos);
      } finally {
          workbook.close();
      }
  }

  @Test
  void testCreateWishlistSummaryRows() throws IOException {
      ProcessingCustomerRecords.wishlistProductCounts.put("headphones and smartphone", 1);
   //   ProcessingCustomerRecords.wishlistProductCounts.put("smartphone", 1);

      Workbook workbook = new XSSFWorkbook();
      Sheet sheet = workbook.createSheet("Test Sheet");
      ProcessingCustomerRecords.createWishlistSummaryRows(sheet);

      Row row1 = sheet.getRow(2);
      assertNotNull(row1);
      assertEquals("headphones and smartphone", row1.getCell(0).getStringCellValue());
      assertEquals("1", row1.getCell(1).getStringCellValue());


      try (FileOutputStream fos = new FileOutputStream(tempDir.resolve("test.xlsx").toFile())) {
          workbook.write(fos);
      } finally {
          workbook.close();
      }
  }
      
  @Test
  public void testSaveToExcel() {
      // Sample data to save
      List<String[]> csvData = new ArrayList<>();
      csvData.add(new String[]{"Folder1", "File1", "Alice", "tablet", "T456", "2", "499.99", "999.98", "2024-07-12", "shipped", "a pair of headphones and a smartphone"});
      csvData.add(new String[]{"Folder2", "File2", "Bob", "laptop", "L123", "1", "999.99", "999.99", "2024-07-10", "delivered", "a smartwatch"});

      String excelFilePath = "test_output.xlsx"; // Path for testing

      // Save data to Excel
      ProcessingCustomerRecords.saveToExcel(csvData, excelFilePath);

      // Verify the Excel file
      File excelFile = new File(excelFilePath);
      assertTrue(excelFile.exists(), "Excel file should exist after saving");

      try (FileInputStream fis = new FileInputStream(excelFile)) {
          Workbook workbook = new XSSFWorkbook(fis);

          // Verify the detailed sheet
          Sheet detailedSheet = workbook.getSheet("Customer Details");
          assertNotNull(detailedSheet, "Customer Details sheet should exist");
          assertEquals(3, detailedSheet.getPhysicalNumberOfRows(), "Customer Details sheet should have 3 rows (1 header + 2 data)");

          // Verify the header row in the detailed sheet
          Row headerRow = detailedSheet.getRow(0);
          assertNotNull(headerRow, "Header row should exist in the Customer Details sheet");
          assertEquals("Customer Name", headerRow.getCell(2).getStringCellValue(), "Third column header should be 'Customer Name'");

          // Verify the data rows in the detailed sheet
          Row dataRow = detailedSheet.getRow(1);
          assertNotNull(dataRow, "First data row should exist in the Customer Details sheet");
          assertEquals("Alice", dataRow.getCell(2).getStringCellValue(), "First data row, third column should be 'Alice'");

          dataRow = detailedSheet.getRow(2);
          assertNotNull(dataRow, "Second data row should exist in the Customer Details sheet");
          assertEquals("Bob", dataRow.getCell(2).getStringCellValue(), "Second data row, third column should be 'Bob'");

          // Verify the summary and product sheet
          Sheet summaryAndProductSheet = workbook.getSheet("Customer and Product Summary");
          assertNotNull(summaryAndProductSheet, "Customer and Product Summary sheet should exist");
          assertEquals("Customer Summary", summaryAndProductSheet.getRow(0).getCell(0).getStringCellValue(), "Summary sheet should have 'Customer Summary' in the first cell");

          // Verify the wishlist summary sheet
          Sheet wishlistSummarySheet = workbook.getSheet("Wishlist Product Summary");
          assertNotNull(wishlistSummarySheet, "Wishlist Product Summary sheet should exist");
          assertEquals("Product Name", wishlistSummarySheet.getRow(0).getCell(0).getStringCellValue(), "Wishlist sheet should have 'Product Name' in the first cell");

      } catch (IOException e) {
          e.printStackTrace();
          fail("Failed to read the Excel file");
      } finally {
          // Clean up test file
          if (excelFile.exists()) {
              assertTrue(excelFile.delete(), "Test Excel file should be deleted after test");
          }
      }
  }
  
  @Test
  void testFormatTime() {
      long currentTime = System.currentTimeMillis();
      String formattedTime = ProcessingCustomerRecords.formatTime(currentTime);
      SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
      assertEquals(sdf.format(new Date(currentTime)), formattedTime);
  }
}
