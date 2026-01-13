import {Component, OnInit} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {Base} from '../base/base';
import {LeaderboardEntryResponse} from '../../responses/bangxephang/leaderboard-entry-response';
import {ChuDe} from '../../models/chude';
import {UserSummaryResponse} from '../../responses/nguoidung/user-summary-response';
import {LichSuTranDauResponse} from '../../responses/trandau/lichsutrandau';
import {RouterLink} from '@angular/router';
import {environment} from '../../environments/environment';

@Component({
  selector: 'app-bang-xep-hang',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './bang-xep-hang.html',
  styleUrl: './bang-xep-hang.scss'
})
export class BangXepHang extends Base implements OnInit {

  loading = true;
  page = 0;
  limit = 20;
  totalPages = 0;

  items: LeaderboardEntryResponse[] = [];
  chu_de_options: ChuDe[] = [];

  // Filter
  time_range: 'ALL' | 'WEEK' | 'MONTH' = 'ALL';
  selected_chu_de_id?: number;

  // Modal
  show_user_modal = false;
  user_loading = false;
  user_summary?: UserSummaryResponse | null;
  user_history_items: LichSuTranDauResponse[] = [];
  user_history_loading = false;

  me = this.userService.getUserResponseFromLocalStorage();

  readonly imageBaseURL = `${environment.apiBaseUrl}/users/profile-images/`;

  ngOnInit() {
    this.loadChuDe();
    this.loadPage(0);
  }

  loadChuDe() {
    this.chuDeService.getChuDe(0, 100).subscribe({
      next: (res) => this.chu_de_options = res.data || [],
      error: () => console.error('Lỗi tải chủ đề')
    });
  }

  loadPage(page: number) {
    if (page < 0) return;
    this.loading = true;

    // Gọi API Filter
    this.leaderboardService.getGlobal(
      page,
      this.limit,
      this.time_range,
      this.selected_chu_de_id
    ).subscribe({
      next: (res) => {
        const data = res.data;
        this.items = data?.items ?? [];
        console.log(this.items);
        this.page = data?.currentPage ?? 0;
        this.totalPages = data?.totalPages ?? 0;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.items = [];
      }
    });
  }

  setTimeRange(range: 'ALL' | 'WEEK' | 'MONTH') {
    this.time_range = range;
    this.loadPage(0); // Reset về trang đầu khi đổi filter
  }

  goToPage(p: number) {
    if (p >= 0 && p < this.totalPages) this.loadPage(p);
  }

  // --- HELPERS ---
  getAvatar(user: LeaderboardEntryResponse): string {
    if (user.anh_dai_dien) {
      return this.imageBaseURL + user.anh_dai_dien;
    }
    return `https://ui-avatars.com/api/?name=${encodeURIComponent(user.ho_ten)}&background=random`;
  }

  getAvatarSummary(avatarUrl: string | null | undefined, name: string = 'User'): string {
    if (avatarUrl) {
      return this.imageBaseURL + avatarUrl;
    }
    return `https://ui-avatars.com/api/?name=${encodeURIComponent(name)}&background=random`;
  }

  mapTierLabel(tier: string | null | undefined): string {
    const t = (tier || '').toUpperCase();
    switch (t) {
      case 'CAO_THU':
        return 'Cao thủ';
      case 'KIM_CUONG':
        return 'Kim cương';
      case 'BACH_KIM':
        return 'Bạch kim';
      case 'VANG':
        return 'Vàng';
      case 'BAC':
        return 'Bạc';
      case 'DONG':
        return 'Đồng';
      default:
        return 'Tập sự';
    }
  }

  // --- MODAL ---
  openUserModal(item: LeaderboardEntryResponse) {
    this.show_user_modal = true;
    this.user_loading = true;

    // Load summary
    this.userService.getUserSummary(item.user_id).subscribe({
      next: (res) => {
        this.user_summary = res.data;
        console.log(this.user_summary);
        if (this.user_summary) {
          // Tính toán tỉ lệ thắng
          const {so_tran_thang, tong_tran} = this.user_summary;
          this.user_summary.ti_le_thang = tong_tran > 0 ? so_tran_thang / tong_tran : 0;
        }
        this.user_loading = false;
        // Sau khi có summary, load history
        this.loadUserHistory(item.user_id);
      },
      error: () => this.user_loading = false
    });
  }

  loadUserHistory(userId: number) {
    this.user_history_loading = true;
    this.tranDauService.getUserHistory(userId, 0, 5).subscribe({
      next: (res) => {
        this.user_history_items = res.data?.items ?? [];
        this.user_history_loading = false;
      },
      error: () => this.user_history_loading = false
    });
  }

  closeUserModal() {
    this.show_user_modal = false;
    this.user_summary = null;
    this.user_history_items = [];
  }

  getProfileImage(filename: string | undefined | null): string {
    // Kiểm tra nếu có tên file thì trả về đường dẫn đầy đủ
    if (filename) {
      return this.imageBaseURL + filename;
    }
    return 'assets/images/default-avatar.png';
  }
}

