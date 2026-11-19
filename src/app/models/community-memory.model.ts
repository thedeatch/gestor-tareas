import { PhotoDateInfo } from './photo-date.model';

export interface CommunityMemory {
  imageId: number;
  name: string;
  albumId: number;
  albumTitle: string;
  date: PhotoDateInfo;
}

export interface CommunityMemoriesPage {
  items: CommunityMemory[];
  total: number;
  page: number;
  size: number;
}
