package com.codemayur.fileutils.service.impl;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.aspose.pdf.Document;
import com.aspose.pdf.Page;
import com.aspose.pdf.PageCollection;
import com.aspose.pdf.XImage;
import com.aspose.pdf.XImageCollection;
import com.codemayur.fileutils.constant.FileUploadConstants;
import com.codemayur.fileutils.exception.StorageException;
import com.codemayur.fileutils.exception.StorageFileNotFoundException;
import com.codemayur.fileutils.properties.StorageProperties;
import com.codemayur.fileutils.service.StorageService;
import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;

@Service
public class FileSystemStorageService implements StorageService {

	private final Path rootLocation;

	@Autowired
	public FileSystemStorageService(StorageProperties properties) {
		this.rootLocation = Paths.get(properties.getLocation());
	}

	@Override
	public void init() {
		try {
			Files.createDirectories(rootLocation);
		} catch (IOException e) {
			throw new StorageException("Could not initialize storage",
					e);
		}
	}

	@Override
	public void store(MultipartFile file) {
		try {
			if (file.isEmpty()) {
				throw new StorageException("Failed to store empty file.");
			}
			Path destinationFile = this.rootLocation.resolve(Paths.get(file.getOriginalFilename()))
					.normalize()
					.toAbsolutePath();
			if (!destinationFile.getParent()
					.equals(this.rootLocation.toAbsolutePath())) {
				// This is a security check
				throw new StorageException("Cannot store file outside current directory.");
			}
			try (InputStream inputStream = file.getInputStream()) {
				Files.copy(inputStream,
						destinationFile,
						StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (IOException e) {
			throw new StorageException("Failed to store file.",
					e);
		}
	}

	@Override
	public Stream<Path> loadAll() {
		try {
			return Files.walk(this.rootLocation,
					1)
					.filter(path -> !path.equals(this.rootLocation))
					.map(this.rootLocation::relativize);
		} catch (IOException e) {
			throw new StorageException("Failed to read stored files",
					e);
		}

	}

	@Override
	public Path load(String filename) {
		return rootLocation.resolve(filename);
	}

	@Override
	public Resource loadAsResource(String filename) {
		try {
			Path file = load(filename);
			Resource resource = new UrlResource(file.toUri());
			if (resource.exists() || resource.isReadable()) {
				return resource;
			} else {
				throw new StorageFileNotFoundException("Could not read file: " + filename);

			}
		} catch (MalformedURLException e) {
			throw new StorageFileNotFoundException("Could not read file: " + filename,
					e);
		}
	}

	@Override
	public void deleteAll() {
		FileSystemUtils.deleteRecursively(rootLocation.toFile());
	}

	/**
	 * @author mayur.somani
	 */
	@Override
	public Map<String, Object> validateFile(MultipartFile file) {

		Map<String, Object> returnMap = new HashMap<>();

		if (file != null && !file.isEmpty()) {

			String mimeType = getMimeTypeOfFile(file);

			// *.DOC
			if (FileUploadConstants.MIME_APPLICATION_MSWORD_DOC.equals(mimeType)) {
				returnMap.put("fileType",
						"DOC");
			}
			// *.DOCX
			else if (FileUploadConstants.MIME_APPLICATION_MSWORD_DOCX.equals(mimeType)) {
				returnMap.put("fileType",
						"DOCX");
			}
			// *.PDF
			else if (FileUploadConstants.MIME_APPLICATION_PDF.equals(mimeType)) {
				returnMap.put("fileType",
						"PDF");
				validatePdfFile(file);
			} else {
				throw new StorageException("Invalid File Type");
			}

		} else {
			throw new NullPointerException("File is null or empty.");
		}
		return returnMap;
	}

	private String getMimeTypeOfFile(MultipartFile file) {
		Path path = new File(file.getOriginalFilename()).toPath();
		String mimeType = "";
		try {
			mimeType = Files.probeContentType(path);
		} catch (IOException e) {
			throw new StorageException("Couldn't get Mime type of the file",
					e);
		}
		return mimeType;
	}

	@SuppressWarnings("unchecked")
	private void validatePdfFile(MultipartFile file) {

		InputStream inputStream = null;
		java.io.OutputStream output = null;
		Document pdfDocument = null;

		try {

			inputStream = file.getInputStream();

			pdfDocument = new Document(inputStream);
			System.out.println(pdfDocument);

			PageCollection pages = pdfDocument.getPages();
			Iterator<Page> pageIterator = pages.iterator();

			int i = 1;
			while (pageIterator.hasNext()) {

				Page page = pageIterator.next();
				System.out.println("Page " + i);

				// System.out.println(String.format("Height: %s Width: %s Top: %s Left: %s
				// Right: %s Bottom: %s",
				// page.getPageInfo()
				// .getHeight(),
				// page.getPageInfo()
				// .getWidth(),
				// page.getPageInfo()
				// .getMargin()
				// .getTop(),
				// page.getPageInfo()
				// .getMargin()
				// .getLeft(),
				// page.getPageInfo()
				// .getMargin()
				// .getRight(),
				// page.getPageInfo()
				// .getMargin()
				// .getBottom()));
				//
				// Document doc = new Document();
				// doc.getPages().add(page);
				// doc.save("temp_" + i++ + ".pdf");

				XImageCollection xImageCollection = page.getResources()
						.getImages();
				Iterator<XImage> imageIterator = xImageCollection.iterator();

				int j = 1;
				while (imageIterator.hasNext()) {

					XImage xImage = imageIterator.next();

					System.out.println(String.format("Image %s, height: %s, width: %s",
							j,
							xImage.getHeight(),
							xImage.getWidth()));

					String imagename = null;
					if (StringUtils.hasText(file.getOriginalFilename()) && 10 < file.getOriginalFilename()
							.length()) {
						imagename = rootLocation + "/" + file.getOriginalFilename()
								.substring(0,
										10)
								+ "_" + i + "_" + j + ".jpeg";
					} else if (file.getOriginalFilename() != null) {
						imagename = rootLocation + "/" + file.getOriginalFilename() + "_" + i + "_" + j + ".jpeg";
					} else {
						imagename = rootLocation + "/" + UUID.randomUUID() + "_" + i + "_" + j + ".jpeg";
					}
					output = new java.io.FileOutputStream(imagename);
					xImage.save(output);
					output.close();
					
					inputStream = file.getInputStream();
					ByteArrayOutputStream bos = new ByteArrayOutputStream();
		            byte[] buf = new byte[1024];
		            try {
		                for (int readNum; (readNum = inputStream.read(buf)) != -1;) {
		                    bos.write(buf, 0, readNum); //no doubt here is 0
		                    //Writes len bytes from the specified byte array starting at offset off to this byte array output stream.
		                    // System.out.println("read " + readNum + " bytes,");
		                }
		            } catch (IOException e) {
		                throw new StorageException("IOException at ByteArrayOutputStream", e);
		            }
		            byte[] bytes = bos.toByteArray();

					ImageData image = ImageDataFactory.create(bytes);
					int x = image.getDpiX();
					int y = image.getDpiY();

					System.out.println(String.format("X: %s Y: %s",
							x,
							y));

					j++;
				}
				i++;
			}
			inputStream.close();
		} catch (IOException e) {
			throw new StorageException("Couldn't get file's inputstream",
					e);
		} catch (Exception e) {
			throw new StorageException("Couldn't parse the uploaded document",
					e);
		} finally {
			try {
				if (output != null) {
					output.close();
				}
				if (inputStream != null) {
					inputStream.close();
				}
			} catch (Exception e) {
				// do nothing
			}
		}
	}

}
