package com.app.backend.services.tinnhan;


import com.app.backend.components.ChatWsPublisher;
import com.app.backend.dtos.SendMessageRequest;
import com.app.backend.exceptions.DataNotFoundException;
import com.app.backend.models.NguoiDung;
import com.app.backend.models.TinNhan;
import com.app.backend.models.enums.LoaiTinNhan;
import com.app.backend.repositories.IKetBanRepository;
import com.app.backend.repositories.INguoiDungRepository;
import com.app.backend.repositories.ITinNhanRepository;
import com.app.backend.responses.PageResponse;
import com.app.backend.responses.chat.ChatInboxItemResponse;
import com.app.backend.responses.chat.ChatMessageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ChatService implements IChatService {

    private final ITinNhanRepository tinNhanRepository;
    private final INguoiDungRepository nguoiDungRepository;
    private final IKetBanRepository ketBanRepository;
    private final ChatWsPublisher chatWsPublisher;

    @Override
    @Transactional
    public ChatMessageResponse sendMessage(Long currentUserId, SendMessageRequest dto) throws Exception {
        Long receiverId = dto.receiver_id();

        if (receiverId == null) {
            throw new IllegalArgumentException("receiver_id không được để trống");
        }
        if (currentUserId.equals(receiverId)) {
            throw new IllegalArgumentException("Không thể tự chat với chính mình");
        }

        // 🔒 BẮT BUỘC PHẢI LÀ BẠN BÈ
        boolean friends = ketBanRepository.areFriends(currentUserId, receiverId);
        if (!friends) {
            throw new IllegalStateException("Chỉ có thể nhắn tin với người đã là bạn bè");
        }

        NguoiDung sender = nguoiDungRepository.findById(currentUserId)
                .orElseThrow(() -> new DataNotFoundException("Người gửi không tồn tại"));

        NguoiDung receiver = nguoiDungRepository.findById(receiverId)
                .orElseThrow(() -> new DataNotFoundException("Người nhận không tồn tại"));

        // Xác định loại tin nhắn
        LoaiTinNhan loaiTinNhan = dto.loai_tin_nhan() != null ? dto.loai_tin_nhan() : LoaiTinNhan.VAN_BAN;

        // Validate: văn bản phải có nội dung, media phải có url
        String content = dto.noi_dung() != null ? dto.noi_dung().trim() : "";
        if (loaiTinNhan == LoaiTinNhan.VAN_BAN && content.isBlank()) {
            throw new IllegalArgumentException("Nội dung tin nhắn không được để trống");
        }
        if ((loaiTinNhan == LoaiTinNhan.HINH_ANH || loaiTinNhan == LoaiTinNhan.TAP_TIN || loaiTinNhan == LoaiTinNhan.AM_THANH)
                && (dto.url_media() == null || dto.url_media().isBlank())) {
            throw new IllegalArgumentException("URL media không được để trống");
        }

        TinNhan entity = TinNhan.builder()
                .tranDau(null)
                .guiBoi(sender)
                .nhanBoi(receiver)
                .noiDung(content.isBlank() ? null : content)
                .loaiTinNhan(loaiTinNhan)
                .urlMedia(dto.url_media())
                .tenFile(dto.ten_file())
                .kichThuocFile(dto.kich_thuoc_file())
                .guiLuc(Instant.now())
                .build();

        TinNhan saved = tinNhanRepository.save(entity);

        // ✅ Response REST cho người gửi: la_toi = true
        ChatMessageResponse respForSender = ChatMessageResponse.fromEntity(saved, currentUserId);

        // 🔔 Push realtime qua WS (có thể dùng bản generic, FE tự so sánh gui_boi_id === currentUserId)
//        ChatMessageResponse wsPayload = ChatMessageResponse.fromEntity(saved);
//        chatWsPublisher.publishPrivateMessage(wsPayload);
        chatWsPublisher.publishNewMessage(saved);

        return respForSender;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ChatMessageResponse> getConversation(
            Long currentUserId,
            Long friendUserId,
            PageRequest pageRequest
    ) throws Exception {

        if (friendUserId == null) {
            throw new IllegalArgumentException("friend_user_id không được để trống");
        }
        if (currentUserId.equals(friendUserId)) {
            throw new IllegalArgumentException("Không thể lấy lịch sử chat với chính mình");
        }

        // 🔒 BẮT BUỘC PHẢI LÀ BẠN BÈ
        boolean friends = ketBanRepository.areFriends(currentUserId, friendUserId);
        if (!friends) {
            throw new IllegalStateException("Chỉ xem lịch sử chat với người đã là bạn bè");
        }

        Page<TinNhan> page = tinNhanRepository
                .findPrivateConversation(currentUserId, friendUserId, pageRequest);

        // ✅ map kèm currentUserId để set la_toi
        Page<ChatMessageResponse> mapped = page.map(
                m -> ChatMessageResponse.fromEntity(m, currentUserId)
        );
        return PageResponse.fromPage(mapped);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ChatMessageResponse> getMyInbox(
            Long currentUserId,
            PageRequest pageRequest
    ) {
        Page<TinNhan> page = tinNhanRepository.findLatestInbox(currentUserId, pageRequest);

        Page<ChatMessageResponse> mapped = page.map(
                m -> ChatMessageResponse.fromEntity(m, currentUserId)
        );
        return PageResponse.fromPage(mapped);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatInboxItemResponse> getInboxWithPartnerInfo(Long currentUserId, int limit) {
        // Lấy tất cả tin nhắn liên quan đến user, sắp xếp theo thời gian mới nhất
        PageRequest pageRequest = PageRequest.of(0, 100);
        Page<TinNhan> allMessages = tinNhanRepository.findLatestInbox(currentUserId, pageRequest);
        
        List<ChatInboxItemResponse> result = new ArrayList<>();
        Set<Long> processedPartners = new HashSet<>();
        
        for (TinNhan msg : allMessages.getContent()) {
            // Xác định partner (người còn lại trong cuộc trò chuyện)
            Long partnerId;
            NguoiDung partner;
            
            if (msg.getGuiBoi().getId().equals(currentUserId)) {
                partnerId = msg.getNhanBoi().getId();
                partner = msg.getNhanBoi();
            } else {
                partnerId = msg.getGuiBoi().getId();
                partner = msg.getGuiBoi();
            }
            
            // Nếu đã xử lý partner này rồi thì bỏ qua
            if (processedPartners.contains(partnerId)) {
                continue;
            }
            
            processedPartners.add(partnerId);
            
            // Tạm thời set unreadCount = 0, sau này có thể thêm field daDoc vào TinNhan
            ChatInboxItemResponse item = ChatInboxItemResponse.builder()
                    .partnerId(partnerId)
                    .partnerName(partner.getHoTen())
                    .partnerAvatarUrl(partner.getAvatarUrl())
                    .lastMessage(msg.getNoiDung())
                    .lastTime(msg.getGuiLuc().toString())
                    .unreadCount(0)
                    .build();
            
            result.add(item);
            
            if (result.size() >= limit) {
                break;
            }
        }
        
        return result;
    }
}


