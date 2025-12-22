package com.weidong.gamebackend.model;

import java.time.LocalDateTime;

public class TurtleSoup {
    private Long id;
    private String userId;
    private String roomId;
    private String title;
    private String solution;
    private LocalDateTime completedAt;

    // Getter
    public Long getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public String getRoomId() {
        return roomId;
    }

    public String getTitle() {
        return title;
    }

    public String getSolution() {
        return solution;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    // Setter
    public void setId(Long id) {
        this.id = id;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setSolution(String solution) {
        this.solution = solution;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    // toString（可选，但推荐，方便调试）
    @Override
    public String toString() {
        return "TurtleSoup{" +
                "id=" + id +
                ", userId='" + userId + '\'' +
                ", roomId='" + roomId + '\'' +
                ", title='" + title + '\'' +
                ", solution='" + solution + '\'' +
                ", completedAt=" + completedAt +
                '}';
    }

}