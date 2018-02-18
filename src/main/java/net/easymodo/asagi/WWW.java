package net.easymodo.asagi;

import com.google.common.collect.ObjectArrays;
import net.easymodo.asagi.exception.ContentGetException;
import net.easymodo.asagi.exception.CfBicClearParseException;
import net.easymodo.asagi.exception.HttpGetException;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.annotation.ThreadSafe;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.impl.client.DecompressingHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;

import java.io.*;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.URLDecoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * This class extends the abstract class Board.
 * It provides basic functionality to fetch things over HTTP.
 * Boards that work over WWW should extend from this class.
 *
 * Fuuka notes:
 * Equivalent to: Board::WWW
 *
 * Implementation notes:
 * Uses Apache HttpComponents to provide functionality similar to Perl's LWP.
 **/
@ThreadSafe
public abstract class WWW extends Board {
    private static final Timer SLEEP_TIMER = new Timer();
    private static HttpClient httpClient;

    protected boolean throttleAPI = true;
    protected String throttleURL;
    protected long throttleMillisec = 0L;

    private static class Timer {
        long timer = 0;

        private long getTimer() {
            return timer;
        }

        private void setTimer(long timer) {
            this.timer = timer;
        }
    }

    private static final Pattern jschl_vcPattern;
    private static final Pattern passPattern;
    private static final Pattern jschl_answerPattern;

    static {
        HttpParams params = new BasicHttpParams();
        HttpConnectionParams.setSoTimeout(params, 5000);
        HttpConnectionParams.setConnectionTimeout(params, 5000);
        params.setParameter(CoreProtocolPNames.USER_AGENT, "Asagi/0.3.0");
        CookieHandler.setDefault(new CookieManager());

        PoolingClientConnectionManager pccm = new PoolingClientConnectionManager();
        pccm.setDefaultMaxPerRoute(20);
        pccm.setMaxTotal(100);
        httpClient = new DecompressingHttpClient(new DefaultHttpClient(pccm, params));

        String jschl_vc_pat = "name=\"jschl_vc\" \\s value=\"(.*?)\"";
        String pass_pat = "name=\"pass\" \\s value=\"(.*?)\"";
        String jschl_answer_pat = "(var \\s s,t,o,p,b,r,e,a,k,i,n,g,f, \\s .*a\\.value \\s = \\s parseInt\\(.*?\\)) \\s \\+ \\s t\\.length;";

        jschl_vcPattern = Pattern.compile(jschl_vc_pat, Pattern.COMMENTS | Pattern.DOTALL);
        passPattern = Pattern.compile(pass_pat, Pattern.COMMENTS | Pattern.DOTALL);
        jschl_answerPattern = Pattern.compile(jschl_answer_pat, Pattern.COMMENTS | Pattern.DOTALL);
    }

    private HttpResponse wget(String link, String referer) throws HttpGetException, CfBicClearParseException {
        return wget(link, referer, "");
    }

    private HttpResponse wget(String link, String referer, String lastMod) throws HttpGetException, CfBicClearParseException {
        HttpGet req = new HttpGet(link);
        if(referer == null || referer.equals("")) referer = link;
        if(referer != null && !referer.equals("")) req.setHeader("Referer", referer);
        if(lastMod != null && !lastMod.equals("")) req.setHeader("If-Modified-Since", lastMod);
        req.setHeader("Accept", "*/*");

        int statusCode;
        HttpResponse res = null;

        try {
            if(throttleAPI && req.getURI().getHost().equalsIgnoreCase(throttleURL)) {
                while(res == null) {
                    boolean okToGo = false;
                    long timer;
                    synchronized(SLEEP_TIMER) {
                        timer = SLEEP_TIMER.getTimer();
                        long now = System.currentTimeMillis();
                        if(timer == 0 || (now - timer) > throttleMillisec) {
                            okToGo = true;
                            SLEEP_TIMER.setTimer(now);
                        }
                    }
                    if(okToGo) {
                        res = httpClient.execute(req);
                    } else {
                        try {
                            Thread.sleep(System.currentTimeMillis() - timer);
                        } catch(InterruptedException e) {
                            // w
                        }
                    }
                }
            } else {
                res = httpClient.execute(req);
            }
            statusCode = res.getStatusLine().getStatusCode();
        } catch(IOException e) {
            // Automatically released in case of IOException
            throw new HttpGetException(e);
        } catch(RuntimeException e) {
            req.abort();
            throw new HttpGetException(e);
        }

        if(statusCode != 200) {
            if(statusCode == 503) {
                System.out.println("Cloudflare browser integrity check detected. Attempting to pass...");
                try {
                    String pageText = EntityUtils.toString(res.getEntity(), "UTF-8");
                    EntityUtils.consumeQuietly(res.getEntity());
                    Matcher mat = jschl_vcPattern.matcher(pageText);
                    if(!mat.find()) {
                        throw new CfBicClearParseException("Error parsing BIC page: Could not parse \"jschl_vc\"");
                    }
                    String jschl_vc = mat.group(1);
                    mat = passPattern.matcher(pageText);
                    if(!mat.find()) {
                        throw new CfBicClearParseException("Error parsing BIC page: Could not parse \"pass\"");
                    }
                    String pass = mat.group(1);
                    mat = jschl_answerPattern.matcher(pageText);
                    if(!mat.find()) {
                        throw new CfBicClearParseException("Error parsing BIC page: Could not parse \"jschl_answer\"");
                    }
                    String jschl_answer = mat.group(1);

                    jschl_answer = jschl_answer.replaceAll("a\\.value = (parseInt\\(.*?\\))", "$1;");
                    jschl_answer = jschl_answer.replaceAll("t = document\\.createElement.*?;", "");
                    jschl_answer = jschl_answer.replaceAll("t\\.innerHTML=\"<a href='/'>x</a>\";", "");
                    jschl_answer = jschl_answer.replaceAll("t = t\\.firstChild\\.href;r = t.match\\(/https\\?:\\\\/\\\\//\\)\\[0\\];", "");
                    jschl_answer = jschl_answer.replaceAll("t = t\\.substr\\(r\\.length\\); t = t\\.substr\\(0,t\\.length-1\\);", "");
                    jschl_answer = jschl_answer.replaceAll("a = document\\.getElementById\\('jschl-answer'\\);", "");
                    jschl_answer = jschl_answer.replaceAll("f = document\\.getElementById\\('challenge-form'\\);", "");
                    jschl_answer = jschl_answer.replaceAll("\\n|(\\s\\s)", "");
                    jschl_answer = String.format("console.log(require('vm').runInNewContext('%s', Object.create(null), {timeout: 5000}));", jschl_answer);

                    String[] args = {"node", "-e", jschl_answer};
                    jschl_answer = this.execCmd(args);
                    Long number = Long.parseLong(jschl_answer.trim()) + req.getURI().getHost().length();

                    Thread.sleep(4000);
                    return this.wget(String.format("%s://%s/cdn-cgi/l/chk_jschl?jschl_vc=%s&pass=%s&jschl_answer=%s",
                            req.getURI().getScheme(), req.getURI().getHost(), jschl_vc, pass, number.toString()), referer);
                } catch(IOException e) {
                    throw new HttpGetException(e);
                } catch(InterruptedException e) {
                    throw new HttpGetException(e);
                } catch(NumberFormatException e) {
                    throw new HttpGetException(e);
                }
            } else {
                // Needed to consume the rest of the response and release the connection
                EntityUtils.consumeQuietly(res.getEntity());
                throw new HttpGetException(res.getStatusLine().getReasonPhrase(), statusCode);
            }
        }

        return res;
    }

