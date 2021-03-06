package de.keawe.carddavcleaner;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;

import de.keawe.gui.HorizontalPanel;
import de.keawe.gui.InputField;
import de.keawe.gui.SelectionDialog;
import de.keawe.gui.Translations;
import de.keawe.gui.VerticalPanel;

public class CardDavCleaner extends JFrame {
	
	private class cleaningThread extends Thread {
		private Component owner;

		public cleaningThread(Component owner) {
			this.owner = owner;
		}

		public void run() {
			try {
				mainPanel.setEnabled(false);
				startCleaning();
				mainPanel.setEnabled(true);
			} catch (Exception e) {
				e.printStackTrace();
				JOptionPane.showMessageDialog(owner, _("Error during server communication!"));
			}
		}
	}

	
	static String _(String text) {
		return Translations.get(text);
	}
	
	static String _(String key, Object insert) {
		return Translations.get(key, insert);
	}

	private InputField addressField;
	private InputField userField;
	private InputField passwordField;
	private File backupPath = null;
	private JProgressBar progressBar;
	private JCheckBox fixSyntaxOption;
	private JCheckBox dropEmptyFieldsOption;
	private JCheckBox dropEmptyContactsOption;
	private VerticalPanel mainPanel;
	private JCheckBox fixLineBreaksOption;
	
