export interface TranDauResponse {
  id: number;
  ten_phong: string;
  ma_phong: string;
  cong_khai: boolean;
  ma_pin?: string | null;
  gioi_han_nguoi_choi: number;
  gioi_han_thoi_gian_cau_giay: number;
  luat_tinh_diem: 'CO_BAN' | 'THUONG_TOC_DO';
  loai_tran_dau: 'THUONG' | 'XEP_HANG';
  trang_thai: 'CHO' | 'DANG_CHOI' | 'HOAN_THANH' | 'HUY';
  chu_phong_ten: string;
  bo_cau_hoi_id: number;
  bo_cau_hoi_tieu_de: string;
  tao_luc: string;
  bat_dau_luc?: string | null;
  ket_thuc_luc?: string | null;
  da_tham_gia: boolean;
}
