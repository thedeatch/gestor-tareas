export type CommunitySearchKind = 'POST' | 'TASK' | 'EVENT' | 'ALBUM';
export type CommunitySearchFilter = 'ALL' | CommunitySearchKind;

export interface CommunitySearchResult {
  kind: CommunitySearchKind;
  id: number;
  title: string;
  excerpt: string;
}

export interface CommunitySearchPage {
  query: string;
  items: CommunitySearchResult[];
  total: number;
  page: number;
  size: number;
}
