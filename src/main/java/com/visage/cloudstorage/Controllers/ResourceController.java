package com.visage.cloudstorage.Controllers;

import com.visage.cloudstorage.Exceptions.NotCorrectNameFileOrPackage;
import com.visage.cloudstorage.Exceptions.ParentFileIsAlreadyExists;
import com.visage.cloudstorage.Model.ErrorResponse;
import com.visage.cloudstorage.Model.FileResource;
import com.visage.cloudstorage.Exceptions.DataNotFoundException;
import com.visage.cloudstorage.Model.User;
import com.visage.cloudstorage.Services.ResourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ResourceController {

    private final ResourceService resourceService;


    @GetMapping("/directory")
    public ResponseEntity<?> lookAllResource(@RequestParam("path") String path, @AuthenticationPrincipal User user) {
        if (path.equals(".") || path.equals("..") || path.contains("../") || path.contains("..\\")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponse.builder().message("Невалидный или отсутствующий путь").status(400).build());
        }
        try {
            return ResponseEntity.ok().body(resourceService.resources(user.getId(), path));
        } catch (DataNotFoundException e) {
            throw new DataNotFoundException("Ресурс не найден");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ErrorResponse.builder().message("Неизвестная ошибка").status(500).build());
        }
    }


    @PostMapping("/directory")
    public ResponseEntity<?> createPackage(@RequestParam("path") String path, @AuthenticationPrincipal User user) {
        if (!path.isEmpty()) {
            if (!path.endsWith("/")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponse.builder().message("Невалидный или отсутствующий путь").status(400).build());
            }
        }
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(resourceService.createPackage(path, user.getId()));
        } catch (NotCorrectNameFileOrPackage e) {
            throw new DataNotFoundException(e.getMessage());
        } catch (ParentFileIsAlreadyExists e) {
            throw new ParentFileIsAlreadyExists(e.getMessage());
        } catch (Exception e) {
            throw new DataNotFoundException("Ресурс не найден");
        }
    }


    @PostMapping("/resource")
    public ResponseEntity<FileResource> uploadFile(@RequestPart("object") List<MultipartFile> files,
                                                   @AuthenticationPrincipal User user,
                                                   @RequestParam String path) {

        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(resourceService.createFile(files, user.getId(), path));
        } catch (DataNotFoundException e) {
            throw new DataNotFoundException("Файл не найден");
        } catch (NotCorrectNameFileOrPackage e) {
            throw new NotCorrectNameFileOrPackage("Такой файл уже существует");
        }
    }

    @GetMapping("/resource/download")
    public ResponseEntity<?> downloadFile(@RequestParam("path") String path,
                                          @AuthenticationPrincipal User user) {
        if (path.isEmpty() || path.equals(".") || path.equals("..") || path.contains("../") || path.contains("..\\")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponse.builder().message("Невалидный или отсутствующий путь").status(400).build());
        }
        InputStreamResource object = null;
        try {
            object = resourceService.downloadResource(path, user.getId());
        } catch (DataNotFoundException e) {
            throw new DataNotFoundException("Ресурс не найден");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ErrorResponse.builder().message("Неизвестная ошибка").status(500).build());
        }
        ContentDisposition contentDisposition = ContentDisposition.attachment().filename(path).build();
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .body(object);
    }

    @GetMapping("/resource/move")
    public ResponseEntity<?> move(@RequestParam("from") String from,
                                  @RequestParam("to") String to,
                                  @AuthenticationPrincipal User user) {
        if (from.isEmpty() || from.equals(".") || from.equals("..") || from.contains("../") || from.contains("..\\") ||
                to.isEmpty() || to.equals(".") || to.equals("..") || to.contains("../") || to.contains("..\\")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponse.builder().message("Невалидный или отсутствующий путь").status(400).build());
        }
        try {
            return ResponseEntity.ok().body(resourceService.moveResoutce(from, to, user.getId()));
        } catch (NotCorrectNameFileOrPackage e) {
            throw new NotCorrectNameFileOrPackage(e.getMessage());
        } catch (DataNotFoundException e) {
            throw new DataNotFoundException(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ErrorResponse.builder().message("Неизвестная ошибка").status(500).build());
        }
    }

    @DeleteMapping("/resource")
    public ResponseEntity<?> delete(@RequestParam("path") String path,
                                    @AuthenticationPrincipal User user) {
        if (path.isEmpty() || path.equals(".") || path.equals("..") || path.contains("../") || path.contains("..\\")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponse.builder().message("Невалидный или отсутствующий путь").status(400).build());
        }
        try {
            System.out.println(resourceService.deleteResource(path, user.getId()));
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        } catch (DataNotFoundException e) {
            throw new DataNotFoundException("Ресурс не найден");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponse.builder().message("Неизвестная ошибка").status(400).build());
        }

    }

    @GetMapping("/resource/search")
    public ResponseEntity<?> search(@RequestParam("query") String query,
                                                     @AuthenticationPrincipal User user) {
        if (query.isEmpty() || query.equals(".") || query.equals("..") || query.contains("../") || query.contains("..\\")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponse.builder().message("Невалидный или отсутствующий путь").status(400).build());
        }
        try {
            return ResponseEntity.ok().body(resourceService.searchResource(query, user.getId()));
        } catch (Exception e) {
            throw new DataNotFoundException("Ресурс не найден");
        }
    }

    @GetMapping("/resource")
    public ResponseEntity<?> lookThisResource(@RequestParam("path") String path, @AuthenticationPrincipal User user) {
        if (path.isEmpty() || path.equals(".") || path.equals("..") || path.contains("../") || path.contains("..\\")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponse.builder().message("Невалидный или отсутствующий путь").status(400).build());
        }
        try {
            return ResponseEntity.ok().body(resourceService.resource(user.getId(), path));
        } catch (Exception e) {
            throw new DataNotFoundException("Ресурс не найден");
        }
    }


}