import { Game, GamesList } from './core/models';

/** Lets pending promises and timers of the code under test run. */
export const settle = () => new Promise<void>((resolve) => setTimeout(resolve));

export function game(rawgId: number, name = `Game ${rawgId}`): Game {
  return { rawgId, name, released: '2020-01-01', backgroundImage: null };
}

export function gamesList(id: number, name: string, ...games: Game[]): GamesList {
  return { id, name, games };
}
