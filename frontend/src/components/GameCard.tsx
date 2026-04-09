import { Link } from 'react-router-dom'
import type { GameSummary } from '../types/game'
import { ComplexityStars } from './ComplexityStars'

interface Props {
  game: GameSummary
}

export function GameCard({ game }: Props) {
  return (
    <Link
      to={`/games/${game.id}`}
      className="flex gap-3 p-3 rounded-lg bg-white hover:bg-gray-50 border border-gray-200 transition-colors"
    >
      {game.thumbnailUrl ? (
        <img
          src={game.thumbnailUrl}
          alt={game.name}
          className="w-14 h-14 object-cover rounded flex-shrink-0"
        />
      ) : (
        <div className="w-14 h-14 bg-gray-200 rounded flex-shrink-0" />
      )}
      <div className="flex-1 min-w-0">
        <div className="flex items-start justify-between gap-2">
          <h3 className="font-semibold text-gray-900 truncate">{game.name}</h3>
          {game.bggRank != null && (
            <span className="text-xs text-gray-500 flex-shrink-0">#{game.bggRank}</span>
          )}
        </div>
        <div className="flex items-center gap-3 mt-1 text-sm text-gray-600">
          {game.avgRating != null && (
            <span className="text-green-600 font-medium">⭐ {game.avgRating.toFixed(1)}</span>
          )}
          <ComplexityStars complexity={game.complexity} />
          {game.minPlayers != null && game.maxPlayers != null && (
            <span>{game.minPlayers}–{game.maxPlayers}p</span>
          )}
        </div>
      </div>
    </Link>
  )
}
