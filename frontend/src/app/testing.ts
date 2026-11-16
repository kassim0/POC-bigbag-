import { Game, GamesList } from './core/models';

/** Lets pending promises and timers of the code under test run. */
export const settle = () => new Promise<void>((resolve) => setTimeout(resolve));

export function game(gameId: number, name = `Game ${gameId}`): Game {
  return { gameId, name, released: '2020-01-01', coverUrl: null };
}

export function gamesList(id: number, name: string, ...games: Game[]): GamesList {
  return { id, name, games };
}
