package com.beaver.live.demo;

import lombok.extern.slf4j.Slf4j;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacv.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * @program: ffmpeg-example
 * @description:
 * @author: zhb
 * @create: 2023-04-19 10:02
 */
@Slf4j
public class FFmpegUtils {

    public static void mp4Tom3u8One() throws Exception {

        String input = "d:/hls/976675899_nb3-1-16.mp4";
        String output = "d:/hls/m3u8/test.m3u8";
        List<String> command = new ArrayList<>();
        command.add("ffmpeg");
        command.add("-i");
        command.add(input);
        command.add("-c:v");
        command.add("copy");
        command.add("-c:a");
        command.add("copy");
        command.add("-f");
        command.add("ssegment");
        command.add("-segment_format");
        command.add("mpegts");
        command.add("-segment_list");
        command.add(output);
        command.add("-segment_time");
        command.add("10");
        command.add("d:/hls/m3u8/test%05d.ts");

        Process videoProcess = new ProcessBuilder(command).redirectErrorStream(true).start();
        videoProcess.waitFor();

        log.info("中间转换已完成,生成文件");
    }

    public static void mp4Tom3u8Two() throws Exception {
        avutil.av_log_set_level(avutil.AV_LOG_INFO);
        FFmpegLogCallback.set();

        InputStream inputStream = new FileInputStream("d:/hls/976675899_nb3-1-16.mp4");

        FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(inputStream);
        grabber.start();

        FFmpegFrameRecorder recorder = new FFmpegFrameRecorder("d:/hls/m3u8/index.m3u8", grabber.getImageWidth(), grabber.getImageHeight(), grabber.getAudioChannels());

        recorder.setFormat("hls");
        recorder.setOption("hls_time", "5");
        recorder.setOption("hls_list_size", "0");
        recorder.setOption("hls_flags", "delete_segments");
        recorder.setOption("hls_delete_threshold", "1");
        recorder.setOption("hls_segment_type", "mpegts");
        recorder.setOption("hls_segment_filename", "d:/hls/m3u8/test-%d.ts");

        // http属性
        //recorder.setOption("method", "POST");

        recorder.setFrameRate(24);
        recorder.setGopSize(2 * 25);
        recorder.setVideoQuality(1.0);
        recorder.setVideoBitrate(640 * 306);

        if (grabber.getAudioChannels() > 0) {
            recorder.setAudioChannels(grabber.getAudioChannels());
            recorder.setAudioBitrate(grabber.getAudioBitrate());
            recorder.setAudioCodec(grabber.getAudioCodec());
        }

        //只有视频
        //recorder.start();
        //Frame frame;
        //while ((frame = grabber.grabImage()) != null) {
        //    try {
        //        recorder.record(frame);
        //    } catch (FrameRecorder.Exception e) {
        //        e.printStackTrace();
        //    }
        //}
        //recorder.setTimestamp(grabber.getTimestamp());
        //recorder.close();
        //grabber.close();


        //音频+视频
        recorder.start(grabber.getFormatContext());
        AVPacket packet;
        while ((packet = grabber.grabPacket()) != null) {
            try {
                recorder.recordPacket(packet);
            } catch (FrameRecorder.Exception e) {
                e.printStackTrace();
            }
        }
        recorder.setTimestamp(grabber.getTimestamp());
        recorder.stop();
        recorder.release();
        grabber.stop();
        grabber.release();
    }

    public static void main(String[] args) throws Exception {
        //FFmpegUtils.mp4Tom3u8One();
        FFmpegUtils.mp4Tom3u8Two();
    }
}