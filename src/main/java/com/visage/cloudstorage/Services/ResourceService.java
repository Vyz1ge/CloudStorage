package com.visage.cloudstorage.Services;


import com.visage.cloudstorage.DTO.FileResource;
import io.minio.Result;
import io.minio.errors.*;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RequiredArgsConstructor
@Transactional
@Service
public class ResourceService {

    private final MinioService minioService;

    public FileResource resource(Integer userId,String path) throws Exception {
        String acceptPath = "the" + userId + "/" + path;
        return minioService.metadataObject(acceptPath, userId);
    }

    public List<FileResource> resources(Integer userId, String path) throws Exception {
////                                                  userId =  12       papka%2F
////
//// Бейз у меня будет base = x%2F
        String base = "the" + userId + "/" + path;
        String prefix;
////   Потому что  %2F == "/" перед  papka
        if (base.endsWith("/")){
            prefix = base; //// эта штука будет prefix = the12/papka/
        }else {
            prefix = base + "/";
        }
        
////    ВСПОМИНАЮ ВСЕ ЭЛЕМЕНТЫ БЕЗ РЕКУРСИИ ДЕРЕВА ТАК КАК ПРОСТО ХОЧУ ПРОЙТИСЬ ПО ЭЛЕМЕНТАМ ИЗ МИНИО
        Iterable<Result<Item>> objects = minioService.getObjects(prefix, false);
////    СОЗДАЮ ПУСТОЙ ЛИСТ МОЕГО РЕСУРСА
        List<FileResource> fileResourceList = new ArrayList<>();
        //// ИТЕРИРУЮСЬ ПО ЭЛЕМЕНТАМ
        for (Result<Item> object : objects) {
            //// СОЗДАЛ ПУСТРОЙ МОЙ ФАЙЛ РЕСУРС
            FileResource fileResource;
            //// ДОСТАЛ АЙТЕМ ИЗ МИНИО
            Item item = object.get();
            System.out.println(item.objectName());
////
////            System.out.println(item.objectName());
////
////            the12/papka/
////            the12/papka/hello (4) (1) (1).txt
////            the12/papka/papkavnytri/
////            the12/papka/
////            the12/papka/hello (4) (1) (1).txt
////            the12/papka/papkavnytri/
////
            //// СОЗДАЛ РОДИТЕЛЬСКУЮ СТРОКУ relative =
////
////        prefix.length() = [t] [h] [e] [1] [2] [/] [p] [a] [p] [k] [a] [/]    =    12
////                           0   1   2   3   4   5   6   7   8   9  10   11
////                           -   -   -   -   -   -   -   -   -   -  -   -
////        the12/papka/hello (4) (1) (1).txt  ->   hello (4) (1) (1).txt
            String relative = item.objectName().substring(prefix.length());//// |
            /////                                                               |
            if (relative.isEmpty()){
                continue; //// object[1]  -> skip
            }

////                   hello (4) (1) (1).txt
////                   papkavnytri/

            int idx  = relative.indexOf("/");
            String name;
            if (idx == -1){
                name = relative; ////       hello (4) (1) (1).txt   ->     hello (4) (1) (1).txt
            } else {////                     12 + 1
                name = relative.substring(0,idx+1); ////  papkavnytri/        ->      papkavnytri
            }
////       ЕСЛИ НЕТ РАЗМЕРА ТО ПАПКА
            if (item.size() == 0) {  ////    ЗАПОЛНЯЮ ПАПКУ
                fileResource = FileResource
                        .builder()
                        .path(prefix) //// the12/papka/
                        .name(name)   //// hello (4) (1) (1).txt  or  papkavnytri
                        .type("DIRECTORY")
                        .build();
            }//// ЕСТЬ РАЗМЕР ФАЙЛ
            else {
                fileResource = FileResource
                        .builder()
                        .path(prefix) //// the12/papka/
                        .name(name)   //// hello (4) (1) (1).txt  or  papkavnytri
                        .size(item.size())
                        .type("FILE")
                        .build();
            }
            fileResourceList.add(fileResource);
        }
        return fileResourceList;
    }

    public FileResource createPackage(String name, Integer userId) throws Exception {
        String path = "the" + userId + "/"+ name;
        minioService.createEmptyDirectory(path);
        return FileResource.builder()
                .type("DIRECTORY")
                .name(name)
                .path("the" + userId + "/")
                .build();
    }

