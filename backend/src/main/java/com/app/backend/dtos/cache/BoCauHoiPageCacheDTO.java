package com.app.backend.dtos.cache;

import lombok.*;

import java.io.Serializable;
import java.util.List;

/**
 * Wrapper DTO để cache kết quả tìm kiếm bộ câu hỏi
 * Bao gồm cả danh sách và tổng số phần tử (để hỗ trợ phân trang)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BoCauHoiPageCacheDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * Danh sách bộ câu hỏi
     */
    private List<BoCauHoiCacheDTO> content;
    
    /**
     * Tổng số phần tử trong DB (để tính phân trang)
     */
    private long totalElements;
    
    /**
     * Tổng số trang
     */
    private int totalPages;
    
    /**
     * Số thứ tự trang hiện tại (0-based)
     */
    private int pageNumber;
    
    /**
     * Số phần tử trên mỗi trang
     */
    private int pageSize;
}
