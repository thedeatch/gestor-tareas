import { HttpClient } from '@angular/common/http';
import { Service, inject } from '@angular/core';
import { CommunityGoalDraft, CommunityGoalOverview } from '../models/community-goal.model';
import { TaskStatus } from '../models/task.model';

@Service()
export class CommunityGoalsService {
  private readonly http = inject(HttpClient);
  private url(groupId: number) { return `/api/communities/${groupId}/goals`; }
  findAll(groupId: number) { return this.http.get<CommunityGoalOverview>(this.url(groupId)); }
  create(groupId: number, draft: CommunityGoalDraft) {
    return this.http.post<CommunityGoalOverview>(this.url(groupId), draft);
  }
  update(groupId: number, id: number, draft: CommunityGoalDraft) {
    return this.http.put<CommunityGoalOverview>(`${this.url(groupId)}/${id}`, draft);
  }
  delete(groupId: number, id: number) {
    return this.http.delete<CommunityGoalOverview>(`${this.url(groupId)}/${id}`);
  }
  changeTaskStatus(groupId: number, id: number, status: TaskStatus) {
    return this.http.patch<void>(`/api/communities/${groupId}/tasks/${id}/status`, { status });
  }
}
