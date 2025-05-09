package com.visage.cloudstorage.integration.Controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.redis.testcontainers.RedisContainer;
import com.visage.cloudstorage.Model.Role;
import com.visage.cloudstorage.Model.User;
import com.visage.cloudstorage.Repositories.UserRepository;
import com.visage.cloudstorage.Services.MinioService;
import io.minio.*;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;


import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@SpringBootTest
@Testcontainers
@AutoConfigureMockMvc(/*addFilters = false*/)
@ActiveProfiles("test")
public class ResourceControllerTest {

    @Container
    @ServiceConnection
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:latest");

//    private static final int MINIO_PORT = 9000;
//    private static final int CONSOLE_PORT = 9001;

//    @Container
//    private static final GenericContainer<?> minioContainer = new GenericContainer<>("minio/minio:latest")
//            .withExposedPorts(MINIO_PORT, CONSOLE_PORT)
//            .withEnv("MINIO_ROOT_USER", "minioadmin")
//            .withEnv("MINIO_ROOT_PASSWORD", "minioadmin")
//            .withCommand("server /data --console-address :9001")
//            .withStartupTimeout(Duration.ofSeconds(30));

    // Внутренние порты контейнера, на которых MinIO слушает
    private static final int MINIO_API_PORT_INTERNAL = 9000;
    private static final int MINIO_CONSOLE_PORT_INTERNAL = 9090; // Консоль будет на 9090 внутри

    // Фиксированные порты на хост-машине
    private static final int MINIO_API_PORT_HOST = 9000;
    private static final int MINIO_CONSOLE_PORT_HOST = 9090;

    @Container
    private static final GenericContainer<?> minioContainer = new GenericContainer<>("minio/minio:latest")
            .withExposedPorts(MINIO_API_PORT_INTERNAL, MINIO_CONSOLE_PORT_INTERNAL) // Все равно декларируем
            .withEnv("MINIO_ROOT_USER", "minioadmin")
            .withEnv("MINIO_ROOT_PASSWORD", "minioadmin")
            .withCommand("server /data --console-address :" + MINIO_CONSOLE_PORT_INTERNAL)
            .withStartupTimeout(Duration.ofSeconds(60))
            .waitingFor(new HttpWaitStrategy()
                    .forPort(MINIO_API_PORT_INTERNAL)
                    .forPath("/minio/health/live")
                    .withStartupTimeout(Duration.ofSeconds(30)))
            // Добавляем фиксированные порты после основных настроек
            // Этот метод НЕ возвращает this, поэтому его нельзя чейнить как .with...
            // Мы модифицируем объект minioContainer "по месту"
            // Для этого нужно, чтобы with... методы были выполнены ранее
            // Более канонично это делать в блоке static {} или конструкторе,
            // но для @Container поля это сложнее.
            // ПРОЩЕ ОБНОВИТЬСЯ.
            // Если этот способ не работает, то ваша версия Testcontainers слишком старая
            // для легкого фиксированного маппинга.
            .withCreateContainerCmdModifier(cmd -> { // Более надежный способ для старых версий
                cmd.withHostConfig(
                        cmd.getHostConfig()
                                .withPortBindings(
                                        new com.github.dockerjava.api.model.PortBinding(
                                                com.github.dockerjava.api.model.Ports.Binding.bindPort(MINIO_API_PORT_HOST),
                                                new com.github.dockerjava.api.model.ExposedPort(MINIO_API_PORT_INTERNAL, com.github.dockerjava.api.model.InternetProtocol.TCP)
                                        ),
                                        new com.github.dockerjava.api.model.PortBinding(
                                                com.github.dockerjava.api.model.Ports.Binding.bindPort(MINIO_CONSOLE_PORT_HOST),
                                                new com.github.dockerjava.api.model.ExposedPort(MINIO_CONSOLE_PORT_INTERNAL, com.github.dockerjava.api.model.InternetProtocol.TCP)
                                        )
                                )
                );
            });



    @Container
    @ServiceConnection
    static RedisContainer redis =
            new RedisContainer(DockerImageName.parse("redis:latest"));


    @Autowired
    private MockMvc mockMvc;


    @Autowired
    private MinioClient minioClient;

    @Autowired
    private MinioService minioService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    private static final User userTest = User.builder()
            .id(1)
            .password("password")
            .username("testuser")
            .role(Role.USER)
            .build();

//    @BeforeAll
//    static void prepare() throws InterruptedException {
//        Thread.sleep(4000);
//    }


    @BeforeEach
    void setUp() throws Exception {
        minioService.createBucketForUser();
        minioService.removeFolderBatch("the1/");
        System.out.println();
    }

    @Test
    void shouldBeLookThisResourceNotCorrect() throws Exception {
        mockMvc.perform(get("/api/resource")
                        .param("path", "Some path")
                        .with(user(userTest)))
                .andExpect(status().isBadRequest());
    }


    @Test
    void shouldBeLookThisResourceCorrect() throws Exception {
        mockMvc.perform(post("/api/directory")
                        .param("path", "testPackage/")
                        .with(user(userTest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("testPackage/"));
        mockMvc.perform(get("/api/resource")
                        .param("path", "testPackage/")
                        .with(user(userTest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("the1/"))
                .andExpect(jsonPath("$.size").value("0"))
                .andExpect(jsonPath("$.path").value("the1/testPackage/"));
    }



    @Test
    void shouldBeLookTheCenterPointResourceNull() throws Exception {
        mockMvc.perform(get("/api/directory")
                        .param("path", "")
                        .with(user(userTest)))
                .andExpect(status().isOk());
    }


    @Test
    void shouldBeLookTheCenterPointResourcePackageCorrect() throws Exception {
        mockMvc.perform(post("/api/directory")
                        .param("path", "testPackage")
                        .with(user(userTest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("testPackage"));
        mockMvc.perform(get("/api/directory")
                        .param("path", "")
                        .with(user(userTest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.[0].name").value("testPackage"));
    }

    @Test
    void shouldBeCreatePackage() throws Exception {
        mockMvc.perform(post("/api/directory")
                        .param("path", "testPackage")
                        .with(user(userTest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("testPackage"));
    }

    @Test
    void shouldBeUploadCorrectFile() throws Exception {
        MockMultipartFile multipartFile = new MockMultipartFile(
                "object",
                "objectName.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "CONTENT CONTENT".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/resource")
                        .file(multipartFile)
                        .param("path", "")
                        .with(user(userTest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("objectName.txt"))
                .andExpect(jsonPath("$.size").value(String.valueOf(multipartFile.getSize())))
                .andExpect(jsonPath("$.path").value("the1/"));

    }

    @Test
    void shouldBeDownloadCorrectFile() throws Exception {
        MockMultipartFile multipartFile = new MockMultipartFile(
                "object",
                "objectName.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "CONTENT CONTENT".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/resource")
                        .file(multipartFile)
                        .param("path", "")
                        .with(user(userTest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("objectName.txt"))
                .andExpect(jsonPath("$.size").value(String.valueOf(multipartFile.getSize())))
                .andExpect(jsonPath("$.path").value("the1/"));

        mockMvc.perform(get("/api/resource/download")
                .param("path", "the1/objectName.txt")
                .with(user(userTest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM))
                .andExpect(header().exists(HttpHeaders.CONTENT_DISPOSITION))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\"the1/objectName.txt\""));
    }

}