	private boolean askForCommit(AddressBook addressBook) {
		final Vector<Contact> updatedContacts = addressBook.getUpdatedContacts();
		final Vector<Contact> removableContacts = addressBook.getRemovableContacts();
		if (updatedContacts.size() + removableContacts.size()<1) {
			JOptionPane.showMessageDialog(null, _("Congratulations. You address book seems to be clean, nothing to do for me."), _("Operation done."), JOptionPane.OK_OPTION);
			return false;
		}
		
		VerticalPanel vp = new VerticalPanel();
		vp.add(new JLabel(_("Summary of your edits:")));
		
		HorizontalPanel hp1 = new HorizontalPanel();
		hp1.add(new JLabel(_("# contacts have been #",new String[]{""+updatedContacts.size(),_("updated")})));
		JButton showUpdates = new JButton(_("Show"));
		showUpdates.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				ContactListPreview.show(updatedContacts,_("Edited contacts"));
			}
		});
		hp1.add(showUpdates);
		hp1.scale();
		vp.add(hp1);
		HorizontalPanel hp2 = new HorizontalPanel();
		hp2.add(new JLabel(_("# contacts have been #",new String[]{""+removableContacts.size(),_("marked for removal")})));
		JButton showRemovals = new JButton(_("Show"));
		showRemovals.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				ContactListPreview.show(removableContacts,_("Obsolete contacts"));
			}
		});
		hp2.add(showRemovals);
		hp2.scale();
		vp.add(hp2);
		vp.add(new JLabel(_("Shall these changes be written to your address book?")));
		vp.scale();
		return JOptionPane.showConfirmDialog(null, vp, _("Confirm updates"), JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
	}
	
	private HorizontalPanel backupPanel() {
		HorizontalPanel backupPanel = new HorizontalPanel(_("Backup settings"));
		final JLabel backupPathLabel = new JLabel(" " + _("No Backup defined.") + "                                                                 ");
		JButton backupPathButton = new JButton(_("Select Backup Location"));
		backupPathButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				selectBackupPath(backupPathLabel);
			}
		});
		backupPanel.add(backupPathButton);
		backupPanel.add(backupPathLabel);
		return backupPanel.scale();
	}
	
	public CardDavCleaner() {
		super(_("Keawe CardDAV cleaner"));
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		createComponents();
		pack();
		setLocationRelativeTo(null);
	}
	
	/**
	 * creates all the components for the server login form
	 */
	private void createComponents() {
		mainPanel = new VerticalPanel();

		mainPanel.add(serverPanel());
		mainPanel.add(backupPanel());
		mainPanel.add(optionsPanel());
		mainPanel.add(progressPanel());
		mainPanel.scale();
		
		add(mainPanel);
		pack();
		setVisible(true);
	}
	
	public void enterPressed() {
		start();
	}
	
	private JComponent locationPanel() {
		HorizontalPanel locationPanel = new HorizontalPanel();
		
		JLabel infoText = new JLabel(_("Address book may be located on a WebDAV-Server or in a local directory.")+"  ");
		JButton locationButton = new JButton(_("Select directory"));
		locationButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				selectSource();
			}
		});
		locationPanel.add(infoText);
		locationPanel.add(locationButton);
		locationPanel.scale();
		return locationPanel;
	}

	public static void main(String[] args) {
		//System.setProperty("jsse.enableSNIExtension", "false");
		boolean testing = false;
		boolean autostart = false;
		CardDavCleaner cleaner = new CardDavCleaner();
		
		for (String arg : args) {
			testing |= arg.equals("--test");
			autostart |= arg.equals("--start");
			if (arg.startsWith("--source=")) cleaner.setSeource(arg.substring(9));
			if (arg.startsWith("--user=")) cleaner.setUsername(arg.substring(7));
			if (arg.startsWith("--pass=")) cleaner.setPassword(arg.substring(7));
		}
		if (testing) {
			test();
		} else {
			cleaner.setVisible(true);
			if (autostart) cleaner.start();
		}
	}
	
	private VerticalPanel optionsPanel() {
		VerticalPanel optionsPanel = new VerticalPanel(_("Optional settings"));
		
		fixSyntaxOption = new JCheckBox(_("Fix field syntax, if broken"));
		fixSyntaxOption.setSelected(true);
		optionsPanel.add(fixSyntaxOption);

		fixLineBreaksOption = new JCheckBox(_("Fix line breaks, if not standard compliant"));
		fixLineBreaksOption.setSelected(true);
		optionsPanel.add(fixLineBreaksOption);

		dropEmptyFieldsOption = new JCheckBox(_("Remove empty fields from contacts"));
		dropEmptyFieldsOption.setSelected(true);
		optionsPanel.add(dropEmptyFieldsOption);
		
		dropEmptyContactsOption = new JCheckBox(_("Remove empty contacts"));
		dropEmptyContactsOption.setSelected(true);
		optionsPanel.add(dropEmptyContactsOption);
		
		optionsPanel.add(new JLabel(_("<html>Some programs cannot handle all fields defined by the vCard standard.<br>To apply workarounds, select programs you use from the follwing list:")));
		
		JCheckBox thunderbirdBox = new JCheckBox(_("Mozilla Thunderbird with default address book"));
		optionsPanel.add(thunderbirdBox);

		return optionsPanel.scale();
	}

	private HorizontalPanel progressPanel() {
		HorizontalPanel bar = new HorizontalPanel();
		
		progressBar = new JProgressBar();
		progressBar.setPreferredSize(new Dimension(800, 32));
		progressBar.setStringPainted(true);
		progressBar.setString(_("Ready."));
		bar.add(progressBar);
		
		JButton startButton = new JButton(_("start"));
		startButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				enterPressed();
			}
		});
		bar.add(startButton);
		
		bar.scale();
		return bar;
	}
	
	protected void selectBackupPath(JLabel label) {
		JFileChooser j = new JFileChooser();
		j.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		
		Integer opt = j.showSaveDialog(this);
		if (opt == JFileChooser.APPROVE_OPTION) {
			backupPath  = j.getSelectedFile();
			label.setText(" " + _("Backup wil be written to #", backupPath));
		}
	}
	
	protected void selectSource() {
		JFileChooser j = new JFileChooser();
		j.setFileHidingEnabled(false);
		j.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		Integer opt = j.showSaveDialog(this);
		if (opt == JFileChooser.APPROVE_OPTION) {
			File source = j.getSelectedFile();
			addressField.setText("file://"+source.toString());
		}
	}
	
	private VerticalPanel serverPanel() {
		VerticalPanel serverPanel = new VerticalPanel(_("Address book settings"));
		serverPanel.add(locationPanel());
		
		serverPanel.add(addressField = new InputField(_("Location of addressbook:")));
		addressField.setText("https://example.com");
		
		serverPanel.add(userField = new InputField(_("User:"), false));
		serverPanel.add(passwordField = new InputField(_("Password:"), true));
		
		addressField.setEnterListener(this);
		userField.setEnterListener(this);
		passwordField.setEnterListener(this);
		return serverPanel.scale();
	}
	
	private void setPassword(String password) {
		passwordField.setText(password);
	}
	
	private void setSeource(String source) {
		addressField.setText(source);
	}
	
	private void setUsername(String username) {
		userField.setText(username);
	}
	
	private void start() {
		cleaningThread cleaningThread = new cleaningThread(this);
		cleaningThread.start();
	}

	protected void startCleaning() {
		progressBar.setString(_("Connecting to address book..."));
		AddressBook addressBook = new AddressBook(addressField.getText(),userField.getText(),passwordField.getText());
		if (fixSyntaxOption.isSelected()) addressBook.enableSytaxFixing();
		if (fixLineBreaksOption.isSelected()) addressBook.enableLineBreakFixing();
		if (dropEmptyFieldsOption.isSelected()) addressBook.enableDropEmptyFields();
		if (dropEmptyContactsOption.isSelected()) addressBook.enableDropEmptyContacts();
		addressBook.enableProgressBar(progressBar);
		
		
		if (addressBook.ready()) {
			try {
				addressBook.loadContacts(backupPath);
				MergeCandidate candidate;
				while ((candidate = addressBook.getMergeCandidate()) != null) {
					progressBar.setValue(progressBar.getValue()+1);
					switch (candidate.propose()) {
						case JOptionPane.CANCEL_OPTION:
							System.exit(-1);
							break;
						case JOptionPane.YES_OPTION:
							Contact mergedContact = candidate.merge();
							handleConflicts(mergedContact);
							break;
					}
				}
				if (askForCommit(addressBook)) {
					addressBook.commit();
					JOptionPane.showMessageDialog(null, _("Cleaning of you address book has been performed."), _("Operation done."), JOptionPane.OK_OPTION);
				}
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
		private void handleConflicts(Contact mergedContact) {
		Conflict conflict;
		while ((conflict = mergedContact.detectConflicts()) != null) { 
			if (conflict.param("UID")) {
				mergedContact.dropConflictingUids(conflict);
			} else {
				System.out.println("\n"+conflict);
				if (conflict.tags()==null) {
					System.out.println("Showing input dialog for missing tag not implemented, yet.");
				} else {
					SelectionDialog dialog = new SelectionDialog(conflict);
					Tag tagToKeep = dialog.selectedTag();
					if (tagToKeep == null) System.exit(-1);
					Vector<Tag> conflictingTags = conflict.tags();
					conflictingTags.remove(tagToKeep);
					mergedContact.removeTags(conflictingTags);
				}
			}
		}
	}

	private static void test() {
		boolean error = false;
		try {
			error |= Contact.test();
			error |= Tag.test();
			System.out.println("#===================================#");
			System.out.println(_(error ? "| Error(s) occured during tests!    |" : "| All tests successfully completed! |"));
			System.out.println("#===================================#");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
