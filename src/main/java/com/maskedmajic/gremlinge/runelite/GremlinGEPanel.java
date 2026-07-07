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
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicScrollBarUI;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.AsyncBufferedImage;
import net.runelite.client.util.SwingUtil;

public class GremlinGEPanel extends PluginPanel {
    private static final int CARD_GAP = 8;
    private static final int CARD_PADDING = 10;
    private static final int CONTENT_HEIGHT = 280;
    private static final int OFFER_ROW_MIN_HEIGHT = 66;
    private static final int FLIP_ROW_MIN_HEIGHT = 58;
    private static final int LIMIT_ROW_MIN_HEIGHT = 42;
    private static final int ICON_SIZE = 28;
    private static final int MAX_SUGGESTIONS = 8;
    private static final int SUGGESTION_DEBOUNCE_MILLIS = 120;
    private static final Color CARD_BG = ColorScheme.DARKER_GRAY_COLOR;
    private static final Color ROW_BG = new Color(36, 36, 36);
    private static final Color LIST_BG = new Color(34, 34, 34);
    private static final Color BORDER_COLOR = new Color(58, 58, 58);
    private static final Color ACCENT_COLOR = new Color(199, 129, 57);
    private static final Color BUY_COLOR = new Color(66, 135, 245);
    private static final Color SELL_COLOR = new Color(196, 96, 76);
    private static final Color PROFIT_POSITIVE = new Color(110, 210, 130);
    private static final Color PROFIT_NEGATIVE = new Color(220, 100, 100);
    private static final Color FIELD_BG = new Color(28, 28, 28);
    private static final Color MUTED_TEXT = new Color(158, 158, 158);
    private static final Color ICON_PLACEHOLDER_BG = new Color(48, 48, 48);

    private static final String TAB_OFFERS = "offers";
    private static final String TAB_FLIPS = "flips";
    private static final String TAB_LIMITS = "limits";

    private final JLabel titleLabel = new JLabel("GremlinGE");
    private final JLabel statusDot = new JLabel("●");
    private final JLabel subtitleLabel = new JLabel("tracking 0 offers");
    private final JLabel openChipValue = new JLabel("0");
    private final JLabel activeChipValue = new JLabel("0");
    private final JLabel partialChipValue = new JLabel("0");
    private final JLabel doneChipValue = new JLabel("0");
    private final JLabel sessionCaption = new JLabel("SESSION P/L");
    private final JLabel profitHeadline = new JLabel("0 gp");
    private final JLabel fillsBadge = new JLabel("0 fills");
    private final JPanel profitCard = new JPanel(new BorderLayout(8, 0));

    private final JPanel offersList = createListPanel();
    private final JPanel flipsList = createListPanel();
    private final JPanel limitsList = createListPanel();

    private final CardLayout sectionCards = new CardLayout();
    private final JPanel sectionCardPanel = new JPanel(sectionCards);
    private final Map<String, JButton> tabButtons = new LinkedHashMap<String, JButton>();
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

    private final JTextField searchField = new JTextField();
    private final DefaultListModel<String> suggestionModel = new DefaultListModel<>();
    private final JList<String> suggestionList = new JList<>(suggestionModel);
    private final JPopupMenu suggestionPopup = new JPopupMenu();
    private final Timer suggestionDebounceTimer;

    private List<FlipRecommendation> lastRecommendations = Collections.emptyList();
    private FlipRecommendation searchResult;
    private String searchedItemName;
    private boolean searchLoading;
    private boolean suppressSearchEvents;
    private String activeTab = TAB_OFFERS;

    private Function<String, List<String>> suggestionProvider = query -> Collections.emptyList();
    private Consumer<String> searchSelectionHandler = itemName -> { };
    private Function<Integer, AsyncBufferedImage> iconLoader = itemId -> null;

    public GremlinGEPanel() {
        super(false);
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(8, 8, 8, 8));
        setBackground(ColorScheme.DARK_GRAY_COLOR);

