package com.maskedmajic.gremlinge.runelite;

import com.maskedmajic.gremlinge.ge.GeOfferEvent;
import com.maskedmajic.gremlinge.ge.GeOfferState;
import com.maskedmajic.gremlinge.ge.LimitStatus;
import com.maskedmajic.gremlinge.profit.ProfitSummary;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.util.Collections;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.PluginPanel;

/**
 * RuneLite panel focused on live GE state first, with limits and profit as support data.
 */
public class GremlinGEPanel extends PluginPanel {
    private static final int SECTION_HEIGHT = 100;

    private final JLabel headerLabel = new JLabel("GremlinGE");
    private final JLabel summaryLabel = new JLabel("Waiting for GE state...");
    private final JTextArea offersArea = new JTextArea();
    private final JTextArea eventsArea = new JTextArea();
    private final JTextArea limitsArea = new JTextArea();
    private final JTextArea profitArea = new JTextArea();

    public GremlinGEPanel() {
        super(false);
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(8, 8, 8, 8));

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        headerLabel.setAlignmentX(LEFT_ALIGNMENT);
        headerLabel.setFont(headerLabel.getFont().deriveFont(Font.BOLD, 16f));
        summaryLabel.setAlignmentX(LEFT_ALIGNMENT);
        summaryLabel.setBorder(new EmptyBorder(4, 0, 8, 0));

        content.add(headerLabel);
        content.add(summaryLabel);

        configureArea(offersArea, "Offers will appear here once GE state is wired in.\n");
        configureArea(eventsArea, "Recent GE events will appear here once state changes are detected.\n");
        configureArea(limitsArea, "Limit usage will appear here once events are flowing.\n");
        configureArea(profitArea, "Profit tracking will appear here once fills are recorded.\n");

        content.add(makeSection("Active Offers", offersArea, 132));
        content.add(makeSection("Recent Events", eventsArea, 110));
        content.add(makeSection("Limit Status", limitsArea, 96));
        content.add(makeSection("Profit", profitArea, 84));

        add(content, BorderLayout.CENTER);
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

        summaryLabel.setText(
            "Open slots: " + openSlots
                + "  |  Active: " + activeOffers
                + "  |  Partial: " + partialOffers
                + "  |  Complete: " + completedOffers
        );
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
              .append(trim(offer.itemName, 22))
              .append(" | ")
              .append(offer.offerType)
              .append(" ")
              .append(offer.state)
              .append(" | ")
              .append(formatQty(offer.filledQuantity))
              .append("/")
              .append(formatQty(offer.totalQuantity))
              .append(" | @ ")
              .append(formatQty(offer.price));

            if (offer.buyLimit > 0) {
                sb.append(" | lim ").append(formatQty(offer.buyLimit));
            }
            sb.append("\n");
        }

        if (sb.length() == 0) {
            sb.append("No active offers detected yet.\n");
        }

        offersArea.setText(sb.toString());
        offersArea.setCaretPosition(0);
    }

    public void updateEvents(List<GeOfferEvent> events) {
        if (events == null || events.isEmpty()) {
            eventsArea.setText("No GE events recorded yet.\n");
            return;
        }

        StringBuilder sb = new StringBuilder();
        int start = Math.max(0, events.size() - 10);
        for (int i = events.size() - 1; i >= start; i--) {
            GeOfferEvent event = events.get(i);
            if (event == null) {
                continue;
            }

            sb.append("#")
              .append(event.slotIndex + 1)
              .append(" ")
              .append(event.type)
              .append(" | ")
              .append(trim(event.itemName, 18))
              .append(" | ")
              .append(event.offerType);

            if (event.deltaFilled > 0) {
                sb.append(" | +").append(formatQty(event.deltaFilled));
            }

            if (event.newFilledQuantity > 0) {
                sb.append(" | total ").append(formatQty(event.newFilledQuantity));
            }

            if (event.price > 0) {
                sb.append(" | @ ").append(formatQty(event.price));
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
            sb.append(trim(status.itemName, 20))
              .append(" | ")
              .append(formatQty(status.boughtInWindow))
              .append("/")
              .append(formatQty(status.buyLimit))
              .append(" used | left ")
              .append(formatQty(status.remaining))
              .append(" | reset ")
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
        sb.append("Bought qty: ").append(formatQty(summary.totalBuys)).append("\n");
        sb.append("Sold qty: ").append(formatQty(summary.totalSells)).append("\n");
        sb.append("Buy value: ").append(formatQty(summary.grossBuyValue)).append(" gp\n");
        sb.append("Sell value: ").append(formatQty(summary.grossSellValue)).append(" gp\n");
        sb.append("Realized P/L: ").append(formatSigned(summary.realizedProfit)).append(" gp\n");
        profitArea.setText(sb.toString());
        profitArea.setCaretPosition(0);
    }

    private JPanel makeSection(String title, JTextArea area, int height) {
        JPanel section = new JPanel(new BorderLayout());
        section.setAlignmentX(LEFT_ALIGNMENT);
        section.setBorder(new EmptyBorder(0, 0, 8, 0));

        JLabel label = new JLabel(title);
        label.setBorder(new EmptyBorder(0, 0, 4, 0));
        section.add(label, BorderLayout.NORTH);

        JScrollPane scroll = makeScroll(area, height);
        section.add(scroll, BorderLayout.CENTER);
        return section;
    }

    private void configureArea(JTextArea area, String initialText) {
        area.setEditable(false);
        area.setLineWrap(false);
        area.setWrapStyleWord(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        area.setText(initialText);
        area.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
    }

    private JScrollPane makeScroll(JTextArea area, int height) {
        JScrollPane scroll = new JScrollPane(area);
        scroll.setAlignmentX(LEFT_ALIGNMENT);
        scroll.setPreferredSize(new Dimension(PluginPanel.PANEL_WIDTH - 16, height));
        scroll.setMinimumSize(new Dimension(PluginPanel.PANEL_WIDTH - 16, Math.min(height, SECTION_HEIGHT)));
        return scroll;
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
