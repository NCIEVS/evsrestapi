package gov.nih.nci.evs.restapi.util;

import java.io.*;
import java.util.*;

import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;


public class HierarchyViewer {
	public static void main(String[] args) {
		String datafile = args[0];
		JFrame frame = new TreeFrame(datafile);
		frame.show();
	}
}

class TreeFrame extends JFrame implements ActionListener {
	HashMap code2NodeMap = new HashMap();
    HierarchyHelper hh = null;

	public DefaultMutableTreeNode getNode(String code) {
		if (code2NodeMap.containsKey(code)) {
			return (DefaultMutableTreeNode) code2NodeMap.get(code);
		}
		String label = hh.getLabel(code);
		String display = label + " (" + code + ")";
		DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(display);
		code2NodeMap.put(code, newNode);
		return newNode;
	}

	public TreeFrame(String datafile) {
        String title = "NCI Thesaurus";
        int n = datafile.indexOf("parent_child.txt");
        if (n > 0) {
			title = datafile.substring(0, n-1);
            title = title.trim();
		}

		Vector parent_child_vec = Utils.readFile(datafile);
		hh = new HierarchyHelper(parent_child_vec);
		setTitle(title + " Hierarchy");
		setSize(2400, 1200);
		addWindowListener(new WindowAdapter() {
		  public void windowClosing(WindowEvent e) {
			System.exit(0);
		  }
		});



		TreeNode root = createTree(title);
		model = new DefaultTreeModel(root);
		tree = new JTree(model);

		//tree.setEditable(true);
		tree.setEditable(false);

		Container contentPane = getContentPane();
		JScrollPane scrollPane = new JScrollPane(tree);
        contentPane.add(scrollPane, "Center");
		/*
		JPanel panel = new JPanel();
		addSiblingButton = new JButton("Add Sibling");
		addSiblingButton.addActionListener(this);
		panel.add(addSiblingButton);
		addChildButton = new JButton("Add Child");
		addChildButton.addActionListener(this);
		panel.add(addChildButton);
		deleteButton = new JButton("Delete");
		deleteButton.addActionListener(this);
		panel.add(deleteButton);
		contentPane.add(panel, "South");
		*/
	}

	public Vector sortByLabel(Vector codes) {
		HashMap label2CodeMap = new HashMap();
		Vector w = new Vector();
		Vector labels = new Vector();
		for (int i=0; i<codes.size(); i++) {
			String code = (String) codes.elementAt(i);
			String label = (String) hh.getLabel(code);
			labels.add(label);
		    label2CodeMap.put(label, code);
		}
		labels = new SortUtils().quickSort(labels);
		for (int i=0; i<labels.size(); i++) {
			int j = labels.size()-i-1;
			String label = (String) labels.elementAt(j);
			String code = (String) label2CodeMap.get(label);
			w.add(code);
		}
		return w;
	}

	public TreeNode createTree(String title) {
		String display = title;
		DefaultMutableTreeNode super_root = new DefaultMutableTreeNode(display);
		code2NodeMap.put("root", super_root);

		Stack stack = new Stack();
		Vector codes = hh.getRoots();
		codes = sortByLabel(codes);
		for (int i=0; i<codes.size(); i++) {
			int k = codes.size()-i-1;
			String code = (String) codes.elementAt(k);
			DefaultMutableTreeNode root = getNode(code);
			super_root.add(root);
    		Vector subs = hh.getSubclassCodes(code);
			if (subs != null) {
				subs = sortByLabel(subs);
				for (int j=0; j<subs.size(); j++) {
					String sub = (String) subs.elementAt(j);
					stack.push(code + "|" + sub);
				}
		    }
		}

		while (!stack.isEmpty()) {
            String line = (String) stack.pop();
            Vector u = StringUtils.parseData(line, '|');
            String sup = (String) u.elementAt(0);
            String sub = (String) u.elementAt(1);
            DefaultMutableTreeNode supNode = getNode(sup);
            DefaultMutableTreeNode subNode = getNode(sub);
            supNode.add(subNode);

			Vector subs = hh.getSubclassCodes(sub);
			if (subs != null) {
				subs = sortByLabel(subs);
				for (int j=0; j<subs.size(); j++) {
					String code = (String) subs.elementAt(j);
					stack.push(sub + "|" + code);
				}
		    }
		}
		return super_root;
	}

	public void actionPerformed(ActionEvent event) {
		DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) tree
			.getLastSelectedPathComponent();

		if (selectedNode == null)
		  return;

		if (event.getSource().equals(deleteButton)) {
		  if (selectedNode.getParent() != null)
			model.removeNodeFromParent(selectedNode);
		  return;
		}

		DefaultMutableTreeNode newNode = new DefaultMutableTreeNode("New");

		if (event.getSource().equals(addSiblingButton)) {
		  DefaultMutableTreeNode parent = (DefaultMutableTreeNode) selectedNode
			  .getParent();

		  if (parent != null) {
			int selectedIndex = parent.getIndex(selectedNode);
			model.insertNodeInto(newNode, parent, selectedIndex + 1);
		  }
		} else if (event.getSource().equals(addChildButton)) {
		  model.insertNodeInto(newNode, selectedNode, selectedNode
			  .getChildCount());
		}

		TreeNode[] nodes = model.getPathToRoot(newNode);
		TreePath path = new TreePath(nodes);
		tree.scrollPathToVisible(path);
	}

	private DefaultTreeModel model;

	private JTree tree;

	private JButton addSiblingButton;

	private JButton addChildButton;

	private JButton deleteButton;

	private JButton editButton;
}
