package com.fongmi.android.tv.api.subscription;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.util.List;

/**
 * SubscriptionParser 的纯函数接缝：无 Android、无网络。
 * fixture 是社区清单的真实形态，走 classpath 读取，与 Gradle 工作目录无关。
 */
public class SubscriptionParserTest {

    @Test
    public void standardListKeepsOrderAndAllFourFields() throws Exception {
        List<SubscriptionParser.Item> items = SubscriptionParser.parse("""
                {"name":"我的清单","list":[
                  {"name":"主线","url":"http://a.top/1.json","logo":"http://a.top/logo.png","remark":"4K 点播"},
                  {"name":"只有名称和地址","url":"http://b.top"},
                  {"url":"http://c.top","remark":"名称缺失时用 host 兜底"}
                ]}""");
        assertEquals(3, items.size());
        assertEquals("主线", items.get(0).getName());
        assertEquals("http://a.top/1.json", items.get(0).getUrl());
        assertEquals("http://a.top/logo.png", items.get(0).getLogo());
        assertEquals("4K 点播", items.get(0).getRemark());
        assertEquals("", items.get(1).getLogo());
        assertEquals("", items.get(1).getRemark());
        assertEquals("c.top", items.get(2).getName());
    }

    @Test
    public void urlsKeyIsAcceptedAsWellAsList() throws Exception {
        List<SubscriptionParser.Item> items = SubscriptionParser.parse("{\"urls\":[{\"name\":\"4K线路\",\"url\":\"http://xhztv.top/4k.json\"}]}");
        assertEquals(1, items.size());
        assertEquals("4K线路", items.get(0).getName());
    }

    @Test
    public void topLevelArrayIsAccepted() throws Exception {
        List<SubscriptionParser.Item> items = SubscriptionParser.parse("[{\"url\":\"http://a.top\"}]");
        assertEquals(1, items.size());
        assertEquals("a.top", items.get(0).getName());
    }

    @Test
    public void jsonCommentsAndLeadingHeaderAreSkipped() throws Exception {
        List<SubscriptionParser.Item> items = SubscriptionParser.parse("""

                //xhz接口  请勿乱填
                /* 注释头 */
                   {"urls":[
                  {"url":"http://xhztv.top/4k.json","name":"4K线路"}, // xhz在线视频
                  {"url":"https://tv.菜妮丝.top","name":"菜妮丝"}
                ]}""");
        assertEquals(2, items.size());
        assertEquals("菜妮丝", items.get(1).getName());
    }

    @Test
    public void itemNameFallsBackToHostAndKeepsChineseDomainReadable() throws Exception {
        List<SubscriptionParser.Item> items = SubscriptionParser.parse("[{\"url\":\"https://tv.菜妮丝.top/path\"}]");
        assertEquals("tv.菜妮丝.top", items.get(0).getName());
    }

    @Test
    public void emptyUrlIsDroppedWithoutAffectingTheRest() throws Exception {
        List<SubscriptionParser.Item> items = SubscriptionParser.parse("{\"list\":[{\"name\":\"空地址条目要丢弃\",\"url\":\"\"},{\"name\":\"主线\",\"url\":\"http://a.top\"}]}");
        assertEquals(1, items.size());
        assertEquals("主线", items.get(0).getName());
    }

    @Test
    public void missingNeitherListNorUrlsIsAnError() {
        assertThrows(Exception.class, () -> SubscriptionParser.parse("{\"name\":\"空壳\",\"other\":1}"));
    }

    @Test
    public void brokenJsonNeverFallsBackToLineParsing() {
        assertThrows(Exception.class, () -> SubscriptionParser.parse("{ \"list\": [ {\"url\": \"http://a.top\", } "));
    }

    @Test
    public void htmlPageIsAnErrorNotAnEmptyList() {
        assertThrows(Exception.class, () -> SubscriptionParser.parse("<!DOCTYPE html>\n<html><body><p>接口地址：https://tv.菜妮丝.top</p></body></html>"));
    }

    @Test
    public void textLinesUseLabelAfterHashOrDoubleSlash() throws Exception {
        List<SubscriptionParser.Item> items = SubscriptionParser.parse("""
                直播源
                # 接口源

                http://xhztv.top/4k.json    #  4K线路
                http://itv.pro//tv/xhz.json    # 徐渣渣接口
                https://tv.菜妮丝.top    #  菜妮丝
                https://www.饭太硬.net/tv

                单仓：
                // 整行注释也跳过
                https://www.kefish.xyz/tv    名称在前   #   写法不认""");
        assertEquals(5, items.size());
        assertEquals("4K线路", items.get(0).getName());
        assertEquals("http://xhztv.top/4k.json", items.get(0).getUrl());
        assertEquals("徐渣渣接口", items.get(1).getName());
        assertEquals("http://itv.pro//tv/xhz.json", items.get(1).getUrl());
        assertEquals("菜妮丝", items.get(2).getName());
        assertEquals("www.饭太硬.net", items.get(3).getName());
        assertEquals("写法不认", items.get(4).getName());
    }

    @Test
    public void nonHttpTextWithoutAnyUrlIsAnError() {
        assertThrows(Exception.class, () -> SubscriptionParser.parse("这是一段没有地址的说明文字\n随便写点什么"));
    }

    @Test
    public void blankContentIsAnError() {
        assertThrows(Exception.class, () -> SubscriptionParser.parse("   \n  "));
        assertThrows(Exception.class, () -> SubscriptionParser.parse(null));
    }

    @Test
    public void fixtureStandardList() throws Exception {
        List<SubscriptionParser.Item> items = SubscriptionParser.parse(fixture("standard-list.txt"));
        assertEquals(3, items.size());
        assertEquals("主线", items.get(0).getName());
        assertEquals("https://xhztv.top/logo.png", items.get(0).getLogo());
        assertEquals("4K 点播", items.get(0).getRemark());
        assertEquals("只有名称和地址", items.get(1).getName());
        assertEquals("", items.get(1).getRemark());
        assertEquals("www.饭太硬.net", items.get(2).getName());
        assertEquals("名称缺失时用 host 兜底", items.get(2).getRemark());
    }

    @Test
    public void fixtureMultiStore() throws Exception {
        List<SubscriptionParser.Item> items = SubscriptionParser.parse(fixture("multi-store.txt"));
        assertEquals(3, items.size());
        assertEquals("4K线路", items.get(0).getName());
        assertEquals("菜妮丝", items.get(1).getName());
        assertEquals("饭太硬", items.get(2).getName());
    }

    @Test
    public void fixturePlainLines() throws Exception {
        List<SubscriptionParser.Item> items = SubscriptionParser.parse(fixture("plain-lines.txt"));
        assertEquals(5, items.size());
        assertEquals("http://xhztv.top/4k.json", items.get(0).getUrl());
        assertEquals("www.饭太硬.net", items.get(3).getName());
        assertTrue(items.get(0).getLogo().isEmpty());
    }

    @Test
    public void fixtureHtmlPage() throws Exception {
        assertThrows(Exception.class, () -> SubscriptionParser.parse(fixture("html-page.txt")));
    }

    private String fixture(String name) throws IOException {
        try (var in = getClass().getResourceAsStream("/subscription/" + name)) {
            return new String(in.readAllBytes());
        }
    }
}
