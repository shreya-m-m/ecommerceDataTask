package com.multithread.task.ecommerceV01;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class ProcessingCustomerRecords {
    // Static fields to store counts
    public static final ConcurrentHashMap<String, Integer> productCounts = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<String, Integer> wishlistProductCounts = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<String, Integer> deliveredProductCounts = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<String, Integer> cancelledProductCounts = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<String, Integer> shippedProductCounts = new ConcurrentHashMap<>();

    public static int deliveredCount = 0;
    public static int cancelledCount = 0;
    public static int shippedCount = 0;
    private static int totalProductCount = 0;
    private static int totalWishlistCount = 0;
    private static int[] wishListCount = {0};
    private static int[] totalCount = {0};
    private static int[] totalProduct = {0};
    
    public static void main(String[] args) {
        // Load configuration settings
        String configFilePath = "config2.txt";
        ConfigSettings configSettings = readConfigSettings(configFilePath);

        if (configSettings == null) {
            System.out.println("Error reading configuration settings.");
            return;
        }

        // Define the regex pattern for extracting customer purchase details
        String customerRegex = "(\\w+) (?:bought a|purchased a|bought a pair of|purchased a pair of) (\\w+) with ID (\\w+), quantity (\\d+), price \\$([\\d.]+)( each)?, total \\$([\\d.]+)( each)?, delivered on (\\d{4}-\\d{2}-\\d{2})\\. Delivery Status: ([\\w, ]+)\\. .*?added (.+?) to (his|her) wishlist\\.";
        Pattern pattern = Pattern.compile(customerRegex);

        // List to store CSV data
        List<String[]> csvData = new ArrayList<>();
        // Get paths from config settings
        String mainDirectoryPath = configSettings.getFolderPath();
        String outputFilePath = configSettings.getOutputPath();
        String excelFilePath = configSettings.getExcelFilePath();
        int folderCount = configSettings.getFolderCount();

        // Get the main directory and list all subdirectories (folders)
        File mainDirectory = new File(mainDirectoryPath);
        File[] folders = mainDirectory.listFiles(File::isDirectory);

        if (folders != null) {
            // Determine the number of threads needed based on the folder count
            int numThreads = (int) Math.ceil((double) folders.length / folderCount);
            ExecutorService executor = Executors.newFixedThreadPool(numThreads);
            long startTime = System.currentTimeMillis();

            // Process each folder and its files
            for (File folder : folders) {
                File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".txt"));

                if (files != null) {
                    for (File file : files) {
                        executor.execute(new CustomerDetailsFiles(file, pattern, csvData));
                    }
                } else {
                    System.out.println("No text files found in folder: " + folder.getName());
                }
            }

            executor.shutdown();
            try {
                executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            long endTime = System.currentTimeMillis();
            long totalTime = endTime - startTime;

            // Print execution summary
            System.out.println("Execution Summary:");
            System.out.println("Start Time: " + formatTime(startTime));
            System.out.println("End Time: " + formatTime(endTime));
            System.out.println("Total Execution Time: " + totalTime + " ms");
            System.out.println("Number of Threads Used: " + numThreads);
            System.out.println();

            // Print customer count summary
            System.out.println("Customer Count Summary:");
            printCustomerCountSummary(csvData);

            // Save the data to Excel file
            saveToExcel(csvData, excelFilePath);
            
        } else {
            System.out.println("No folders found in the directory: " + mainDirectoryPath);
        }
    }

    public static void printCustomerCountSummary(List<String[]> csvData) {
        // Concurrent hash maps to store counts
        ConcurrentHashMap<String, Integer> folderCustomerCounts = new ConcurrentHashMap<>();
        ConcurrentHashMap<String, Integer> fileCustomerCounts = new ConcurrentHashMap<>();

        int[] totalFileCount = {0};

        for (String[] rowData : csvData) {
            String folderName = rowData[0];
            String fileName = rowData[1];
            String productName = rowData[3];
            int quantity = Integer.parseInt(rowData[5]);
            String deliveryStatus = rowData[9];
            String wishlistItems = rowData[10];

            // Update folder and file customer counts
            folderCustomerCounts.put(folderName, folderCustomerCounts.getOrDefault(folderName, 0) + 1);
            fileCustomerCounts.put(fileName, fileCustomerCounts.getOrDefault(fileName, 0) + 1);

            // Update product counts
            productCounts.put(productName, productCounts.getOrDefault(productName, 0) + quantity);

            // Update delivery status counts
            switch (deliveryStatus.toLowerCase()) {
                case "delivered":
                    deliveredCount++;
                    deliveredProductCounts.merge(productName, quantity, Integer::sum);
                    break;
                case "cancelled":
                    cancelledCount++;
                    cancelledProductCounts.merge(productName, quantity, Integer::sum);
                    break;
                case "shipped":
                    shippedCount++;
                    shippedProductCounts.merge(productName, quantity, Integer::sum);
                    break;
            }

            // Process wishlist items if present
            if (wishlistItems != null && !wishlistItems.isEmpty()) {
                String[] wishlistProducts = wishlistItems.split(", ");
                totalWishlistCount += wishlistProducts.length;
                for (String wishlistProduct : wishlistProducts) {
                    wishlistProductCounts.put(wishlistProduct, wishlistProductCounts.getOrDefault(wishlistProduct, 0) + 1);
                }
            }
        }

        // Calculate total counts
        totalProductCount = productCounts.values().stream().mapToInt(Integer::intValue).sum();

        // Print results
        System.out.println("Per Folder Customer Counts:");
        folderCustomerCounts.forEach((folder, count) -> {
            System.out.println(folder + ": " + count);
            totalCount[0] += count;
        });
        System.out.println("Total count of all folders: " + totalCount[0]);
        System.out.println();

        System.out.println("Per File Customer Counts:");
        fileCustomerCounts.forEach((file, count) -> {
            System.out.println(file + ": " + count);
            totalFileCount[0] += count;
        });
        System.out.println("Total count of all files: " + totalFileCount[0]);
        System.out.println();

        System.out.println("Customer Count");
        System.out.println("Delivered Customer Count: " + deliveredCount);
        System.out.println("Cancelled Customer Count: " + cancelledCount);
        System.out.println("Shipped Customer Count: " + shippedCount);
        System.out.println();
        
        System.out.println("Product Count");
        System.out.println("Delivered Product Count: " + deliveredProductCounts);
        System.out.println("Cancelled Product Count: " + cancelledProductCounts);
        System.out.println("Shipped Product Count: " + shippedProductCounts);
        
        System.out.println();
        
       
        
        System.out.println("Product Counts:");
        productCounts.forEach((product, count) -> {
            System.out.println(product + ": " + count);
        
			totalProduct[0]+= count;
			
        });
        System.out.println();
        
        // Print wishlist product counts based on product counts
      //  System.out.println("Wishlist Product Counts:");
        wishlistProductCounts.forEach((product, count) -> {
            if (productCounts.containsKey(product)) {
               // System.out.println(product + ": " + count);
            }
        });
        System.out.println();
        System.out.println("Total Product Count : "+totalProduct[0]);
        System.out.println("Added to Wishlist Product Count: " + totalWishlistCount);
        
       
    }

    public static void saveToExcel(List<String[]> csvData, String excelFilePath) {
        Workbook workbook = new XSSFWorkbook();
        // Create a detailed sheet
        Sheet detailedSheet = workbook.createSheet("Customer Details");
        createHeaderRow(detailedSheet,0, new String[]{"Folder", "File", "Customer Name", "Product Name", "Product ID", "Quantity", "Price", "Total", "Purchased Date", "Delivery Status", "Wishlist Items"});

        for (int i = 0; i < csvData.size(); i++) {
            createDataRow(detailedSheet, i + 1, csvData.get(i));
        }
     // Create a summary sheet with both summary counts and product summary
        Sheet summaryAndProductSheet = workbook.createSheet("Customer and Product Summary");

        // Create Summary Counts section
        createHeaderRow(summaryAndProductSheet,0, new String[]{"Customer Summary"});
        createHeaderRow(summaryAndProductSheet,2, new String[]{"Total Customers", "Delivered Count", "Cancelled Count", "Shipped Count"});
        createDataRow(summaryAndProductSheet, 3, new String[]{
            String.valueOf(totalCount[0]),
            String.valueOf(deliveredCount),
            String.valueOf(cancelledCount),
            String.valueOf(shippedCount)
        });

        // Add a blank row for separation
        int rowIndex = 6; // Next row after summary counts (2 rows)

        // Create Product Summary section
        createHeaderRow(summaryAndProductSheet, rowIndex, new String[]{"Product Summary"});
        rowIndex+=2;
        createHeaderRow(summaryAndProductSheet, rowIndex, new String[]{"Product Name", "Delivered Count", "Cancelled Count", "Shipped Count", "Total Count"});
        rowIndex++; // Move to the next row after headers
        createProductSummaryRows(summaryAndProductSheet, rowIndex);

        // Create a wishlist summary sheet
        Sheet wishlistSummarySheet = workbook.createSheet("Wishlist Product Summary");
        createHeaderRow(wishlistSummarySheet,0, new String[]{"Product Name", "Wishlist Count"});
        createWishlistSummaryRows(wishlistSummarySheet);

        try (FileOutputStream fos = new FileOutputStream(excelFilePath)) {
            workbook.write(fos);
            System.out.println("Data saved to Excel file: " + excelFilePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void createHeaderRow(Sheet sheet, int rowIndex, String[] headers) {
        Row headerRow = sheet.createRow(rowIndex);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
        }
    }

    public static void createDataRow(Sheet sheet, int rowIndex, String[] data) {
        Row row = sheet.createRow(rowIndex);
        for (int i = 0; i < data.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(data[i]);
        }
    }

    public static void createProductSummaryRows(Sheet sheet, int startRowIndex) {
        int rowIndex = startRowIndex;
        for (String productName : productCounts.keySet()) {
            int deliveredCount = deliveredProductCounts.getOrDefault(productName, 0);
            int cancelledCount = cancelledProductCounts.getOrDefault(productName, 0);
            int shippedCount = shippedProductCounts.getOrDefault(productName, 0);
            int totalCount = deliveredCount + cancelledCount + shippedCount;

            String[] data = {productName, String.valueOf(deliveredCount), String.valueOf(cancelledCount), String.valueOf(shippedCount), String.valueOf(totalCount)};
            createDataRow(sheet, rowIndex++, data);
        }
    }

    public static void createWishlistSummaryRows(Sheet sheet) {
        int rowIndex = 1;
        for (String productName : wishlistProductCounts.keySet()) {
            int wishlistCount = wishlistProductCounts.get(productName);
            String[] data = {productName, String.valueOf(wishlistCount)};
            createDataRow(sheet, rowIndex++, data);
           
            
        }
    }

    public static String formatTime(long millis) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(new Date(millis));
    }

    public static ConfigSettings readConfigSettings(String filePath) {
        Properties properties = new Properties();
        try (FileInputStream fis = new FileInputStream(filePath)) {
            properties.load(fis);
            return new ConfigSettings(
                    properties.getProperty("folderPath"),
                    properties.getProperty("outputPath"),
                    properties.getProperty("excelFilePath"),
                    Integer.parseInt(properties.getProperty("folderCount"))
            );
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}


//private static void saveToCSV(List<String[]> csvData, String outputFilePath) {
//try (PrintWriter writer = new PrintWriter(new FileWriter(outputFilePath))) {
//  writer.println("Folder,File,Customer Name,Product Name,Product ID,Quantity,Price,Total,Delivery Date,Delivery Status,Wishlist Items");
//
//  for (String[] rowData : csvData) {
//      writer.println(String.join(",", rowData));
//  }
//
//  System.out.println("CSV file saved: " + outputFilePath);
//} catch (IOException e) {
//  System.err.println("Error writing CSV file: " + outputFilePath);
//  e.printStackTrace();
//}
//}


