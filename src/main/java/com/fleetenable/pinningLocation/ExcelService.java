package com.fleetenable.pinningLocation;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

@Service
public class ExcelService {
    public List<String> extractAddressesFromExcel(String filePath) throws IOException {
        List<String> addresses = new ArrayList<>();

        try (FileInputStream fis = new FileInputStream(filePath); Workbook workbook = new XSSFWorkbook(fis)) {
            Sheet sheet = workbook.getSheetAt(0);

            for (Row row : sheet) {
                Cell cell = row.getCell(0);
                if (cell != null) {
                    String address = cell.getStringCellValue();
                    address = cleanAddress(address);
                    if (!address.isEmpty()) {
                        addresses.add(address);
                    }
                }
            }
        }

        return addresses;
    }

    private String cleanAddress(String address) {
        if (address == null) {
            return "";
        }

        address = address.replaceAll("\\s+", " ").trim();
        address = address.replaceAll(",\\s*,", ",").replaceAll(",\\s*$", "");
        if (address.trim().isEmpty()) {
            return "";
        }

        return address;
    }
}
