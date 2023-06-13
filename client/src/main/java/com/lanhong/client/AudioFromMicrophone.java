package com.lanhong.client;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.HttpEntityWrapper;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.HttpClients;

import javax.sound.sampled.*;
import java.io.ByteArrayOutputStream;

public class AudioFromMicrophone {


    private static final String speechToTextUrl = "http://localhost:8081/speech-to-text";
    public static void main(String[] args) throws Exception {


        // 获取默认音频捕获设备（麦克风）
        AudioFormat format = new AudioFormat(16000, 16, 1, true, true);
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
        TargetDataLine microphone = (TargetDataLine) AudioSystem.getLine(info);

        // 打开音频捕获设备
        microphone.open(format);
        microphone.start();

        // 创建HttpClient实例
        HttpClient httpClient = HttpClients.createDefault();

        // 创建POST请求
        HttpPost httpPost = new HttpPost(speechToTextUrl);

        // 创建字节数组输出流
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        // 定义缓冲区
        byte[] buffer = new byte[1024*1000*1000];

        // 创建数据线程，实时读取麦克风数据并发送到指定地址
        Thread transmitThread = new Thread(() -> {
            try {
                while (true) {
                    // 从麦克风读取音频数据
                    int bytesRead = microphone.read(buffer, 0, buffer.length);

                    // 写入字节数组输出流
                    outputStream.write(buffer, 0, bytesRead);

                    // 获取当前输出流中的音频数据
                    byte[] audioData = outputStream.toByteArray();

                    // 设置请求体为音频数据
                    ByteArrayEntity entity = new ByteArrayEntity(audioData);

                    httpPost.setEntity(entity);


                    // 发送POST请求
                    HttpResponse response = httpClient.execute(httpPost);

                    // 处理响应（根据需求自行添加）

                    // 清空输出流，准备接收下一次的音频数据
                    outputStream.reset();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        // 启动传输线程
        transmitThread.start();

        // 等待线程结束（这里使用了无限循环，需要手动中断线程）
        // transmitThread.join();

        // 关闭音频捕获设备
        microphone.stop();
        microphone.close();
    }
}

