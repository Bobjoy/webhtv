package com.fongmi.android.tv.api.subscription;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * 订阅门禁授权码的纯函数接缝：输入"已经拿到手的 codes.txt 原文"，输出是否命中。
 * 规则与 webhtv-sub/codes.txt 约定一致：一行一个 4 位数字，# 开头为注释，比对前 trim。
 */
public class SubscriptionCodesTest {

    private static final String CODES = """
            # webhtv 订阅功能授权码
            0473
            5108

              7742 \s
            0473
            9x9x
            """;

    @Test
    public void anyCodeInFileMatches() throws Exception {
        assertTrue(SubscriptionCodes.check(CODES, "0473"));
        assertTrue(SubscriptionCodes.check(CODES, "5108"));
        assertFalse(SubscriptionCodes.check(CODES, "1234"));
    }

    @Test
    public void surroundingWhitespaceIsTrimmedOnBothSides() throws Exception {
        assertTrue(SubscriptionCodes.check("  0473  \n", " 0473 "));
    }

    @Test
    public void commentAndBlankLinesAreNotCodes() throws Exception {
        assertFalse(SubscriptionCodes.check("# 0473\n\n   \n5108", "0473"));
        assertTrue(SubscriptionCodes.check("# 0473\n\n   \n5108", "5108"));
    }

    @Test
    public void duplicateLinesAreHarmless() throws Exception {
        assertTrue(SubscriptionCodes.check("0473\n0473\n0473", "0473"));
    }

    @Test
    public void leadingZeroIsPartOfTheCode() throws Exception {
        assertFalse(SubscriptionCodes.check(CODES, "473"));
        assertFalse(SubscriptionCodes.check("4730", "0473"));
    }

    @Test
    public void onlyFourDigitsCountAsCodes() throws Exception {
        assertTrue(SubscriptionCodes.check("9x9x\n0473", "0473"));
        assertThrows(Exception.class, () -> SubscriptionCodes.check("04731\n123\nabcd", "0473"));
    }

    @Test
    public void inputShorterThanFourDigitsNeverMatches() throws Exception {
        assertFalse(SubscriptionCodes.check(CODES, ""));
        assertFalse(SubscriptionCodes.check(CODES, "47"));
        assertFalse(SubscriptionCodes.check(CODES, "04731"));
    }

    @Test
    public void emptyNullOrHtmlBodyIsReportedAsUnavailable() {
        assertThrows(Exception.class, () -> SubscriptionCodes.check("", "0473"));
        assertThrows(Exception.class, () -> SubscriptionCodes.check(null, "0473"));
        assertThrows(Exception.class, () -> SubscriptionCodes.check("<html><body>0473</body></html>", "0473"));
        assertThrows(Exception.class, () -> SubscriptionCodes.check("# 只有注释\n0473 后面缺行\n", "0473"));
    }

    @Test
    public void nullInputNeverMatches() throws Exception {
        assertFalse(SubscriptionCodes.check(CODES, null));
    }
}
