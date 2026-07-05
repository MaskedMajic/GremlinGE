package com.maskedmajic.gremlinge.runelite;

import com.maskedmajic.gremlinge.FlipRecommendation;
import com.maskedmajic.gremlinge.ge.GeOfferEvent;
import com.maskedmajic.gremlinge.ge.GeOfferState;
import com.maskedmajic.gremlinge.ge.LimitStatus;
import com.maskedmajic.gremlinge.profit.ProfitSummary;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.Collections;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;

/**
 * GremlinGE RuneLite panel with an overview-first, card-style layout.
 */
public class GremlinGEPanel extends PluginPanel {
    private static final int CARD_GAP = 8;
    private static final int SMALL_CARD_HEIGHT = 78;
    private static final int MEDIUM_CARD_HEIGHT = 118;
    private static final int LARGE_CARD_HEIGHT = 132;

    private final JLabel titleLabel = new JLabel("GremlinGE");
    private final JLabel subtitleLabel = new JLabel("GE helper");

    private final JLabel openSlotsValue = createStatValue("0");
    private final JLabel activeValue = createStatValue("0");
    private final JLabel partialValue = createStatValue("0");
    private final JLabel completeValue = createStatValue("0");

    private final JTextArea offersArea = createTextArea("Waiting for live GE offers...\n");
    private final JTextArea candidatesArea = createTextArea("Waiting for scanner-backed suggestions...\n");
    private final JTextArea eventsArea = createTextArea("No GE events recorded yet.\n");
    private final JTextArea limitsArea = createTextArea("No active limit usage tracked yet.\n");
    private final JTextArea profitArea = createTextArea("No profit data yet.\n");

