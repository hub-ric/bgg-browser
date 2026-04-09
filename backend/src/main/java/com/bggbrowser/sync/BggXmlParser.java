package com.bggbrowser.sync;

import com.bggbrowser.game.Game;
import org.springframework.stereotype.Component;
import org.w3c.dom.*;
import javax.xml.parsers.*;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Component
public class BggXmlParser {

    public List<Game> parse(String xml) {
        List<Game> results = new ArrayList<>();
        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
            NodeList items = doc.getElementsByTagName("item");
            for (int i = 0; i < items.getLength(); i++) {
                Element item = (Element) items.item(i);
                if (!"boardgame".equals(item.getAttribute("type"))) continue;
                Game game = parseItem(item);
                if (game != null) results.add(game);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse BGG XML", e);
        }
        return results;
    }

    private Game parseItem(Element item) {
        Game game = new Game();
        game.setId(Long.parseLong(item.getAttribute("id")));
        game.setLastSyncedAt(Instant.now());

        NodeList names = item.getElementsByTagName("name");
        for (int i = 0; i < names.getLength(); i++) {
            Element name = (Element) names.item(i);
            if ("primary".equals(name.getAttribute("type"))) {
                game.setName(name.getAttribute("value"));
                break;
            }
        }
        if (game.getName() == null) return null;

        game.setThumbnailUrl(textContent(item, "thumbnail"));
        game.setDescription(textContent(item, "description"));
        game.setYearPublished(intAttr(item, "yearpublished", "value"));
        game.setMinPlayers(intAttr(item, "minplayers", "value"));
        game.setMaxPlayers(intAttr(item, "maxplayers", "value"));
        game.setPlayTimeMin(intAttr(item, "minplaytime", "value"));
        game.setPlayTimeMax(intAttr(item, "maxplaytime", "value"));

        NodeList ratings = item.getElementsByTagName("ratings");
        if (ratings.getLength() > 0) {
            Element r = (Element) ratings.item(0);
            game.setAvgRating(decimalAttr(r, "average", "value"));
            game.setComplexity(decimalAttr(r, "averageweight", "value"));

            NodeList ranks = r.getElementsByTagName("rank");
            for (int i = 0; i < ranks.getLength(); i++) {
                Element rank = (Element) ranks.item(i);
                if ("boardgame".equals(rank.getAttribute("name"))) {
                    String val = rank.getAttribute("value");
                    if (!val.isBlank() && !"Not Ranked".equals(val)) {
                        game.setBggRank(Integer.parseInt(val));
                    }
                    break;
                }
            }
        }
        return game;
    }

    private String textContent(Element parent, String tag) {
        NodeList nl = parent.getElementsByTagName(tag);
        if (nl.getLength() == 0) return null;
        String text = nl.item(0).getTextContent().trim();
        return text.isEmpty() ? null : text;
    }

    private Integer intAttr(Element parent, String tag, String attr) {
        NodeList nl = parent.getElementsByTagName(tag);
        if (nl.getLength() == 0) return null;
        String val = ((Element) nl.item(0)).getAttribute(attr);
        if (val.isBlank()) return null;
        try { return Integer.parseInt(val); } catch (NumberFormatException e) { return null; }
    }

    private BigDecimal decimalAttr(Element parent, String tag, String attr) {
        NodeList nl = parent.getElementsByTagName(tag);
        if (nl.getLength() == 0) return null;
        String val = ((Element) nl.item(0)).getAttribute(attr);
        if (val.isBlank() || "0".equals(val)) return null;
        try { return new BigDecimal(val); } catch (NumberFormatException e) { return null; }
    }
}
