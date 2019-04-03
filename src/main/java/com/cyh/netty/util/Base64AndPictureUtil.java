package com.cyh.netty.util;

import org.apache.commons.lang3.StringUtils;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.io.*;

/**
 * 图片 和 Base64 相互转换 ， 图片置灰
 */
public class Base64AndPictureUtil {

    /**	将图片置灰 ---  使用现成的类
     * @param originalImage
     * @return
     */
    public static BufferedImage getGrayImage(BufferedImage originalImage){
        BufferedImage grayImage;
        int imageWidth = originalImage.getWidth();
        int imageHeight = originalImage.getHeight();
        //TYPE_3BYTE_BGR -->  表示一个具有 8 位 RGB 颜色分量的图像，
        // 对应于 Windows 风格的 BGR 颜色模型，具有用 3 字节存储的 Blue、Green 和 Red 三种颜色。
        grayImage = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_3BYTE_BGR);
        ColorConvertOp cco = new ColorConvertOp(ColorSpace.getInstance(ColorSpace.CS_GRAY), null);
        cco.filter(originalImage, grayImage);
        return grayImage;
    }

    /**
     *  base64字符串转换成图片
     *  @param imgStr		base64字符串
     *  @param imgFilePath	图片存放路径
     *  @return
     */
    public static boolean Base64ToImage(String imgStr,String imgFilePath) {
        // 对字节数组字符串进行Base64解码并生成图片
        if (StringUtils.isEmpty(imgStr))
            // 图像数据为空
            return false;
        BASE64Decoder decoder = new BASE64Decoder();
        try {
            // Base64解码
            byte[] b = decoder.decodeBuffer(imgStr);
            for (int i = 0; i < b.length; ++i) {
                if (b[i] < 0) {
                    // 调整异常数据
                    b[i] += 256;
                }
            }
            OutputStream out = new FileOutputStream(imgFilePath);
            out.write(b);
            out.flush();
            out.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 本地图片转换成base64字符串
     * @param imgFile	图片本地路径
     * @return base64字符串
     */
    public static String ImageToBase64(String imgFile) {
        // 将图片文件转化为字节数组字符串，并对其进行Base64编码处理
        InputStream in;
        byte[] data = null;
        // 读取图片字节数组
        try {
            in = new FileInputStream(imgFile);
            data = new byte[in.available()];
            in.read(data);
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        // 对字节数组Base64编码
        BASE64Encoder encoder = new BASE64Encoder();
        // 返回Base64编码过的字节数组字符串
        return encoder.encode(data);
    }
}
