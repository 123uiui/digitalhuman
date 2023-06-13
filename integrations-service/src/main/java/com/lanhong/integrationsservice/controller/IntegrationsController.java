package com.lanhong.integrationsservice.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Controller;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

@Controller
public class IntegrationsController {

    private Logger logger = LoggerFactory.getLogger(IntegrationsController.class);

    private final String speechToTextUrl = "http://localhost:8081/speech-to-text";
    private final String textToSpeechUrl = "http://localhost:8083/text-to-speech";

    private final String chatGptServiceUrl = "http://192.168.0.106:8082/chat";

    private final WebClient webClient = WebClient
            .builder()
            .exchangeStrategies(ExchangeStrategies
                    .builder()
                    .codecs(codecs -> codecs
                            .defaultCodecs()
                            .maxInMemorySize(500 * 1024*1000))
                    .build())
            .build();

    @ResponseBody
    @PostMapping("/integrate")
    public void integrate(@RequestParam("audioData") MultipartFile multipartFile) throws IOException {

        String textFromAzure = speechToTextFromAzure(multipartFile);
        logger.info(textFromAzure);

        String answer = answerFromChatGpt(textFromAzure);
        logger.info(answer);
        byte[] byteString = textToSpeechFromAzure(answer);

        saveToFileFromBytes(byteString,"output1.wav");

        //logger.info(new String(byteString));

    }

    // 调用语音转文本服务
    private String speechToTextFromAzure(MultipartFile multipartFile) throws IOException {
        MultipartBodyBuilder multipartBodyBuilder = new MultipartBodyBuilder();

        multipartBodyBuilder.part("audioData", new ByteArrayResource(multipartFile.getBytes())).filename(multipartFile.getOriginalFilename());

        ResponseEntity<String> speechToTextResponse = WebClient.create()
                .post()
                .uri(speechToTextUrl)
                .bodyValue(new ByteArrayResource(multipartFile.getBytes()))
                //.bodyValue(multipartBodyBuilder.build())
                .retrieve()
                .toEntity(String.class)
                .block();

        if (speechToTextResponse.getStatusCodeValue() == 200) {
            return speechToTextResponse.getBody();
        } else {
            return "";
        }
    }

    // 调用chatgpt
    private String answerFromChatGpt(String content) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("content", content);
        ResponseEntity<String> chatGptServiceResponse = webClient.post()
                .uri(chatGptServiceUrl)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .toEntity(String.class)
                .block();
        if (chatGptServiceResponse.getStatusCodeValue() == 200) {
            return chatGptServiceResponse.getBody();
        } else {
            return "";
        }
    }

    // 调用文本转语音服务
    private byte[] textToSpeechFromAzure(String content) {

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("text", content);
        ResponseEntity<DataBuffer> bufferResponseEntity = webClient.post()
                .uri(textToSpeechUrl)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .toEntity(DataBuffer.class)
                .block();

        DataBuffer dataBuffer = bufferResponseEntity.getBody();
        byte[] resultBytes = new byte[dataBuffer.readableByteCount()];
        dataBuffer.read(resultBytes);
        int statusCodeValue = bufferResponseEntity.getStatusCodeValue();

        if (statusCodeValue == 200) {
            return resultBytes;
        } else {
            return new byte[0];
        }

    }


    private void saveToFileFromBytes(byte[] data, String path) throws IOException {
        Path filePath = Path.of(path);
        Files.write(filePath,data, StandardOpenOption.CREATE);
    }


}
