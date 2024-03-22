package top.lucki.lottery.common.utils;

import lombok.extern.slf4j.Slf4j;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class CommonUtils {

    //中文正则
    private static final Pattern ZHONGWEN_PATTERN = Pattern.compile("[\u4e00-\u9fa5]");

    /**
     * 判断文件名是否带盘符，重新处理
     *
     * @param fileName
     * @return
     */
    public static String getFileName(String fileName) {
        //判断是否带有盘符信息
        // Check for Unix-style path
        int unixSep = fileName.lastIndexOf('/');
        // Check for Windows-style path
        int winSep = fileName.lastIndexOf('\\');
        // Cut off at latest possible point
        int pos = (winSep > unixSep ? winSep : unixSep);
        if (pos != -1) {
            // Any sort of path separator found...
            fileName = fileName.substring(pos + 1);
        }
        //替换上传文件名字的特殊字符
        fileName = fileName.replace("=", "").replace(",", "").replace("&", "").replace("#", "");
        //替换上传文件名字中的空格
        fileName = fileName.replaceAll("\\s", "");
        return fileName;
    }

    // java 判断字符串里是否包含中文字符
    public static boolean ifContainChinese(String str) {
        if (str.getBytes().length == str.length()) {
            return false;
        } else {
            Matcher m = ZHONGWEN_PATTERN.matcher(str);
            if (m.find()) {
                return true;
            }
            return false;
        }
    }


    /**
     * @param targetStr 目标字符串
     * @param str       查找字符串
     * @param index     第n次出现
     * @param order     顺序(大于0表示正序,小于0表示反序)
     * @return
     */
    public static String evaluate(String targetStr, String str, int index, int order) {
        /**
         * 当 str 不存在于 targetStr 时，不管是正序还是反序都返回原字符串
         * 当index大于 str 在  targetStr 中出现的次数，不管是正序还是反序都返回原字符串
         */
        String result = targetStr;//默认返回字符串为原字符串
        String pair_str = str;

        if (targetStr == null || targetStr.trim().length() == 0) {
            return result;
        }

        //当index=0时，返回空
        if (index == 0) {
            return "";
        }

        //判断是正序还是反序（大于等于0表示正序，小于0表示反序）
        if (order < 0) {
            targetStr = new StringBuffer(targetStr).reverse().toString();
            pair_str = new StringBuffer(pair_str).reverse().toString();
        }

        int beginIndex = 0;//用于匹配字符串的起始位置
        int count = 0; //记录字符串出现的次数
        while ((beginIndex = targetStr.indexOf(pair_str, beginIndex)) != -1) {
            count++;
            //当index与字符串出现次数相同时，开始返回结果
            if (count == index) {
                if (order < 0) {//反序时
                    targetStr = new StringBuffer(targetStr).reverse().toString();
                    result = targetStr.substring(targetStr.length() - beginIndex);
                } else {//正序时
                    result = targetStr.substring(0, beginIndex);
                }
                return result;
            }
            beginIndex = beginIndex + pair_str.length();//更改匹配字符串的起始位置
        }
        return result;
    }
}
