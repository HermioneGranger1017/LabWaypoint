package com.itheima.utils;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

/**
 * 富文本 HTML 清洗工具：
 * - 仅允许 p / br / h1-h4 / strong / em / u / ul / ol / li / blockquote / pre / code / a / img；
 * - 去掉 script / style / 所有 on* 事件属性 和 data URL；
 * - img.src 仅接受 /uploads/tutorial/ / http(s)://。
 */
public final class HtmlSanitizer {

    private HtmlSanitizer() {}

    private static final Safelist SAFELIST = Safelist.none()
            .addTags("p", "br", "h1", "h2", "h3", "h4", "strong", "em", "u",
                    "ul", "ol", "li", "blockquote", "pre", "code", "a", "img")
            .addAttributes("a", "href", "title", "target")
            .addAttributes("img", "src", "alt", "title")
            .addAttributes("pre", "class")
            .addAttributes("code", "class")
            .addAttributes("span", "class")
            .addProtocols("a", "href", "http", "https")

            .preserveRelativeLinks(true);

    /** 清洗 HTML，并强制把非法 img.src 删除。 */
    public static String clean(String html) {
        if (html == null || html.isBlank()) return "";
        Document doc = Jsoup.parseBodyFragment(html);
        // 扫描所有 img，删除不合规 src
        for (Element img : doc.select("img")) {
            String src = img.absUrl("src");
            String rawSrc = img.attr("src");
            if (rawSrc == null || rawSrc.isBlank()) { img.remove(); continue; }
            if (!isAllowedImageSrc(src, rawSrc)) { img.remove(); continue; }
            img.removeAttr("onerror").removeAttr("onload");
        }
        // 删除 span 上不合规 style/事件（保留 class）
        for (Element el : doc.select("[style],[onclick],[onmouseover],[onerror],[onload],[onmouseout]")) {
            el.removeAttr("style").removeAttr("onclick").removeAttr("onmouseover")
              .removeAttr("onerror").removeAttr("onload").removeAttr("onmouseout");
        }
        String safe = Jsoup.clean(doc.body().html(), SAFELIST);
        return safe == null ? "" : safe;
    }

    private static boolean isAllowedImageSrc(String abs, String raw) {
        // 优先看 raw，因为 Jsoup 可能还没解析成绝对路径
        String check = raw != null && !raw.isBlank() ? raw : abs;
        if (check == null) return false;
        if (check.toLowerCase().startsWith("data:")) return false;
        if (check.startsWith("/uploads/tutorial/")) return true;
        if (check.toLowerCase().startsWith("http://")) return true;
        if (check.toLowerCase().startsWith("https://")) return true;
        return false;
    }

    /** 从清洗后的 HTML 中提取所有合规 img.src。 */
    public static List<String> extractImageUrls(String html) {
        List<String> urls = new ArrayList<>();
        if (html == null || html.isBlank()) return urls;
        Document doc = Jsoup.parseBodyFragment(html);
        Elements imgs = doc.select("img[src]");
        for (Element img : imgs) {
            String src = img.attr("src");
            if (isAllowedImageSrc(src, src)) urls.add(src);
        }
        return urls;
    }

    /** 剥离所有 HTML 标签的纯文本，用于长度校验。 */
    public static String stripHtml(String html) {
        if (html == null) return "";
        String text = Jsoup.parse(html).text();
        return text == null ? "" : text;
    }

    /** 把 List<String> 序列化为 JSON 数组字符串。 */
    public static String toJsonArray(List<String> list) {
        if (list == null || list.isEmpty()) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append('"').append(list.get(i).replace("\"", "\\\"")).append('"');
        }
        sb.append(']');
        return sb.toString();
    }
}