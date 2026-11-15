import { DOCUMENT } from '@angular/common';
import { Injectable, inject } from '@angular/core';

/** Applies a wallet's background to the whole page (see body[data-bg] in styles.css). */
@Injectable({ providedIn: 'root' })
export class Theme {
  private readonly document = inject(DOCUMENT);

  set(background: string | null | undefined): void {
    this.document.body.dataset['bg'] = background || 'cozy';
  }
}
