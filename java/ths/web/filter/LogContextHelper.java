package ths.web.filter;

import static ths.commons.lang.Assert.*;
import static ths.commons.util.StringUtils.*;
import static java.util.Collections.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.MDC;

/**
 * 设置或清除logging MDC的工具类。
 * <p>
 * 该工具类可被用于valve和filter中。在请求开始的时候，调用<code>setLoggingContext()</code>，结束时调用
 * <code>clearLoggingContext()</code>。 如在<code>clearLoggingContext()</code>
 * 之前，多次调用<code>setLoggingContext()</code>，不会增加额外的开销。
 * </p>
 * <p>
 * 调用<code>setLoggingContext()</code>之后，SLF4j
 * MDC中会创建如下值，这些值可在logback或log4j配置文件中直接引用。
 * </p>
 * <table border="1" cellpadding="5">
 * <tr>
 * <td colspan="2"><strong>请求信息</strong></td>
 * </tr>
 * <tr>
 * <td>%X{method}</td>
 * <td>请求类型：GET、POST</td>
 * </tr>
 * <tr>
 * <td>%X{requestURL}</td>
 * <td>完整的URL</td>
 * </tr>
 * <tr>
 * <td>%X{requestURLWithQueryString}</td>
 * <td>完整的URL，含querydata</td>
 * </tr>
 * <tr>
 * <td>%X{requestURI}</td>
 * <td>不包括host信息的URL</td>
 * </tr>
 * <tr>
 * <td>%X{requestURIWithQueryString}</td>
 * <td>不包括host信息的URL，含querydata</td>
 * </tr>
 * <tr>
 * <td>%X{queryString}</td>
 * <td>Querydata</td>
 * </tr>
 * <tr>
 * <td>%X{cookies}</td>
 * <td>所有cookie的名称，以逗号分隔</td>
 * </tr>
 * <tr>
 * <td>%X{cookie.*}</td>
 * <td>指定cookie的值，例如：cookie.JSESSIONID</td>
 * </tr>
 * <tr>
 * <td colspan="2"><strong>客户端信息</strong></td>
 * </tr>
 * <tr>
 * <td>%X{remoteAddr}</td>
 * <td>用户IP地址</td>
 * </tr>
 * <tr>
 * <td>%X{remoteHost}</td>
 * <td>用户域名（也可能是IP地址）</td>
 * </tr>
 * <tr>
 * <td>%X{userAgent}</td>
 * <td>用户浏览器</td>
 * </tr>
 * <tr>
 * <td>%X{referrer}</td>
 * <td>上一个链接</td>
 * </tr>
 * </table>
 *
 */
public class LogContextHelper {
    public static final String MDC_METHOD = "method";
    public static final String MDC_REQUEST_URL = "requestURL";
    public static final String MDC_REQUEST_URL_WITH_QUERY_STRING = "requestURLWithQueryString";
    public static final String MDC_REQUEST_URI = "requestURI";
    public static final String MDC_REQUEST_URI_WITH_QUERY_STRING = "requestURIWithQueryString";
    public static final String MDC_QUERY_STRING = "queryString";
    public static final String MDC_REMOTE_ADDR = "remoteAddr";
    public static final String MDC_REMOTE_HOST = "remoteHost";
    public static final String MDC_USER_AGENT = "userAgent";
    public static final String MDC_REFERRER = "referrer";
    public static final String MDC_COOKIES = "cookies";
    public static final String MDC_COOKIE_PREFIX = "cookie.";
    private static final String FLAG_MDC_HAS_ALREADY_SET = "_flag_mdc_has_already_set";
    private final HttpServletRequest request;

    public LogContextHelper(HttpServletRequest request) {
        this.request = assertNotNull(request, "request");
    }

    public void setLoggingContext() {
        if (testAndSet()) {
            Map<String, String> mdc = getMDCCopy();

            populateMDC(mdc);
            setMDC(mdc);
        }
    }

    public void clearLoggingContext() {
        if (this == request.getAttribute(FLAG_MDC_HAS_ALREADY_SET)) {
            request.removeAttribute(FLAG_MDC_HAS_ALREADY_SET);
            clearMDC();
        }
    }

    protected void populateMDC(Map<String, String> mdc) {
        // GET or POST
        putMDC(mdc, MDC_METHOD, request.getMethod());

        // request URL：完整的URL
        StringBuffer requestURL = request.getRequestURL();
        String queryString = trimToNull(request.getQueryString());

        putMDC(mdc, MDC_REQUEST_URL, getRequestURL(requestURL, null));
        putMDC(mdc, MDC_REQUEST_URL_WITH_QUERY_STRING, getRequestURL(requestURL, queryString));

        // request URI：不包括host信息的URL
        String requestURI = request.getRequestURI();
        String requestURIWithQueryString = queryString == null ? requestURI : requestURI + "?" + queryString;

        putMDC(mdc, MDC_REQUEST_URI, requestURI);
        putMDC(mdc, MDC_REQUEST_URI_WITH_QUERY_STRING, requestURIWithQueryString);
        putMDC(mdc, MDC_QUERY_STRING, queryString);

        // client info
        putMDC(mdc, MDC_REMOTE_HOST, request.getRemoteHost());
        putMDC(mdc, MDC_REMOTE_ADDR, request.getRemoteAddr());

        // user agent
        putMDC(mdc, MDC_USER_AGENT, request.getHeader("User-Agent"));

        // referrer
        putMDC(mdc, MDC_REFERRER, request.getHeader("Referer"));

        // cookies
        Cookie[] cookies = request.getCookies();
        List<String> names = emptyList();

        if (cookies != null) {
            names = new ArrayList<String>(cookies.length);

            for (Cookie cookie : cookies) {
                names.add(cookie.getName());
                putMDC(mdc, MDC_COOKIE_PREFIX + cookie.getName(), cookie.getValue());
            }

            sort(names);
        }

        putMDC(mdc, MDC_COOKIES, names.toString());
    }

    private boolean testAndSet() {
        if (request.getAttribute(FLAG_MDC_HAS_ALREADY_SET) == null) {
            request.setAttribute(FLAG_MDC_HAS_ALREADY_SET, this);
            return true;
        }

        return false;
    }

    private String getRequestURL(StringBuffer requestURL, String queryString) {
        int length = requestURL.length();

        try {
            if (queryString != null) {
                requestURL.append('?').append(queryString);
            }

            return requestURL.toString();
        } finally {
            requestURL.setLength(length);
        }
    }

    private void putMDC(Map<String, String> mdc, String key, String value) {
        if (value != null) {
            mdc.put(key, value);
        }
    }

    @SuppressWarnings("unchecked")
    protected Map<String, String> getMDCCopy() {
        Map<String, String> mdc = MDC.getCopyOfContextMap();

        if (mdc == null) {
            mdc = new HashMap<String, String>();
        }

        return mdc;
    }

    protected void setMDC(Map<String, String> mdc) {
        MDC.setContextMap(mdc);
    }

    protected void clearMDC() {
        MDC.clear();
    }
}