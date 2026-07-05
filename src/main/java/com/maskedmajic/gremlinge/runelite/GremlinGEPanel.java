package com.maskedmajic.gremlinge.runelite;

import com.maskedmajic.gremlinge.FlipRecommendation;
import com.maskedmajic.gremlinge.ge.GeOfferState;
import com.maskedmajic.gremlinge.ge.LimitStatus;
import com.maskedmajic.gremlinge.profit.ProfitSummary;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicScrollBarUI;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;

public class GremlinGEPanel extends PluginPanel {
    private static final int CARD_GAP = 8;
    private static final int CONTENT_HEIGHT = 296;
    private static final int OFFER_ROW_HEIGHT = 72;
    private static final int FLIP_ROW_HEIGHT = 72;
    private static final int LIMIT_ROW_HEIGHT = 48;

    private static final String TAB_OFFERS = "offers";
    private static final String TAB_FLIPS = "flips";
    private static final String TAB_LIMITS = "limits";

    private final JLabel titleLabel = new JLabel("GremlinGE");
    private final JLabel subtitleLabel = new JLabel("GE helper");
    private final JLabel overviewLine1 = new JLabel("Open: 0   Active: 0");
    private final JLabel overviewLine2 = new JLabel("Partial: 0   Done: 0");
    private final JLabel profitHeadline = new JLabel("P/L: 0 gp");

    private final JPanel offersList = createListPanel();
    private final JPanel flipsList = createListPanel();
    private final JPanel limitsList = createListPanel();

    private final CardLayout sectionCards = new CardLayout();
    private final JPanel sectionCardPanel = new JPanel(sectionCards);
    private final Map<String, JButton> tabButtons = new LinkedHashMap<String, JButton>();
    private final JLabel workspaceSubtitle = new JLabel("Offers and progress");
    private final JPanel workspaceActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
    private final JButton refreshFlipsButton = new JButton("Refresh Flips");
    private final JButton resetProfitButton = new JButton("Reset Profit");
    private final JComboBox<String> tierDropdown = new JComboBox<>(new String[] {
        "All",
        "0-100k",
        "100k-1m",
        "1m-10m",
        "10m-50m",
        "50m+"
    });
    private String activeTab = TAB_OFFERS;

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
        content.add(buildTabbedSectionCard());

        add(content, BorderLayout.CENTER);

        configureActionButton(refreshFlipsButton);
        configureActionButton(resetProfitButton);
        configureDropdown(tierDropdown);
        workspaceActions.setOpaque(false);
        workspaceActions.add(refreshFlipsButton);

        setPlaceholder(offersList, "Waiting for live GE offers...");
        setPlaceholder(flipsList, "Waiting for scanner-backed suggestions...");
        setPlaceholder(limitsList, "No active limit usage tracked yet.");
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

