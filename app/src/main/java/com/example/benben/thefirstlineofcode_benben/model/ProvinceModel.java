package com.example.benben.thefirstlineofcode_benben.model;

/**
 * Created by benebn on 2016/5/31.
 * 关联天气 省级 Model
 */
public class ProvinceModel {
    private int id;
    private String provinceName;

    public String getProvinceCode() {
        return provinceCode;
    }

    public void setProvinceCode(String provinceCode) {
        this.provinceCode = provinceCode;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getProvinceName() {
        return provinceName;
    }

    public void setProvinceName(String provinceName) {
        this.provinceName = provinceName;
    }

    private String provinceCode;
}
