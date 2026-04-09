interface Props {
  complexity: number | null
}

export function ComplexityStars({ complexity }: Props) {
  if (complexity == null) return null
  const filled = Math.round(complexity)
  return (
    <span className="flex gap-0.5" title={`Complexity: ${complexity.toFixed(1)} / 5`}>
      {Array.from({ length: 5 }, (_, i) =>
        i < filled
          ? <span key={i} data-testid="star-filled" className="text-amber-400">★</span>
          : <span key={i} data-testid="star-empty" className="text-gray-600">★</span>
      )}
    </span>
  )
}
