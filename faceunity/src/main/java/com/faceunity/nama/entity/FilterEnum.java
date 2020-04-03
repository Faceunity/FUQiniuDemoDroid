package com.faceunity.nama.entity;

import com.faceunity.nama.R;

import java.util.ArrayList;

/**
 * 美颜滤镜列表
 *
 * @author Richie on 2019.12.20
 */
public enum FilterEnum {
    /**
     * 滤镜
     */
    origin("origin", R.drawable.nature, "原图"),

    delta("delta", R.drawable.delta, "delta"),
    electric("electric", R.drawable.electric, "electric"),
    slowlived("slowlived", R.drawable.slowlived, "slowlived"),
    tokyo("tokyo", R.drawable.tokyo, "tokyo"),
    warm("warm", R.drawable.warm, "warm"),

    ziran("ziran", R.drawable.origin, "自然"),
    danya("danya", R.drawable.qingxin, "淡雅"),
    fennen("fennen", R.drawable.shaonv, "粉嫩"),
    qingxin("qingxin", R.drawable.ziran, "清新"),
    hongrun("hongrun", R.drawable.hongrun, "红润");

    private String name;
    private int iconId;
    private String description;

    FilterEnum(String name, int iconId, String description) {
        this.name = name;
        this.iconId = iconId;
        this.description = description;
    }

    public Filter filter() {
        return new Filter(name, iconId, description);
    }

    public static ArrayList<Filter> getFilters() {
        FilterEnum[] filterEnums = FilterEnum.values();
        ArrayList<Filter> filters = new ArrayList<>(filterEnums.length);
        for (FilterEnum f : filterEnums) {
            filters.add(f.filter());
        }
        return filters;
    }
}
