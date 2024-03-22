package top.lucki.lottery.common.utils;

import cn.hutool.crypto.SecureUtil;
import lombok.extern.slf4j.Slf4j;
import top.lucki.lottery.common.exception.BaseException;

import javax.servlet.http.HttpServletRequest;

/**
 * sql注入处理工具类
 */
@Slf4j
public class SqlInjectionUtil {
    /**
     * sign 用于表字典加签的盐值【SQL漏洞】
     */
    private final static String TABLE_DICT_SIGN_SALT = "lijiukeji";
    private final static String xssStr = "'|and |exec |insert |select |delete |update |drop |count |chr |mid |master |truncate |char |declare |;|or |+";

    /*
     * 针对表字典进行额外的sign签名校验（增加安全机制）
     * @param dictCode:
     * @param sign:
     * @param request:
     * @Return: void
     */
    public static void checkDictTableSign(String dictCode, String sign, HttpServletRequest request) {
        //表字典SQL注入漏洞,签名校验
        String accessToken = request.getHeader("X-Access-Token");
        String signStr = dictCode + SqlInjectionUtil.TABLE_DICT_SIGN_SALT + accessToken;
        String javaSign = SecureUtil.md5(signStr);
        if (!javaSign.equals(sign)) {
            log.error("表字典，SQL注入漏洞签名校验失败 ：" + sign + "!=" + javaSign + ",dictCode=" + dictCode);
            throw new BaseException("无权限访问！");
        }
        log.info(" 表字典，SQL注入漏洞签名校验成功！sign=" + sign + ",dictCode=" + dictCode);
    }


    /**
     * sql注入过滤处理，遇到注入关键字抛异常
     *
     * @param value
     * @return
     */
    public static void filterContent(String value) {
        if (value == null || "".equals(value)) {
            return;
        }
        // 统一转为小写
        value = value.toLowerCase();
        String[] xssArr = xssStr.split("\\|");
        for (int i = 0; i < xssArr.length; i++) {
            if (value.indexOf(xssArr[i]) > -1) {
                log.error("请注意，存在SQL注入关键词---> {}", xssArr[i]);
                log.error("请注意，值可能存在SQL注入风险!---> {}", value);
                throw new RuntimeException("请注意，值可能存在SQL注入风险!--->" + value);
            }
        }
        return;
    }

    /**
     * sql注入过滤处理，遇到注入关键字抛异常
     *
     * @param values
     * @return
     */
    public static void filterContent(String[] values) {
        String[] xssArr = xssStr.split("\\|");
        for (String value : values) {
            if (value == null || "".equals(value)) {
                return;
            }
            // 统一转为小写
            value = value.toLowerCase();
            for (int i = 0; i < xssArr.length; i++) {
                if (value.indexOf(xssArr[i]) > -1) {
                    log.error("请注意，存在SQL注入关键词---> {}", xssArr[i]);
                    log.error("请注意，值可能存在SQL注入风险!---> {}", value);
                    throw new RuntimeException("请注意，值可能存在SQL注入风险!--->" + value);
                }
            }
        }
        return;
    }

    /**
     * @param value
     * @return
     * @特殊方法(不通用) 仅用于字典条件SQL参数，注入过滤
     */
    @Deprecated
    public static void specialFilterContent(String value) {
        String specialXssStr = " exec | insert | select | delete | update | drop | count | chr | mid | master | truncate | char | declare |;|+|";
        String[] xssArr = specialXssStr.split("\\|");
        if (value == null || "".equals(value)) {
            return;
        }
        // 统一转为小写
        value = value.toLowerCase();
        for (int i = 0; i < xssArr.length; i++) {
            if (value.indexOf(xssArr[i]) > -1 || value.startsWith(xssArr[i].trim())) {
                log.error("请注意，存在SQL注入关键词---> {}", xssArr[i]);
                log.error("请注意，值可能存在SQL注入风险!---> {}", value);
                throw new RuntimeException("请注意，值可能存在SQL注入风险!--->" + value);
            }
        }
        return;
    }

}
