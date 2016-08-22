package com.hiroshi.cimoc.core.source;

import com.hiroshi.cimoc.core.manager.SourceManager;
import com.hiroshi.cimoc.core.source.base.MangaParser;
import com.hiroshi.cimoc.model.Chapter;
import com.hiroshi.cimoc.model.Comic;
import com.hiroshi.cimoc.model.ImageUrl;
import com.hiroshi.cimoc.utils.MachiSoup;
import com.hiroshi.cimoc.utils.MachiSoup.Node;

import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import okhttp3.Request;

/**
 * Created by Hiroshi on 2016/8/9.
 */
public class Wnacg extends MangaParser {

    @Override
    public Request getSearchRequest(String keyword, int page) {
        String url = String.format(Locale.CHINA, "http://www.wnacg.com/albums-index-page-%d-sname-%s.html", page, keyword);
        return new Request.Builder().url(url).build();
    }

    @Override
    public List<Comic> parseSearch(String html, int page) {
        Node body = MachiSoup.body(html);
        List<Comic> list = new LinkedList<>();
        for (Node node : body.list("#bodywrap > div.grid > div > ul > li")) {
            String cid = node.attr("div.info > div.title > a", "href", "-|\\.", 3);
            String title = node.text("div.info > div.title > a");
            String cover = node.attr("div.pic_box > a > img", "data-original");
            String update = node.text("div.info > div.info_col").trim();
            update = MachiSoup.match("\\d{4}-\\d{2}-\\d{2}", update, 0);
            list.add(new Comic(SourceManager.SOURCE_WNACG, cid, title, cover, update, null, true));
        }
        return list;
    }

    @Override
    public Request getInfoRequest(String cid) {
        String url = String.format(Locale.CHINA, "http://www.wnacg.com/photos-index-aid-%s.html", cid);
        return new Request.Builder().url(url).build();
    }

    @Override
    public List<Chapter> parseInfo(String html, Comic comic) {
        List<Chapter> list = new LinkedList<>();
        Node body = MachiSoup.body(html);
        String length = body.text("#bodywrap > div > div.uwconn > label:eq(1)", 3, -2);
        int size = Integer.parseInt(length) % 12 == 0 ? Integer.parseInt(length) / 12 : Integer.parseInt(length) / 12 + 1;
        for (int i = 1; i <= size; ++i) {
            list.add(0, new Chapter("Ch" + i, String.valueOf(i)));
        }

        String title = body.text("#bodywrap > h2");
        String intro = body.text("#bodywrap > div > div.uwconn > p", 3);
        String author = body.text("#bodywrap > div > div.uwuinfo > p");
        String cover = body.attr("#bodywrap > div > div.uwthumb > img", "data-original");
        comic.setInfo(title, cover, "", intro, author, true);

        return list;
    }

    @Override
    public Request getImagesRequest(String cid, String path) {
        String url = String.format(Locale.CHINA, "http://www.wnacg.com/photos-index-page-%s-aid%s.html", path, cid);
        return new Request.Builder().url(url).build();
    }

    @Override
    public List<ImageUrl> parseImages(String html) {
        List<ImageUrl> list = new LinkedList<>();
        Node body = MachiSoup.body(html);
        for (Node node : body.list("#bodywrap > div.grid > div > ul > li > div.pic_box > a")) {
            String url = String.format(Locale.CHINA, "http://www.wnacg.com/%s", node.attr("href"));
            list.add(new ImageUrl(url, true));
        }
        return list;
    }

    @Override
    public Request getLazyRequest(String url) {
        return new Request.Builder().url(url).build();
    }

    @Override
    public String parseLazy(String html) {
        return MachiSoup.body(html).attr("#picarea", "src");
    }

}