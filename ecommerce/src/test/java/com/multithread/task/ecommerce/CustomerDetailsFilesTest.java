package com.multithread.task.ecommerce;

import static org.junit.jupiter.api.Assertions.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import com.multithread.task.ecommerceV01.CustomerDetailsFiles;

class CustomerDetailsFilesTest  {

	  @Test
	    public void testRun_ExtractsDataCorrectly() throws IOException {
	        // Prepare test data
	        String testContent = "Alice bought a laptop with ID L123, quantity 1, price $999.99, total $999.99, delivered on 2024-07-10. Delivery Status: delivered. She also added a smartwatch to her wishlist.";
	        File tempFile = File.createTempFile("test", ".txt");
	        try (BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {
	            writer.write(testContent);
	        }

	        // Define the regex pattern to match the test content
	        String customerRegex = "(\\w+) (?:bought a|purchased a|bought a pair of|purchased a pair of) (\\w+) with ID (\\w+), quantity (\\d+), price \\$([\\d.]+)( each)?, total \\$([\\d.]+)( each)?, delivered on (\\d{4}-\\d{2}-\\d{2})\\. Delivery Status: ([\\w, ]+)\\. .*?added (.+?) to (his|her) wishlist\\.";
	        Pattern pattern = Pattern.compile(customerRegex);
	        // Prepare a shared list to store CSV data
	        List<String[]> csvData = new ArrayList<>();

	        // Create the CustomerDetailsFiles instance
	        CustomerDetailsFiles customerDetailsFiles = new CustomerDetailsFiles(tempFile, pattern, csvData);

	        // Run the processing in a separate thread
	        Thread thread = new Thread(customerDetailsFiles);
	        thread.start();
	        try {
	            thread.join(); // Wait for the thread to complete
	        } catch (InterruptedException e) {
	            e.printStackTrace();
	        }

	        // Debug: Print the CSV data list to verify contents
	        System.out.println("CSV Data List: ");
	        for (String[] row : csvData) {
	            System.out.println(String.join(", ", row));
	        }

	        // Verify that the data was extracted correctly
	        assertEquals(1, csvData.size(), "The CSV data list should contain exactly one entry");
	        String[] rowData = csvData.get(0);
	    //    assertEquals("test", rowData[0], "Parent directory name");
	        assertEquals(tempFile.getName(), rowData[1], "File name");
	        assertEquals("Alice", rowData[2], "Customer Name");
	        assertEquals("laptop", rowData[3], "Product Name");
	        assertEquals("L123", rowData[4], "Product ID");
	        assertEquals("1", rowData[5], "Quantity");
	        assertEquals("999.99", rowData[6], "Price");
	        assertEquals("999.99", rowData[7], "Total");
	        assertEquals("2024-07-10", rowData[8], "Delivery Date");
	        assertEquals("delivered", rowData[9], "Delivery Status");
	        assertEquals("smartwatch", rowData[10], "Wishlist Items");

	        // Clean up temporary file
	        tempFile.delete();
	    }

}
