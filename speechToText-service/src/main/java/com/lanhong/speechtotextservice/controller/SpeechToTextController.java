package com.lanhong.speechtotextservice.controller;

import com.microsoft.cognitiveservices.speech.*;
import com.microsoft.cognitiveservices.speech.audio.*;
import com.microsoft.cognitiveservices.speech.audio.AudioInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

@Controller
public class SpeechToTextController {

    private Logger logger = LoggerFactory.getLogger(SpeechToTextController.class);


    private static String speechKey = "7deb64bd83e649abae17e7ae01b84133";
    //"4ea354b89ca74f79adc3ab7393891b4b";
    private static String speechRegion = "eastasia";
                    //"eastus";


    @PostMapping("/speech-to-text")
    public ResponseEntity<String> convertSpeechToText(InputStream inputStream) throws ExecutionException, InterruptedException, IOException, UnsupportedAudioFileException {


        AudioConfig audioConfig = getAudioConfig("/Users/zhangfan/Documents/projects/digitalhuman/test.wav", inputStream);


        SpeechConfig speechConfig = SpeechConfig.fromSubscription(speechKey, speechRegion);
        speechConfig.setSpeechRecognitionLanguage("zh-CN");
        SpeechRecognizer speechRecognizer = new SpeechRecognizer(speechConfig, audioConfig);
        //开始识别
        Future<SpeechRecognitionResult> task = speechRecognizer.recognizeOnceAsync();
        // 获取识别的结果
        SpeechRecognitionResult speechRecognitionResult = task.get();
        String result = new String();
        result = "";
        if (speechRecognitionResult.getReason() == ResultReason.RecognizedSpeech) {
            System.out.println("RECOGNIZED: Text=" + speechRecognitionResult.getText());
            result = speechRecognitionResult.getText();
        } else if (speechRecognitionResult.getReason() == ResultReason.NoMatch) {
            System.out.println("NOMATCH: Speech could not be recognized.");
        } else if (speechRecognitionResult.getReason() == ResultReason.Canceled) {
            CancellationDetails cancellation = CancellationDetails.fromResult(speechRecognitionResult);
            System.out.println("CANCELED: Reason=" + cancellation.getReason());

            if (cancellation.getReason() == CancellationReason.Error) {
                System.out.println("CANCELED: ErrorCode=" + cancellation.getErrorCode());
                System.out.println("CANCELED: ErrorDetails=" + cancellation.getErrorDetails());
                System.out.println("CANCELED: Did you set the speech resource key and region values?");
            }
        }
        return new ResponseEntity<String>(result, HttpStatus.OK);
    }

    private AudioConfig getAudioConfig(String path,InputStream inputStream) throws IOException {
        saveInputStreamToFile(inputStream,"test.wav");
        AudioConfig audioConfig = AudioConfig.fromWavFileInput(path);
        return audioConfig;
    }

    private AudioConfig getAudioConfig(InputStream inputStream) throws IOException {
        byte channels = 1;
        byte bitsPerSample = 16;
        int samplesPerSecond = 16000; // or 8000
        AudioStreamFormat audioStreamFormat = AudioStreamFormat.getDefaultInputFormat();
        PushAudioInputStream pushStream = AudioInputStream.createPushStream(audioStreamFormat);
        pushStream.write(inputStream.readAllBytes());
        AudioConfig audioConfig = AudioConfig.fromStreamInput(pushStream);
        return audioConfig;
    }




    private Boolean checkAudioSampleRate(InputStream inputStream) throws UnsupportedAudioFileException, IOException {


        javax.sound.sampled.AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(inputStream);
        AudioFormat audioFormat = audioInputStream.getFormat();
        float sampleRate = audioFormat.getSampleRate();

        boolean result = false;
        if ((int) sampleRate == 16000) {
            result = true;
        }
        logger.info(audioFormat.toString());
        audioInputStream.close();
        return result;
    }

    private InputStream modifyAudioSampleRate(InputStream inputStream) throws UnsupportedAudioFileException, IOException {
        javax.sound.sampled.AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(inputStream);
        AudioFormat audioFormat = audioInputStream.getFormat();
        // 指定目标音频流的格式，将采样率设置为16 kHz
        AudioFormat targetFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                16000,  // 目标采样率为16 kHz
                audioFormat.getSampleSizeInBits(),
                audioFormat.getChannels(),
                audioFormat.getFrameSize(),
                audioFormat.getFrameRate(),
                audioFormat.isBigEndian());
        // 使用转换器将源音频流转换为目标音频流
        javax.sound.sampled.AudioInputStream targetStream = AudioSystem.getAudioInputStream(targetFormat,audioInputStream);
        logger.info(targetStream.getFormat().toString());
        // 将目标音频流转换为字节数组
        byte[] audioData = new byte[targetStream.available()];
        targetStream.read(audioData);
        // 将字节数组包装为 InputStream
        return new ByteArrayInputStream(audioData);

    }

    public void saveInputStreamToFile(InputStream inputStream, String filePath) throws IOException {
        OutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(filePath);
            byte[] buffer = new byte[4096]; // 缓冲区大小，可以根据需要进行调整
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        } finally {
            if (outputStream != null) {
                outputStream.close();
            }
        }
    }


}
