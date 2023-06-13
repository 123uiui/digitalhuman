package com.lanhong.texttospeechservice.controller;

import com.microsoft.cognitiveservices.speech.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ExecutionException;

@RestController
public class TextToSpeechController {
    private static String speechKey = "4ea354b89ca74f79adc3ab7393891b4b";
    private static String speechRegion = "eastus";


    @RequestMapping("/text-to-speech")
    public ResponseEntity<byte[]> convertTextToSpeech(@RequestParam("text") String text) throws ExecutionException, InterruptedException, IOException {

        SpeechConfig speechConfig = SpeechConfig.fromSubscription(speechKey, speechRegion);

        speechConfig.setSpeechSynthesisVoiceName("zh-cn-YunfengNeural");
        speechConfig.setSpeechRecognitionLanguage("zh-CN");

        SpeechSynthesizer speechSynthesizer = new SpeechSynthesizer(speechConfig);
        SpeechSynthesisResult speechSynthesisResult = speechSynthesizer.SpeakTextAsync(text).get();


        if (speechSynthesisResult.getReason() == ResultReason.SynthesizingAudioCompleted) {
            System.out.println("Speech synthesized to speaker for text [" + text + "]");
            byte[] audioData = speechSynthesisResult.getAudioData();

            saveToFileFromBytes(audioData,"output.wav");

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=file.txt");

            ResponseEntity<byte[]> responseEntity = ResponseEntity
                    .ok()
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(audioData);

            // 返回ResponseEntity对象，设置状态码、响应头和响应体
            return responseEntity;

        }
        else if (speechSynthesisResult.getReason() == ResultReason.Canceled) {
            SpeechSynthesisCancellationDetails cancellation = SpeechSynthesisCancellationDetails.fromResult(speechSynthesisResult);
            System.out.println("CANCELED: Reason=" + cancellation.getReason());

            if (cancellation.getReason() == CancellationReason.Error) {
                System.out.println("CANCELED: ErrorCode=" + cancellation.getErrorCode());
                System.out.println("CANCELED: ErrorDetails=" + cancellation.getErrorDetails());
                System.out.println("CANCELED: Did you set the speech resource key and region values?");
            }
        }

        byte[] emptyBytes = new byte[0];
        return new ResponseEntity<byte[]>(emptyBytes, HttpStatus.BAD_REQUEST);
    }


    private void saveToFileFromBytes(byte[] data, String path) throws IOException {
        Path filePath = Path.of(path);
        Files.write(filePath,data, StandardOpenOption.CREATE);
    }


}
