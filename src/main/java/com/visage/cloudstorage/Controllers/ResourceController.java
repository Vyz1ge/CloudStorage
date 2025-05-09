package com.visage.cloudstorage.Controllers;

import com.visage.cloudstorage.Model.FileResource;
import com.visage.cloudstorage.Exceptions.DataNotFoundException;
import com.visage.cloudstorage.Model.User;
import com.visage.cloudstorage.Services.ResourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
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

    @GetMapping("/resource")
    public ResponseEntity<FileResource> lookThisResource(@RequestParam("path") String path, @AuthenticationPrincipal User user)  {
        try {
            return ResponseEntity.ok().body(resourceService.resource(user.getId(),path));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @GetMapping("/directory")
    public ResponseEntity<List<FileResource>> lookAllResource(@RequestParam("path") String path, @AuthenticationPrincipal User user)  {
        try {
            return ResponseEntity.ok().body(resourceService.resources(user.getId(),path));
        } catch (Exception e) {
            throw new DataNotFoundException("Ресурс не найден");
        }
    }


    @PostMapping("/directory")
    public ResponseEntity<List<FileResource>> createPackage(@RequestParam("path") String path, @AuthenticationPrincipal User user)  {

        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(resourceService.createPackage(path, user.getId()));
        } catch (Exception e) {
            throw new DataNotFoundException("Ресурс не найден");
        }
    }


    @PostMapping("/resource")
    public ResponseEntity<FileResource> uploadFile(@RequestPart("object") List<MultipartFile> files,
                                                   @AuthenticationPrincipal User user,
                                                   @RequestParam String path) {

        return ResponseEntity.status(HttpStatus.CREATED).body(resourceService.createFile(files, user.getId(), path));
    }

    @GetMapping("/resource/download")
    public ResponseEntity<Resource> downloadFile(@RequestParam("path") String path,
                                                 @AuthenticationPrincipal User user)  {

        InputStreamResource object = null;
        try {
            object = resourceService.downloadResource(path, user.getId());
        } catch (Exception e) {
            throw new DataNotFoundException("Ресурс не найден");
        }
        ContentDisposition contentDisposition = ContentDisposition.attachment().filename(path).build();
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .body(object);
    }

    @GetMapping("/resource/move")
    public ResponseEntity<FileResource> move(@RequestParam("from") String from,
                                             @RequestParam("to") String to,
                                             @AuthenticationPrincipal User user)  {


        try {
            return ResponseEntity.ok().body(resourceService.moveResoutce(from,to,user.getId()));
        } catch (Exception e) {
            throw new DataNotFoundException("Ресурс не найден");
        }
    }

    @DeleteMapping("/resource")
    public ResponseEntity<FileResource> delete(@RequestParam("path") String path,
                                               @AuthenticationPrincipal User user) {

        try {
            System.out.println(resourceService.deleteResource(path));
        } catch (Exception e) {
            throw new DataNotFoundException("Ресурс не найден");
        }

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @GetMapping("/resource/search")
    public ResponseEntity<List<FileResource>> search(@RequestParam("query") String query,
                                     @AuthenticationPrincipal User user) {

        try {
            return ResponseEntity.ok().body(resourceService.searchResource(query,user.getId()));
        } catch (Exception e) {
            throw new DataNotFoundException("Ресурс не найден");
        }
    }




}
