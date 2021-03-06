package com.alibaba.csb.sdk.security;

import com.alibaba.csb.sdk.CsbSDKConstants;
import com.alibaba.csb.sdk.SdkLogger;
import com.alibaba.csb.security.spi.SignService;
import com.alibaba.csb.security.spi.SignServiceRuntime;

import java.util.*;

/**
 * SignUtil for signing http parameters
 *
 * @author liaotian.wq 2017年1月20日
 */
public class SignUtil {
    private static final Random random = new Random(System.currentTimeMillis());

    /**
     * 参数签名处理并放回生成的http-header Map信息
     *
     * @param paramsMap          待签名的请求参数key=values键值对
     * @param apiName            CSB服务名
     * @param version            CSB服务版本
     * @param accessKey          accessKey, 在后端认证系统(如:DAuth), 通过accessKey获取进行签名的securityKey
     * @param securityKey        securityKey 进行签名的安全码
     * @param timestampFlag      是否当前系统的时间戳参与签名
     * @param nonceFlag          是否防重放随机数参与签名
     * @param signDiagnosticInfo 返回参与签名的所有key vlaue 信息， 这是一个诊断相关的返回串
     * @param extSignHeaders     附加的参与签名的key=value键值对  @return 将生成的签名及一些关键字段以key=value的方式返回
     * @param signDiagnosticInfo 签名诊断
     * @param signImpl           签名实现类
     * @param vefifySignImpl     验证签名类
     * @param signAlgothrim      签名算法
     */
    public static Map<String, String> newParamsMap(final Map<String, List<String>> paramsMap, String apiName, String version
            , String accessKey, String securityKey, boolean timestampFlag, boolean nonceFlag
            , final Map<String, String> extSignHeaders, final StringBuilder signDiagnosticInfo
            , String signImpl, String vefifySignImpl, String signAlgothrim) {

        Map<String, List<String>> newParamsMap = new HashMap<String, List<String>>();
        Map<String, String> headerParamsMap = new HashMap<String, String>();

        if (paramsMap != null) {
            newParamsMap.putAll(paramsMap);
        }

        // put apiName
        if (apiName != null) {
            newParamsMap.put(CsbSDKConstants.API_NAME_KEY, Arrays.asList(apiName));
            headerParamsMap.put(CsbSDKConstants.API_NAME_KEY, apiName);
        }
        // put version
        if (version != null) {
            newParamsMap.put(CsbSDKConstants.VERSION_KEY, Arrays.asList(version));
            headerParamsMap.put(CsbSDKConstants.VERSION_KEY, version);
        }

        // put timestamp
        String timestampStr = System.getProperty("timestamp");
        if (timestampStr == null) {
            Long ts = System.currentTimeMillis();
            timestampStr = ts.toString();
        }

        if (nonceFlag && CsbSDKConstants.isNonceEnabled) {
            // put nonce
            String nonceStr = System.getProperty("nonce");
            if (nonceStr == null) {
                Long nonce = random.nextLong();
                nonceStr = nonce.toString();
            }
            newParamsMap.put(CsbSDKConstants.NONCE_KEY, Arrays.asList(nonceStr));
            headerParamsMap.put(CsbSDKConstants.NONCE_KEY, nonceStr);
        }


        if (timestampFlag) {
            newParamsMap.put(CsbSDKConstants.TIMESTAMP_KEY, Arrays.asList(timestampStr));
            headerParamsMap.put(CsbSDKConstants.TIMESTAMP_KEY, timestampStr);
        }

        if (extSignHeaders != null) {
            for (Map.Entry<String, String> kv : extSignHeaders.entrySet()) {
                newParamsMap.put(kv.getKey(), Arrays.asList(kv.getValue()));
                headerParamsMap.put(kv.getKey(), kv.getValue());
            }
        }

        vefifySignImpl = SignServiceRuntime.pickVerifySignImplName(vefifySignImpl);
        if (vefifySignImpl != null) {
            newParamsMap.put(CsbSDKConstants.VERIFY_SIGN_IMPL_KEY, Arrays.asList(vefifySignImpl));
            headerParamsMap.put(CsbSDKConstants.VERIFY_SIGN_IMPL_KEY, vefifySignImpl);
        }

        // last step, put accessKey & the generated signature
        if (accessKey != null) {
            headerParamsMap.put(CsbSDKConstants.ACCESS_KEY, accessKey);
            newParamsMap.put(CsbSDKConstants.ACCESS_KEY, Arrays.asList(accessKey));
            // ensure the signature and security are not sent !!
            newParamsMap.remove(CsbSDKConstants.SIGNATURE_KEY);
            newParamsMap.remove(CsbSDKConstants.SECRET_KEY);
            long currT = System.currentTimeMillis();

            SignService signService = SignServiceRuntime.pickSignService(signImpl);
            //Add an extra http-header to tell what signimpl is being used on client side
            signImpl = signService.getClass().getCanonicalName();
            if (signImpl.equals(DefaultSignServiceImpl.class.getCanonicalName()) == false) { //设置非默认的签名类http头
                newParamsMap.put(CsbSDKConstants.SIGN_IMPL_KEY, Arrays.asList(signImpl));
                headerParamsMap.put(CsbSDKConstants.SIGN_IMPL_KEY, signImpl);
            }

            SortedParamList paramNodeList = convertMultiValueParams(newParamsMap);

            SpasSigner.SigningAlgorithm signingAlgorithm = signAlgothrim == null ? SpasSigner.SigningAlgorithm.HmacSHA1 : SpasSigner.SigningAlgorithm.valueOf(signAlgothrim);
            String signKey = signService.generateSignature(paramNodeList, accessKey, securityKey, signingAlgorithm);

            if (SdkLogger.isLoggable() || signDiagnosticInfo != null) {
                StringBuilder msg = new StringBuilder();
                msg.append("sign parameters:\n");
                boolean first = true;
                for (String key : newParamsMap.keySet()) {
                    if (!first) {
                        msg.append(", ");
                    }
                    msg.append(String.format("%s=%s", key, newParamsMap.get(key)));
                    first = false;

                }
                msg.append("===signature:" + signKey).append(", ").append("===costs time:" + (System.currentTimeMillis() - currT) + "ms");
                if (signDiagnosticInfo != null) {
                    signDiagnosticInfo.setLength(0);
                    signDiagnosticInfo.append(msg.toString());
                }

                if (SdkLogger.isLoggable()) {
                    SdkLogger.print(msg.toString());
                }
            }
            headerParamsMap.put(CsbSDKConstants.SIGNATURE_KEY, signKey);
        }

        return headerParamsMap;
    }

