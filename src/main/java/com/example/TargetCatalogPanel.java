package com.example;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
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
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.IconTextField;

@Singleton
@Slf4j
class TargetCatalogPanel extends PluginPanel
{
	private static final int MAX_RESULTS = 250;

	private final TargetCatalog catalog;
	private final TargetCatalogConfigEditor configEditor;
	private final JComboBox<CatalogKind> catalogSelector = new JComboBox<>(CatalogKind.values());
	private final IconTextField searchField = new IconTextField();
	private final DefaultListModel<CatalogEntry> resultModel = new DefaultListModel<>();
	private final JList<CatalogEntry> resultList = new JList<>(resultModel);
	private final JLabel resultInfoLabel = new JLabel();
	private final JLabel actionStatusLabel = new JLabel();
	private final JButton addRareButton = new JButton("Add to rare IDs");
	private final JButton addShownRareButton = new JButton("Add shown to rare IDs");
	private final JButton addBossButton = new JButton("Add to boss IDs");
	private final JButton addShownBossButton = new JButton("Add shown to boss IDs");
	private final JButton addHiddenButton = new JButton("Add to hidden IDs");
	private final JButton addShownHiddenButton = new JButton("Add shown to hidden IDs");
	private final Timer searchDebounce;
	private int searchGeneration;
	private boolean searchInitialized;
	private boolean actionInProgress;

	@Inject
	private TargetCatalogPanel(TargetCatalog catalog, TargetCatalogConfigEditor configEditor)
	{
		super(false);
		this.catalog = catalog;
		this.configEditor = configEditor;
		this.searchDebounce = new Timer(175, event -> runSearch());
		this.searchDebounce.setRepeats(false);

		setLayout(new BorderLayout(0, 8));
		setBackground(ColorScheme.DARK_GRAY_COLOR);
		setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

		add(buildHeader(), BorderLayout.NORTH);
		add(buildResultsSection(), BorderLayout.CENTER);
		add(buildFooter(), BorderLayout.SOUTH);

		resultInfoLabel.setText("Open the browser and type at least 2 characters or an exact ID when available.");
		actionStatusLabel.setText(" ");
		updateActionButtons();
	}

	@Override
	public void onActivate()
	{
		if (!searchInitialized)
		{
			searchInitialized = true;
			runSearch();
		}
	}

	private JComponent buildHeader()
	{
		final JPanel header = new JPanel();
		header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
		header.setBackground(ColorScheme.DARK_GRAY_COLOR);

		final JLabel titleLabel = new JLabel("Target Browser");
		titleLabel.setForeground(Color.WHITE);
		titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

		final JLabel descriptionLabel = new JLabel("<html>Search the local NPC and object catalogs plus the built-in boss list. Boss actions write mapped NPC IDs when available and also write the exact boss names.</html>");
		descriptionLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		descriptionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

		final JPanel controlsRow = new JPanel(new BorderLayout(6, 0));
		controlsRow.setBackground(ColorScheme.DARK_GRAY_COLOR);
		controlsRow.setAlignmentX(Component.LEFT_ALIGNMENT);

		catalogSelector.addActionListener(event ->
		{
			updateActionButtons();
			if (searchInitialized)
			{
				runSearch();
			}
		});
		catalogSelector.setFocusable(false);
		controlsRow.add(catalogSelector, BorderLayout.WEST);

		searchField.setIcon(IconTextField.Icon.SEARCH);
		searchField.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		searchField.setHoverBackgroundColor(ColorScheme.DARK_GRAY_HOVER_COLOR);
		searchField.getDocument().addDocumentListener(new DocumentListener()
		{
			@Override
			public void insertUpdate(DocumentEvent event)
			{
				scheduleSearch();
			}

			@Override
			public void removeUpdate(DocumentEvent event)
			{
				scheduleSearch();
			}

			@Override
			public void changedUpdate(DocumentEvent event)
			{
				scheduleSearch();
			}
		});
		controlsRow.add(searchField, BorderLayout.CENTER);

		header.add(titleLabel);
		header.add(Box.createVerticalStrut(4));
		header.add(descriptionLabel);
		header.add(Box.createVerticalStrut(8));
		header.add(controlsRow);
		return header;
	}

