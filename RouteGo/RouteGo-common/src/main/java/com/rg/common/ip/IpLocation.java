package com.rg.common.ip;

import java.io.Serializable;

/**
 * IP 归属地结果。
 * <p>
 * 刻意使用可变 POJO 而不是 record：Redis 序列化器按 {@code NON_FINAL} 记录类型信息，
 * record / 不可变 List 属于 final 类不会写入类型标识，读回时会变成 LinkedHashMap。
 */
public class IpLocation implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 国家。 */
    private String country;
    /** 省。 */
    private String province;
    /** 市。 */
    private String city;
    /** 区县。 */
    private String district;
    /** 运营商。 */
    private String isp;
    /** 完整描述。 */
    private String text;

    public IpLocation() {
    }

    public IpLocation(String country, String province, String city,
                      String district, String isp, String text) {
        this.country = country;
        this.province = province;
        this.city = city;
        this.district = district;
        this.isp = isp;
        this.text = text;
    }

    /**
     * 用于列表展示的短描述：省 市 · 运营商。
     */
    public String shortText() {
        StringBuilder builder = new StringBuilder();
        if (province != null && !province.isBlank()) {
            builder.append(province);
        }
        if (city != null && !city.isBlank() && !city.equals(province)) {
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(city);
        }
        if (builder.isEmpty()) {
            return text == null ? "" : text;
        }
        if (isp != null && !isp.isBlank()) {
            builder.append(" · ").append(isp);
        }
        return builder.toString();
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getProvince() {
        return province;
    }

    public void setProvince(String province) {
        this.province = province;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getIsp() {
        return isp;
    }

    public void setIsp(String isp) {
        this.isp = isp;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
