package com.codemayur.fileutils;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import com.codemayur.fileutils.exception.StorageException;
import com.codemayur.fileutils.properties.StorageProperties;
import com.codemayur.fileutils.service.StorageService;

@SpringBootApplication
@EnableConfigurationProperties(StorageProperties.class)
public class FileUtilsApplication {

	static {
		com.aspose.pdf.License license = new com.aspose.pdf.License();
		try {
			// specify the path of license file
			license.setLicense(FileUtilsApplication.class.getClassLoader()
					.getResourceAsStream("Aspose.Total.Java.lic"));
		} catch (Exception e) {
			throw new StorageException("Couldn't activate Aspose License", e);
		}
	}

	public static void main(String[] args) {
		SpringApplication.run(FileUtilsApplication.class,
				args);
	}

	@Bean
	CommandLineRunner init(StorageService storageService) {
		return (args) -> {
			storageService.deleteAll();
			storageService.init();
		};
	}

}