	private JComponent buildResultsSection()
	{
		final JPanel resultsSection = new JPanel(new BorderLayout(0, 6));
		resultsSection.setBackground(ColorScheme.DARK_GRAY_COLOR);

		resultInfoLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		resultsSection.add(resultInfoLabel, BorderLayout.NORTH);

		resultList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		resultList.setCellRenderer(new CatalogEntryRenderer());
		resultList.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		resultList.setSelectionBackground(ColorScheme.DARK_GRAY_HOVER_COLOR);
		resultList.setSelectionForeground(Color.WHITE);
		resultList.addListSelectionListener(event ->
		{
			if (!event.getValueIsAdjusting())
			{
				updateActionButtons();
			}
		});

		final JScrollPane scrollPane = new JScrollPane(resultList);
		scrollPane.setPreferredSize(new Dimension(PluginPanel.PANEL_WIDTH, 360));
		scrollPane.setBorder(BorderFactory.createLineBorder(ColorScheme.BORDER_COLOR));
		resultsSection.add(scrollPane, BorderLayout.CENTER);

		return resultsSection;
	}

	private JComponent buildFooter()
	{
		final JPanel footer = new JPanel();
		footer.setLayout(new BoxLayout(footer, BoxLayout.Y_AXIS));
		footer.setBackground(ColorScheme.DARK_GRAY_COLOR);

		final JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
		buttonRow.setBackground(ColorScheme.DARK_GRAY_COLOR);

		configureActionButton(addRareButton, event -> addSelectedEntries(ActionType.RARE));
		configureActionButton(addShownRareButton, event -> addShownEntries(ActionType.RARE));
		configureActionButton(addBossButton, event -> addSelectedEntries(ActionType.BOSS));
		configureActionButton(addShownBossButton, event -> addShownEntries(ActionType.BOSS));
		configureActionButton(addHiddenButton, event -> addSelectedEntries(ActionType.HIDDEN));
		configureActionButton(addShownHiddenButton, event -> addShownEntries(ActionType.HIDDEN));

		buttonRow.add(addRareButton);
		buttonRow.add(addShownRareButton);
		buttonRow.add(addBossButton);
		buttonRow.add(addShownBossButton);
		buttonRow.add(addHiddenButton);
		buttonRow.add(addShownHiddenButton);

		actionStatusLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);

