export interface CommunityPollOption {
  id: number;
  label: string;
  votes: number;
}

export interface CommunityPoll {
  id: number;
  question: string;
  authorId: number;
  authorName: string;
  createdAt: string;
  closesAt: string | null;
  closed: boolean;
  canClose: boolean;
  selectedOptionId: number | null;
  totalVotes: number;
  options: CommunityPollOption[];
}

export interface CreateCommunityPoll {
  question: string;
  options: string[];
  closesAt: string | null;
}
