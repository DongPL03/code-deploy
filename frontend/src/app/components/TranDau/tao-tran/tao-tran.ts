import {Component, OnInit, ViewChild} from '@angular/core';
import {Base} from '../../base/base';
import {FormsModule, NgForm} from '@angular/forms';
import {CommonModule} from '@angular/common';
import {TaoTranDauDTO} from '../../../dtos/tran-dau/taotran-dto';
import Swal from 'sweetalert2';
import {ResponseObject} from '../../../responses/response-object';
import {TranDauResponse} from '../../../responses/trandau/trandau-response';
import {environment} from '../../../environments/environment';
import {PageResponse} from '../../../responses/page-response';
import {BoCauHoiResponse} from '../../../responses/bocauhoi/bocauhoi-response';

@Component({
  selector: 'app-tao-tran',
  imports: [CommonModule, FormsModule],
  templateUrl: './tao-tran.html',
  styleUrl: './tao-tran.scss',
  standalone: true
})
export class TaoTran extends Base implements OnInit {
  @ViewChild('createForm') createForm!: NgForm;
  saving = false;

  form: TaoTranDauDTO = new TaoTranDauDTO({});

  protected readonly environment = environment;

  isModalOpen = false;

  boCauHoiOptions: BoCauHoiResponse[] = [];

  keywordBoCauHoi = '';

  current_mode: 'THUONG' | 'XEP_HANG' = 'THUONG';
  loading_sets = false;

  preview_difficulty_counts: any = {};
  preview_type_counts: any = {};

  preview_loading = false;
  preview_questions: {
    loai_noi_dung: 'VAN_BAN' | 'HINH_ANH' | 'AM_THANH' | 'VIDEO';
    do_kho: 'DE' | 'TRUNG_BINH' | 'KHO';
    duong_dan_tep: string | null | undefined;
    noi_dung: string;
    id: number;
  }[] = [];
  preview_total = 0;

  showBoCauHoiDropdown = false;
  showLuatDropdown = false;

  ngOnInit() {
    this.form.loai_tran_dau = 'THUONG';
    this.form.luat_tinh_diem = 'CO_BAN';
    this.current_mode = 'THUONG';
    this.loadBoCauHoiForMode('THUONG');
  }

  loadBoCauHoiForMode(mode: 'THUONG' | 'XEP_HANG') {
    this.loading_sets = true;
    this.current_mode = mode;

    const obs = mode === 'XEP_HANG'
      ? this.bocauHoiService.getRankedBattleSets()
      : this.bocauHoiService.getCasualBattleSets();

    obs.subscribe({
      next: (res: ResponseObject<BoCauHoiResponse[] | PageResponse<BoCauHoiResponse>>) => {
        this.loading_sets = false;
        if (Array.isArray(res.data)) {
          this.boCauHoiOptions = res.data as BoCauHoiResponse[];
        } else {
          const page = res.data as PageResponse<BoCauHoiResponse> | undefined;
          this.boCauHoiOptions = page?.items ?? [];
        }
      },
      error: () => {
        this.loading_sets = false;
        this.boCauHoiOptions = [];
        Swal.fire('Lỗi', 'Không tải được danh sách bộ câu hỏi', 'error');
      }
    });
  }

  onLoaiTranDauChange(mode: 'THUONG' | 'XEP_HANG') {
    if (this.current_mode === mode && this.boCauHoiOptions.length > 0) {
      return;
    }
    this.form.loai_tran_dau = mode;
    this.form.bo_cau_hoi_id = 0 as any;
    this.loadBoCauHoiForMode(mode);
  }

  onBoCauHoiChanged(bo_cau_hoi_id: number) {
    this.form.bo_cau_hoi_id = bo_cau_hoi_id;
    if (!bo_cau_hoi_id) {
      this.preview_questions = [];
      this.preview_total = 0;
      return;
    }
    this.fetchPreview(bo_cau_hoi_id);
    this.isModalOpen = true;
  }

  closeModal() {
    this.isModalOpen = false;
    this.fetchPreview(0);
  }

