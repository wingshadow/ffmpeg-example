package com.beaver.live.demo;

import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.crypto.symmetric.SymmetricAlgorithm;
import cn.hutool.crypto.symmetric.SymmetricCrypto;
import io.lindstrom.m3u8.model.MediaPlaylist;
import io.lindstrom.m3u8.model.MediaSegment;
import io.lindstrom.m3u8.parser.MediaPlaylistParser;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.*;
import org.bytedeco.javacv.Frame;
import sun.font.FontDesignMetrics;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

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

    public static void m3u8ToMP4() throws IOException {
        MediaPlaylistParser parser = new MediaPlaylistParser();
        MediaPlaylist playlist = parser.readPlaylist(Paths.get("D:\\hls\\m3u8\\index.m3u8"));
        List<MediaSegment> list = playlist.mediaSegments();
        List<String> files = list.stream().map(mediaSegment -> mediaSegment.uri()).collect(Collectors.toList());

        List<File> fileList = new ArrayList<>();
        files.forEach(s -> {
            fileList.add(new File("D:\\hls\\m3u8\\" + s));
        });
        mergeFile(fileList, "", false);
    }

    public static void mergeFile(List<File> collect, String key, boolean needDecode) {
        Collections.sort(collect, new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                int i1 = Integer.parseInt(FileNameUtil.getPrefix(o1).replace("test-", ""));
                int i2 = Integer.parseInt(FileNameUtil.getPrefix(o2).replace("test-", ""));
                return i1 - i2;
            }
        });
        for (int i = 0; i < collect.size(); i++) {
            System.out.println(collect.get(i).getName());
        }
        File finalOutPutFile = new File("d:\\hls\\demo.mp4");

        try {
            FileOutputStream fileOutputStream = new FileOutputStream(finalOutPutFile, true);
            for (int i = 0; i < collect.size(); i++) {
                FileInputStream fileInputStream = new FileInputStream(collect.get(i));
                byte b[] = new byte[4096];
                int size = -1;
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                while ((size = fileInputStream.read(b, 0, b.length)) != -1) {
                    byteArrayOutputStream.write(b, 0, size);
                }
                fileInputStream.close();
                byte[] bytes = byteArrayOutputStream.toByteArray();
                byteArrayOutputStream.close();
                byte[] newbyte;
                if (needDecode) {
                    SymmetricCrypto aes = new SymmetricCrypto(SymmetricAlgorithm.AES, key.getBytes());
                    newbyte = aes.decrypt(bytes);
                } else {
                    newbyte = bytes;
                }
                fileOutputStream.write(newbyte);

            }
            if (fileOutputStream != null) {
                fileOutputStream.close();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void addSubtitle(String inputVideoFile, String outputVideoFile) throws IOException {


        String[] test = {"世上无难事", "只怕有心人", "只要思想不滑坡", "办法总比困难多", "长江后浪推前浪", "前浪死在沙滩上"};

        //为连续的50帧设置同一个测试字幕文本
        List<String> testStr = new ArrayList<String>();
        for (int i = 0; i < 300; i++) {
            testStr.add(test[i / 50]);
        }
        //设置源视频、加字幕后的视频文件路径
        FFmpegFrameGrabber grabber = FFmpegFrameGrabber.createDefault(inputVideoFile);
        grabber.start();

        //视频相关配置，取原视频配置
        FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(outputVideoFile, grabber.getImageWidth(), grabber.getImageHeight(), grabber.getAudioChannels());
        recorder.setFrameRate(grabber.getFrameRate());
        recorder.setVideoCodec(grabber.getVideoCodec());
        recorder.setVideoQuality(1.0);
        //音频相关配置，取原音频配置
        if (grabber.getAudioChannels() > 0) {
            recorder.setAudioChannels(grabber.getAudioChannels());
            recorder.setAudioBitrate(grabber.getAudioBitrate());
            recorder.setAudioCodec(grabber.getAudioCodec());
        }

        //recorder.setFrameRate(24);
        //recorder.setGopSize(2 * 25);
        ////设置视频编码质量
        //recorder.setVideoQuality(1.0);
        //recorder.setVideoBitrate(640 * 306);
        //
        //if (grabber.getAudioChannels() > 0) {
        //    recorder.setAudioChannels(grabber.getAudioChannels());
        //    recorder.setAudioBitrate(grabber.getAudioBitrate());
        //    recorder.setAudioCodec(grabber.getAudioCodec());
        //}

        recorder.start();
        System.out.println("准备开始推流...");
        Java2DFrameConverter converter = new Java2DFrameConverter();

        Frame frame;
        int i = 0;
        while ((frame = grabber.grab()) != null) {
            //从视频帧中获取图片
            if (frame.image != null) {
                //对图片进行文本合入
                BufferedImage bufferedImage = converter.getBufferedImage(frame);
                //视频帧赋值，写入输出流
                bufferedImage = addStr(bufferedImage, testStr.get(i++ % 300));
                frame.image = converter.getFrame(bufferedImage).image;
                recorder.record(frame);
            }
            //音频帧写入输出流
            if (frame.samples != null) {
                recorder.record(frame);
            }
        }
        System.out.println("推流结束...");
        grabber.stop();
        recorder.stop();
    }

    /*** 图片添加文本
     *
     *@parambufImg
     *@paramsubTitleContent
     *@return
     */
    private static BufferedImage addStr(BufferedImage bufImg, String subTitleContent) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        //添加字幕时的时间
        Font font = new Font("微软雅黑", Font.BOLD, 32);
        String timeContent = sdf.format(new Date());
        FontDesignMetrics metrics = FontDesignMetrics.getMetrics(font);
        Graphics2D graphics = bufImg.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        //设置图片背景
        graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));
        //设置左上方时间显示
        graphics.drawImage(bufImg, 0, 0, bufImg.getWidth(), bufImg.getHeight(), null);
        graphics.setColor(Color.orange);
        graphics.setFont(font);
        //计算文字长度，计算居中的x点坐标
        graphics.drawString(timeContent, 0, metrics.getAscent());
        int textWidth = metrics.stringWidth(subTitleContent);
        int widthX = (bufImg.getWidth() - textWidth) / 2;
        graphics.setColor(Color.red);
        graphics.setFont(font);
        graphics.drawString(subTitleContent, widthX, bufImg.getHeight() - 100);
        graphics.dispose();
        return bufImg;
    }

    public static void main(String[] args) throws Exception {
        //FFmpegUtils.mp4Tom3u8One();
        //FFmpegUtils.mp4Tom3u8Two();
        //FFmpegUtils.m3u8ToMP4();
        FFmpegUtils.addSubtitle("D:\\hls\\976675899_nb3-1-16.mp4","D:\\hls\\demo1.mp4");
        //File file = new File("D:\\hls\\m3u8");
        //FFmpegUtils.mergeFile(file,"",false);
    }
}