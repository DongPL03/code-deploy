export interface NotificationResponse {
  thong_bao_id: number;
  nguoi_gui_id: number;
  nguoi_gui_ten: string;
  nguoi_gui_avatar_url: string | null;
  loai:
    | 'LOI_MOI_KET_BAN'
    | 'LOI_MOI_TRAN_DAU'
    | 'HE_THONG'
    | 'BO_CAU_HOI_DUYET'
    | 'BO_CAU_HOI_TU_CHOI'
    | 'DAT_THANH_TICH'
    | 'TANG_HANG'
    | string;
  noi_dung: string;
  metadata: string | null;
  da_doc: boolean;
  tao_luc: string;
}
