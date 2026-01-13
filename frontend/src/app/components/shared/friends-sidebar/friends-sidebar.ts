import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { RouterModule } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';
import { FriendSummaryResponse } from '../../../responses/banbe/friend_summary_response';
import { ChatBoxService } from '../../../services/chat-box.service';
import { FriendService } from '../../../services/friend.service';
import {environment} from '../../../environments/environment';

@Component({
  selector: 'app-friends-sidebar',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './friends-sidebar.html',
  styleUrl: './friends-sidebar.scss',
})
export class FriendsSidebarComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();

  friends: FriendSummaryResponse[] = [];
  onlineFriends: FriendSummaryResponse[] = [];
  offlineFriends: FriendSummaryResponse[] = [];

  loading = false;
  searchQuery = '';
  showSearch = false;

  readonly imageBaseURL = `${environment.apiBaseUrl}/users/profile-images/`;

  constructor(private friendService: FriendService, private chatBoxService: ChatBoxService) {}

  ngOnInit(): void {
    this.loadFriends();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadFriends(): void {
    this.loading = true;
    this.friendService
      .getFriends()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (res) => {
          this.friends = res.data || [];
          this.filterFriends();
          this.loading = false;
        },
        error: () => {
          this.loading = false;
        },
      });
  }

  filterFriends(): void {
    const query = this.searchQuery.toLowerCase().trim();
    let filtered = this.friends;

    if (query) {
      filtered = this.friends.filter((f) => f.ho_ten.toLowerCase().includes(query));
    }

    this.onlineFriends = filtered.filter((f) => f.trang_thai === 'TRUC_TUYEN');
    this.offlineFriends = filtered.filter((f) => f.trang_thai !== 'TRUC_TUYEN');
  }

  onSearch(event: Event): void {
    this.searchQuery = (event.target as HTMLInputElement).value;
    this.filterFriends();
  }

  toggleSearch(): void {
    this.showSearch = !this.showSearch;
    if (!this.showSearch) {
      this.searchQuery = '';
      this.filterFriends();
    }
  }

  openChat(friend: FriendSummaryResponse): void {
    // Open chat box popup
    this.chatBoxService.openChat(friend);
  }

  getAvatarUrl(friend: FriendSummaryResponse): string {
    if (friend.avatar_url) {
      return this.imageBaseURL + friend.avatar_url;
    }
    return 'assets/images/default-avatar.png';
  }
}