    /**
     * convert parameter to Signature requried ParamNode format
     *
     * @param map
     * @return
     */
    public static SortedParamList convertMultiValueParams(Map<String, List<String>> map) {
        SortedParamList pnList = new SortedParamList();
        if (map == null) {
            return pnList;
        }

        String key;
        for (Map.Entry<String, List<String>> entry : map.entrySet()) {
            key = entry.getKey();
            List<String> vlist = entry.getValue();
            if (vlist == null) {
                ParamNode node = new ParamNode(key, null);
                pnList.add(node);
            } else {
                for (String v : vlist) {
                    ParamNode node = new ParamNode(key, v);
                    pnList.add(node);
                }
            }
        }

        return pnList;
    }

    /**
     * convert parameter to Signature requried ParamNode format
     *
     * @param map
     * @return
     */
    private static SortedParamList convertSingleValueParms(Map<String, String> map) {
        SortedParamList sortedParamList = new SortedParamList();
        if (map != null) {
            for (Map.Entry<String, String> entry : map.entrySet()) {
                String key = entry.getKey();
                ParamNode node = new ParamNode(key, entry.getValue());
                sortedParamList.add(node);
            }
        }

        return sortedParamList;
    }


    /**
     * Signature single value parameter list with security key
     *
     * @param paramsMap
     * @param secretKey
     * @return
     */
    public static String sign(Map<String, String> paramsMap, String secretKey) {
        return sign(convertSingleValueParms(paramsMap), secretKey);
    }

    private static String sign(SortedParamList sortedParamList, String secretKey) {
        return SpasSigner.sign(sortedParamList, secretKey);
    }

    public static void warmup() {
        SignServiceRuntime.pickSignService(null).generateSignature(new SortedParamList(), "ak", "sk");
    }
}
