// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.util.tree;

import java.util.Collection;
import java.util.Hashtable;
import java.util.Vector;
import java.util.regex.Pattern;

import org.joval.intf.util.IObserver;
import org.joval.intf.util.IProducer;
import org.joval.intf.util.tree.IForest;
import org.joval.intf.util.tree.ITree;
import org.joval.intf.util.tree.INode;

/**
 * Utility class for an IProducer.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class Forest implements IForest {
    Hashtable<String, ITree> trees;

    public Forest() {
	trees = new Hashtable<String, ITree>();
    }

    public void addTree(ITree tree) {
	trees.put(tree.getRoot().getName(), tree);
    }

    // Implement IForest

    public ITree makeTree(String name, String delimiter) {
	Tree tree = new Tree(name, delimiter);
	trees.put(name, tree);
	return tree;
    }

    public ITree getTree(String name) {
	return trees.get(name);
    }

    public Collection<ITree> getTrees() {
	return trees.values();
    }

    public Collection<String> search(Pattern p, boolean followLinks) {
	Collection<String> result = new Vector<String>();
	for (ITree tree : getTrees()) {
	    result.addAll(tree.search(p, followLinks));
	}
	return result;
    }
}