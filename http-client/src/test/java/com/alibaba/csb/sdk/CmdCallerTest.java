package com.alibaba.csb.sdk;

import com.alibaba.csb.sdk.internel.HttpClientHelper;
import com.alibaba.csb.sdk.security.SpasSigner;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 旧的命令行方式调用HttpCaller
 */
public class CmdCallerTest {
    //@Test
    public void testMultiTimes() throws Exception {
        int iCount = 2000;
        Thread[] ts = new Thread[iCount];

        for (int i = 0; i < iCount; i++) {
            ts[i] = new Thread(new Runnable() {
                public void run() {
                    try {
                        String ret = HttpCaller.doPost("http://10.125.60.151:8086?name=abc", "PING", "vcsb", null);
                        System.out.println(ret);
                    } catch (HttpCallerException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    //CmdCaller.main(new String[] { "post", "http://10.125.60.151:8086?name=abc", "PING", "vcsb", "ak", "sk" });
                }
            });
            ts[i].start();
        }

        for (int i = 0; i < iCount; i++) {
            ts[i].join();
        }
        System.out.print("done");
    }

    @Test
    public void testCGet() {
        System.setProperty("hfile", "/tmp/hfile.prop");
        CmdCaller.main(new String[]{"cget", "http://abc:123?a=b&c=abc", "testget", "1.0.0", "ak", "sk"});
    }

    @Test
    public void testCPost() {
        CmdCaller.main(new String[]{"cpost", "http://abc:123?a=b&c=abc", "testpost", "1.0.0", "ak", "sk", "abc"});
    }

    @Test
    public void testUsage() {
        CmdCaller.main(new String[]{"abc", "http://abc:123?a=b&c=abc", "testpost", "1.0.0", "ak", "sk"});
    }

    @Test
    public void testVersion() {
        CmdCaller.main(new String[]{"-v", "http://abc:123?a=b&c=abc", "testpost", "1.0.0", "ak", "sk"});
    }

    @Test
    public void testSign() {
        HashMap<String, List<String>> urlParamsMap = new HashMap<String, List<String>>();
        StringBuilder signDiagnostic = new StringBuilder();
        urlParamsMap.put("MT_B8_ECCTOWMS_REQUEST", Arrays.asList("abc"));
        Map<String, String> headerParamsMap = HttpClientHelper.newParamsMap(urlParamsMap,
                "After_sale_information_query", "1.0.0", "6073f9ba68ff49baa0add7dbe9521a6a",
                "1LZd4oOltnNkA6DMXWDayzDa3Lc=", true, true, null
                , signDiagnostic, null, null, SpasSigner.SigningAlgorithm.HmacSHA1.name());

        System.out.println("headerParamsMap=" + headerParamsMap);
        System.out.println("signDiagnostic=" + signDiagnostic);
    }
}