    public GremlinGEPanel() {
        super(false);
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(8, 8, 8, 8));
        setBackground(ColorScheme.DARK_GRAY_COLOR);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);

        content.add(buildHeaderCard());
        content.add(Box.createVerticalStrut(CARD_GAP));
        content.add(buildOverviewCard());
        content.add(Box.createVerticalStrut(CARD_GAP));
        content.add(buildSectionCard("Active Offers", "Live slots", offersArea, LARGE_CARD_HEIGHT));
        content.add(Box.createVerticalStrut(CARD_GAP));
        content.add(buildSectionCard("Next Flips", "Best candidates", candidatesArea, LARGE_CARD_HEIGHT));
        content.add(Box.createVerticalStrut(CARD_GAP));
        content.add(buildSectionCard("Recent Events", "Latest changes", eventsArea, MEDIUM_CARD_HEIGHT));
        content.add(Box.createVerticalStrut(CARD_GAP));
        content.add(buildSectionCard("Buy Limits", "Usage + reset", limitsArea, MEDIUM_CARD_HEIGHT));
        content.add(Box.createVerticalStrut(CARD_GAP));
        content.add(buildSectionCard("Profit", "Tracked totals", profitArea, SMALL_CARD_HEIGHT));

        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);
    }

    public void updateSummary(List<GeOfferState> offers) {
        if (offers == null) {
            offers = Collections.emptyList();
        }

        int openSlots = 0;
        int activeOffers = 0;
        int partialOffers = 0;
        int completedOffers = 0;

        for (GeOfferState offer : offers) {
            if (offer == null) {
                continue;
            }
            if (offer.isEmpty()) {
                openSlots++;
                continue;
            }
            activeOffers++;
            if (offer.filledQuantity > 0 && offer.filledQuantity < offer.totalQuantity) {
                partialOffers++;
            }
            if (offer.totalQuantity > 0 && offer.filledQuantity >= offer.totalQuantity) {
                completedOffers++;
            }
        }

        openSlotsValue.setText(Integer.toString(openSlots));
        activeValue.setText(Integer.toString(activeOffers));
        partialValue.setText(Integer.toString(partialOffers));
        completeValue.setText(Integer.toString(completedOffers));
    }

    public void updateOffers(List<GeOfferState> offers) {
        if (offers == null) {
            offers = Collections.emptyList();
        }

        StringBuilder sb = new StringBuilder();
        for (GeOfferState offer : offers) {
            if (offer == null || offer.state == null || offer.isEmpty()) {
                continue;
            }

            sb.append("#")
                .append(offer.slotIndex + 1)
                .append(" ")
                .append(trim(offer.itemName, 17))
                .append("\n")
                .append("  ")
                .append(offer.offerType)
                .append(" ")
                .append(offer.state)
                .append(" • ")
                .append(formatQty(offer.filledQuantity))
                .append("/")
                .append(formatQty(offer.totalQuantity))
                .append(" @ ")
                .append(formatQty(offer.price))
                .append("\n");
        }

        if (sb.length() == 0) {
            sb.append("No active offers detected yet.\n");
        }

        offersArea.setText(sb.toString());
        offersArea.setCaretPosition(0);
    }

    public void updateRecommendations(List<FlipRecommendation> recommendations) {
        if (recommendations == null || recommendations.isEmpty()) {
            candidatesArea.setText("No scanner-backed flip suggestions available yet.\n");
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (FlipRecommendation recommendation : recommendations) {
            if (recommendation == null || recommendation.candidate == null) {
                continue;
            }

            sb.append(trim(recommendation.candidate.name, 18))
                .append("\n")
                .append("  margin ")
                .append(formatQty(recommendation.candidate.margin))
                .append(" • left ")
                .append(formatQty(recommendation.remainingLimit))
                .append(" • ")
                .append(recommendation.candidate.volumeTag.toUpperCase())
                .append("\n");
        }

        candidatesArea.setText(sb.toString());
        candidatesArea.setCaretPosition(0);
    }

    public void updateEvents(List<GeOfferEvent> events) {
        if (events == null || events.isEmpty()) {
            eventsArea.setText("No GE events recorded yet.\n");
            return;
        }

        StringBuilder sb = new StringBuilder();
        int start = Math.max(0, events.size() - 8);
        for (int i = events.size() - 1; i >= start; i--) {
            GeOfferEvent event = events.get(i);
            if (event == null) {
                continue;
            }

            sb.append(event.type)
                .append(" • #")
                .append(event.slotIndex + 1)
                .append(" • ")
                .append(trim(event.itemName, 16));

            if (event.deltaFilled > 0) {
                sb.append(" • +").append(formatQty(event.deltaFilled));
            }
            if (event.newFilledQuantity > 0) {
                sb.append(" • ").append(formatQty(event.newFilledQuantity));
            }
            sb.append("\n");
        }
        eventsArea.setText(sb.toString());
        eventsArea.setCaretPosition(0);
    }

    public void updateLimits(List<LimitStatus> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            limitsArea.setText("No active limit usage tracked yet.\n");
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (LimitStatus status : statuses) {
            sb.append(trim(status.itemName, 18))
                .append("\n")
                .append("  ")
                .append(formatQty(status.boughtInWindow))
                .append("/")
                .append(formatQty(status.buyLimit))
                .append(" used • left ")
                .append(formatQty(status.remaining))
                .append("\n")
                .append("  reset ")
                .append(status.resetEta)
                .append("\n");
        }
        limitsArea.setText(sb.toString());
        limitsArea.setCaretPosition(0);
    }

    public void updateProfit(ProfitSummary summary) {
        if (summary == null) {
            profitArea.setText("No profit data yet.\n");
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("P/L  ").append(formatSigned(summary.realizedProfit)).append(" gp\n")
            .append("Buys ").append(formatQty(summary.totalBuys))
            .append(" • Sells ").append(formatQty(summary.totalSells)).append("\n")
            .append("In ").append(formatQty(summary.grossBuyValue))
            .append(" • Out ").append(formatQty(summary.grossSellValue)).append("\n");

        profitArea.setText(sb.toString());
        profitArea.setCaretPosition(0);
    }

    private JPanel buildHeaderCard() {
        JPanel card = createCardPanel();
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 18f));
        titleLabel.setForeground(Color.WHITE);

        subtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitleLabel.setForeground(Color.LIGHT_GRAY);
        subtitleLabel.setBorder(new EmptyBorder(2, 0, 0, 0));

        card.add(titleLabel);
        card.add(subtitleLabel);
        return card;
    }

    private JPanel buildOverviewCard() {
        JPanel card = createCardPanel();

        JLabel heading = createCardHeading("Overview");
        JLabel subheading = createCardSubheading("Quick GE state");
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        subheading.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel statsGrid = new JPanel(new GridLayout(2, 2, 8, 8));
        statsGrid.setOpaque(false);
        statsGrid.add(createStatCard("Open", openSlotsValue));
        statsGrid.add(createStatCard("Active", activeValue));
        statsGrid.add(createStatCard("Partial", partialValue));
        statsGrid.add(createStatCard("Done", completeValue));
        statsGrid.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(heading);
        card.add(subheading);
        card.add(Box.createVerticalStrut(8));
        card.add(statsGrid);
        return card;
    }

    private JPanel buildSectionCard(String title, String subtitle, JTextArea area, int height) {
        JPanel card = createCardPanel();
        JLabel heading = createCardHeading(title);
        JLabel subheading = createCardSubheading(subtitle);
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        subheading.setAlignmentX(Component.LEFT_ALIGNMENT);

        JScrollPane scrollPane = createScrollPane(area, height);
        scrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(heading);
        card.add(subheading);
        card.add(Box.createVerticalStrut(8));
        card.add(scrollPane);
        return card;
    }

    private JPanel createCardPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(true);
        panel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setBorder(new CompoundBorder(
            BorderFactory.createLineBorder(new Color(58, 58, 58)),
            new EmptyBorder(10, 10, 10, 10)
        ));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        return panel;
    }

    private JLabel createCardHeading(String text) {
        JLabel label = new JLabel(text);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 14f));
        label.setForeground(Color.WHITE);
        return label;
    }

    private JLabel createCardSubheading(String text) {
        JLabel label = new JLabel(text);
        label.setFont(label.getFont().deriveFont(Font.PLAIN, 11f));
        label.setForeground(Color.LIGHT_GRAY);
        return label;
    }

    private JPanel createStatCard(String labelText, JLabel valueLabel) {
        JPanel statCard = new JPanel();
        statCard.setLayout(new BoxLayout(statCard, BoxLayout.Y_AXIS));
        statCard.setOpaque(true);
        statCard.setBackground(new Color(44, 44, 44));
        statCard.setBorder(new CompoundBorder(
            BorderFactory.createLineBorder(new Color(64, 64, 64)),
            new EmptyBorder(8, 8, 8, 8)
        ));

        JLabel label = new JLabel(labelText);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        label.setForeground(Color.LIGHT_GRAY);
        label.setFont(label.getFont().deriveFont(Font.PLAIN, 11f));

        valueLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        statCard.add(valueLabel);
        statCard.add(Box.createVerticalStrut(2));
        statCard.add(label);
        return statCard;
    }

    private JLabel createStatValue(String text) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setForeground(Color.WHITE);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 18f));
        return label;
    }

    private JTextArea createTextArea(String initialText) {
        JTextArea area = new JTextArea(initialText);
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        area.setForeground(Color.WHITE);
        area.setBackground(new Color(34, 34, 34));
        area.setBorder(new EmptyBorder(6, 6, 6, 6));
        return area;
    }

    private JScrollPane createScrollPane(JTextArea area, int height) {
        JScrollPane scrollPane = new JScrollPane(area);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(58, 58, 58)));
        scrollPane.setPreferredSize(new Dimension(PluginPanel.PANEL_WIDTH - 24, height));
        scrollPane.setMinimumSize(new Dimension(PluginPanel.PANEL_WIDTH - 24, height));
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getViewport().setBackground(new Color(34, 34, 34));
        return scrollPane;
    }

    private String trim(String value, int max) {
        if (value == null || value.trim().isEmpty()) {
            return "Unknown";
        }
        if (value.length() <= max) {
            return value;
        }
        return value.substring(0, Math.max(0, max - 3)) + "...";
    }

    private String formatQty(long value) {
        return String.format("%,d", value);
    }

    private String formatSigned(long value) {
        if (value > 0) {
            return "+" + formatQty(value);
        }
        return formatQty(value);
    }
}
