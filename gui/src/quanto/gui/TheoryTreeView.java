/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package quanto.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import quanto.gui.QuantoCore.CoreException;

/**
 *
 * @author alex
 */
public class TheoryTreeView extends JTree {

	private final static Logger logger =
		LoggerFactory.getLogger(TheoryTreeView.class);

	TheoryManager manager;
	ViewPort viewPort;
	QuantoApp app;

	public TheoryTreeView(TheoryManager manager, QuantoApp app, ViewPort viewPort) {
		super(manager.getTreeModel());
		this.manager = manager;
		this.viewPort = viewPort;
		this.app = app;

		setCellRenderer(new TheoryCellRenderer());
		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				boolean rightClick =
					(e.getButton() == MouseEvent.BUTTON3) ||
					(QuantoApp.isMac &&
					 e.isControlDown() &&
					 e.getButton() == MouseEvent.BUTTON1);
				final TheoryManager.TheoryTreeModel tmodel = TheoryTreeView.this.manager.getTreeModel();
				if (rightClick) {
					TreePath p = getPathForLocation(e.getX(), e.getY());
					if (p != null) {
						Object node = p.getLastPathComponent();
						if (node == tmodel.getRoot()) { // the root
							JPopupMenu menu = new JPopupMenu();
							JMenuItem item = new JMenuItem("Load theory...");
							item.addActionListener(new ActionListener() {
								public void actionPerformed(ActionEvent e) {
									loadTheory();
								}
							});
							menu.add(item);

							item = new JMenuItem("Refresh rulsets");
							item.addActionListener(new WrappedActionListener() {
								@Override
								public void wrappedAction(ActionEvent e) throws CoreException {
									TheoryTreeView.this.manager.reloadTheoriesFromCore();
								}
							});
							menu.add(item);

							menu.show(TheoryTreeView.this, e.getX(), e.getY());
						} else if (tmodel.isTheoryNode(node)) { // a theory
							Theory rset = tmodel.getTheory(node);
							new TheoryMenu(rset).show(TheoryTreeView.this, e.getX(), e.getY());
						} else if (tmodel.isRuleNode(node)) { // a rule
							Theory rset = tmodel.getTheory(node);
							String rule = tmodel.getRuleName(node);
							new RuleMenu(rset, rule).show(TheoryTreeView.this, e.getX(), e.getY());
						}
					}
				}
			}
		});
		// don't want to steal keyboard focus from the active InteractiveView
		setFocusable(false);
	}

	public void loadTheory() {
		String lastDir = app.getPreference(QuantoApp.LAST_THEORY_OPEN_DIR);
		if (lastDir != null) {
			app.getFileChooser().setCurrentDirectory(new File(lastDir));
		}

		int retVal = app.getFileChooser().showDialog(this, "Open");
		if (retVal == JFileChooser.APPROVE_OPTION) {
			try {
				File file = app.getFileChooser().getSelectedFile();
				if (file.getParent() != null) {
					app.setPreference(QuantoApp.LAST_THEORY_OPEN_DIR, file.getParent());
				}
				String thyname = file.getName().replaceAll("\\.theory|\\n|\\r", "");
				String filename = file.getCanonicalPath().replaceAll("\\n|\\r", "");
				manager.loadTheory(thyname, filename);
			}
			catch (QuantoCore.CoreException e) {
				logger.error("Failed to load theory", e);
				app.errorDialog(e.getMessage());
			}
			catch (java.io.IOException ioe) {
				logger.error("Failed to load theory", ioe);
				app.errorDialog(ioe.getMessage());
			}
		}
	}

	@SuppressWarnings("serial")
	private class TheoryCellRenderer extends DefaultTreeCellRenderer {
		@Override
		public Component getTreeCellRendererComponent(JTree tree, Object value,
				boolean selected, boolean expanded, boolean leaf, int row,
				boolean hasFocus) {
			// let parent set the basic component properties
			super.getTreeCellRendererComponent(tree, value, selected, expanded,
					leaf, row, hasFocus);

			// ghost the theory if it isn't active
			Theory th = manager.getTreeModel().getTheory(value);
			if (th != null && !th.isActive())
				setForeground(Color.gray);

			return this;
		}
	}

	/**
	 * Generic action listener that reports core errors to a dialog box.
	 */
	private abstract class WrappedActionListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			try {
				wrappedAction(e);
			}
			catch (QuantoCore.CoreException err) {
				JOptionPane.showMessageDialog(
					TheoryTreeView.this,
					err.getMessage(),
					"Console Error",
					JOptionPane.ERROR_MESSAGE);
			}
		}

		public abstract void wrappedAction(ActionEvent e) throws QuantoCore.CoreException;
	}

	/*
	 * this class uses the "tree" instance var
	 */
	@SuppressWarnings("serial")
	private class TheoryMenu extends JPopupMenu {
		public TheoryMenu(final Theory rset) {
			JMenuItem item = new JMenuItem("Activate");
			if (rset.isActive()) {
				item.setEnabled(false);
			} else {
				item.addActionListener(new WrappedActionListener() {
					public void wrappedAction(ActionEvent e) throws CoreException {
						manager.setTheoryActive(rset, true);
					}
				});
			}
			add(item);
			item = new JMenuItem("Deactivate");
			if (!rset.isActive()) {
				item.setEnabled(false);
			} else {
				item.addActionListener(new WrappedActionListener() {
					public void wrappedAction(ActionEvent e) throws CoreException {
						manager.setTheoryActive(rset, false);
					}
				});
			}
			add(item);

			item = new JMenuItem("Unload");
			item.addActionListener(new WrappedActionListener() {
				public void wrappedAction(ActionEvent e) throws CoreException {
					manager.unloadTheory(rset);
				}
			});
			add(item);
		}
	}



	/*
	 * this class uses the "tree" instance var
	 */
	@SuppressWarnings("serial")
	private class RuleMenu extends JPopupMenu {
		public RuleMenu(final Theory rset, final String rule) {
			JMenuItem item;

			class RuleAL extends WrappedActionListener {
				private int side; // BOTH = 0, LEFT = 1, RIGHT = 2
				public RuleAL(int side) { super(); this.side = side; }

				public void wrappedAction(ActionEvent e) throws CoreException {
					QuantoGraph gr1 = (side == 0 || side == 1) ?
							rset.getRuleLhs(rule) :
							rset.getRuleRhs(rule);
					InteractiveGraphView igv1 = new InteractiveGraphView(manager.getCore(), gr1);
					igv1.updateGraph();

					InteractiveView view = igv1;
					if (side == 0) { // if opening both
						QuantoGraph gr2 = rset.getRuleRhs(rule);
						InteractiveGraphView igv2 = new InteractiveGraphView(manager.getCore(), gr2);
						igv2.updateGraph();
						view = new SplitGraphView(rset, rule, igv1, igv2);
					}
					viewPort.getViewManager().addView(view);
					try {
						viewPort.attachView(view);
					}
					catch (ViewUnavailableException ex) {
						throw new Error("Caught a ViewUnavailableException: this shouldn't happen");
					}
				}
			}

			item = new JMenuItem("Open Rule");
			item.addActionListener(new RuleAL(0));
			add(item);
			item = new JMenuItem("Open LHS");
			item.addActionListener(new RuleAL(1));
			add(item);
			item = new JMenuItem("Open RHS");
			item.addActionListener(new RuleAL(2));
			add(item);

		}
	}
}