    public FileResource createFile(List<MultipartFile> files, Integer userId, String path) {
        String base = "the"+userId + "/" + path;
        System.out.println(base);
        for (MultipartFile file : files) {
            try {
//                System.out.println(file.getInputStream());
                minioService.upload(file.getInputStream(),base,file);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return FileResource
                .builder()
                .path("the" + userId + "/" + "file.getName()")
                .name("file.getName()")
                .size(123L)
                .type("FILE")
                .build();

    }

    public InputStreamResource downloadResource(String path, Integer userId) throws Exception {
        if (!path.endsWith("/")){
            InputStream item = minioService.getObject(path);
            return new InputStreamResource(item);

        }
        String prefix = path;
        if (!prefix.endsWith("/")){
            prefix = prefix + "/";
        }
        Iterable<Result<Item>> objects = minioService.getObjects(prefix, true);
        ByteArrayOutputStream zipBuffer = new ByteArrayOutputStream();
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(zipBuffer)) {
            for (Result<Item> object : objects) {
                Item item = object.get();
                String relative = item.objectName();
                if (relative.isEmpty()) {
                    continue;
                }

                String name = relative.substring(prefix.length());
//                if (name.isEmpty()) {
//                    continue;
//                }
                InputStream input = minioService.getObject(relative);
                zipOutputStream.putNextEntry(new ZipEntry(name));
                input.transferTo(zipOutputStream);
                zipOutputStream.closeEntry();
            }
        }
        ByteArrayInputStream zipContent = new ByteArrayInputStream(zipBuffer.toByteArray());

        return new InputStreamResource(zipContent);
    }




    public void createTheCentralPoint(Integer userId)  {
        String path = "the"+userId + "/";
        try {
            minioService.createEmptyDirectory(path);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public FileResource moveResoutce(String from, String to, Integer userId) throws Exception {
        if (!from.endsWith("/")){
            minioService.copy(from,to);
            minioService.removeObject(from);
            return FileResource.builder()
                    .path("accept")
                    .name("accept")
                    .type("FILE")
                    .build();
        }
        String prefix = from;
        if (!prefix.endsWith("/")){
            prefix = prefix + "/";
        }
        Iterable<Result<Item>> objects = minioService.getObjects(prefix, true);
        for (Result<Item> object : objects) {
            Item item = object.get();
            String relativeFrom = item.objectName();
            String name = relativeFrom.substring(prefix.length());
            String relativeTo = to + name;
//            if (relative.isEmpty()) {
//                continue;
//            }
            minioService.copy(relativeFrom,relativeTo);
            minioService.removeObject(relativeFrom);

        }
        return FileResource.builder()
                .path("accept")
                .name("accept")
                .type("FILE")
                .build();

//        minioService.copy(from,to);
//        System.out.println(from + " from");
//        System.out.println(to + " to");
//        minioService.removeObject(from);
//        return FileResource.builder()
//                .path("random")
//                .name("random")
//                .type("DIRECTORY")
//                .build();
    }

    public boolean deleteResource(String path) throws Exception {
        if (path.endsWith("/")){
            minioService.removeFolderBatch(path);
        }else {
            minioService.removeObject(path);
        }
        return true;
    }

    public List<FileResource> searchResource(String query, Integer userId) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        String base = "the"+ userId + "/";
        List<FileResource> resources = new ArrayList<>();
        Iterable<Result<Item>> objects = minioService.getObjects(base, true);
        for (Result<Item> object : objects) {
            Item item = object.get();
            if (!(item.size() == 0)){
                String pathFull = item.objectName();
                int idx = pathFull.indexOf("/");
                while (idx != -1) {
                    pathFull = pathFull.substring(idx + 1);
                    idx = pathFull.indexOf("/");
                }
                int idxPoint = pathFull.indexOf(".");
                String nameWithoutPoint = pathFull.substring(0,idxPoint);
                if (nameWithoutPoint.equals(query)) {
                    resources.add(FileResource.builder()
                            .path(item.objectName().substring(0, item.objectName().length() - pathFull.length()))
                            .name(pathFull)
                            .size(item.size())
                            .type("FILE")
                            .build());
                }
            }
        }

        return resources;


    }
}
