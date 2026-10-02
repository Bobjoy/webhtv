package com.fongmi.android.tv.api.subscription;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 启动口令门禁的授权码表：{@code codes.txt} 一行一个 4 位数字，{@code #} 开头为注释，比对前 trim。
 * 码表明文公开、比对发生在客户端，作用是防误入而不是访问控制（ADR-0008 修订）；
 * 一个可用码都没有时抛异常，让上层能把它与「码不对」分成两类提示。
 */
public class SubscriptionCodes {

    public static final String URL = "https://raw.githubusercontent.com/Bobjoy/webhtv-sub/main/codes.txt";

    private static final Pattern CODE = Pattern.compile("\\d{4}");

    public static boolean check(String content, String input) throws Exception {
        Set<String> codes = new HashSet<>();
        for (String line : (content == null ? "" : content).split("\\R")) {
            String value = line.trim();
            if (CODE.matcher(value).matches()) codes.add(value);
        }
        if (codes.isEmpty()) throw new Exception("没有解析出任何授权码");
        return input != null && codes.contains(input.trim());
    }
}
