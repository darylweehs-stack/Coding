import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, ElementRef, inject, signal, ViewChild } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { interval, startWith } from 'rxjs';
import { FriendResponse, MessageResponse, MessengerApiService, TokenResponse } from './messenger-api.service';

@Component({
  imports: [CommonModule, FormsModule],
  selector: 'app-root',
  styleUrls: ['./app.css', './chat.css'],
  templateUrl: './messenger-page.html',
})
export class App {
  private readonly api = inject(MessengerApiService);
  private readonly destroyRef = inject(DestroyRef);
  readonly session = signal<TokenResponse | null>(this.restoreSession());
  readonly friends = signal<FriendResponse[]>([]);
  readonly messages = signal<MessageResponse[]>([]);
  readonly selectedFriendId = signal<number | null>(null);
  readonly unreadCounts = signal<Record<number, number>>({});
  readonly mode = signal<'login' | 'register'>('login');
  readonly authBusy = signal(false);
  readonly loading = signal(false);
  readonly historyLoading = signal(false);
  readonly sending = signal(false);
  readonly addingFriend = signal(false);
  readonly error = signal('');
  readonly inboxError = signal('');
  readonly notice = signal('');

  authUsername = '';
  authPassword = '';
  friendUserId: number | null = null;
  friendSearch = '';
  messageContent = '';

  @ViewChild('threadPanel') private threadPanel?: ElementRef<HTMLDivElement>;

  constructor() {
    if (this.session()) this.loadFriends();
    interval(500)
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
      next: (friends) => {
        this.friends.set(friends);
        if (!friends.some((friend) => friend.userId === this.selectedFriendId())) {
          this.selectFriend(friends[0]?.userId ?? null);
        }
      },
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
        this.friendUserId = null;
        this.selectFriend(friend.userId);
        this.addingFriend.set(false);
      },
      error: (error: unknown) => {
        this.error.set(this.errorMessage(error, 'Unable to add this friend.'));
        this.addingFriend.set(false);
      },
    });
  }

  selectFriend(userId: number | null): void {
    this.selectedFriendId.set(userId);
    this.error.set('');
    if (userId === null) return;
    this.unreadCounts.update((counts) => ({ ...counts, [userId]: 0 }));
    const session = this.session();
    if (!session) return;
    this.historyLoading.set(true);
    this.api.conversation(session, userId).subscribe({
      next: (history) => {
        this.messages.update((existing) => this.mergeMessages(existing, history));
        this.historyLoading.set(false);
        this.scrollToLatest();
      },
      error: (error: unknown) => {
        this.error.set(this.errorMessage(error, 'Unable to load this conversation.'));
        this.historyLoading.set(false);
      },
    });
  }

  get visibleFriends(): FriendResponse[] {
    const search = this.friendSearch.trim().toLocaleLowerCase();
    return [...this.friends()]
      .filter((friend) => !search
        || friend.username.toLocaleLowerCase().includes(search)
        || String(friend.userId).includes(search))
      .sort((first, second) => {
        const firstTime = this.latestMessage(first.userId)?.timestamp ?? '';
        const secondTime = this.latestMessage(second.userId)?.timestamp ?? '';
        return secondTime.localeCompare(firstTime) || first.username.localeCompare(second.username);
      });
  }

  get activeFriend(): FriendResponse | null {
    return this.friends().find((friend) => friend.userId === this.selectedFriendId()) ?? null;
  }

  get activeMessages(): MessageResponse[] {
    const session = this.session();
    const friendId = this.selectedFriendId();
    if (!session || friendId === null) return [];
    return this.messages()
      .filter((message) =>
        (message.senderId === session.userId && message.userId === friendId)
        || (message.senderId === friendId && message.userId === session.userId))
      .sort((first, second) => first.timestamp.localeCompare(second.timestamp) || first.id - second.id);
  }

  latestMessage(friendId: number): MessageResponse | null {
    const session = this.session();
    if (!session) return null;
    return this.messages()
      .filter((message) =>
        (message.senderId === session.userId && message.userId === friendId)
        || (message.senderId === friendId && message.userId === session.userId))
      .reduce<MessageResponse | null>((latest, message) =>
        !latest || message.timestamp > latest.timestamp ? message : latest, null);
  }

  previewFor(friendId: number): string {
    const latest = this.latestMessage(friendId);
    if (!latest) return 'Start a conversation';
    return `${latest.senderId === this.session()?.userId ? 'You: ' : ''}${latest.content}`;
  }

  isOutgoing(message: MessageResponse): boolean {
    return message.senderId === this.session()?.userId;
  }

  sendOnEnter(event: Event): void {
    if (!(event instanceof KeyboardEvent) || event.shiftKey) return;
    event.preventDefault();
    this.sendMessage();
  }

  private mergeMessages(existing: MessageResponse[], incoming: MessageResponse[]): MessageResponse[] {
    const byId = new Map(existing.map((message) => [message.id, message]));
    for (const message of incoming) byId.set(message.id, message);
    return [...byId.values()].sort((first, second) =>
      first.timestamp.localeCompare(second.timestamp) || first.id - second.id);
  }

  private scrollToLatest(): void {
    requestAnimationFrame(() => {
      const thread = this.threadPanel?.nativeElement;
      if (thread) thread.scrollTop = thread.scrollHeight;
    });
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
            this.messages.update((existing) => this.mergeMessages(existing, newlyReceived));
            const activeFriendId = this.selectedFriendId();
            this.unreadCounts.update((counts) => {
              const updated = { ...counts };
              for (const message of newlyReceived) {
                if (message.senderId !== session.userId && message.senderId !== activeFriendId) {
                  updated[message.senderId] = (updated[message.senderId] ?? 0) + 1;
                }
              }
              return updated;
            });
            if (newlyReceived.some((message) => message.senderId === activeFriendId)) this.scrollToLatest();
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
    const recipient = this.activeFriend;
    const content = this.messageContent.trim();
    if (!session || !recipient || !content || this.sending()) return;

    this.sending.set(true);
    this.error.set('');
    this.notice.set('');
    this.api.send(session, { userId: recipient.userId, content, timestamp: new Date().toISOString() }).subscribe({
      next: (message) => {
        this.messages.update((existing) => this.mergeMessages(existing, [message]));
        this.messageContent = '';
        this.sending.set(false);
        this.scrollToLatest();
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
    this.selectedFriendId.set(null);
    this.unreadCounts.set({});
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
