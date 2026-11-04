import { HttpClient } from '@angular/common/http';
import { inject, Service } from '@angular/core';
import { Observable } from 'rxjs';

import { StoredFile } from '../models/stored-file.model';

@Service()
export class FileStorageService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = '/api/files';

  list(): Observable<StoredFile[]> {
    return this.http.get<StoredFile[]>(this.apiUrl);
  }

  upload(file: File): Observable<StoredFile> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<StoredFile>(this.apiUrl, formData);
  }

  download(id: number): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/${id}`, {
      responseType: 'blob'
    });
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
