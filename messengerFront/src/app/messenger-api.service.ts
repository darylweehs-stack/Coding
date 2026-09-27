import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

export interface TokenResponse {
  userId: number;
  username: string;
  token: string;
  tokenType: string;
}

export interface MessageResponse {
  id: number;
  senderId: number;
  userId: number;
  content: string;
  timestamp: string;
}

export interface FriendResponse {
  userId: number;
  username: string;
}

export interface SendMessageRequest {
  userId: number;
  content: string;
  timestamp: string;
}

@Injectable({ providedIn: 'root' })
export class MessengerApiService {
  private readonly http = inject(HttpClient);

  register(credentials: { username: string; password: string }): Observable<TokenResponse> {
    return this.http.post<TokenResponse>('/api/auth/register', credentials);
  }

  login(credentials: { username: string; password: string }): Observable<TokenResponse> {
    return this.http.post<TokenResponse>('/api/auth/login', credentials);
  }

  listFriends(session: TokenResponse): Observable<FriendResponse[]> {
    return this.http.get<FriendResponse[]>('/api/friends', {
      headers: this.authorization(session),
    });
  }

  addFriend(session: TokenResponse, userId: number): Observable<FriendResponse> {
    return this.http.post<FriendResponse>(`/api/friends/${userId}`, null, {
      headers: this.authorization(session),
    });
  }

  receiveNew(session: TokenResponse): Observable<MessageResponse[]> {
    return this.http.get<MessageResponse[]>('/api/messages/new', {
      headers: this.authorization(session),
    });
  }

  conversation(session: TokenResponse, friendId: number): Observable<MessageResponse[]> {
    return this.http.get<MessageResponse[]>(`/api/messages/conversations/${friendId}`, {
      headers: this.authorization(session),
    });
  }

  send(session: TokenResponse, request: SendMessageRequest): Observable<MessageResponse> {
    return this.http.post<MessageResponse>('/api/messages/send', request, {
      headers: this.authorization(session),
    });
  }

  private authorization(session: TokenResponse): HttpHeaders {
    return new HttpHeaders({ Authorization: `${session.tokenType} ${session.token}` });
  }
}