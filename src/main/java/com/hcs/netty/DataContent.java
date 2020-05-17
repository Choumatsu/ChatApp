package com.hcs.netty;



import java.io.Serializable;

public class DataContent implements Serializable {


    private static final long serialVersionUID = -7025349412992561544L;
    private Integer action;//动作类型

    private ChatMsg chatMsg;//聊天消息

    private String extand; //扩展字段

    public Integer getAction() {
        return action;
    }

    public void setAction(Integer action) {
        this.action = action;
    }

    public ChatMsg getChatMsg() {
        return chatMsg;
    }

    public void setChatMsg(ChatMsg chatMsg) {
        this.chatMsg = chatMsg;
    }

    public String getExtand() {
        return extand;
    }

    public void setExtand(String extand) {
        this.extand = extand;
    }
}
