package com.alibaba.csb.security.spi;

import com.alibaba.csb.sdk.security.SortedParamList;
import com.alibaba.csb.sdk.security.SpasSigner;

/**
 * 客户端请求签名接口
 */
public interface SignService {
    /**
     * 客户端请求参数进行签名处理
     *
     * @param paramNodeList 已排序的待签名请求参数key=values键值对列表
     * @param accessKey     进行签名的凭证识别码
     * @param secretKey     进行签名的安全码
     * @return 客户端签名串
     */
    String generateSignature(final SortedParamList paramNodeList, final String accessKey, final String secretKey);

    /**
     * 客户端请求参数进行签名处理
     *
     * @param paramNodeList 已排序的待签名请求参数key=values键值对列表
     * @param accessKey     进行签名的凭证识别码
     * @param secretKey     进行签名的安全码
     * @param algorithm     签名算法
     * @return 客户端签名串
     */
    String generateSignature(final SortedParamList paramNodeList, final String accessKey, final String secretKey, SpasSigner.SigningAlgorithm algorithm);
}
