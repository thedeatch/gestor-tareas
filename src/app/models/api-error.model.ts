import { HttpErrorResponse } from '@angular/common/http';

export interface ApiError {
  timestamp: string;
  status: number;
  code: string;
  message: string;
  path: string;
  fieldErrors: Record<string, string>;
}

export const getApiError = (error: unknown): Partial<ApiError> | null => {
  if (!(error instanceof HttpErrorResponse) ||
      typeof error.error !== 'object' ||
      error.error === null) {
    return null;
  }

  return error.error as Partial<ApiError>;
};

export const getApiErrorMessage = (
  error: unknown,
  fallback: string
): string => {
  const apiError = getApiError(error);
  if (typeof apiError?.message === 'string' && apiError.message.trim()) {
    return apiError.message;
  }

  // Compatibilidad temporal con respuestas ProblemDetail anteriores.
  const legacyDetail = error instanceof HttpErrorResponse
    ? error.error?.detail
    : null;
  return typeof legacyDetail === 'string' && legacyDetail.trim()
    ? legacyDetail
    : fallback;
};