        overviewLine1.setText("Open: " + openSlots + "   Active: " + activeOffers);
        overviewLine2.setText("Partial: " + partialOffers + "   Done: " + completedOffers);
    }

    public void updateOffers(List<GeOfferState> offers) {
        resetList(offersList);
        if (offers == null) {
            offers = Collections.emptyList();
        }
        for (GeOfferState offer : offers) {
            if (offer == null || offer.state == null || offer.isEmpty()) {
                continue;
            }
            offersList.add(buildOfferRow(offer));
            offersList.add(Box.createVerticalStrut(6));
        }
        if (offersList.getComponentCount() == 0) {
            setPlaceholder(offersList, "No active offers detected yet.");
        }
        refreshList(offersList);
    }

    public void updateRecommendations(List<FlipRecommendation> recommendations) {
        resetList(flipsList);
        if (recommendations == null || recommendations.isEmpty()) {
            setPlaceholder(flipsList, "No scanner-backed flip suggestions available yet.");
            refreshList(flipsList);
            return;
        }
        for (FlipRecommendation recommendation : recommendations) {
            if (recommendation == null || recommendation.candidate == null) {
                continue;
            }
            flipsList.add(buildFlipRow(recommendation));
            flipsList.add(Box.createVerticalStrut(6));
        }
        refreshList(flipsList);
    }

    public void updateLimits(List<LimitStatus> statuses) {
        resetList(limitsList);
        if (statuses == null || statuses.isEmpty()) {
            setPlaceholder(limitsList, "No active limit usage tracked yet.");
            refreshList(limitsList);
            return;
        }
        for (LimitStatus status : statuses) {
            limitsList.add(buildLimitRow(status));
            limitsList.add(Box.createVerticalStrut(6));
        }
        refreshList(limitsList);
    }

    public void updateProfit(ProfitSummary summary) {
        if (summary == null) {
            profitHeadline.setText("P/L: 0 gp");
            profitHeadline.setForeground(Color.WHITE);
            return;
        }

        profitHeadline.setText("P/L: " + formatSigned(summary.realizedProfit) + " gp");
        profitHeadline.setForeground(summary.realizedProfit >= 0 ? new Color(100, 220, 120) : new Color(220, 100, 100));
    }

    public JButton getRefreshFlipsButton() {
        return refreshFlipsButton;
    }

    public JButton getResetProfitButton() {
        return resetProfitButton;
    }

    public JComboBox<String> getTierDropdown() {
        return tierDropdown;
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
        JLabel overviewHeading = createCardHeading("Overview");
        overviewHeading.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(overviewHeading);
        card.add(Box.createVerticalStrut(6));

        configureOverviewLabel(overviewLine1);
        configureOverviewLabel(overviewLine2);
        card.add(overviewLine1);
        card.add(Box.createVerticalStrut(2));
        card.add(overviewLine2);
        card.add(Box.createVerticalStrut(8));

        profitHeadline.setAlignmentX(Component.CENTER_ALIGNMENT);
        profitHeadline.setHorizontalAlignment(SwingConstants.CENTER);
        profitHeadline.setFont(profitHeadline.getFont().deriveFont(Font.BOLD, 15f));
        profitHeadline.setForeground(Color.WHITE);
        card.add(profitHeadline);
        card.add(Box.createVerticalStrut(6));

        JPanel resetProfitWrap = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        resetProfitWrap.setOpaque(false);
        resetProfitWrap.setAlignmentX(Component.CENTER_ALIGNMENT);
        resetProfitWrap.add(resetProfitButton);
        card.add(resetProfitWrap);
        return card;
    }

    private JPanel buildTabbedSectionCard() {
        JPanel card = createCardPanel();
        card.add(createCardHeading("Tier Filter"));
        card.add(Box.createVerticalStrut(6));
        card.add(tierDropdown);
        card.add(Box.createVerticalStrut(8));
        card.add(buildTabBar());
        card.add(Box.createVerticalStrut(6));
        card.add(workspaceActions);
        card.add(Box.createVerticalStrut(8));

        sectionCardPanel.setOpaque(false);
        sectionCardPanel.add(createScrollPane(offersList), TAB_OFFERS);
        sectionCardPanel.add(createScrollPane(flipsList), TAB_FLIPS);
        sectionCardPanel.add(createScrollPane(limitsList), TAB_LIMITS);
        sectionCards.show(sectionCardPanel, activeTab);
        updateWorkspaceChrome();

        card.add(sectionCardPanel);
        return card;
    }

    private JPanel buildTabBar() {
        JPanel bar = new JPanel();
        bar.setLayout(new BoxLayout(bar, BoxLayout.X_AXIS));
        bar.setOpaque(false);
        addTabButton(bar, TAB_OFFERS, "Offers");
        addTabButton(bar, TAB_FLIPS, "Flips");
        addTabButton(bar, TAB_LIMITS, "Limits");
        refreshTabStyles();
        return bar;
    }

    private void addTabButton(JPanel bar, final String key, String label) {
        JButton button = new JButton(label);
        button.setFocusPainted(false);
        button.setAlignmentY(Component.CENTER_ALIGNMENT);
        button.setMaximumSize(new Dimension(96, 26));
        button.setPreferredSize(new Dimension(96, 26));
        button.setFont(button.getFont().deriveFont(Font.BOLD, 10f));
        button.addActionListener(e -> switchTab(key));
        tabButtons.put(key, button);
        bar.add(button);
        bar.add(Box.createHorizontalStrut(4));
    }

    private void switchTab(String key) {
        activeTab = key;
        sectionCards.show(sectionCardPanel, key);
        refreshTabStyles();
        updateWorkspaceChrome();
    }

    private void refreshTabStyles() {
        for (Map.Entry<String, JButton> entry : tabButtons.entrySet()) {
            boolean selected = entry.getKey().equals(activeTab);
            JButton button = entry.getValue();
            button.setBackground(selected ? new Color(66, 135, 245) : new Color(52, 52, 52));
            button.setForeground(Color.WHITE);
            button.setOpaque(true);
            button.setBorder(BorderFactory.createLineBorder(selected ? new Color(94, 156, 255) : new Color(72, 72, 72)));
        }
    }

    private void updateWorkspaceChrome() {
        refreshFlipsButton.setVisible(TAB_FLIPS.equals(activeTab));
        workspaceActions.revalidate();
        workspaceActions.repaint();
    }

    private JPanel buildOfferRow(GeOfferState offer) {
        JPanel row = createRowCard(OFFER_ROW_HEIGHT);
        row.add(buildTitleLine(trim(offer.itemName, 28), buildBadge(offer.offerType.toString(), badgeColorForType(offer.offerType.toString()))));
        row.add(Box.createVerticalStrut(2));
        row.add(buildMainValueLabel(offer.state + "  @ " + formatQty(offer.price)));
        row.add(Box.createVerticalStrut(4));
        JProgressBar progressBar = new JProgressBar(0, Math.max(1, offer.totalQuantity));
        progressBar.setValue(Math.min(offer.filledQuantity, Math.max(1, offer.totalQuantity)));
        progressBar.setStringPainted(true);
        progressBar.setString(formatQty(offer.filledQuantity) + "/" + formatQty(offer.totalQuantity));
        progressBar.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.add(progressBar);
        return row;
    }

    private JPanel buildFlipRow(FlipRecommendation recommendation) {
        JPanel row = createRowCard(FLIP_ROW_HEIGHT);
        JPanel title = buildTitleLine(trim(recommendation.candidate.name, 26), buildBadge(recommendation.candidate.volumeTag.toUpperCase(), badgeColorForVolume(recommendation.candidate.volumeTag)));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.add(title);
        row.add(Box.createVerticalStrut(2));

        JLabel net = buildBigNumberLabel(formatQty(recommendation.candidate.margin) + " gp net");
        net.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.add(net);
        row.add(Box.createVerticalStrut(2));

        JLabel prices = buildMainValueLabel("Buy " + formatQty(recommendation.candidate.buy) + "  Sell " + formatQty(recommendation.candidate.sell));
        prices.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.add(prices);
        row.add(Box.createVerticalStrut(2));

        JLabel meta = buildMetaLabel("Gross " + formatQty(recommendation.candidate.grossMargin) + "  Tax " + formatQty(recommendation.candidate.tax) + "  Left " + formatQty(recommendation.remainingLimit));
        meta.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.add(meta);
        return row;
    }

    private JPanel buildLimitRow(LimitStatus status) {
        JPanel row = createRowCard(LIMIT_ROW_HEIGHT);
        row.add(buildTitleLine(trim(status.itemName, 28), null));
        row.add(Box.createVerticalStrut(2));
        row.add(buildMainValueLabel(formatQty(status.boughtInWindow) + "/" + formatQty(status.buyLimit) + "  (" + status.resetEta + ")"));
        return row;
    }

    private JPanel buildTitleLine(String title, JLabel badge) {
        JPanel line = new JPanel(new BorderLayout(6, 0));
        line.setOpaque(false);
        line.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel label = new JLabel(title);
        label.setForeground(Color.WHITE);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 13f));
        line.add(label, BorderLayout.CENTER);
        if (badge != null) line.add(badge, BorderLayout.EAST);
        return line;
    }

    private JLabel buildMainValueLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(Color.WHITE);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 12f));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private JLabel buildBigNumberLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(Color.WHITE);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 14f));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private JLabel buildMetaLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(Color.LIGHT_GRAY);
        label.setFont(label.getFont().deriveFont(Font.PLAIN, 11f));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private JLabel buildBadge(String text, Color background) {
        JLabel badge = new JLabel(text);
        badge.setOpaque(true);
        badge.setBackground(background);
        badge.setForeground(Color.WHITE);
        badge.setFont(badge.getFont().deriveFont(Font.BOLD, 10f));
        badge.setBorder(new EmptyBorder(2, 6, 2, 6));
        return badge;
    }

    private JPanel createCardPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(true);
        panel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setBorder(new CompoundBorder(BorderFactory.createLineBorder(new Color(58, 58, 58)), new EmptyBorder(10, 10, 10, 10)));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        return panel;
    }

    private JPanel createRowCard(int rowHeight) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(true);
        panel.setBackground(new Color(36, 36, 36));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setBorder(new CompoundBorder(BorderFactory.createLineBorder(new Color(55, 55, 55)), new EmptyBorder(6, 8, 6, 8)));
        panel.setMinimumSize(new Dimension(0, rowHeight));
        panel.setPreferredSize(new Dimension(0, rowHeight));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, rowHeight));
        return panel;
    }

    private JPanel createListPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(true);
        panel.setBackground(new Color(34, 34, 34));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        return panel;
    }

    private JScrollPane createScrollPane(JPanel panel) {
        JScrollPane scrollPane = new JScrollPane(panel);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(58, 58, 58)));
        scrollPane.setPreferredSize(new Dimension(PluginPanel.PANEL_WIDTH - 24, CONTENT_HEIGHT));
        scrollPane.setMinimumSize(new Dimension(PluginPanel.PANEL_WIDTH - 24, CONTENT_HEIGHT));
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getViewport().setBackground(new Color(34, 34, 34));
        styleScrollbar(scrollPane.getVerticalScrollBar());
        return scrollPane;
    }

    private void styleScrollbar(JScrollBar scrollBar) {
        scrollBar.setPreferredSize(new Dimension(8, Integer.MAX_VALUE));
        scrollBar.setUI(new BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                thumbColor = new Color(45, 45, 45);
                trackColor = new Color(22, 22, 22);
            }

            protected JButton createDecreaseButton(int orientation) {
                return createZeroButton();
            }

            protected JButton createIncreaseButton(int orientation) {
                return createZeroButton();
            }

            @Override
            protected void paintThumb(Graphics g, JComponent c, java.awt.Rectangle thumbBounds) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(55, 55, 55));
                g2.fillRoundRect(thumbBounds.x + 1, thumbBounds.y, thumbBounds.width - 2, thumbBounds.height, 8, 8);
                g2.dispose();
            }

            @Override
            protected void paintTrack(Graphics g, JComponent c, java.awt.Rectangle trackBounds) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(20, 20, 20));
                g2.fillRect(trackBounds.x, trackBounds.y, trackBounds.width, trackBounds.height);
                g2.dispose();
            }

            private JButton createZeroButton() {
                JButton button = new JButton();
                button.setPreferredSize(new Dimension(0, 0));
                button.setMinimumSize(new Dimension(0, 0));
                button.setMaximumSize(new Dimension(0, 0));
                return button;
            }
        });
    }

    private void setPlaceholder(JPanel panel, String text) {
        resetList(panel);
        JLabel label = new JLabel(text);
        label.setForeground(Color.LIGHT_GRAY);
        label.setBorder(new EmptyBorder(8, 8, 8, 8));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(label);
        refreshList(panel);
    }

    private void resetList(JPanel panel) { panel.removeAll(); }
    private void refreshList(JPanel panel) { panel.revalidate(); panel.repaint(); }

    private JLabel createCardHeading(String text) {
        JLabel label = new JLabel(text);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 14f));
        label.setForeground(Color.WHITE);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private void configureOverviewLabel(JLabel label) {
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setForeground(Color.WHITE);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 12f));
    }

    private void configureActionButton(JButton button) {
        button.setFocusPainted(false);
        button.setFont(button.getFont().deriveFont(Font.BOLD, 10f));
        button.setBackground(new Color(58, 58, 58));
        button.setForeground(Color.WHITE);
        button.setBorder(BorderFactory.createLineBorder(new Color(82, 82, 82)));
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.setMaximumSize(new Dimension(120, 24));
    }

    private void configureDropdown(JComboBox<String> dropdown) {
        dropdown.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        dropdown.setPreferredSize(new Dimension(PluginPanel.PANEL_WIDTH - 32, 26));
        dropdown.setAlignmentX(Component.LEFT_ALIGNMENT);
        dropdown.setBackground(new Color(52, 52, 52));
        dropdown.setForeground(Color.WHITE);
        dropdown.setFocusable(false);
    }

    private Color badgeColorForType(String type) { return "BUY".equalsIgnoreCase(type) ? new Color(66, 135, 245) : new Color(196, 96, 76); }
    private Color badgeColorForVolume(String tag) {
        if ("high".equalsIgnoreCase(tag)) return new Color(78, 138, 76);
        if ("med".equalsIgnoreCase(tag)) return new Color(177, 128, 53);
        return new Color(92, 92, 92);
    }

    private String trim(String value, int max) {
        if (value == null || value.trim().isEmpty()) return "Unknown";
        if (value.length() <= max) return value;
        return value.substring(0, Math.max(0, max - 3)) + "...";
    }

    private String formatQty(long value) { return String.format("%,d", value); }
    private String formatSigned(long value) { return value > 0 ? "+" + formatQty(value) : formatQty(value); }
}
