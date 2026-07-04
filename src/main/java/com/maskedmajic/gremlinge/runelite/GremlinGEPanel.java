package com.maskedmajic.gremlinge.runelite;

import com.maskedmajic.gremlinge.ge.GeOfferState;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.util.Collections;
import java.util.List;

/**
 * Basic GremlinGE panel skeleton.
 *
 * This is intentionally simple: we want to prove in-client state rendering
 * before building fancy tables and controls.
 */
public class GremlinGEPanel extends JPanel {
    private final JLabel headerLabel = new JLabel("GremlinGE");
    private final JTextArea offersArea = new JTextArea();
    private final JTextArea limitsArea = new JTextArea();

    public GremlinGEPanel() {
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

        add(content, BorderLayout.CENTER);
    }

    public void updateOffers(List<GeOfferState> offers) {
        if (offers == null) {
            offers = Collections.emptyList();
        }

        StringBuilder sb = new StringBuilder();
        for (GeOfferState offer : offers) {
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

    public void updateLimits(String text) {
        limitsArea.setText(text == null || text.isEmpty()
            ? "No active limit usage tracked yet.\n"
            : text);
    }
}
