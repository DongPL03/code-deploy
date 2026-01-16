import { isPlatformBrowser } from '@angular/common';
import { inject, Injectable, PLATFORM_ID } from '@angular/core';

export type AudioTrack = 'home' | 'lobby' | 'battle' | 'victory' | 'defeat';

@Injectable({ providedIn: 'root' })
export class AudioService {
  private bgMusic: HTMLAudioElement | null = null;
  private sfx: HTMLAudioElement | null = null;
  private currentTrack: AudioTrack | null = null;
  private pendingTrack: AudioTrack | null = null; // Track chờ phát sau khi user tương tác
  private readonly isBrowser: boolean;
  private _userInteracted = false; // Đã có tương tác từ user chưa

  // Có thể thay đổi bằng file nhạc thực tế
  private readonly tracks: Record<AudioTrack, string> = {
    home: 'assets/audio/home-bgm.mp3',
    lobby: 'assets/audio/lobby-bgm.mp3',
    battle: 'assets/audio/battle-bgm.mp3',
    victory: 'assets/audio/victory.mp3',
    defeat: 'assets/audio/defeat.mp3',
  };

  // Volume mặc định (0-1)
  private _bgVolume = 0.3;
  private _sfxVolume = 0.5;
  private _isMuted = false;

  constructor() {
    this.isBrowser = isPlatformBrowser(inject(PLATFORM_ID));
    // Load settings từ localStorage (chỉ trên browser)
    if (this.isBrowser) {
      this.loadSettings();
      this.setupUserInteractionListener();
    }
  }

  /**
   * Lắng nghe sự kiện click/keypress đầu tiên để unlock audio
   */
  private setupUserInteractionListener(): void {
    const unlockAudio = () => {
      if (this._userInteracted) return;

      this._userInteracted = true;
      // console.log('🎵 User đã tương tác - Audio đã được unlock!');

      // Phát track đang chờ nếu có
      if (this.pendingTrack && !this._isMuted) {
        this.playBgMusic(this.pendingTrack);
        this.pendingTrack = null;
      }

      // Remove listeners sau khi đã unlock
      document.removeEventListener('click', unlockAudio);
      document.removeEventListener('keydown', unlockAudio);
      document.removeEventListener('touchstart', unlockAudio);
    };

    document.addEventListener('click', unlockAudio, { once: false });
    document.addEventListener('keydown', unlockAudio, { once: false });
    document.addEventListener('touchstart', unlockAudio, { once: false });
  }

  private loadSettings(): void {
    if (!this.isBrowser) return;

    const saved = localStorage.getItem('audio_settings');
    if (saved) {
      try {
        const settings = JSON.parse(saved);
        this._bgVolume = settings.bgVolume ?? 0.3;
        this._sfxVolume = settings.sfxVolume ?? 0.5;
        this._isMuted = settings.isMuted ?? false;
      } catch {
        // ignore
      }
    }
  }

  private saveSettings(): void {
    if (!this.isBrowser) return;

    localStorage.setItem(
      'audio_settings',
      JSON.stringify({
        bgVolume: this._bgVolume,
        sfxVolume: this._sfxVolume,
        isMuted: this._isMuted,
      })
    );
  }

  /**
   * Phát nhạc nền
   */
  playBgMusic(track: AudioTrack, loop = true): void {
    if (!this.isBrowser) return;

    // Nếu user chưa tương tác, lưu lại track để phát sau
    if (!this._userInteracted) {
      // console.log('🎵 Chờ user tương tác để phát nhạc:', track);
      this.pendingTrack = track;
      return;
    }

    // Nếu đang phát cùng track thì không làm gì
    if (this.currentTrack === track && this.bgMusic && !this.bgMusic.paused) {
      return;
    }

    // Dừng nhạc cũ và SFX nếu có
    this.stopBgMusic();
    this.stopSfx();

    const src = this.tracks[track];
    if (!src) return;

    this.bgMusic = new Audio(src);
    this.bgMusic.loop = loop;
    this.bgMusic.volume = this._isMuted ? 0 : this._bgVolume;
    this.currentTrack = track;

    this.bgMusic.play().catch((err) => {
      // Autoplay bị chặn - lưu pending track
      console.warn('🎵 Không thể phát nhạc:', err.message);
      this.pendingTrack = track;
    });
  }

  /**
   * Dừng nhạc nền
   */
  stopBgMusic(): void {
    if (!this.isBrowser) return;

    if (this.bgMusic) {
      this.bgMusic.pause();
      this.bgMusic.currentTime = 0;
      this.bgMusic = null;
      this.currentTrack = null;
    }
  }

