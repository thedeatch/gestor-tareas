import { PhotoDateInfo } from './photo-date.model';

export interface GalleryPhoto {
  id: number;
  name: string;
  commentCount: number;
  likeCount: number;
  liked: boolean;
  date?: PhotoDateInfo;
}

export interface GalleryPhotoPage {
  items: GalleryPhoto[];
  total: number;
  page: number;
  size: number;
}
