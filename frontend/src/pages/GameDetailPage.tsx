import { useNavigate, useParams } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { fetchGame } from '../api/games'
import { ComplexityStars } from '../components/ComplexityStars'

export function GameDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const { data: game, isLoading, isError } = useQuery({
    queryKey: ['game', id],
    queryFn: () => fetchGame(Number(id)),
    enabled: id != null,
  })

  if (isLoading) {
    return (
      <div className="max-w-2xl mx-auto p-6">
        <div className="h-8 w-24 bg-gray-200 rounded animate-pulse mb-6" />
        <div className="h-64 bg-gray-100 rounded-lg animate-pulse" />
      </div>
    )
  }

  if (isError || !game) {
    return (
      <div className="max-w-2xl mx-auto p-6">
        <button onClick={() => navigate(-1)} className="text-blue-600 hover:underline text-sm">← Back</button>
        <p className="mt-8 text-center text-red-500">Game not found.</p>
      </div>
    )
  }

  return (
    <div className="max-w-2xl mx-auto p-6">
      <button onClick={() => navigate(-1)} className="text-blue-600 hover:underline text-sm">
        ← Back to list
      </button>

      <div className="mt-6 flex gap-6">
        {game.thumbnailUrl ? (
          <img src={game.thumbnailUrl} alt={game.name}
            className="w-32 h-32 object-cover rounded-lg flex-shrink-0 shadow" />
        ) : (
          <div className="w-32 h-32 bg-gray-200 rounded-lg flex-shrink-0" />
        )}
        <div>
          <h1 className="text-2xl font-bold text-gray-900">{game.name}</h1>
          {game.yearPublished && (
            <p className="text-gray-500 text-sm mt-1">{game.yearPublished}</p>
          )}
          {game.avgRating != null && (
            <p className="text-green-600 font-semibold mt-2">⭐ {game.avgRating.toFixed(1)}</p>
          )}
          {game.bggRank != null && (
            <p className="text-gray-500 text-sm">BGG Rank #{game.bggRank}</p>
          )}
        </div>
      </div>

      <div className="mt-6 grid grid-cols-2 sm:grid-cols-4 gap-3">
        {game.complexity != null && (
          <div className="bg-gray-50 rounded-lg p-3">
            <p className="text-xs text-gray-500 mb-1">Complexity</p>
            <ComplexityStars complexity={game.complexity} />
            <p className="text-xs text-gray-400 mt-1">{game.complexity.toFixed(1)} / 5</p>
          </div>
        )}
        {game.minPlayers != null && (
          <div className="bg-gray-50 rounded-lg p-3">
            <p className="text-xs text-gray-500 mb-1">Players</p>
            <p className="font-semibold">{game.minPlayers}–{game.maxPlayers}</p>
          </div>
        )}
        {game.playTimeMin != null && (
          <div className="bg-gray-50 rounded-lg p-3">
            <p className="text-xs text-gray-500 mb-1">Play Time</p>
            <p className="font-semibold">
              {game.playTimeMin === game.playTimeMax
                ? `${game.playTimeMin}m`
                : `${game.playTimeMin}–${game.playTimeMax}m`}
            </p>
          </div>
        )}
      </div>

      {game.description && (
        <div className="mt-6">
          <h2 className="font-semibold text-gray-800 mb-2">About</h2>
          <p className="text-gray-600 text-sm leading-relaxed whitespace-pre-line">
            {game.description
              .replace(/&#\d+;/g, c => String.fromCharCode(parseInt(c.slice(2, -1))))
              .replace(/&amp;/g, '&')
              .replace(/&quot;/g, '"')}
          </p>
        </div>
      )}

      <div className="mt-6">
        <a
          href={`https://boardgamegeek.com/boardgame/${game.id}`}
          target="_blank"
          rel="noopener noreferrer"
          className="text-blue-600 hover:underline text-sm"
        >View on BoardGameGeek ↗</a>
      </div>
    </div>
  )
}
