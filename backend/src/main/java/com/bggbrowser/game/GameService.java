package com.bggbrowser.game;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class GameService {

    private final GameRepository repository;

    public GameService(GameRepository repository) {
        this.repository = repository;
    }

    public Page<GameSummaryDto> getGames(GameFilterParams params) {
        Specification<Game> spec = buildSpec(params);
        Sort sort = buildSort(params.sort(), params.sortDir());
        Pageable pageable = PageRequest.of(params.page(), params.size(), sort);
        return repository.findAll(spec, pageable).map(GameSummaryDto::from);
    }

    public GameDetailDto getGameById(Long id) {
        return repository.findById(id)
            .map(GameDetailDto::from)
            .orElseThrow(() -> new GameNotFoundException(id));
    }

    private Specification<Game> buildSpec(GameFilterParams p) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (p.name() != null && !p.name().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("name")),
                    "%" + p.name().toLowerCase() + "%"));
            }
            if (p.minPlayers() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("minPlayers"), p.minPlayers()));
            }
            if (p.maxPlayers() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("maxPlayers"), p.maxPlayers()));
            }
            if (p.minComplexity() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("complexity"),
                    BigDecimal.valueOf(p.minComplexity())));
            }
            if (p.maxComplexity() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("complexity"),
                    BigDecimal.valueOf(p.maxComplexity())));
            }
            if (p.maxRank() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("bggRank"), p.maxRank()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Sort buildSort(String sort, String dir) {
        String field = switch (sort) {
            case "rating" -> "avgRating";
            case "complexity" -> "complexity";
            case "name" -> "name";
            default -> "bggRank";
        };
        return Sort.by("desc".equalsIgnoreCase(dir) ? Sort.Direction.DESC : Sort.Direction.ASC, field);
    }
}
