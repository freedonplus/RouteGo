package com.rg.common.sms;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rg.common.exception.BusinessException;
import com.rg.common.exception.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

/**
 * apihz（接口盒子）短信代发送客户端。
 * <p>
 * 文档：{@code GET https://cn.apihz.cn/api/sms/dfapi.php?id=&key=&phone=&code=}
 * <ul>
 *   <li>成功：{@code {"code":200,"msg":"发送成功。","qid":"..."}}</li>
 *   <li>失败：{@code {"code":400,"msg":"通讯秘钥错误。"}}</li>
 * </ul>
 * 同一手机号 30 秒内限 3 条、1 小时 20 条、1 天 20 条，因此调用前必须做本地限流，
 * 不要把限流完全交给服务商。
 */
public class ApihzSmsClient implements SmsClient {

    private static final Logger log = LoggerFactory.getLogger(ApihzSmsClient.class);

    private final SmsProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public ApihzSmsClient(SmsProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        int timeout = (int) properties.getTimeout().toMillis();
        factory.setConnectTimeout(timeout);
        factory.setReadTimeout(timeout);
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    @Override
    public String sendVerificationCode(String phone, String code) {
        URI uri = UriComponentsBuilder.fromUriString(properties.getEndpoint())
                .queryParam("id", properties.getId())
                .queryParam("key", properties.getKey())
                .queryParam("phone", phone)
                .queryParam("code", code)
                .queryParamIfPresent("dkey",
                        java.util.Optional.ofNullable(properties.getDkey()).filter(s -> !s.isBlank()))
                .build()
                .encode()
                .toUri();

        String body;
        try {
            body = restClient.get().uri(uri).retrieve().body(String.class);
        } catch (RestClientException e) {
            log.error("短信网关调用失败 phone={}", maskPhone(phone), e);
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "短信服务繁忙，请稍后重试");
        }

        JsonNode node = parse(body);
        int status = node.path("code").asInt(-1);
        if (status != 200) {
            String msg = node.path("msg").asText("短信发送失败");
            log.warn("短信发送失败 phone={} code={} msg={}", maskPhone(phone), status, msg);
            // 直接把服务商的提示回给前端，便于排查（如「通讯秘钥错误」「余额不足」）
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "短信发送失败：" + msg);
        }
        String qid = node.path("qid").asText(null);
        log.info("短信验证码已发送 phone={} qid={}", maskPhone(phone), qid);
        return qid;
    }

    private JsonNode parse(String body) {
        if (body == null || body.isBlank()) {
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "短信服务返回为空");
        }
        try {
            return objectMapper.readTree(body);
        } catch (Exception e) {
            log.error("短信网关响应解析失败: {}", body, e);
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "短信服务响应异常");
        }
    }

    static String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return "***";
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
