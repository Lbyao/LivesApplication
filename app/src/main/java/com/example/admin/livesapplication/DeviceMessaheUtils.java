package com.example.admin.livesapplication;

import com.google.gson.JsonObject;

/**
 * 设备UDP的通信类
 * Created by Administrator on 2018/1/30.
 */
public class DeviceMessaheUtils {

    /**
     * 获取data格式
     * @param action
     * @return
     */
    private static JsonObject getData(String property,int action) {
        JsonObject header = new JsonObject();
        header.addProperty(property, action);
        return header;
    }

    /**
     * 移动命令的json
     * @param action 移动方向
     * @return 返回移动的json串
     */
    public static String getMoveMessage(int action) {
        JsonObject object = new JsonObject();
        object.addProperty("type", 1003);
        object.addProperty("operator","request");
        object.add("data", getData("action",action));

        return object.toString();
    }

    /**
     * 模式切换的json
     * @param type 1001 自动切手动 1002 手动切自动
     * @param action 1 自动切手动 0 手动切自动
     * @return 返回模式切换的json串
     */
    public static String getModeChangeMessage(int type,int action){
        JsonObject object = new JsonObject();
        object.addProperty("type", type);
        object.addProperty("operator","request");
        object.add("data", getData("set",action));

        return object.toString();
    }
}
