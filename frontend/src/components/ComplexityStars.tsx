interface Props {
  complexity: number | null
}

export function ComplexityStars({ complexity }: Props) {
  if (complexity == null) return null

  const filled = Math.round(complexity)
  const empty = 5 - filled

  return (
    <div className="flex gap-1">
      {Array.from({ length: filled }, (_, i) => (
        <span key={`filled-${i}`} data-testid="star-filled">★</span>
      ))}
      {Array.from({ length: empty }, (_, i) => (
        <span key={`empty-${i}`} data-testid="star-empty">☆</span>
      ))}
    </div>
  )
}
