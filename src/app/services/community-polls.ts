import { HttpClient } from '@angular/common/http';
import { Service, inject } from '@angular/core';
import { CommunityPoll, CreateCommunityPoll } from '../models/community-poll.model';

@Service()
export class CommunityPollsService {
  private readonly http = inject(HttpClient);

  findAll(groupId: number) {
    return this.http.get<CommunityPoll[]>(this.url(groupId));
  }

  create(groupId: number, poll: CreateCommunityPoll) {
    return this.http.post<CommunityPoll>(this.url(groupId), poll);
  }

  vote(groupId: number, pollId: number, optionId: number) {
    return this.http.put<CommunityPoll>(`${this.url(groupId)}/${pollId}/vote`, { optionId });
  }

  close(groupId: number, pollId: number) {
    return this.http.put<CommunityPoll>(`${this.url(groupId)}/${pollId}/close`, null);
  }

  private url(groupId: number): string {
    return `/api/communities/${groupId}/polls`;
  }
}
