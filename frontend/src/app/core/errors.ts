import { HttpErrorResponse } from '@angular/common/http';

/** Human readable message for a failed API call. */
export function errorMessage(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    if (typeof error.error?.error === 'string') {
      return error.error.error;
    }
    if (error.status === 0 || error.status >= 500) {
      return 'The BingBang server is unreachable. Please try again.';
    }
  }
  return 'Something went wrong. Please try again.';
}
