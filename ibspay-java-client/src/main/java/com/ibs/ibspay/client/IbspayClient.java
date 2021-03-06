package com.ibs.ibspay.client;

import com.ibs.ibspay.common.model.*;
import com.ibs.ibspay.common.util.JsonUtils;
import com.ibs.ibspay.common.util.SignUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;

import java.io.IOException;

public class IbspayClient {
    private static final String HEADER_KEY = "x-ibspay-key";
    private static final String HEADER_NONCE = "x-ibspay-nonce";
    private static final String HEADER_ACTION = "x-ibspay-action";
    private static final String HEADER_SIGN = "x-ibspay-sign";

    private final String url;
    private final String accessKey;
    private final String accessSecret;
    private final HttpClient httpClient = HttpClients.createDefault();

    public IbspayClient(String baseUrl, String accessKey, String accessSecret) {
        this.url = baseUrl + "/open";
        this.accessKey = accessKey;
        this.accessSecret = accessSecret;
    }

    public InitiatePaymentResponse initiate(InitiatePaymentRequest request) {
        String json = JsonUtils.toJson(request);
        String response = post(json, RequestAction.INITIATE_PAYMENT);
        return JsonUtils.fromJson(response, InitiatePaymentResponse.class);
    }

    public ConfirmPaymentResponse confirm(ConfirmPaymentRequest request) {
        String json = JsonUtils.toJson(request);
        String response = post(json, RequestAction.CONFIRM_PAYMENT);
        return JsonUtils.fromJson(response, ConfirmPaymentResponse.class);
    }

    public InitiateRefundResponse refund(InitiateRefundRequest request) {
        String json = JsonUtils.toJson(request);
        String response = post(json, RequestAction.INITIATE_REFUND);
        return JsonUtils.fromJson(response, InitiateRefundResponse.class);
    }

    public QueryPaymentResponse queryPayment(QueryPaymentRequest request) {
        String json = JsonUtils.toJson(request);
        String response = post(json, RequestAction.QUERY_PAYMENT);
        return JsonUtils.fromJson(response, QueryPaymentResponse.class);
    }

    public QueryRefundResponse queryRefund(QueryRefundRequest request) {
        String json = JsonUtils.toJson(request);
        String response = post(json, RequestAction.QUERY_REFUND);
        return JsonUtils.fromJson(response, QueryRefundResponse.class);
    }

    private String post(String json, RequestAction action) {
        String nonce = System.currentTimeMillis() + "";
        String sign = SignUtils.sign(accessKey, action.toString(), nonce, json, accessSecret);
        Header[] headers = new BasicHeader[]{
                new BasicHeader(HEADER_KEY, accessKey),
                new BasicHeader(HEADER_NONCE, nonce),
                new BasicHeader(HEADER_ACTION, action.toString()),
                new BasicHeader(HEADER_SIGN, sign)};
        HttpPost httpPost = new HttpPost(url);
        httpPost.setHeaders(headers);
        httpPost.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));
        try {
            HttpResponse response = httpClient.execute(httpPost);
            int statusCode = response.getStatusLine().getStatusCode();
            String responseBody = EntityUtils.toString(response.getEntity());
            if (statusCode >= 200 && statusCode < 300) {
                return responseBody;
            } else {
                ErrorResponse errorResponse = JsonUtils.fromJson(responseBody, ErrorResponse.class);
                throw new IbspayException(errorResponse);
            }
        } catch (IOException e) {
            throw new IbspayException(e);
        }
    }

}
