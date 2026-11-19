export type SavedItemKind = 'POST' | 'IMAGE' | 'EVENT';
export type SavedItemsFilter = 'ALL' | SavedItemKind;

export interface SavedItem {
  kind: SavedItemKind;
  targetId: number;
  groupId: number;
  groupName: string;
  albumId: number | null;
  title: string;
  excerpt: string;
  savedAt: string;
}

export interface SavedItemsPage {
  items: SavedItem[];
  total: number;
  page: number;
  size: number;
}

export interface SavedItemsState {
  postIds: number[];
  imageIds: number[];
  eventIds: number[];
}
