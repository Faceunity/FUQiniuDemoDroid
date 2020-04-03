package com.faceunity.nama.entity;

import com.faceunity.nama.R;

import java.util.ArrayList;

/**
 * 道具贴纸列表
 *
 * @author Richie on 2019.12.20
 */
public enum EffectEnum {
    /**
     * 道具贴纸
     */
    Effect_none(R.drawable.ic_delete_all, "", "none"),

    Effect_sdlu(R.drawable.sdlu, "normal/sdlu.bundle", "sdlu"),
    Effect_daisypig(R.drawable.daisypig, "normal/daisypig.bundle", "daisypig"),
    Effect_fashi(R.drawable.fashi, "normal/fashi.bundle", "fashi"),
    Effect_chri1(R.drawable.chri1, "normal/chri1.bundle", "chri1"),
    Effect_xueqiu_lm_fu(R.drawable.xueqiu_lm_fu, "normal/xueqiu_lm_fu.bundle", "xueqiu_lm_fu"),
    Effect_wobushi(R.drawable.wobushi, "normal/wobushi.bundle", "wobushi"),
    Effect_gaoshiqing(R.drawable.gaoshiqing, "normal/gaoshiqing.bundle", "gaoshiqing");

    private int iconId;
    private String filePath;
    private String description;

    EffectEnum(int iconId, String filePath, String description) {
        this.iconId = iconId;
        this.filePath = filePath;
        this.description = description;
    }

    public Effect effect() {
        return new Effect(iconId, filePath, description);
    }

    public static ArrayList<Effect> getEffects() {
        EffectEnum[] effectEnums = EffectEnum.values();
        ArrayList<Effect> effects = new ArrayList<>(effectEnums.length);
        for (EffectEnum e : effectEnums) {
            effects.add(e.effect());
        }
        return effects;
    }
}