		footer.add(buttonRow);
		footer.add(Box.createVerticalStrut(6));
		footer.add(actionStatusLabel);
		return footer;
	}

	private void configureActionButton(JButton button, java.awt.event.ActionListener listener)
	{
		button.setFocusable(false);
		button.addActionListener(listener);
	}

	private void scheduleSearch()
	{
		if (searchInitialized)
		{
			searchDebounce.restart();
		}
	}

	private void runSearch()
	{
		final CatalogKind kind = getSelectedKind();
		final String query = searchField.getText();
		final int requestId = ++searchGeneration;
		resultInfoLabel.setText("Searching " + kind.getPluralLabel().toLowerCase(Locale.ROOT) + "...");
		actionStatusLabel.setText(" ");
		resultModel.clear();
		updateActionButtons();

		new SwingWorker<TargetCatalog.SearchResult, Void>()
		{
			@Override
			protected TargetCatalog.SearchResult doInBackground()
			{
				return catalog.search(kind, query, MAX_RESULTS);
			}

			@Override
			protected void done()
			{
				if (requestId != searchGeneration)
				{
					return;
				}

				try
				{
					applySearchResult(kind, get());
				}
				catch (Exception ex)
				{
					log.warn("Target Browser search failed for {} with query '{}'", kind, query, ex);
					resultModel.clear();
					resultInfoLabel.setText("Search failed for " + kind.getPluralLabel().toLowerCase(Locale.ROOT) + ".");
					actionStatusLabel.setText("Unable to search the local catalog right now.");
				}
			}
		}.execute();
	}

	private void applySearchResult(CatalogKind kind, TargetCatalog.SearchResult searchResult)
	{
		resultModel.clear();
		for (CatalogEntry entry : searchResult.getResults())
		{
			resultModel.addElement(entry);
		}

		if (searchResult.isQueryTooShort())
		{
			if (searchResult.getTotalEntries() >= 0)
			{
				resultInfoLabel.setText(String.format(
					Locale.ROOT,
					"%,d %s available. Type at least 2 characters or an exact ID when available.",
					searchResult.getTotalEntries(),
					kind.getPluralLabel().toLowerCase(Locale.ROOT)
				));
			}
			else
			{
				resultInfoLabel.setText("Type at least 2 characters or an exact ID when available.");
			}
		}
		else if (searchResult.getTotalMatches() == 0)
		{
			resultInfoLabel.setText("No " + kind.getPluralLabel().toLowerCase(Locale.ROOT) + " matched that search.");
		}
		else
		{
			resultInfoLabel.setText(String.format(
				Locale.ROOT,
				"Showing %,d of %,d %s from a catalog of %,d.",
				searchResult.getResults().size(),
				searchResult.getTotalMatches(),
				kind.getPluralLabel().toLowerCase(Locale.ROOT),
				searchResult.getTotalEntries()
			));
		}

		updateActionButtons();
	}

	private void addSelectedEntries(ActionType actionType)
	{
		applyEntries(actionType, resultList.getSelectedValuesList(), "selected");
	}

	private void addShownEntries(ActionType actionType)
	{
		final List<CatalogEntry> shownEntries = getShownEntries();
		applyEntries(actionType, shownEntries, "shown");
	}

	private void applyEntries(ActionType actionType, List<CatalogEntry> entries, String sourceLabel)
	{
		final CatalogKind kind = getSelectedKind();
		if (entries.isEmpty() || actionInProgress)
		{
			return;
		}

		actionInProgress = true;
		updateActionButtons();
		actionStatusLabel.setText("Updating config...");

		new SwingWorker<ActionResult, Void>()
		{
			@Override
			protected ActionResult doInBackground()
			{
				final List<Integer> ids = entries.stream()
					.map(CatalogEntry::getId)
					.filter(id -> id >= 0)
					.distinct()
					.collect(Collectors.toList());
				final List<String> names = entries.stream()
					.map(CatalogEntry::getDisplayName)
					.filter(name -> name != null && !name.isBlank())
					.distinct()
					.collect(Collectors.toList());
				final int added;
				switch (actionType)
				{
					case BOSS:
						added = kind == CatalogKind.BOSS
							? configEditor.addNpcIdsToBossAllowlist(ids) + configEditor.addBossNamesToAllowlist(names)
							: configEditor.addNpcIdsToBossAllowlist(ids);
						break;
					case HIDDEN:
						added = kind == CatalogKind.OBJECT
							? configEditor.addObjectIdsToHidden(ids) + configEditor.addObjectNamesToHidden(names)
							: (kind == CatalogKind.BOSS
								? configEditor.addBossIdsToHidden(ids) + configEditor.addBossNamesToHidden(names)
								: configEditor.addNpcIdsToHidden(ids) + configEditor.addNpcNamesToHidden(names));
						break;
					case RARE:
					default:
						added = kind == CatalogKind.OBJECT
							? configEditor.addObjectIdsToRare(ids) + configEditor.addObjectNamesToRare(names)
							: (kind == CatalogKind.BOSS
								? configEditor.addBossIdsToRare(ids) + configEditor.addBossNamesToRare(names)
								: configEditor.addNpcIdsToRare(ids) + configEditor.addNpcNamesToRare(names));
						break;
				}

				final int affectedCount = entries.size();
				return new ActionResult(added, affectedCount);
			}

			@Override
			protected void done()
			{
				actionInProgress = false;
				try
				{
					final ActionResult result = get();
					final String status = buildActionStatus(kind, actionType, result.addedCount, result.affectedCount, sourceLabel);
					actionStatusLabel.setText(status);
					actionStatusLabel.setToolTipText(status);
				}
				catch (Exception ex)
				{
					log.warn("Target Browser action {} failed for {} entries in {}", actionType, entries.size(), kind, ex);
					actionStatusLabel.setText("Unable to update config right now.");
					actionStatusLabel.setToolTipText("Unable to update config right now.");
				}
				updateActionButtons();
			}
		}.execute();
	}

	private String buildActionStatus(CatalogKind kind, ActionType actionType, int addedCount, int selectedCount, String sourceLabel)
	{
		final String label;
		switch (actionType)
		{
			case BOSS:
				label = kind == CatalogKind.BOSS ? "boss rules" : "boss IDs";
				break;
			case HIDDEN:
				label = kind == CatalogKind.BOSS ? "hidden boss rules" : "hidden rules";
				break;
			case RARE:
			default:
				label = kind == CatalogKind.BOSS ? "rare boss rules" : "rare rules";
				break;
		}

		if (addedCount <= 0)
		{
			return String.format(
				Locale.ROOT,
				"No new %s values from %s %s.",
				label,
				sourceLabel,
				kind.getPluralLabel().toLowerCase(Locale.ROOT)
			);
		}

		return String.format(
			Locale.ROOT,
			"%s updated: +%,d values from %,d %s %s.",
			capitalize(label),
			addedCount,
			selectedCount,
			sourceLabel,
			kind.getPluralLabel().toLowerCase(Locale.ROOT)
		);
	}

	private void updateActionButtons()
	{
		final boolean hasSelection = !resultList.isSelectionEmpty();
		final boolean hasShownResults = !resultModel.isEmpty();
		final boolean bossCapableCatalog = getSelectedKind() == CatalogKind.NPC || getSelectedKind() == CatalogKind.BOSS;
		final boolean bossCatalog = getSelectedKind() == CatalogKind.BOSS;
		addRareButton.setText(bossCatalog ? "Add to rare bosses" : "Add to rare IDs");
		addShownRareButton.setText(bossCatalog ? "Add shown to rare bosses" : "Add shown to rare IDs");
		addBossButton.setText(bossCatalog ? "Add to boss allowlist" : "Add to boss IDs");
		addShownBossButton.setText(bossCatalog ? "Add shown to boss allowlist" : "Add shown to boss IDs");
		addHiddenButton.setText(bossCatalog ? "Add to hidden bosses" : "Add to hidden IDs");
		addShownHiddenButton.setText(bossCatalog ? "Add shown to hidden bosses" : "Add shown to hidden IDs");
		addRareButton.setEnabled(hasSelection && !actionInProgress);
		addShownRareButton.setEnabled(hasShownResults && !actionInProgress);
		addHiddenButton.setEnabled(hasSelection && !actionInProgress);
		addShownHiddenButton.setEnabled(hasShownResults && !actionInProgress);
		addBossButton.setVisible(bossCapableCatalog);
		addShownBossButton.setVisible(bossCapableCatalog);
		addBossButton.setEnabled(bossCapableCatalog && hasSelection && !actionInProgress);
		addShownBossButton.setEnabled(bossCapableCatalog && hasShownResults && !actionInProgress);
	}

	private List<CatalogEntry> getShownEntries()
	{
		return java.util.Collections.list(resultModel.elements());
	}

	private CatalogKind getSelectedKind()
	{
		final CatalogKind selected = (CatalogKind) catalogSelector.getSelectedItem();
		return selected == null ? CatalogKind.NPC : selected;
	}

	private String capitalize(String value)
	{
		if (value == null || value.isBlank())
		{
			return "";
		}

		return Character.toUpperCase(value.charAt(0)) + value.substring(1);
	}

	private static final class CatalogEntryRenderer extends DefaultListCellRenderer
	{
		@Override
		public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus)
		{
			final JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
			label.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
			if (!(value instanceof CatalogEntry))
			{
				return label;
			}

			final CatalogEntry entry = (CatalogEntry) value;
			final StringBuilder html = new StringBuilder(128)
				.append("<html><b>")
				.append(escape(entry.getDisplayName()))
				.append("</b>");
			if (entry.hasId())
			{
				html.append(" <span style='color:#a7a7a7'>[#")
					.append(entry.getId())
					.append("]</span>");
			}
			if (entry.shouldShowRawName())
			{
				html.append("<br><span style='color:#8f8f8f'>")
					.append(escape(entry.getRawName()))
					.append("</span>");
			}
			html.append("</html>");

			label.setText(html.toString());
			label.setToolTipText(entry.hasId()
				? entry.getKind().getSingularLabel() + " ID " + entry.getId() + ": " + entry.getRawName()
				: entry.getKind().getSingularLabel() + ": " + entry.getDisplayName());
			return label;
		}

		private static String escape(String value)
		{
			return value
				.replace("&", "&amp;")
				.replace("<", "&lt;")
				.replace(">", "&gt;");
		}
	}

	private enum ActionType
	{
		RARE,
		BOSS,
		HIDDEN
	}

	private static final class ActionResult
	{
		private final int addedCount;
		private final int affectedCount;

		private ActionResult(int addedCount, int affectedCount)
		{
			this.addedCount = addedCount;
			this.affectedCount = affectedCount;
		}
	}
}
