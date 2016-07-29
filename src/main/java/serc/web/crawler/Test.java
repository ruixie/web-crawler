package serc.web.crawler;

import java.net.CookieHandler;
import java.net.CookiePolicy;
import java.net.HttpCookie;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONPath;
import com.github.kevinsawicki.http.HttpRequest;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class Test {
    
    private static final String HOST = "";
    private static final String HOME_URL = HOST;
    private static final String TONGJI_URL = HOST + "tongji/%s.json?random=%s";
    private static final String COMPANY_URL = HOST + "company/%s.json";
    private static java.net.CookieManager manager = new java.net.CookieManager();
    static {
        manager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);  
        CookieHandler.setDefault(manager);
    }
    
    private static void getCookies() {
        HttpRequest httpRequest = HttpRequest.get(HOME_URL);
        if(!httpRequest.ok()) {
            System.err.println("get home page error:" + httpRequest.headers());
            return;            
        }
        System.out.println(httpRequest.headers());
    }

    public static String fromCharCode(Integer... codePoints) {
        StringBuilder builder = new StringBuilder(codePoints.length);
        for (int codePoint : codePoints) {
            builder.append(Character.toChars(codePoint));
        }
        return builder.toString();
    }
    
    private static String getToken(String id) throws URISyntaxException {
        String tongjiUrl = String.format(TONGJI_URL, id, new Date().getTime());
        HttpRequest httpRequest = HttpRequest.get(tongjiUrl);
        httpRequest.headers(ImmutableMap.of(
                "Content-Type", "application/json; charset=UTF-8", 
                "Tyc-From", "normal",
                "Accept", "application/json, text/javascript, */*; q=0.01")
                );
        if(!httpRequest.ok()) {
            System.err.println("get tongji info error:" + tongjiUrl);
            return null;
        }
        String setTokenChar = JSONPath.eval(JSON.parse(httpRequest.body()), "$.data.v").toString();
        String setTokenString = fromCharCode(Lists.newArrayList(setTokenChar.split(",")).stream().map(Integer::parseInt).toArray(Integer[]::new));
        String token = setTokenString.substring(setTokenString.indexOf("token=") + 6, setTokenString.indexOf(";path="));
        HttpCookie tokkenCookie = new HttpCookie("token", token);
        tokkenCookie.setDomain(".tianyancha.com");
        tokkenCookie.setPath("/");
        manager.getCookieStore().add(new URI(HOST), tokkenCookie);
        return token;
    }
    
    private static String getCompanyInfo(String id, String token) throws URISyntaxException {
        List<HttpCookie> cookies = manager.getCookieStore().get(new URI(HOST));
        for(HttpCookie cookie : cookies) {
            System.out.println(cookie.toString());
        }
        String url = String.format(COMPANY_URL, id);
        HttpRequest httpRequest = HttpRequest.get(url);
        Map<String, String> headers = Maps.newHashMap();
        headers.put("Accept", "application/json, text/javascript, */*; q=0.01");
        headers.put("Accept-Encoding", "gzip, deflate, sdch");
        headers.put("Accept-Language", "zh-CN,zh;q=0.8,en;q=0.6");
        headers.put("CheckError", "check");
        headers.put("Connection", "keep-alive");
        headers.put("Host", HOST);
        headers.put("Referer", HOST + "company/" + id);
        headers.put("Tyc-From", "normal");
        headers.put("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/51.0.2704.103 Safari/537.36");
        httpRequest.headers(headers);
        if(!httpRequest.ok()) {
            System.err.println("get company info error: [" + httpRequest.code() + "]" + url);
            System.err.println(httpRequest.headers());
            httpRequest.receive(System.err);
            return null;
        }
        return httpRequest.body();
    }
    
    public static void main(String[] args) throws URISyntaxException, InterruptedException {
        getCookies();
        for(Integer integer = 0; integer < 1; integer++) {
            String id = "22822";
            String token = getToken(id);
            String companyInfo = getCompanyInfo(id, token);
            System.out.println(companyInfo);            
            Thread.sleep(1000);
        }
    }

}