  /**
   * Pause/Resume nhạc nền
   */
  toggleBgMusic(): void {
    if (!this.isBrowser || !this.bgMusic) return;

    if (this.bgMusic.paused) {
      this.bgMusic.play().catch(() => {});
    } else {
      this.bgMusic.pause();
    }
  }

  /**
   * Phát hiệu ứng âm thanh (không loop)
   */
  playSfx(track: AudioTrack): void {
    if (!this.isBrowser) return;

    const src = this.tracks[track];
    if (!src || this._isMuted) return;

    this.sfx = new Audio(src);
    this.sfx.volume = this._sfxVolume;
    this.sfx.play().catch(() => {});
  }

  /**
   * Dừng hiệu ứng âm thanh đang phát
   */
  stopSfx(): void {
    if (!this.isBrowser) return;

    if (this.sfx) {
      this.sfx.pause();
      this.sfx.currentTime = 0;
      this.sfx = null;
    }
  }

  /**
   * Fade chuyển từ track này sang track khác
   */
  fadeToTrack(newTrack: AudioTrack, duration = 1000): void {
    if (!this.isBrowser) return;
    if (this.currentTrack === newTrack) return;

    // Stop SFX ngay lập tức
    this.stopSfx();

    const oldMusic = this.bgMusic;
    const originalVolume = this._bgVolume;

    if (oldMusic) {
      // Fade out
      const fadeOutInterval = setInterval(() => {
        if (oldMusic.volume > 0.05) {
          oldMusic.volume = Math.max(0, oldMusic.volume - 0.05);
        } else {
          clearInterval(fadeOutInterval);
          oldMusic.pause();
        }
      }, duration / 20);
    }

    // Delay rồi fade in track mới
    setTimeout(() => {
      this.currentTrack = null; // Reset để playBgMusic hoạt động
      this.playBgMusic(newTrack);

      if (this.bgMusic) {
        this.bgMusic.volume = 0;
        const fadeInInterval = setInterval(() => {
          if (this.bgMusic && this.bgMusic.volume < originalVolume - 0.05) {
            this.bgMusic.volume = Math.min(originalVolume, this.bgMusic.volume + 0.05);
          } else {
            clearInterval(fadeInInterval);
            if (this.bgMusic) this.bgMusic.volume = originalVolume;
          }
        }, duration / 20);
      }
    }, duration / 2);
  }

  // ========== GETTERS & SETTERS ==========

  get bgVolume(): number {
    return this._bgVolume;
  }

  set bgVolume(value: number) {
    this._bgVolume = Math.max(0, Math.min(1, value));
    if (this.bgMusic && !this._isMuted) {
      this.bgMusic.volume = this._bgVolume;
    }
    this.saveSettings();
  }

  get sfxVolume(): number {
    return this._sfxVolume;
  }

  set sfxVolume(value: number) {
    this._sfxVolume = Math.max(0, Math.min(1, value));
    this.saveSettings();
  }

  get isMuted(): boolean {
    return this._isMuted;
  }

  set isMuted(value: boolean) {
    this._isMuted = value;
    if (this.bgMusic) {
      this.bgMusic.volume = value ? 0 : this._bgVolume;
    }
    this.saveSettings();
  }

  toggleMute(): void {
    this.isMuted = !this._isMuted;

    // Nếu unmute và có pending track, phát luôn
    if (!this._isMuted && this.pendingTrack) {
      this._userInteracted = true; // User đã click = đã tương tác
      this.playBgMusic(this.pendingTrack);
      this.pendingTrack = null;
    }
  }

  get isPlaying(): boolean {
    return this.bgMusic !== null && !this.bgMusic.paused;
  }

  get currentTrackName(): AudioTrack | null {
    return this.currentTrack;
  }

  get hasPendingTrack(): boolean {
    return this.pendingTrack !== null;
  }

  get userInteracted(): boolean {
    return this._userInteracted;
  }

  /**
   * Gọi khi user click nút nhạc - đảm bảo phát được
   */
  userClickPlay(): void {
    this._userInteracted = true;

    if (this.pendingTrack && !this._isMuted) {
      this.playBgMusic(this.pendingTrack);
      this.pendingTrack = null;
    } else if (this.currentTrack && this.bgMusic?.paused) {
      this.bgMusic.play().catch(() => {});
    }
  }
}
