package com.docuscan;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DocuScanApplication {
    public static void main(String[] args) {
        System.out.println("🚀 DocuScan starting...");
        SpringApplication.run(DocuScanApplication.class, args);
        System.out.println("✅ DocuScan is ready at http://localhost:8080");
    }
}