    /**
     * Gets an arbitrary HTTP link, returns an InputStream.
     *
     * @param link the HTTP link to fetch.
     * @return an InputStream with the content you desire.
     *         Make sure you always close it, or I'll hurt you.
     */
    public InputStream wget(String link) throws HttpGetException, CfBicClearParseException {
        try {
            return this.wget(link, "").getEntity().getContent();
        } catch(IOException e) {
            throw new HttpGetException(e);
        }
    }


    public String[] wgetText(String link, String lastMod) throws ContentGetException, CfBicClearParseException {
        // Throws ContentGetException on failure
        HttpResponse httpResponse = this.wget(link, "", lastMod);

        Header[] newLastModHead = httpResponse.getHeaders("Last-Modified");
        String newLastMod = null;
        if(newLastModHead.length > 0)
            newLastMod = newLastModHead[0].getValue();

        String pageText;
        try {
            pageText = EntityUtils.toString(httpResponse.getEntity(), "UTF-8");
        } catch(UnsupportedEncodingException e) {
            throw new ContentGetException("Unsupported encoding in HTTP response");
        } catch(IOException e) {
            throw new HttpGetException(e);
        } finally {
            // EntityUtils.toString should close the stream for us, but I DON'T EVEN KNOW
            EntityUtils.consumeQuietly(httpResponse.getEntity());
        }

        // we don't need to process empty content
        if(pageText == null || pageText.equals("")) {
            throw new ContentGetException("HTTP response returned empty body");
        }

        return new String[] {pageText, newLastMod};
    }

    public String doClean(String text) {
        if(text == null || text.isEmpty()) return null;

        // Replaces &#dddd; HTML entities with the proper Unicode character
        Matcher htmlEscapeMatcher = Pattern.compile("&#(\\d+);").matcher(text);
        StringBuffer textSb = new StringBuffer();
        while(htmlEscapeMatcher.find()) {
            String escape = (char) Integer.parseInt(htmlEscapeMatcher.group(1)) + "";
            htmlEscapeMatcher.appendReplacement(textSb, escape);
        }
        htmlEscapeMatcher.appendTail(textSb);
        text = textSb.toString();

        // Replaces some other HTML entities
        text = text.replaceAll("&gt;", ">");
        text = text.replaceAll("&lt;", "<");
        text = text.replaceAll("&quot;", "\"");
        text = text.replaceAll("&amp;", "&");

        // Trims whitespace at the beginning and end of lines
        text = text.replaceAll("\\s*$", "");
        text = text.replaceAll("^\\s*$", "");

        return text;
    }

    public String doCleanLink(String link) {
        if(link == null) return null;

        try {
            link = URLDecoder.decode(link, "UTF-8");
        } catch(UnsupportedEncodingException e) { throw new AssertionError("le broken JVM face"); }

        return link;
    }

    private String execCmd(String[] cmd) throws java.io.IOException {
        java.util.Scanner s = new java.util.Scanner(Runtime.getRuntime().exec(cmd).getInputStream()).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }
}
