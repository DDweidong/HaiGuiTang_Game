package com.weidong.gamebackend.model;


public class ChatForm {
    private String memoryId; //对话id
    private String message; //用户问题

    public ChatForm() {
    }

    public String getMemoryId() {
        return memoryId;
    }

    public void setMemoryId(String memoryId) {
        this.memoryId = memoryId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public ChatForm(String memoryId, String message) {
        this.memoryId = memoryId;
        this.message = message;
    }
}

