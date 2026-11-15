export interface Game {
  rawgId: number;
  name: string;
  released: string | null;
  backgroundImage: string | null;
  /** The user's commentary on this game in this list (absent in search results). */
  comment?: string | null;
}

export interface GamesList {
  id: number;
  name: string;
  games: Game[];
}

export interface Wallet {
  id: string;
  background: string;
  /** Account that owns the wallet; null for a guest wallet. */
  username: string | null;
  lists: GamesList[];
}

export interface GameSearchResult {
  count: number;
  page: number;
  hasNext: boolean;
  results: Game[];
}

/** The "special backgrounds" a user can give to their wallet. */
export const BACKGROUNDS = ['cozy', 'night', 'forest', 'sunset', 'ocean', 'arcade'] as const;

export function listTitle(list: { name: string }): string {
  return list.name || 'Untitled list';
}
