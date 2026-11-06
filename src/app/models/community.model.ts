export type CommunityVisibility = 'PUBLIC' | 'PRIVATE' | 'HIDDEN';
export type CommunityRole =
  | 'OWNER'
  | 'COMMUNITY_ADMIN'
  | 'COMMUNITY_MODERATOR'
  | 'MEMBER';

export interface CommunitySummary {
  id: number;
  name: string;
  slug: string;
  description: string;
  visibility: CommunityVisibility;
  currentUserRole: CommunityRole;
  memberCount: number;
  createdAt: string;
}

export interface CommunityMember {
  userId: number;
  name: string;
  email: string;
  role: CommunityRole;
  joinedAt: string;
}

export interface CommunityDetail extends Omit<CommunitySummary, 'memberCount'> {
  members: CommunityMember[];
}

export interface CreateCommunityRequest {
  name: string;
  description: string;
  visibility: CommunityVisibility;
}

export type CommunityAccessStatus =
  | 'PENDING'
  | 'APPROVED'
  | 'REJECTED'
  | 'CANCELLED';
export type CommunityAccessType = 'JOIN_REQUEST' | 'INVITATION';

export interface CommunityAccess {
  id: number;
  communityId: number;
  communityName: string;
  userId: number;
  userName: string;
  userEmail: string;
  type: CommunityAccessType;
  status: CommunityAccessStatus;
  createdAt: string;
}

export interface CommunityDiscovery {
  id: number;
  name: string;
  slug: string;
  description: string;
  memberCount: number;
  requestStatus: CommunityAccessStatus | null;
}