        suggestionDebounceTimer = new Timer(SUGGESTION_DEBOUNCE_MILLIS, e -> refreshSuggestions());
        suggestionDebounceTimer.setRepeats(false);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);

        content.add(buildHeaderCard());
        content.add(Box.createVerticalStrut(CARD_GAP));
        content.add(buildOverviewCard());
        content.add(Box.createVerticalStrut(CARD_GAP));
        content.add(buildSearchCard());
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

        openChipValue.setText(String.valueOf(openSlots));
        activeChipValue.setText(String.valueOf(activeOffers));
        activeChipValue.setForeground(activeOffers > 0 ? ACCENT_COLOR : Color.WHITE);
        partialChipValue.setText(String.valueOf(partialOffers));
        doneChipValue.setText(String.valueOf(completedOffers));

        statusDot.setForeground(activeOffers > 0 ? new Color(110, 210, 130) : MUTED_TEXT);
        subtitleLabel.setText("tracking " + activeOffers + " offer" + (activeOffers == 1 ? "" : "s"));
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
        lastRecommendations = recommendations != null ? recommendations : Collections.<FlipRecommendation>emptyList();
        renderFlips();
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
            profitHeadline.setText("0 gp");
            fillsBadge.setText("0 fills");
            stylePofitCard(true);
            return;
        }

        boolean positive = summary.realizedProfit >= 0;
        profitHeadline.setText(formatSigned(summary.realizedProfit) + " gp");
        fillsBadge.setText((summary.totalBuys + summary.totalSells) + " fills");
        stylePofitCard(positive);
    }

    private void stylePofitCard(boolean positive) {
        Color textColor = positive ? PROFIT_POSITIVE : PROFIT_NEGATIVE;
        Color bg = positive ? new Color(32, 46, 36) : new Color(48, 32, 32);
        Color border = positive ? new Color(66, 100, 74) : new Color(100, 66, 66);
        profitHeadline.setForeground(textColor);
        sessionCaption.setForeground(positive ? new Color(140, 190, 150) : new Color(210, 150, 150));
        profitCard.setBackground(bg);
        profitCard.setBorder(new CompoundBorder(BorderFactory.createLineBorder(border), new EmptyBorder(8, 10, 8, 10)));
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

    /** Called on the EDT to resolve an item's icon (e.g. itemManager::getImage). May return null while unresolved. */
    public void setIconLoader(Function<Integer, AsyncBufferedImage> iconLoader) {
        this.iconLoader = iconLoader != null ? iconLoader : itemId -> null;
    }

    /** Called on the EDT with in-memory prefix/substring matches for the current search text. Keep this fast (no network). */
    public void setSuggestionProvider(Function<String, List<String>> suggestionProvider) {
        this.suggestionProvider = suggestionProvider != null ? suggestionProvider : query -> Collections.emptyList();
    }

    /** Called on the EDT when the user commits a search (click, or Enter). Do the actual lookup off-EDT. */
    public void setSearchSelectionHandler(Consumer<String> searchSelectionHandler) {
        this.searchSelectionHandler = searchSelectionHandler != null ? searchSelectionHandler : itemName -> { };
    }

    /** Must be called on the EDT once a background lookup for {@code itemName} completes successfully. */
    public void showSearchResult(String itemName, FlipRecommendation recommendation) {
        if (searchedItemName == null || !searchedItemName.equals(itemName)) {
            return;
        }
        searchLoading = false;
        searchResult = recommendation;
        switchTab(TAB_FLIPS);
        renderFlips();
    }

    /** Must be called on the EDT if a background lookup for {@code itemName} finds nothing or fails. */
    public void showSearchError(String itemName) {
        if (searchedItemName == null || !searchedItemName.equals(itemName)) {
            return;
        }
        searchLoading = false;
        searchResult = null;
        switchTab(TAB_FLIPS);
        renderFlips();
    }

    private JPanel buildHeaderCard() {
        JPanel card = createCardPanel();
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel avatar = new JLabel("G");
        avatar.setPreferredSize(new Dimension(32, 32));
        avatar.setHorizontalAlignment(SwingConstants.CENTER);
        avatar.setVerticalAlignment(SwingConstants.CENTER);
        avatar.setOpaque(true);
        avatar.setBackground(ACCENT_COLOR);
        avatar.setForeground(Color.WHITE);
        avatar.setFont(FontManager.getDefaultBoldFont().deriveFont(15f));
        avatar.setBorder(BorderFactory.createLineBorder(new Color(160, 100, 40)));

        JPanel titleBlock = new JPanel();
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));
        titleBlock.setOpaque(false);

        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        titleLabel.setFont(FontManager.getDefaultBoldFont().deriveFont(16f));
        titleLabel.setForeground(Color.WHITE);

        JPanel statusLine = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        statusLine.setOpaque(false);
        statusLine.setAlignmentX(Component.LEFT_ALIGNMENT);
        statusDot.setFont(statusDot.getFont().deriveFont(8f));
        statusDot.setForeground(MUTED_TEXT);
        subtitleLabel.setForeground(MUTED_TEXT);
        subtitleLabel.setFont(FontManager.getDefaultFont().deriveFont(11f));
        statusLine.add(statusDot);
        statusLine.add(subtitleLabel);

        titleBlock.add(titleLabel);
        titleBlock.add(statusLine);

        row.add(avatar, BorderLayout.WEST);
        row.add(titleBlock, BorderLayout.CENTER);
        card.add(row);
        return card;
    }

    private JPanel buildOverviewCard() {
        JPanel card = createCardPanel();
        card.add(createCardHeading("Overview"));
        card.add(Box.createVerticalStrut(8));

        JPanel statsGrid = new JPanel(new GridLayout(2, 2, 8, 8));
        statsGrid.setOpaque(false);
        statsGrid.setAlignmentX(Component.LEFT_ALIGNMENT);
        statsGrid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 84));
        statsGrid.add(buildStatChip(openChipValue, "OPEN"));
        statsGrid.add(buildStatChip(activeChipValue, "ACTIVE"));
        statsGrid.add(buildStatChip(partialChipValue, "PARTIAL"));
        statsGrid.add(buildStatChip(doneChipValue, "DONE"));
        card.add(statsGrid);
        card.add(Box.createVerticalStrut(10));

        profitCard.setOpaque(true);
        profitCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        profitCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));

        JPanel textBlock = new JPanel();
        textBlock.setLayout(new BoxLayout(textBlock, BoxLayout.Y_AXIS));
        textBlock.setOpaque(false);
        sessionCaption.setFont(FontManager.getDefaultFont().deriveFont(9f));
        sessionCaption.setAlignmentX(Component.LEFT_ALIGNMENT);
        profitHeadline.setFont(FontManager.getDefaultBoldFont().deriveFont(17f));
        profitHeadline.setAlignmentX(Component.LEFT_ALIGNMENT);
        textBlock.add(sessionCaption);
        textBlock.add(profitHeadline);

        fillsBadge.setForeground(MUTED_TEXT);
        fillsBadge.setFont(FontManager.getDefaultFont().deriveFont(10f));
        fillsBadge.setVerticalAlignment(SwingConstants.CENTER);

        profitCard.add(textBlock, BorderLayout.CENTER);
        profitCard.add(fillsBadge, BorderLayout.EAST);
        stylePofitCard(true);
        card.add(profitCard);
        card.add(Box.createVerticalStrut(8));

        JPanel resetProfitWrap = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        resetProfitWrap.setOpaque(false);
        resetProfitWrap.setAlignmentX(Component.LEFT_ALIGNMENT);
        resetProfitWrap.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        resetProfitWrap.add(resetProfitButton);
        card.add(resetProfitWrap);
        return card;
    }

    private JPanel buildStatChip(JLabel valueLabel, String caption) {
        JPanel chip = new JPanel(new BorderLayout());
        chip.setOpaque(true);
        chip.setBackground(ROW_BG);
        chip.setBorder(new CompoundBorder(BorderFactory.createLineBorder(BORDER_COLOR), new EmptyBorder(5, 4, 5, 4)));

        valueLabel.setForeground(Color.WHITE);
        valueLabel.setFont(FontManager.getDefaultBoldFont().deriveFont(15f));
        valueLabel.setHorizontalAlignment(SwingConstants.CENTER);

        JLabel captionLabel = new JLabel(caption);
        captionLabel.setForeground(MUTED_TEXT);
        captionLabel.setFont(FontManager.getDefaultFont().deriveFont(9f));
        captionLabel.setHorizontalAlignment(SwingConstants.CENTER);

        JPanel captionWrap = new JPanel(new BorderLayout());
        captionWrap.setOpaque(false);
        captionWrap.setBorder(new EmptyBorder(2, 0, 0, 0));
        captionWrap.add(captionLabel, BorderLayout.CENTER);

        chip.add(valueLabel, BorderLayout.CENTER);
        chip.add(captionWrap, BorderLayout.SOUTH);
        return chip;
    }

    private JPanel buildSearchCard() {
        JPanel card = createCardPanel();
        card.add(createCardHeading("Search"));
        card.add(Box.createVerticalStrut(6));

        searchField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        searchField.setPreferredSize(new Dimension(10, 26));
        searchField.setBackground(FIELD_BG);
        searchField.setForeground(Color.WHITE);
        searchField.setCaretColor(Color.WHITE);
        searchField.setFont(FontManager.getDefaultFont().deriveFont(12f));
        searchField.setBorder(new CompoundBorder(BorderFactory.createLineBorder(BORDER_COLOR), new EmptyBorder(2, 8, 2, 8)));
        searchField.setToolTipText("Search any GE item by name");
        searchField.setAlignmentX(Component.LEFT_ALIGNMENT);
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { onSearchTextChanged(); }
            @Override public void removeUpdate(DocumentEvent e) { onSearchTextChanged(); }
            @Override public void changedUpdate(DocumentEvent e) { onSearchTextChanged(); }
        });
        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                handleSearchKey(e);
            }
        });
        card.add(searchField);

        configureSuggestionList();
        return card;
    }

    private void configureSuggestionList() {
        suggestionList.setBackground(FIELD_BG);
        suggestionList.setForeground(Color.WHITE);
        suggestionList.setSelectionBackground(ACCENT_COLOR);
        suggestionList.setSelectionForeground(Color.WHITE);
        suggestionList.setFont(FontManager.getDefaultFont().deriveFont(12f));
        suggestionList.setFixedCellHeight(24);
        suggestionList.setBorder(new EmptyBorder(2, 8, 2, 8));
        suggestionList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int index = suggestionList.locationToIndex(e.getPoint());
                if (index >= 0) {
                    commitSelection(suggestionModel.get(index));
                }
            }
        });

        suggestionPopup.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
        suggestionPopup.setBackground(FIELD_BG);
        suggestionPopup.add(suggestionList);
        suggestionPopup.setFocusable(false);
    }

    private void onSearchTextChanged() {
        if (suppressSearchEvents) {
            return;
        }

        String rawQuery = currentQueryText();
        if (searchedItemName != null && !searchedItemName.equalsIgnoreCase(rawQuery)) {
            searchedItemName = null;
            searchResult = null;
            searchLoading = false;
        }
        renderFlips();

        if (rawQuery.isEmpty()) {
            suggestionDebounceTimer.stop();
            hideSuggestions();
            return;
        }
        suggestionDebounceTimer.restart();
    }

    private void refreshSuggestions() {
        showSuggestions(suggestionProvider.apply(currentQueryText()));
    }

    private void showSuggestions(List<String> matches) {
        if (matches == null || matches.isEmpty()) {
            hideSuggestions();
            return;
        }

        suggestionModel.clear();
        int limit = Math.min(matches.size(), MAX_SUGGESTIONS);
        for (int i = 0; i < limit; i++) {
            suggestionModel.addElement(matches.get(i));
        }
        suggestionList.setSelectedIndex(0);
        suggestionPopup.setPreferredSize(new Dimension(Math.max(160, searchField.getWidth()), limit * 24 + 4));
        if (!suggestionPopup.isVisible()) {
            suggestionPopup.show(searchField, 0, searchField.getHeight());
        }
    }

    private void hideSuggestions() {
        if (suggestionPopup.isVisible()) {
            suggestionPopup.setVisible(false);
        }
    }

    private void handleSearchKey(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_DOWN) {
            if (suggestionPopup.isVisible() && !suggestionModel.isEmpty()) {
                int next = Math.min(suggestionList.getSelectedIndex() + 1, suggestionModel.size() - 1);
                suggestionList.setSelectedIndex(Math.max(next, 0));
            }
            e.consume();
        } else if (e.getKeyCode() == KeyEvent.VK_UP) {
            if (suggestionPopup.isVisible() && !suggestionModel.isEmpty()) {
                int prev = Math.max(suggestionList.getSelectedIndex() - 1, 0);
                suggestionList.setSelectedIndex(prev);
            }
            e.consume();
        } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
            if (suggestionPopup.isVisible() && suggestionList.getSelectedIndex() >= 0) {
                commitSelection(suggestionModel.get(suggestionList.getSelectedIndex()));
            } else if (!currentQueryText().isEmpty()) {
                commitSelection(currentQueryText());
            }
        } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            hideSuggestions();
        }
    }

    private void commitSelection(String itemName) {
        suggestionDebounceTimer.stop();
        hideSuggestions();
        searchedItemName = itemName;
        searchResult = null;
        searchLoading = true;

        suppressSearchEvents = true;
        try {
            searchField.setText(itemName);
        } finally {
            suppressSearchEvents = false;
        }
        renderFlips();

        searchSelectionHandler.accept(itemName);
    }

    private String currentQueryText() {
        String text = searchField.getText();
        return text == null ? "" : text.trim();
    }

    private JPanel buildTabbedSectionCard() {
        JPanel card = createCardPanel();
        card.add(createCardHeading("Tier Filter"));
        card.add(Box.createVerticalStrut(6));
        card.add(tierDropdown);
        card.add(Box.createVerticalStrut(10));
        card.add(buildTabBar());
        card.add(Box.createVerticalStrut(8));
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
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
        bar.setOpaque(false);
        bar.setAlignmentX(Component.LEFT_ALIGNMENT);
        bar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        addTabButton(bar, TAB_OFFERS, "Offers");
        addTabButton(bar, TAB_FLIPS, "Flips");
        addTabButton(bar, TAB_LIMITS, "Limits");
        refreshTabStyles();
        return bar;
    }

    private void addTabButton(JPanel bar, final String key, String label) {
        JButton button = new JButton(label);
        SwingUtil.removeButtonDecorations(button);
        button.setOpaque(true);
        button.setFocusPainted(false);
        button.setFont(FontManager.getDefaultBoldFont().deriveFont(10f));
        button.addActionListener(e -> switchTab(key));
        tabButtons.put(key, button);
        bar.add(button);
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
            button.setBackground(selected ? ACCENT_COLOR : new Color(52, 52, 52));
            button.setForeground(Color.WHITE);
            button.setBorder(new CompoundBorder(
                BorderFactory.createLineBorder(selected ? new Color(219, 154, 88) : new Color(72, 72, 72)),
                new EmptyBorder(3, 7, 3, 7)));
        }
    }

    private void updateWorkspaceChrome() {
        refreshFlipsButton.setVisible(TAB_FLIPS.equals(activeTab));
        workspaceActions.revalidate();
        workspaceActions.repaint();
    }

    private void renderFlips() {
        resetList(flipsList);

        String rawQuery = currentQueryText();
        String query = rawQuery.toLowerCase();
        boolean showingSearchResult = searchedItemName != null;

        if (showingSearchResult) {
            if (searchLoading) {
                flipsList.add(buildSearchStatusRow("Searching \"" + trim(searchedItemName, 30) + "\"..."));
            } else if (searchResult != null) {
                flipsList.add(buildSearchResultRow(searchResult));
            } else {
                flipsList.add(buildSearchErrorRow(searchedItemName));
            }
            flipsList.add(Box.createVerticalStrut(10));
        }

        List<FlipRecommendation> filtered = new ArrayList<FlipRecommendation>();
        for (FlipRecommendation recommendation : lastRecommendations) {
            if (recommendation == null || recommendation.candidate == null) {
                continue;
            }
            if (!query.isEmpty() && !recommendation.candidate.name.toLowerCase().contains(query)) {
                continue;
            }
            if (showingSearchResult && recommendation.candidate.name.equalsIgnoreCase(searchedItemName)) {
                continue;
            }
            filtered.add(recommendation);
        }

        for (FlipRecommendation recommendation : filtered) {
            flipsList.add(buildFlipRow(recommendation));
            flipsList.add(Box.createVerticalStrut(6));
        }

        if (filtered.isEmpty() && !showingSearchResult) {
            String message = lastRecommendations.isEmpty()
                ? "No scanner-backed flip suggestions available yet."
                : "No flips match \"" + rawQuery + "\".";
            setPlaceholder(flipsList, message);
        }

        refreshList(flipsList);
    }

    private JPanel buildSearchResultRow(FlipRecommendation recommendation) {
        JPanel row = buildFlipRow(recommendation);
        row.setBorder(new CompoundBorder(BorderFactory.createLineBorder(ACCENT_COLOR, 2), new EmptyBorder(6, 7, 6, 7)));
        return row;
    }

    private JPanel buildSearchErrorRow(String itemName) {
        return buildSearchStatusRow("No live price data for \"" + trim(itemName, 30) + "\"", new Color(196, 96, 76));
    }

    private JPanel buildSearchStatusRow(String text) {
        return buildSearchStatusRow(text, BORDER_COLOR);
    }

    private JPanel buildSearchStatusRow(String text, Color borderColor) {
        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.Y_AXIS));
        row.setOpaque(true);
        row.setBackground(ROW_BG);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setBorder(new CompoundBorder(BorderFactory.createLineBorder(borderColor, 2), new EmptyBorder(8, 10, 8, 10)));
        row.setMinimumSize(new Dimension(0, 40));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        JLabel label = new JLabel(text);
        label.setForeground(Color.LIGHT_GRAY);
        label.setFont(FontManager.getDefaultFont().deriveFont(12f));
        row.add(label);
        return row;
    }

    private JPanel buildOfferRow(GeOfferState offer) {
        JPanel row = createRowCard(OFFER_ROW_MIN_HEIGHT);
        Color typeColor = badgeColorForType(offer.offerType.toString());
        row.add(buildIconLabel(offer.itemId), BorderLayout.WEST);

        JPanel content = buildContentBox();
        content.add(buildTitleLine(trim(offer.itemName, 20), buildBadge(offer.offerType.toString(), typeColor)));
        content.add(Box.createVerticalStrut(5));

        SlimBar bar = new SlimBar(typeColor);
        bar.setFraction(offer.totalQuantity > 0 ? (double) offer.filledQuantity / offer.totalQuantity : 0);
        bar.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(bar);
        content.add(Box.createVerticalStrut(5));

        JPanel bottomLine = new JPanel(new BorderLayout());
        bottomLine.setOpaque(false);
        bottomLine.setAlignmentX(Component.LEFT_ALIGNMENT);
        bottomLine.setMaximumSize(new Dimension(Integer.MAX_VALUE, 16));
        bottomLine.add(buildMainValueLabel(formatQty(offer.filledQuantity) + "/" + formatQty(offer.totalQuantity) + " @ " + formatQty(offer.price) + "gp"), BorderLayout.WEST);
        bottomLine.add(buildMetaLabel("Slot " + (offer.slotIndex + 1)), BorderLayout.EAST);
        content.add(bottomLine);

        row.add(content, BorderLayout.CENTER);
        return row;
    }

    private JPanel buildFlipRow(FlipRecommendation recommendation) {
        JPanel row = createRowCard(FLIP_ROW_MIN_HEIGHT);
        row.add(buildIconLabel(recommendation.candidate.itemId), BorderLayout.WEST);

        JPanel content = buildContentBox();
        content.add(buildTitleLine(trim(recommendation.candidate.name, 18), buildBadge(recommendation.candidate.volumeTag.toUpperCase(), badgeColorForVolume(recommendation.candidate.volumeTag))));
        content.add(Box.createVerticalStrut(3));

        JLabel primary = buildBigNumberLabel(formatQty(recommendation.candidate.margin) + " gp net  •  "
            + formatQty(recommendation.candidate.buy) + " → " + formatQty(recommendation.candidate.sell));
        content.add(primary);
        content.add(Box.createVerticalStrut(2));

        JLabel meta = buildMetaLabel("Gross " + formatQty(recommendation.candidate.grossMargin) + "  Tax " + formatQty(recommendation.candidate.tax) + "  Left " + formatQty(recommendation.remainingLimit));
        content.add(meta);

        row.add(content, BorderLayout.CENTER);
        return row;
    }

    private JPanel buildLimitRow(LimitStatus status) {
        JPanel row = createRowCard(LIMIT_ROW_MIN_HEIGHT);
        row.add(buildIconLabel(status.itemId), BorderLayout.WEST);

        JPanel content = buildContentBox();
        content.add(buildTitleLine(trim(status.itemName, 20), null));
        content.add(Box.createVerticalStrut(3));
        content.add(buildMainValueLabel(formatQty(status.boughtInWindow) + "/" + formatQty(status.buyLimit) + "  (" + status.resetEta + ")"));

        row.add(content, BorderLayout.CENTER);
        return row;
    }

    private JLabel buildIconLabel(int itemId) {
        JLabel iconLabel = new JLabel();
        iconLabel.setPreferredSize(new Dimension(ICON_SIZE, ICON_SIZE));
        iconLabel.setMinimumSize(new Dimension(ICON_SIZE, ICON_SIZE));
        iconLabel.setMaximumSize(new Dimension(ICON_SIZE, ICON_SIZE));
        iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
        iconLabel.setVerticalAlignment(SwingConstants.CENTER);
        iconLabel.setOpaque(true);
        iconLabel.setBackground(ICON_PLACEHOLDER_BG);
        iconLabel.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));

        if (itemId > 0) {
            AsyncBufferedImage image = iconLoader.apply(itemId);
            if (image != null) {
                image.addTo(iconLabel);
            }
        }
        return iconLabel;
    }

    /** Thin rounded progress indicator; replaces the native JProgressBar, which doesn't respect custom theming well. */
    private static class SlimBar extends JComponent {
        private final Color fillColor;
        private double fraction;

        SlimBar(Color fillColor) {
            this.fillColor = fillColor;
            setOpaque(false);
            setPreferredSize(new Dimension(10, 6));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 6));
            setAlignmentX(Component.LEFT_ALIGNMENT);
        }

        void setFraction(double fraction) {
            this.fraction = Math.max(0, Math.min(1, fraction));
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int h = getHeight();
            int w = getWidth();
            g2.setColor(new Color(55, 55, 55));
            g2.fillRoundRect(0, 0, w, h, h, h);
            int fillWidth = (int) Math.round(w * fraction);
            if (fillWidth > 0) {
                g2.setColor(fillColor);
                g2.fillRoundRect(0, 0, Math.max(fillWidth, h), h, h, h);
            }
            g2.dispose();
        }
    }

    private JPanel buildContentBox() {
        JPanel box = new JPanel();
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setOpaque(false);
        return box;
    }

    private JPanel buildTitleLine(String title, JLabel badge) {
        JPanel line = new JPanel(new BorderLayout(6, 0));
        line.setOpaque(false);
        line.setAlignmentX(Component.LEFT_ALIGNMENT);
        line.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        JLabel label = new JLabel(title);
        label.setForeground(Color.WHITE);
        label.setFont(FontManager.getDefaultBoldFont().deriveFont(13f));
        line.add(label, BorderLayout.CENTER);
        if (badge != null) line.add(badge, BorderLayout.EAST);
        return line;
    }

    private JLabel buildMainValueLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(Color.WHITE);
        label.setFont(FontManager.getDefaultBoldFont().deriveFont(11f));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private JLabel buildBigNumberLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(Color.WHITE);
        label.setFont(FontManager.getDefaultBoldFont().deriveFont(13f));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private JLabel buildMetaLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(MUTED_TEXT);
        label.setFont(FontManager.getDefaultFont().deriveFont(10f));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private JLabel buildBadge(String text, Color background) {
        JLabel badge = new JLabel(text);
        badge.setOpaque(true);
        badge.setBackground(background);
        badge.setForeground(Color.WHITE);
        badge.setFont(FontManager.getDefaultBoldFont().deriveFont(10f));
        badge.setBorder(new EmptyBorder(3, 6, 3, 6));
        return badge;
    }

    private JPanel createCardPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(true);
        panel.setBackground(CARD_BG);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setBorder(new CompoundBorder(BorderFactory.createLineBorder(BORDER_COLOR), new EmptyBorder(CARD_PADDING, CARD_PADDING, CARD_PADDING, CARD_PADDING)));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        return panel;
    }

    private JPanel createRowCard(int minHeight) {
        JPanel panel = new JPanel(new BorderLayout(8, 0));
        panel.setOpaque(true);
        panel.setBackground(ROW_BG);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setBorder(new CompoundBorder(BorderFactory.createLineBorder(new Color(55, 55, 55)), new EmptyBorder(7, 8, 7, 8)));
        panel.setMinimumSize(new Dimension(0, minHeight));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        return panel;
    }

    /**
     * Constrains list content to the scroll viewport's width (see {@link #getScrollableTracksViewportWidth()}),
     * so row content (e.g. badges anchored to the row's right edge) can never be clipped off-screen horizontally.
     */
    private static class ScrollableListPanel extends JPanel implements Scrollable {
        ScrollableListPanel() {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 24;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 120;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    private JPanel createListPanel() {
        JPanel panel = new ScrollableListPanel();
        panel.setOpaque(true);
        panel.setBackground(LIST_BG);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        return panel;
    }

    private JScrollPane createScrollPane(JPanel panel) {
        JScrollPane scrollPane = new JScrollPane(panel);
        scrollPane.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
        scrollPane.setPreferredSize(new Dimension(PluginPanel.PANEL_WIDTH - 24, CONTENT_HEIGHT));
        scrollPane.setMinimumSize(new Dimension(PluginPanel.PANEL_WIDTH - 24, CONTENT_HEIGHT));
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getViewport().setBackground(LIST_BG);
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
            protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(55, 55, 55));
                g2.fillRoundRect(thumbBounds.x + 1, thumbBounds.y, thumbBounds.width - 2, thumbBounds.height, 8, 8);
                g2.dispose();
            }

            @Override
            protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
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
        label.setFont(FontManager.getDefaultFont().deriveFont(12f));
        label.setBorder(new EmptyBorder(10, 10, 10, 10));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(label);
        refreshList(panel);
    }

    private void resetList(JPanel panel) { panel.removeAll(); }
    private void refreshList(JPanel panel) { panel.revalidate(); panel.repaint(); }

    private JLabel createCardHeading(String text) {
        JLabel label = new JLabel(text);
        label.setFont(FontManager.getDefaultBoldFont().deriveFont(14f));
        label.setForeground(Color.WHITE);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private void configureActionButton(JButton button) {
        SwingUtil.removeButtonDecorations(button);
        button.setOpaque(true);
        button.setFocusPainted(false);
        button.setFont(FontManager.getDefaultBoldFont().deriveFont(11f));
        button.setBackground(new Color(58, 58, 58));
        button.setForeground(Color.WHITE);
        button.setBorder(new CompoundBorder(BorderFactory.createLineBorder(new Color(82, 82, 82)), new EmptyBorder(4, 10, 4, 10)));
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
    }

    private void configureDropdown(JComboBox<String> dropdown) {
        dropdown.setUI(new BasicComboBoxUI());
        dropdown.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        dropdown.setAlignmentX(Component.LEFT_ALIGNMENT);
        dropdown.setBackground(new Color(46, 46, 46));
        dropdown.setForeground(Color.WHITE);
        dropdown.setFont(FontManager.getDefaultFont().deriveFont(12f));
        dropdown.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
        dropdown.setFocusable(false);
        dropdown.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                label.setOpaque(true);
                label.setBackground(isSelected ? ACCENT_COLOR : new Color(46, 46, 46));
                label.setForeground(Color.WHITE);
                label.setFont(FontManager.getDefaultFont().deriveFont(12f));
                label.setBorder(new EmptyBorder(5, 8, 5, 8));
                return label;
            }
        });
    }

    private Color badgeColorForType(String type) { return "BUY".equalsIgnoreCase(type) ? BUY_COLOR : SELL_COLOR; }
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
