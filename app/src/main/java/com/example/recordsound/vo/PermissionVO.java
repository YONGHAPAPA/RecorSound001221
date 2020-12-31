package com.example.recordsound.vo;

public class PermissionVO {

    private Integer requestId;
    private String permission;
    private Integer isGranted;

    public PermissionVO(Integer requestId, String permission, Integer isGranted){
        this.requestId = requestId;
        this.permission = permission;
        this.isGranted = isGranted;
    }

    public void setRequestId(Integer requestId) {
        this.requestId = requestId;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    public void setIsGranted(Integer isGranted) {
        this.isGranted = isGranted;
    }

    public Integer getRequestId() {
        return requestId;
    }

    public String getPermission() {
        return permission;
    }

    public Integer getIsGranted() {
        return isGranted;
    }
}
