package com.rg.common.ip;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Optional;

/**
 * apihz IP 归属地客户端。
 * <p>
 * 文档：{@code GET https://cn.apihz.cn/api/ip/chaapi.php?id=&key=&ip=&td=0}，
 * 成功返回 {@code {"code":200,"guo":"中国","sheng":"四川省","shi":"绵阳市","isp":"中国联通",...}}，
 * 失败返回 {@code {"code":400,"msg":"..."}}。
 */
public class ApihzIpLocationClient implements IpLocationClient {

    private static final Logger log = LoggerFactory.getLogger(ApihzIpLocationClient.class);

    private final IpLocationProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public ApihzIpLocationClient(IpLocationProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        int timeout = (int) properties.getTimeout().toMillis();
        factory.setConnectTimeout(timeout);
        factory.setReadTimeout(timeout);
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    @Override
    public IpLocation lookup(String ip) {
        URI uri = UriComponentsBuilder.fromUriString(properties.getEndpoint())
                .queryParam("id", properties.getId())
                .queryParam("key", properties.getKey())
                .queryParam("ip", ip)
                .queryParamIfPresent("td", Optional.ofNullable(properties.getTd()))
                .build()
                .encode()
                .toUri();
        String body;
        try {
            body = restClient.get().uri(uri).retrieve().body(String.class);
        } catch (RestClientException e) {
            log.warn("IP 归属地查询失败 ip={} reason={}", ip, e.getMessage());
            return null;
        }
        if (body == null || body.isBlank()) {
            return null;
        }
        try {
            JsonNode node = objectMapper.readTree(body);
            if (node.path("code").asInt(-1) != 200) {
                log.warn("IP 归属地查询返回错误 ip={} msg={}", ip, node.path("msg").asText());
                return null;
            }
            return new IpLocation(
                    node.path("guo").asText(null),
                    node.path("sheng").asText(null),
                    node.path("shi").asText(null),
                    node.path("qu").asText(null),
                    node.path("isp").asText(null),
                    node.path("msg").asText(null));
        } catch (Exception e) {
            log.warn("IP 归属地响应解析失败 ip={} body={}", ip, body);
            return null;
        }
    }
}
