import {Component, OnInit} from '@angular/core';
import {CommonModule} from '@angular/common';
import {Base} from '../../base/base';
import {UserSummaryResponse} from '../../../responses/nguoidung/user-summary-response';
import {LichSuTranDauResponse} from '../../../responses/trandau/lichsutrandau';
import {ResponseObject} from '../../../responses/response-object';
import {PageResponse} from '../../../responses/page-response';
import {environment} from '../../../environments/environment';


@Component({
  selector: 'app-ho-so-nguoi-choi',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './ho-so-nguoi-choi.html',
  styleUrl: './ho-so-nguoi-choi.scss'
})
export class HoSoNguoiChoi extends Base implements OnInit {

  user_id!: number;

  loading_summary = false;
  loading_history = false;

  summary?: UserSummaryResponse | null;
  history_items: LichSuTranDauResponse[] = [];
  history_page = 0;
  history_limit = 10;
  history_total_pages = 0;
  readonly imageBaseURL = `${environment.apiBaseUrl}/users/profile-images/`;


  ngOnInit() {
    this.route.paramMap.subscribe(params => {
      const idParam = params.get('id');
      if (idParam) {
        this.user_id = Number(idParam);
        this.loadSummary();
        this.loadHistory(0);
      }
    });
  }

  loadSummary() {
    this.loading_summary = true;
    this.userService.getUserSummary(this.user_id).subscribe({
      next: (res: ResponseObject<UserSummaryResponse>) => {
        this.summary = res.data ?? null;
        if (this.summary) {
          // Tính lại tỉ lệ thắng cho chắc chắn
          const total = this.summary.tong_tran || 0;
          this.summary.ti_le_thang = total > 0 ? (this.summary.so_tran_thang / total) : 0;
        }
        this.loading_summary = false;
      },
      error: () => this.loading_summary = false
    });
  }

  loadHistory(page: number) {
    this.loading_history = true;
    this.tranDauService.getUserHistory(this.user_id, page, this.history_limit)
      .subscribe({
        next: (res: ResponseObject<PageResponse<LichSuTranDauResponse>>) => {
          const data = res.data!;
          this.history_items = data.items ?? [];
          this.history_page = data.currentPage ?? 0;
          this.history_total_pages = data.totalPages ?? 0;
          this.loading_history = false;
        },
        error: () => this.loading_history = false
      });
  }

  changeHistoryPage(p: number) {
    if (p < 0 || p >= this.history_total_pages) {
      return;
    }
    this.loadHistory(p);
  }

  isMe(): boolean {
    const me = this.userService.getUserResponseFromLocalStorage();
    return !!me && me.id === this.user_id;
  }

  getAvatar(user: UserSummaryResponse): string {
    if (user.avatar_url) {
      return this.imageBaseURL + user.avatar_url;
    }
    return `https://ui-avatars.com/api/?name=${encodeURIComponent(user.ho_ten)}&background=random&color=fff&size=128`;
  }

  mapTierLabel(tier: string | null | undefined): string {
    const t = (tier || '').toUpperCase();
    switch (t) {
      case 'CAO_THU': return 'Cao thủ';
      case 'KIM_CUONG': return 'Kim cương';
      case 'BACH_KIM': return 'Bạch kim';
      case 'VANG': return 'Vàng';
      case 'BAC': return 'Bạc';
      case 'DONG': return 'Đồng';
      default: return 'Tập sự';
    }
  }
}