  fetchPreview(bo_cau_hoi_id: number) {
    // console.log('Fetching preview for BoCauHoi ID:', bo_cau_hoi_id);
    this.preview_loading = true;
    this.preview_questions = [];
    this.preview_difficulty_counts = {};
    this.preview_type_counts = {};
    this.preview_total = 0;

    this.cauHoiService.getByBoCauHoi(bo_cau_hoi_id).subscribe({
      next: (res) => {
        const page = res.data!;
        this.preview_questions = page.items.map(q => ({
          id: q.id,
          noi_dung: q.noi_dung,
          do_kho: q.do_kho,
          loai_noi_dung: q.loai_noi_dung,
          duong_dan_tep: q.duong_dan_tep
        }));
        this.preview_total = page.items.length;

        this.preview_difficulty_counts = page.items.reduce((acc, q) => {
          const key = q.do_kho;
          acc[key] = (acc[key] || 0) + 1;
          return acc;
        }, {} as any);

        this.preview_type_counts = page.items.reduce((acc, q) => {
          const key = q.loai_noi_dung;
          acc[key] = (acc[key] || 0) + 1;
          return acc;
        }, {} as any);

        this.preview_loading = false;
      },
      error: () => {
        this.preview_loading = false;
      }
    });
  }

  onSubmit() {
    if (!this.form.cong_khai && (!this.form.ma_pin || !this.form.ma_pin.trim())) {
      Swal.fire('Thiếu PIN', 'Phòng riêng tư bắt buộc nhập mã PIN', 'info').then(r => {
      });
      return;
    }

    if (this.form.gioi_han_nguoi_choi < 2 || this.form.gioi_han_nguoi_choi > 30) {
      Swal.fire('Sai giới hạn người chơi', 'Giới hạn người chơi phải từ 2–30', 'info').then(r => {
      });
      return;
    }

    if (this.form.gioi_han_thoi_gian_cau_giay < 10) {
      Swal.fire('Thời gian mỗi câu quá thấp', 'Tối thiểu 10 giây mỗi câu', 'info').then(r => {
      });
      return;
    }

    this.saving = true;
    this.tranDauService.createBattle(this.form).subscribe({
      next: (res: ResponseObject<TranDauResponse>) => {
        this.saving = false;
        const data = res.data!;
        Swal.fire(
          'Tạo phòng thành công',
          `Mã phòng: ${data.ma_phong}${data.cong_khai ? '' : ' — PIN: ' + (data.ma_pin ?? '')}`,
          'success'
        ).then(() => {
          this.router.navigate(['/tran-dau/phong', data.id]).then(r => {
          });
        });
      },
      error: (e) => {
        this.saving = false;
        Swal.fire('Không thể tạo phòng', e?.error?.message || 'Thử lại sau', 'error').then(r => {
        });
      }
    });
  }

  getTieuDeBoCauHoi(): string {
    const id = this.form?.bo_cau_hoi_id;
    if (id === null || id === undefined) {
      return 'Chưa chọn';
    }
    const boCauHoi = this.boCauHoiOptions.find(b => b.id === id);
    return boCauHoi?.tieu_de || 'Không tìm thấy';
  }

  cancel() {
    this.router.navigateByUrl('/tran-dau/pending').then(r => {
    });
  }

  toggleBoCauHoiDropdown() {
    if (this.loading_sets) return; // Đang tải thì không cho mở
    this.showBoCauHoiDropdown = !this.showBoCauHoiDropdown;
    this.showLuatDropdown = false; // Đóng cái kia nếu đang mở
  }

  selectBoCauHoi(id: number) {
    this.form.bo_cau_hoi_id = id;
    this.showBoCauHoiDropdown = false;
    this.onBoCauHoiChanged(id); // Gọi hàm preview cũ của bạn
  }

  getSelectedBoCauHoiName(): string {
    if (!this.form.bo_cau_hoi_id) return '— Chọn bộ câu hỏi —';
    const selected = this.boCauHoiOptions.find(b => b.id === this.form.bo_cau_hoi_id);
    return selected ? selected.tieu_de : '— Chọn bộ câu hỏi —';
  }

  toggleLuatDropdown() {
    this.showLuatDropdown = !this.showLuatDropdown;
    this.showBoCauHoiDropdown = false;
  }

  selectLuat(luat: 'CO_BAN' | 'THUONG_TOC_DO') {
    this.form.luat_tinh_diem = luat;
    this.showLuatDropdown = false;
  }

  getSelectedLuatName(): string {
    switch (this.form.luat_tinh_diem) {
      case 'CO_BAN': return 'Điểm cơ bản';
      case 'THUONG_TOC_DO': return 'Điểm cơ bản + Thưởng tốc độ';
      default: return '-- Chọn luật --';
    }
  }

  closeAllDropdowns() {
    this.showBoCauHoiDropdown = false;
    this.showLuatDropdown = false;
  }
}
