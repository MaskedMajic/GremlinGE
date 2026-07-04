package com.maskedmajic.gremlinge.runelite;

import com.maskedmajic.gremlinge.ge.GeOfferState;
import com.maskedmajic.gremlinge.ge.LimitStatus;
import com.maskedmajic.gremlinge.profit.ProfitSummary;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.util.Collections;
import java.util.List;
import net.runelite.client.ui.PluginPanel;

/**
 * Basic GremlinGE panel skeleton.
 */
public class GremlinGEPanel extends PluginPanel {
    private final JLabel headerLabel = new JLabel("GremlinGE");
    private final JTextArea offersArea = new JTextArea();
    private final JTextArea limitsArea = new JTextArea();
    private final JTextArea profitArea = new JTextArea();

    public GremlinGEPanel() {
        super(false);
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(8, 8, 8, 8));

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        headerLabel.setAlignmentX(LEFT_ALIGNMENT);
        content.add(headerLabel);

        offersArea.setEditable(false);
        offersArea.setLineWrap(true);
        offersArea.setWrapStyleWord(true);
        offersArea.setText("Offers will appear here once GE state is wired in.\n");
        JScrollPane offersScroll = new JScrollPane(offersArea);
        offersScroll.setAlignmentX(LEFT_ALIGNMENT);
        content.add(new JLabel("Active Offers"));
        content.add(offersScroll);

        limitsArea.setEditable(false);
        limitsArea.setLineWrap(true);
        limitsArea.setWrapStyleWord(true);
        limitsArea.setText("Limit usage will appear here once events are flowing.\n");
        JScrollPane limitsScroll = new JScrollPane(limitsArea);
        limitsScroll.setAlignmentX(LEFT_ALIGNMENT);
        content.add(new JLabel("Limit Status"));
        content.add(limitsScroll);

        profitArea.setEditable(false);
        profitArea.setLineWrap(true);
        profitArea.setWrapStyleWord(true);
        profitArea.setText("Profit tracking will appear here once fills are recorded.\n");
        JScrollPane profitScroll = new JScrollPane(profitArea);
        profitScroll.setAlignmentX(LEFT_ALIGNMENT);
        content.add(new JLabel("Profit"));
        content.add(profitScroll);

        add(content, BorderLayout.CENTER);
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

            sb.append("Slot ").append(offer.slotIndex + 1)
              .append(" • ")
              .append(offer.itemName != null ? offer.itemName : "Unknown")
              .append(" • ")
              .append(offer.offerType)
              .append(" • ")
              .append(offer.state)
              .append(" • ")
              .append(offer.filledQuantity)
              .append("/")
              .append(offer.totalQuantity)
              .append(" @ ")
              .append(offer.price)
              .append("\n");
        }

        if (sb.length() == 0) {
            sb.append("No active offers detected yet.\n");
        }

        offersArea.setText(sb.toString());
    }

    public void updateLimits(List<LimitStatus> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            limitsArea.setText("No active limit usage tracked yet.\n");
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (LimitStatus status : statuses) {
            sb.append(status.itemName)
              .append(" • used ")
              .append(status.boughtInWindow)
              .append("/")
              .append(status.buyLimit)
              .append(" • left ")
              .append(status.remaining)
              .append(" • reset ")
              .append(status.resetEta)
              .append("\n");
        }
        limitsArea.setText(sb.toString());
    }

    public void updateProfit(ProfitSummary summary) {
        if (summary == null) {
            profitArea.setText("No profit data yet.\n");
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Bought qty: ").append(summary.totalBuys).append("\n");
        sb.append("Sold qty: ").append(summary.totalSells).append("\n");
        sb.append("Gross buy value: ").append(summary.grossBuyValue).append("\n");
        sb.append("Gross sell value: ").append(summary.grossSellValue).append("\n");
        sb.append("Realized P/L: ").append(summary.realizedProfit).append("\n");
        profitArea.setText(sb.toString());
    }
}
