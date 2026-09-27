import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { interval, startWith } from 'rxjs';
import { FriendResponse, MessageResponse, MessengerApiService, TokenResponse } from './messenger-api.service';

@Component({
  imports: [CommonModule, FormsModule],
  selector: 'app-root',
  styleUrl: './app.css',
  templateUrl: './messenger-page.html',
})
export class App {
  private readonly api = inject(MessengerApiService);
  private readonly destroyRef = inject(DestroyRef);
  readonly session = signal<TokenResponse | null>(this.restoreSession());
  readonly friends = signal<FriendResponse[]>([]);
  readonly messages = signal<MessageResponse[]>([]);
  readonly mode = signal<'login' | 'register'>('login');
  readonly authBusy = signal(false);
  readonly loading = signal(false);
  readonly sending = signal(false);
  readonly addingFriend = signal(false);
  readonly error = signal('');
  readonly inboxError = signal('');
  readonly notice = signal('');

  authUsername = '';
  authPassword = '';
  friendUserId: number | null = null;
  selectedRecipientId: number | null = null;
  messageContent = '';

  constructor() {
    if (this.session()) this.loadFriends();
    interval(1000)
      .pipe(startWith(0), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.refreshInbox());
  }

  setMode(mode: 'login' | 'register'): void {
    this.mode.set(mode);
    this.error.set('');
  }

  authenticate(): void {
    if (this.authBusy()) return;
    this.error.set('');
    this.authBusy.set(true);
    const credentials = { username: this.authUsername.trim(), password: this.authPassword };
    const request = this.mode() === 'register'
      ? this.api.register(credentials)
      : this.api.login(credentials);

    request.subscribe({
      next: (session) => {
        localStorage.setItem('messenger.session', JSON.stringify(session));
        this.session.set(session);
        this.authPassword = '';
        this.authBusy.set(false);
        this.loadFriends();
        this.refreshInbox();
      },
      error: (error: unknown) => {
        this.error.set(this.errorMessage(error, 'Unable to authenticate.'));
        this.authBusy.set(false);
      },
    });
  }

  loadFriends(): void {
    const session = this.session();
    if (!session) return;
    this.api.listFriends(session).subscribe({
      next: (friends) => this.friends.set(friends),
      error: (error: unknown) => this.error.set(this.errorMessage(error, 'Unable to load your friends.')),
    });
  }

  addFriend(): void {
    const session = this.session();
    const userId = this.friendUserId;
    if (!session || userId === null || this.addingFriend()) return;

    this.addingFriend.set(true);
    this.error.set('');
    this.api.addFriend(session, userId).subscribe({
      next: (friend) => {
        this.friends.update((friends) => [...friends.filter((item) => item.userId !== friend.userId), friend]
          .sort((first, second) => first.username.localeCompare(second.username)));
        this.selectedRecipientId = friend.userId;
        this.friendUserId = null;
        this.notice.set(`${friend.username} added to your friends.`);
        this.addingFriend.set(false);
      },
      error: (error: unknown) => {
        this.error.set(this.errorMessage(error, 'Unable to add this friend.'));
        this.addingFriend.set(false);
      },
    });
  }

  selectFriend(userId: number): void {
    this.selectedRecipientId = userId;
  }

  refreshInbox(): void {
    const session = this.session();
    if (!session || this.loading()) return;
    this.loading.set(true);
    this.api.receiveNew(session).subscribe({
      next: (messages) => {
        if (messages.length > 0) {
          const knownIds = new Set(this.messages().map((message) => message.id));
          const newlyReceived = messages.filter((message) => !knownIds.has(message.id));
          if (newlyReceived.length > 0) {
            this.messages.update((existing) => [...existing, ...newlyReceived]);
            this.notice.set(`${newlyReceived.length} new ${newlyReceived.length === 1 ? 'message' : 'messages'} received.`);
          }
        }
        this.inboxError.set('');
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.inboxError.set(this.errorMessage(error, 'Unable to load your inbox.'));
        this.loading.set(false);
      },
    });
  }

  sendMessage(): void {
    const session = this.session();
    const recipient = this.friends().find((friend) => friend.userId === this.selectedRecipientId);
    const content = this.messageContent.trim();
    if (!session || !recipient || !content || this.sending()) return;

    this.sending.set(true);
    this.error.set('');
    this.notice.set('');
    this.api.send(session, { userId: recipient.userId, content, timestamp: new Date().toISOString() }).subscribe({
      next: () => {
        this.messageContent = '';
        this.notice.set(`Message sent to ${recipient.username}.`);
        this.sending.set(false);
      },
      error: (error: unknown) => {
        this.error.set(this.errorMessage(error, 'Unable to send this message.'));
        this.sending.set(false);
      },
    });
  }

  signOut(): void {
    localStorage.removeItem('messenger.session');
    this.session.set(null);
    this.friends.set([]);
    this.messages.set([]);
    this.inboxError.set('');
    this.notice.set('');
    this.error.set('');
  }

  private restoreSession(): TokenResponse | null {
    const saved = localStorage.getItem('messenger.session');
    if (!saved) return null;
    try {
      const session = JSON.parse(saved) as TokenResponse;
      return session.token && session.username && session.userId ? session : null;
    } catch {
      localStorage.removeItem('messenger.session');
      return null;
    }
  }

  private errorMessage(error: unknown, fallback: string): string {
    if (error instanceof HttpErrorResponse) {
      if (error.status === 0) return 'Cannot reach the messenger API. Check that the backend is running on port 8080.';
      const detail = error.error?.message ?? error.error?.detail;
      if (typeof detail === 'string' && detail.trim()) return detail;
    }
    return fallback;
  }
}
