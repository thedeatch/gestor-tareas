import { HttpClient } from '@angular/common/http';
import { inject, Service } from '@angular/core';

export interface BlockedUser {
  id: number;
  name: string;
}

export interface BlockStatus {
  canBlock: boolean;
  blocked: boolean;
}

@Service()
export class UserBlocksService {
  private readonly http = inject(HttpClient);
  private readonly url = '/api/users/me/blocks';

  list() {
    return this.http.get<BlockedUser[]>(this.url);
  }

  status(id: number) {
    return this.http.get<BlockStatus>(`${this.url}/${id}`);
  }

  set(id: number, blocked: boolean) {
    const url = `${this.url}/${id}`;
    return blocked ? this.http.put<void>(url, {}) : this.http.delete<void>(url);
  }
}