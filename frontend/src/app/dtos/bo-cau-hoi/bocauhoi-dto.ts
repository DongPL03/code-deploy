import { IsString } from 'class-validator';

export class BoCauHoiDTO {
  @IsString()
  tieu_de: string;

  @IsString()
  mo_ta?: string;

  chu_de_id: number;

  @IsString()
  che_do_hien_thi: string; // "CONG_KHAI" | "RIENG_TU"

  muon_tao_tra_phi?: boolean; // User muốn tạo bộ câu hỏi trả phí hay không

  constructor(data: Partial<BoCauHoiDTO>) {
    this.tieu_de = data.tieu_de ?? '';
    this.mo_ta = data.mo_ta ?? '';
    this.chu_de_id = data.chu_de_id ?? 0;
    this.che_do_hien_thi = data.che_do_hien_thi ?? 'CONG_KHAI';
    this.muon_tao_tra_phi = data.muon_tao_tra_phi ?? false;
  }
}
