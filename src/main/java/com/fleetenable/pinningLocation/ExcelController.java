package com.fleetenable.pinningLocation;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class ExcelController {
    @Autowired
    private ExcelService excelService;
    @PostMapping("/upload")
    public ResponseEntity<List<String>> uploadFile(@RequestParam("file") MultipartFile file) throws IOException {
        String tempFilePath = System.getProperty("java.io.tmpdir") + "/" + file.getOriginalFilename();
        file.transferTo(new java.io.File(tempFilePath));
        List<String> addresses = excelService.extractAddressesFromExcel(tempFilePath);

        return ResponseEntity.ok(addresses);
    }
}
