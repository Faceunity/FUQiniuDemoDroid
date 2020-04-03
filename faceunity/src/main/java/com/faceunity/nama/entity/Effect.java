package com.faceunity.nama.entity;

/**
 * 道具贴纸
 *
 * @author Richie on 2019.12.20
 */
public class Effect {
    private int iconId;
    private String filePath;
    private String description;

    public Effect(int iconId, String filePath, String description) {
        this.iconId = iconId;
        this.filePath = filePath;
        this.description = description;
    }

    public int getIconId() {
        return iconId;
    }

    public void setIconId(int iconId) {
        this.iconId = iconId;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return "Effect{" +
                "filePath='" + filePath + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}
