package com.codemayur.fileutils.controller;

import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.codemayur.fileutils.exception.StorageFileNotFoundException;
import com.codemayur.fileutils.service.StorageService;

@Controller
public class FileUploadController {

	private final StorageService storageService;

	@Autowired
	public FileUploadController(StorageService storageService) {
		this.storageService = storageService;
	}

	@GetMapping("/")
	public String listUploadedFiles(Model model) {

		model.addAttribute("files",
				storageService.loadAll()
						.map(path -> MvcUriComponentsBuilder.fromMethodName(FileUploadController.class,
								"serveFile",
								path.getFileName()
										.toString())
								.build()
								.toUri()
								.toString())
						.collect(Collectors.toList()));

		return "uploadForm";
	}

	@GetMapping("/files/{filename:.+}")
	@ResponseBody
	public ResponseEntity<Resource> serveFile(@PathVariable String filename) {

		Resource file = storageService.loadAsResource(filename);
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION,
						"attachment; filename=\"" + file.getFilename() + "\"")
				.body(file);
	}

	@PostMapping("/")
	public String handleFileUpload(@RequestParam("file") MultipartFile file,
			RedirectAttributes redirectAttributes) {

		storageService.store(file);

		// ASPOSE & OCR CHECK HERE!!!
		Map<String, Object> map = storageService.validateFile(file);

		redirectAttributes.addFlashAttribute("fileType",
				map.get("fileType"));

		if ("PDF".equals(map.get("fileType"))) {

			redirectAttributes.addFlashAttribute("height",
					map.get("height"));
			redirectAttributes.addFlashAttribute("width",
					map.get("width"));
		}

		redirectAttributes.addFlashAttribute("message",
				String.format("You successfully uploaded %s.",
						file.getOriginalFilename()));

		return "redirect:/";
	}

	@ExceptionHandler(StorageFileNotFoundException.class)
	public ResponseEntity<Object> handleStorageFileNotFound(StorageFileNotFoundException exc) {
		return ResponseEntity.notFound()
				.build();
	}

}
