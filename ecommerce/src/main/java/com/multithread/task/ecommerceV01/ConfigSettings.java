package com.multithread.task.ecommerceV01;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class ConfigSettings {
	private String folderPath;
    private String outputPath;
    private String excelFilePath;
    private int folderCount;

    public ConfigSettings(String folderPath, String outputPath, String excelFilePath, int folderCount) {
        this.folderPath = folderPath;
        this.outputPath = outputPath;
        this.excelFilePath = excelFilePath;
        this.folderCount = folderCount;
    }

    public String getFolderPath() {
        return folderPath;
    }

    public String getOutputPath() {
        return outputPath;
    }

    public String getExcelFilePath() {
        return excelFilePath;
    }

    public int getFolderCount() {
        return folderCount;
    }
}