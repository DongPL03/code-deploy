package com.app.backend.responses.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response cho một item trong danh sách inbox (tin nhắn gần đây)
 * Hiển thị partner info + tin nhắn cuối + số chưa đọc
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatInboxItemResponse {
    private Long partnerId;
    private String partnerName;
    private String partnerAvatarUrl;
    private String lastMessage;
    private String lastTime;   // ISO format
    private Integer unreadCount;
}
