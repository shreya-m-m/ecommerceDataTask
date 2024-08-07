package com.multithread.task.ecommerceV01;

import java.io.*;
import java.util.List;
import java.util.regex.*;
import java.util.concurrent.*;

public class CustomerDetailsFiles implements Runnable {
    private final File file; // File to process
    private final Pattern pattern; // Regex pattern for matching data
    private final List<String[]> csvData; // Shared list to store extracted data

    public CustomerDetailsFiles(File file, Pattern pattern, List<String[]> csvData) {
        this.file = file;
        this.pattern = pattern;
        this.csvData = csvData;
    }

    @Override
    public void run() {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    String customerName = matcher.group(1);
                    String productName = matcher.group(2);
                    String productId = matcher.group(3);
                    int quantity = Integer.parseInt(matcher.group(4));
                    double price = Double.parseDouble(matcher.group(5));
                    double total = Double.parseDouble(matcher.group(7));
                    String deliveryDate = matcher.group(9);
                    String deliveryStatus = matcher.group(10);
                    String wishlistItems = matcher.group(11);

                    wishlistItems = wishlistItems.replaceAll("(a pair of |a )", "");

                    String[] rowData = {
                        file.getParentFile().getName(),
                        file.getName(),
                        customerName,
                        productName,
                        productId,
                        String.valueOf(quantity),
                        String.valueOf(price),
                        String.valueOf(total),
                        deliveryDate,
                        deliveryStatus,
                        wishlistItems
                    };
                    synchronized (csvData) {
                        csvData.add(rowData);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading file: " + file.getPath());
            e.printStackTrace();
        }
    }
}
