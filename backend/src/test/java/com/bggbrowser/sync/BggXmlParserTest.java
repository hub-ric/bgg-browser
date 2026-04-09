package com.bggbrowser.sync;

import com.bggbrowser.game.Game;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class BggXmlParserTest {

    private final BggXmlParser parser = new BggXmlParser();

    private static final String SAMPLE_XML = """
        <?xml version="1.0" encoding="utf-8"?>
        <items>
          <item type="boardgame" id="224517">
            <thumbnail>https://example.com/thumb.jpg</thumbnail>
            <name type="primary" sortindex="1" value="Brass: Birmingham"/>
            <description>A game about industry.</description>
            <yearpublished value="2018"/>
            <minplayers value="2"/>
            <maxplayers value="4"/>
            <minplaytime value="60"/>
            <maxplaytime value="120"/>
            <statistics page="1">
              <ratings>
                <average value="8.61"/>
                <averageweight value="3.89"/>
                <ranks>
                  <rank type="subtype" name="boardgame" value="1"/>
                </ranks>
              </ratings>
            </statistics>
          </item>
          <item type="boardgameexpansion" id="999">
            <name type="primary" value="Some Expansion"/>
          </item>
        </items>
        """;

    @Test
    void parsesGameFields() {
        List<Game> games = parser.parse(SAMPLE_XML);
        assertThat(games).hasSize(1);
        Game g = games.get(0);
        assertThat(g.getId()).isEqualTo(224517L);
        assertThat(g.getName()).isEqualTo("Brass: Birmingham");
        assertThat(g.getYearPublished()).isEqualTo(2018);
        assertThat(g.getBggRank()).isEqualTo(1);
        assertThat(g.getAvgRating()).isEqualByComparingTo("8.61");
        assertThat(g.getComplexity()).isEqualByComparingTo("3.89");
        assertThat(g.getMinPlayers()).isEqualTo(2);
        assertThat(g.getMaxPlayers()).isEqualTo(4);
        assertThat(g.getPlayTimeMin()).isEqualTo(60);
        assertThat(g.getPlayTimeMax()).isEqualTo(120);
        assertThat(g.getThumbnailUrl()).isEqualTo("https://example.com/thumb.jpg");
        assertThat(g.getDescription()).isEqualTo("A game about industry.");
    }

    @Test
    void skipsNonBoardgameItems() {
        List<Game> games = parser.parse(SAMPLE_XML);
        assertThat(games).noneMatch(g -> g.getId() == 999L);
    }

    @Test
    void returnsEmptyListForEmptyItems() {
        List<Game> games = parser.parse("<items></items>");
        assertThat(games).isEmpty();
    }
}
