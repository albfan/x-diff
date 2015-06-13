/**
  * Copyright (c) 2001 - 2005
  * 	Yuan Wang. All rights reserved.
  *
  * Redistribution and use in source and binary forms, with or without
  * modification, are permitted provided that the following conditions
  * are met:
  * 1. Redistributions of source code must retain the above copyright 
  * notice, this list of conditions and the following disclaimer.
  * 2. Redistributions in binary form must reproduce the above copyright
  * notice, this list of conditions and the following disclaimer in the 
  * documentation and/or other materials provided with the distribution.
  * 3. Redistributions in any form must be accompanied by information on
  * how to obtain complete source code for the X-Diff software and any
  * accompanying software that uses the X-Diff software.  The source code
  * must either be included in the distribution or be available for no
  * more than the cost of distribution plus a nominal fee, and must be
  * freely redistributable under reasonable conditions.  For an executable
  * file, complete source code means the source code for all modules it
  * contains.  It does not include source code for modules or files that
  * typically accompany the major components of the operating system on
  * which the executable file runs.
  *
  * THIS SOFTWARE IS PROVIDED BY YUAN WANG "AS IS" AND ANY EXPRESS OR IMPLIED
  * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
  * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, OR NON-INFRINGEMENT,
  * ARE DISCLAIMED.  IN NO EVENT SHALL YUAN WANG BE LIABLE FOR ANY DIRECT,
  * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
  * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
  * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
  * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
  * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING
  * IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
  * POSSIBILITY OF SUCH DAMAGE.
  *
  */


import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.IOException;
import java.util.Random;
import java.util.Vector;

/**
  * <code>XDiff</code> computes the difference of two input XML documents.
  */
class XDiff
{
	private static String	_usage = "java XDiff [-o|-g] [-p percent] [-e encoding] xml_file1 xml_file2 diff_result\nOptions:\n  The default setting is \"-o -p 0.3 -e UTF8\"\n  -o\tThe optimal mode, to get the minimum editing distance.\n  -g\tThe greedy mode, to find a difference quickly.\n  -p\tThe maximum change percentage allowed.\n\tDefault value: 1.0 for -o mode; 0.3 for -g mode.\n  -e\tThe encoding of the output file.\n\tDefault value: UTF8.";

	private static final int	_CIRCUIT_SIZE = 2048;
	private static final int	_MATRIX_SIZE = 1024;
	private	static final int	_ATTRIBUTE_SIZE = 1024;
	private static final int	_TEXT_SIZE = 1024;
	private static boolean		_oFlag = false, _gFlag = false;
	private static double		_NO_MATCH_THRESHOLD = 0.3;
	private static final int	_sampleCount = 3;
	private static final boolean	_DEBUG = false;
	private static String		_encoding = "UTF8";

	private XTree	_xtree1, _xtree2;
	private XLut	_xlut;
	private int	_leastCostMatrix[][], _pathMatrix[][], _circuit[];

	private int	_attrList1[], _attrList2[], _textList1[], _textList2[];
	private boolean _attrMatch[], _textMatch1[], _textMatch2[];
	private long	_attrHash[], _textHash[];
	private String	_attrTag[];

	private int	_matchp[];
	private boolean	_needNewLine;
	

	/**
	  * Constructor
	  * @param	input1		input file #1
	  * @param	input2		input file #2
	  * @param	output		output file
	  */
	public XDiff(String input1, String input2, String output)
	{
		// Parse input files
		XParser	parser = new XParser();
		long	t0 = System.currentTimeMillis();
		_xtree1 = parser.parse(input1);
		long	t1 = System.currentTimeMillis();
		parser = new XParser();
		_xtree2 = parser.parse(input2);
		long	t2 = System.currentTimeMillis();

		// check both root nodes.
		int	root1 = _xtree1.getRoot();
		int	root2 = _xtree2.getRoot();
		if (_xtree1.getHashValue(root1) == _xtree2.getHashValue(root2))
		{
			System.out.println("No difference!");
			System.out.println("Execution time: " + (t2 - t0) +
					   " ms");
			System.out.println("Parsing " + input1 + ": " +
					   (t1 - t0) + " ms");
			System.out.println("Parsing " + input2 + ": " +
					   (t2 - t1) + " ms");
		}
		else
		{
			_xlut = new XLut();
			_matchp = new int[2];

			if (_xtree1.getTag(root1).compareTo(_xtree2.getTag(root2)) != 0)
			{
				System.out.println("The root is changed!");
				_matchp[0] = XTree.NO_MATCH;
				_xtree1.addMatching(root1, _matchp);
				_xtree2.addMatching(root2, _matchp);
			}
			else
			{
				// initialize data structures.
				_attrList1	= new int[_ATTRIBUTE_SIZE];
				_attrList2	= new int[_ATTRIBUTE_SIZE];
				_attrMatch	= new boolean[_ATTRIBUTE_SIZE];
				_attrHash	= new long[_ATTRIBUTE_SIZE];
				_attrTag	= new String[_ATTRIBUTE_SIZE];

				_textList1	= new int[_TEXT_SIZE];
				_textList2	= new int[_TEXT_SIZE];
				_textMatch1	= new boolean[_TEXT_SIZE];
				_textMatch2	= new boolean[_TEXT_SIZE];
				_textHash	= new long[_TEXT_SIZE];

				_leastCostMatrix = new int[_MATRIX_SIZE][];
				_pathMatrix	 = new int[_MATRIX_SIZE][];
				_circuit	 = new int[_CIRCUIT_SIZE];

				for (int i = 0; i < _MATRIX_SIZE; i++)
				{
					_leastCostMatrix[i] = new int[_MATRIX_SIZE];
					_pathMatrix[i] = new int[_MATRIX_SIZE];
				}

				_matchp[0] = XTree.CHANGE;
				_matchp[1] = root2;
				_xtree1.addMatching(root1, _matchp);
				_matchp[1] = root1;
				_xtree2.addMatching(root2, _matchp);
				xdiff(root1, root2, false);
			}

			long	t3 = System.currentTimeMillis();
			writeDiff(input1, output);
			long	t4 = System.currentTimeMillis();

			System.out.println("Difference detected!");
			System.out.println("Execution time: " + (t4 - t0) +
					   " ms");
			System.out.println("Parsing " + input1 + ": " +
					   (t1 - t0) + " ms");
			System.out.println("Parsing " + input2 + ": " +
					   (t2 - t1) + " ms");
			System.out.println("Diffing: " + (t3 - t2) + " ms");
			System.out.println("Writing result: " + (t4 - t3) +
					   " ms");
		}
	}

	/**
	  * Diff two element lists
	  * This is the official one that records matching top-down
	  * @param	pid1		parent id #1
	  * @param	pid2		parent id #2
	  * @param	matchFlag	indicates if distance computation needed
	  */
	private void xdiff(int pid1, int pid2, boolean matchFlag)
	{
		// diff attributes.
		int	attrCount1 = 0;
		int	attrCount2 = 0;
		int	attr1 = _xtree1.getFirstAttribute(pid1);
		while (attr1 != XTree.NULL_NODE)
		{
			_attrList1[attrCount1++] = attr1;
			attr1 = _xtree1.getNextAttribute(attr1);
		}
		int	attr2 = _xtree2.getFirstAttribute(pid2);
		while (attr2 != XTree.NULL_NODE)
		{
			_attrList2[attrCount2++] = attr2;
			attr2 = _xtree2.getNextAttribute(attr2);
		}

		if (attrCount1 > 0)
		{
			if (attrCount2 > 0)
				diffAttributes(attrCount1, attrCount2);
			else
			{
				_matchp[0] = XTree.NO_MATCH;
				for (int i = 0; i < attrCount1; i++)
					_xtree1.addMatching(_attrList1[i],
							    _matchp);
			}
		}
		else if (attrCount2 > 0)	// attrCount1 == 0
		{
			_matchp[0] = XTree.NO_MATCH;
			for (int i = 0; i < attrCount2; i++)
				_xtree2.addMatching(_attrList2[i], _matchp);
		}

		// Match element nodes.
		int	count1 = _xtree1.getChildrenCount(pid1) - attrCount1;
		int	count2 = _xtree2.getChildrenCount(pid2) - attrCount2;

		if (count1 == 0)
		{
			_matchp[0] = XTree.NO_MATCH;
			int	node2 = _xtree2.getFirstChild(pid2);
			_xtree2.addMatching(node2, _matchp);
			for (int i = 1; i < count2; i++)
			{
				node2 = _xtree2.getNextSibling(node2);
				_xtree2.addMatching(node2, _matchp);
			}
		}
		else if (count2 == 0)
		{
			_matchp[0] = XTree.NO_MATCH;
			int	node1 = _xtree1.getFirstChild(pid1);
			_xtree1.addMatching(node1, _matchp);
			for (int i = 1; i < count1; i++)
			{
				node1 = _xtree1.getNextSibling(node1);
				_xtree1.addMatching(node1, _matchp);
			}
		}
		else if ((count1 == 1) && (count2 == 1))
		{
			int	node1 = _xtree1.getFirstChild(pid1);
			int	node2 = _xtree2.getFirstChild(pid2);

			if (_xtree1.getHashValue(node1) == _xtree2.getHashValue(node2))
				return;

			boolean	isE1 = _xtree1.isElement(node1);
			boolean isE2 = _xtree2.isElement(node2);

			if (isE1 && isE2)
			{
				String	tag1 = _xtree1.getTag(node1);
				String	tag2 = _xtree2.getTag(node2);
				if (tag1.compareTo(tag2) == 0)
				{
					_matchp[0] = XTree.CHANGE;
					_matchp[1] = node2;
					_xtree1.addMatching(node1, _matchp);
					_matchp[1] = node1;
					_xtree2.addMatching(node2, _matchp);

					xdiff(node1, node2, matchFlag);
				}
				else
				{
					_matchp[0] = XTree.NO_MATCH;
					_xtree1.addMatching(node1, _matchp);
					_xtree2.addMatching(node2, _matchp);
				}
			}
			else if (!isE1 && !isE2)
			{
				_matchp[0] = XTree.CHANGE;
				_matchp[1] = node2;
				_xtree1.addMatching(node1, _matchp);
				_matchp[1] = node1;
				_xtree2.addMatching(node2, _matchp);
			}
			else
			{
				_matchp[0] = XTree.NO_MATCH;
				_xtree1.addMatching(node1, _matchp);
				_xtree2.addMatching(node2, _matchp);
			}
		}
		else
		{
			int[]		elements1 = new int[count1];
			int[]		elements2 = new int[count2];
			int		elementCount1 = 0, textCount1 = 0;
			int		elementCount2 = 0, textCount2 = 0;

			int	child1 = _xtree1.getFirstChild(pid1);
			if (_xtree1.isElement(child1))
				elements1[elementCount1++] = child1;
			else
				_textList1[textCount1++] = child1;
			for (int i = 1; i < count1; i++)
			{
				child1 = _xtree1.getNextSibling(child1);
				if (_xtree1.isElement(child1))
					elements1[elementCount1++] = child1;
				else
					_textList1[textCount1++] = child1;
			}

			int	child2 = _xtree2.getFirstChild(pid2);
			if (_xtree2.isElement(child2))
				elements2[elementCount2++] = child2;
			else
				_textList2[textCount2++] = child2;
			for (int i = 1; i < count2; i++)
			{
				child2 = _xtree2.getNextSibling(child2);
				if (_xtree2.isElement(child2))
					elements2[elementCount2++] = child2;
				else
					_textList2[textCount2++] = child2;
			}

			// Match text nodes.
			if (textCount1 > 0)
			{
				if (textCount2 > 0)
					diffText(textCount1, textCount2);
				else
				{
					_matchp[0] = XTree.NO_MATCH;
					for (int i = 0; i < textCount1; i++)
						_xtree1.addMatching(_textList1[i], _matchp);
				}
			}
			else if (textCount2 > 0)
			{
				_matchp[0] = XTree.NO_MATCH;
				for (int i = 0; i < textCount2; i++)
					_xtree2.addMatching(_textList2[i],
							    _matchp);
			}

			boolean[]	matched1 = new boolean[elementCount1];
			boolean[]	matched2 = new boolean[elementCount2];
			int	mcount = _matchFilter(elements1, elementCount1,
						      elements2, elementCount2,
						      matched1, matched2);

			if ((elementCount1 == mcount) &&
			    (elementCount2 == mcount))
				return;

			if (elementCount1 == mcount)
			{
				_matchp[0] = XTree.NO_MATCH;
				for (int i = 0; i < elementCount2; i++)
				{
					if (!matched2[i])
						_xtree2.addMatching(elements2[i], _matchp);
				}
				return;
			}
			if (elementCount2 == mcount)
			{
				_matchp[0] = XTree.NO_MATCH;
				for (int i = 0; i < elementCount1; i++)
				{
					if (!matched1[i])
						_xtree1.addMatching(elements1[i], _matchp);
				}
				return;
			}

			// Write the list of unmatched nodes.
			int	ucount1 = elementCount1 - mcount;
			int	ucount2 = elementCount2 - mcount;
			int[]	unmatched1 = new int[ucount1];
			int[]	unmatched2 = new int[ucount2];
			int	muc1 = 0, muc2 = 0;
			int	start = 0;

			while ((muc1 < ucount1) && (muc2 < ucount2))
			{
				for (; (start < elementCount1) && matched1[start]; start++);
				String	startTag = _xtree1.getTag(elements1[start]);
				int	uele1 = 0, uele2 = 0;
				muc1++;
				unmatched1[uele1++] = elements1[start];
				matched1[start++] = true;

				for (int i = start; (i < elementCount1) && (muc1 < ucount1); i++)
				{
					if (!matched1[i] && startTag.equals(_xtree1.getTag(elements1[i])))
					{
						matched1[i] = true;
						muc1++;
						unmatched1[uele1++] = elements1[i];
					}
				}

				for (int i = 0; (i < elementCount2) && (muc2 < ucount2); i++)
				{
					if (!matched2[i] && startTag.equals(_xtree2.getTag(elements2[i])))
					{
						matched2[i] = true;
						muc2++;
						unmatched2[uele2++] = elements2[i];
					}
				}

				if (uele2 == 0)
				{
					_matchp[0] = XTree.NO_MATCH;
					for (int i = 0; i < uele1; i++)
						_xtree1.addMatching(unmatched1[i], _matchp);
				}
				else
				{
					if ((uele1 == 1) && (uele2 == 1))
					{
						_matchp[0] = XTree.CHANGE;
						_matchp[1] = unmatched2[0];
						_xtree1.addMatching(unmatched1[0], _matchp);
						_matchp[1] = unmatched1[0];
						_xtree2.addMatching(unmatched2[0], _matchp);
						xdiff(unmatched1[0],
						      unmatched2[0],
						      matchFlag);
					}
					// To find minimal-cost matching between those unmatched.
					else if (uele1 >= uele2)
					{
						if ((uele2 <= _sampleCount) || !_gFlag)
							matchListO(unmatched1, unmatched2, uele1, uele2, true, matchFlag);
						else
							matchList(unmatched1, unmatched2, uele1, uele2, true, matchFlag);
					}	
					else
					{
						if ((uele1 <= _sampleCount) || !_gFlag)
							matchListO(unmatched2, unmatched1, uele2, uele1, false, matchFlag);
						else
							matchList(unmatched2, unmatched1, uele2, uele1, false, matchFlag);
					}
				}
			}

			if (muc1 < ucount1)
			{
				_matchp[0] = XTree.NO_MATCH;
				for (int i = start; i < elementCount1; i++)
				{
					if (!matched1[i])
						_xtree1.addMatching(elements1[i], _matchp);
				}
			}
			else if (muc2 < ucount2)
			{
				_matchp[0] = XTree.NO_MATCH;
				for (int i = 0; i < elementCount2; i++)
				{
					if (!matched2[i])
						_xtree2.addMatching(elements2[i], _matchp);
				}
			}
		}
	}

	/**
	  * Diff and match two lists of attributes
	  * @param	attrCount1	number of attributes in the 1st list
	  * @param	attrCount2	number of attributes in the 2nd list
	  */
	private void diffAttributes(int attrCount1, int attrCount2)
	{
		if ((attrCount1 == 1) && (attrCount2 == 1))
		{
			long	ah1 = _xtree1.getHashValue(_attrList1[0]);
			long	ah2 = _xtree2.getHashValue(_attrList2[0]);
			if (ah1 == ah2)
				return;

			String	tag1 = _xtree1.getTag(_attrList1[0]);
			String	tag2 = _xtree2.getTag(_attrList2[0]);
			if (tag1.compareTo(tag2) == 0)
			{
				_matchp[0] = XTree.CHANGE;
				_matchp[1] = _attrList2[0];
				_xtree1.addMatching(_attrList1[0], _matchp);

				_matchp[1] = _attrList1[0];
				_xtree2.addMatching(_attrList2[0], _matchp);

				int tid1 = _xtree1.getFirstChild(_attrList1[0]);
				int tid2 = _xtree2.getFirstChild(_attrList2[0]);
				_matchp[1] = tid2;
				_xtree1.addMatching(tid1, _matchp);

				_matchp[1] = tid1;
				_xtree2.addMatching(tid2, _matchp);

				return;
			}
			else
			{
				_matchp[0] = XTree.NO_MATCH;
				_xtree1.addMatching(_attrList1[0], _matchp);
				_xtree2.addMatching(_attrList2[0], _matchp);
				return;
			}
		}

		for (int i = 0; i < attrCount2; i++)
		{
			_attrHash[i] = _xtree2.getHashValue(_attrList2[i]);
			_attrTag[i] = _xtree2.getTag(_attrList2[i]);
			_attrMatch[i] = false;
		}

		int	matchCount = 0;
		for (int i = 0; i < attrCount1; i++)
		{
			int	attr1 = _attrList1[i];
			long	ah1 = _xtree1.getHashValue(attr1);
			String	tag1 = _xtree1.getTag(attr1);

			boolean	found = false;
			for (int j = 0; j < attrCount2; j++)
			{
				int	attr2 = _attrList2[j];
				if (_attrMatch[j])
					continue;
				else if (ah1 == _attrHash[j])
				{
					_attrMatch[j] = true;
					matchCount++;
					found = true;
					break;
				}
				else if (tag1.compareTo(_attrTag[j]) == 0)
				{
					_attrMatch[j] = true;
					matchCount++;

					_matchp[0] = XTree.CHANGE;
					_matchp[1] = attr2;
					_xtree1.addMatching(attr1, _matchp);

					_matchp[1] = attr1;
					_xtree2.addMatching(attr2, _matchp);

					int tid1 = _xtree1.getFirstChild(attr1);
					int tid2 = _xtree2.getFirstChild(attr2);
					_matchp[1] = tid2;
					_xtree1.addMatching(tid1, _matchp);

					_matchp[1] = tid1;
					_xtree2.addMatching(tid2, _matchp);

					found = true;
					break;
				}
			}

			if (!found)
			{
				_matchp[0] = XTree.NO_MATCH;
				_xtree1.addMatching(attr1, _matchp);
			}
		}

		if (matchCount != attrCount2)
		{
			_matchp[0] = XTree.NO_MATCH;
			for (int i = 0; i < attrCount2; i++)
			{
				if (!_attrMatch[i])
					_xtree2.addMatching(_attrList2[i],
							    _matchp);
			}
		}
	}

	/**
	  * Diff and match two lists of text nodes.
	  * XXX This is just a hack that treats text nodes as unordered, to
	  * be consistent with the entire algorithm.
	  * @param	textCount1	number of text nodes in the 1st list
	  * @param	textCount2	number of text nodes in the 2nd list
	  */
	private void diffText(int textCount1, int textCount2)
	{
		for (int i = 0; i < textCount1; i++)
			_textMatch1[i] = false;
		for (int i = 0; i < textCount2; i++)
		{
			_textMatch2[i] = false;
			_textHash[i] = _xtree2.getHashValue(_textList2[i]);
		}

		int	mcount = 0;
		for (int i = 0; i < textCount1; i++)
		{
			long	hash1 = _xtree1.getHashValue(_textList1[i]);
			for (int j = 0; j < textCount2; j++)
			{
				if (!_textMatch2[j] && (hash1 == _textHash[j]))
				{
					_textMatch1[i] = true;
					_textMatch2[j] = true;
					mcount++;
					break;
				}
			}

			if (mcount == textCount2)
				break;
		}

		if ((mcount < textCount1) && (textCount1 <= textCount2))
		{
			_matchp[0] = XTree.CHANGE;
			for (int i = 0, j = 0;
			     (i < textCount1) && (mcount < textCount1); i++)
			{
				if (_textMatch1[i])
					continue;
				for (; _textMatch2[j]; j++);
				_matchp[1] = _textList2[j];
				_xtree1.addMatching(_textList1[i], _matchp);
				_textMatch1[i] = true;
				_matchp[1] = _textList1[i];
				_xtree2.addMatching(_textList2[j], _matchp);
				_textMatch2[j] = true;
				mcount++;
			}
		}
		else if ((mcount < textCount2) && (textCount2 < textCount1))
		{
			_matchp[0] = XTree.CHANGE;
			for (int i = 0, j = 0;
			     (i < textCount2) && (mcount < textCount2); i++)
			{
				if (_textMatch2[i])
					continue;
				for (; _textMatch1[j]; j++);
				_matchp[1] = _textList1[j];
				_xtree2.addMatching(_textList2[i], _matchp);
				_textMatch2[i] = true;
				_matchp[1] = _textList2[i];
				_xtree1.addMatching(_textList1[j], _matchp);
				_textMatch1[j] = true;
				mcount++;
			}
		}

		_matchp[0] = XTree.NO_MATCH;
		if (mcount < textCount1)
		{
			for (int i = 0; i < textCount1; i++)
			{
				if (!_textMatch1[i])
					_xtree1.addMatching(_textList1[i],
							    _matchp);
			}
		}
		else if (mcount < textCount2)
		{
			for (int i = 0; i < textCount2; i++)
			{
				if (!_textMatch2[i])
					_xtree2.addMatching(_textList2[i],
							    _matchp);
			}
		}
	}

	/**
	  * Filter out matched nodepairs.
	  * @param	elements1	node list #1
	  * @param	elements2	node list #2
	  * @param	matched1	match list #1
	  * @param	matched2	match list #2
	  * @return	how many matched pairs found
	  */
	private int _matchFilter(int elements1[], int count1,
				 int elements2[], int count2,
				 boolean matched1[], boolean matched2[])
	{
		long[]	value1 = new long[count1];
		long[]	value2 = new long[count2];

		for (int i = 0; i < count1; i++)
		{
			value1[i] = _xtree1.getHashValue(elements1[i]);
			matched1[i] = false;
		}
		for (int i = 0; i < count2; i++)
		{
			value2[i] = _xtree2.getHashValue(elements2[i]);
			matched2[i] = false;
		}

		int	mcount = 0;
		for (int i = 0; i < count2; i++)
			for (int j = 0; j < count1; j++)
			{
				if (!matched1[j] && !matched2[i] &&
				    (value1[j] == value2[i]))
				{
					matched1[j] = true;
					matched2[i] = true;
					mcount++;
					break;
				}
			}

		return mcount;
	}

	/**
	  * Find minimal cost matching between two node lists;
	  * Record the matching info back to the trees
	  * Using the original algorithm
	  * @param	nodes1		node list #1
	  * @param	nodes2		node list #2
	  * @param	count1		# of nodes in node list #1
	  * @param	count2		# of nodes in node list #2
	  * @param	treeOrder	true for original, false for inverse
	  * @param	matchFlag	indicates if distance computation needed
	  */
	private void matchListO(int nodes1[], int nodes2[], int count1,
				int count2, boolean treeOrder,
				boolean matchFlag)
	{
		int[][]	distance = new int[count1+1][];
		int[]	matching1 = new int[count1];
		int[]	matching2 = new int[count2];

		// insert cost.
		distance[count1] = new int[count2+1];
		for (int i = 0; i < count2; i++)
			distance[count1][i] = (treeOrder ? _xtree2.getDecendentsCount(nodes2[i]) : _xtree1.getDecendentsCount(nodes2[i])) + 1;

		for (int i = 0; i < count1; i++)
		{
			distance[i] = new int[count2+1];
			int	deleteCost = (treeOrder ? _xtree1.getDecendentsCount(nodes1[i]) : _xtree2.getDecendentsCount(nodes1[i])) + 1;
			for (int j = 0; j < count2; j++)
			{
				int	dist = 0;
				if (matchFlag)
					dist = treeOrder ? _xlut.get(nodes1[i], nodes2[j]) : _xlut.get(nodes2[j], nodes1[i]);
				else
				{
					dist = treeOrder ? distance(nodes1[i], nodes2[j], true, XTree.NO_CONNECTION) : distance(nodes2[j], nodes1[i], true, XTree.NO_CONNECTION);
					// the default mode.
					if (!_oFlag && (dist > 1) && (dist >= _NO_MATCH_THRESHOLD * (deleteCost + distance[count1][j])))
						dist = XTree.NO_CONNECTION;
					if (dist < XTree.NO_CONNECTION)
						if (treeOrder)
							_xlut.add(nodes1[i],
								  nodes2[j],
								  dist);
						else
							_xlut.add(nodes2[j],
								  nodes1[i],
								  dist);
				}
				distance[i][j] = dist;
			}
			// delete cost.
			distance[i][count2] = deleteCost;
		}

		// compute the minimal cost matching.
		findMatching(count1, count2, distance, matching1, matching2);

		for (int i = 0; i < count1; i++)
		{
			if (matching1[i] == XTree.NO_MATCH)
				_matchp[0] = XTree.NO_MATCH;
			else
			{
				_matchp[0] = XTree.CHANGE;
				_matchp[1] = nodes2[matching1[i]];
			}
			if (treeOrder)
				_xtree1.addMatching(nodes1[i], _matchp);
			else
				_xtree2.addMatching(nodes1[i], _matchp);
		}

		for (int i = 0; i < count2; i++)
		{
			if (matching2[i] == XTree.NO_MATCH)
				_matchp[0] = XTree.NO_MATCH;
			else
			{
				_matchp[0] = XTree.CHANGE;
				_matchp[1] = nodes1[matching2[i]];
			}
			if (treeOrder)
				_xtree2.addMatching(nodes2[i], _matchp);
			else
				_xtree1.addMatching(nodes2[i], _matchp);
		}

		for (int i = 0; i < count1; i++)
		{
			if (matching1[i] != XTree.NO_MATCH)
			{
				int	todo1 = nodes1[i];
				int	todo2 = nodes2[matching1[i]];
				if (treeOrder)
				{
					if (_xtree1.isElement(todo1) &&
					    _xtree2.isElement(todo2))
						xdiff(todo1, todo2, true);
				}
				else
				{
					if (_xtree1.isElement(todo2) &&
					    _xtree2.isElement(todo1))
						xdiff(todo2, todo1, true);
				}
			}
		}
	}

	/**
	  * Find minimal cost matching between two node lists;
	  * Record the matching info back to the trees
	  * Do sampling.
	  * @param	nodes1		node list #1
	  * @param	nodes2		node list #2
	  * @param	count1		# of nodes in node list #1
	  * @param	count2		# of nodes in node list #2
	  * @param	treeOrder	true for original, false for inverse
	  * @param	matchFlag	indicates if distance computation needed
	  */
	private void matchList(int nodes1[], int nodes2[], int count1,
			       int count2, boolean treeOrder, boolean matchFlag)
	{
		int[]	matching1 = new int[count1];
		int[]	matching2 = new int[count2];
		for (int i = 0; i < count1; i++)
			matching1[i] = XTree.NO_MATCH;
		for (int i = 0; i < count2; i++)
			matching2[i] = XTree.NO_MATCH;

		if (matchFlag)
		{
			for (int i = 0; i < count1; i++)
			{
				for (int j = 0; j < count2; j++)
				{
					int	d = treeOrder ? _xlut.get(nodes1[i], nodes2[j]) : _xlut.get(nodes2[j], nodes1[i]);
					if (d != XTree.NO_CONNECTION)
					{
						matching1[i] = j;
						matching2[j] = i;
						break;
					}
				}
			}
		}
		else
		{
			Random	r = new Random(System.currentTimeMillis());
			int	scount1 = 0;
			int	scount2 = 0;
			int	matchingThreshold = 0;
			for (int i = 0; ((i < _sampleCount) && (scount2 < count2)); scount2++)
			{
				int	snode = r.nextInt(count2 - scount2) + scount2;
				int	dist = XTree.NO_CONNECTION;
				int	bestmatch = XTree.NO_MATCH;
				for (int j = scount1; j < count1; j++)
				{
					int	d = treeOrder ? distance(nodes1[j], nodes2[snode], false, dist) : distance(nodes2[snode], nodes1[j], false, dist);
					if (d < dist)
					{
						dist = d;
						bestmatch = j;
						if (d == 1)
							break;
					}
				}

				int	deleteCost = (treeOrder ? _xtree2.getDecendentsCount(nodes2[snode]) : _xtree1.getDecendentsCount(nodes2[snode])) + 1;
				if ((dist > 1) &&
				    (dist > (_NO_MATCH_THRESHOLD * deleteCost)))
				{
					int	tmp = nodes2[snode];
					nodes2[snode] = nodes2[scount2];
					nodes2[scount2] = tmp;
				}
				else
				{
					int	tmp = nodes1[bestmatch];
					nodes1[bestmatch] = nodes1[scount1];
					nodes1[scount1] = tmp;
					tmp = nodes2[snode];
					nodes2[snode] = nodes2[scount2];
					nodes2[scount2] = tmp;

					if (treeOrder)
						_xlut.add(nodes1[scount1], nodes2[scount2], dist);
					else
						_xlut.add(nodes2[scount2], nodes1[scount1], dist);
					matching1[scount1] = scount2;
					matching2[scount2] = scount1;

					i++;
					scount1++;
					if (matchingThreshold < dist)
						matchingThreshold = dist;
				}
			}

			for (;scount2 < count2; scount2++)
			{
				int	dist = XTree.NO_CONNECTION;
				int	bestmatch = XTree.NO_MATCH;
				for (int i = scount1; i < count1; i++)
				{
					int	d = treeOrder ? distance(nodes1[i], nodes2[scount2], false, dist) : distance(nodes2[scount2], nodes1[i], false, dist);
					if (d <= matchingThreshold)
					{
						dist = d;
						bestmatch = i;
						break;
					}
					else if (d < dist)
					{
						dist = d;
						bestmatch = i;
					}
				}

				if (bestmatch != XTree.NO_MATCH)
				{
					int	tmp = nodes1[bestmatch];
					nodes1[bestmatch] = nodes1[scount1];
					nodes1[scount1] = tmp;

					if (treeOrder)
						_xlut.add(nodes1[scount1], nodes2[scount2], dist);
					else
						_xlut.add(nodes2[scount2], nodes1[scount1], dist);
					matching1[scount1] = scount2;
					matching2[scount2] = scount1;
					scount1++;
				}
			}
		}

		// Record matching
		for (int i = 0; i < count1; i++)
		{
			if (matching1[i] == XTree.NO_MATCH)
				_matchp[0] = XTree.NO_MATCH;
			else
			{
				_matchp[0] = XTree.CHANGE;
				_matchp[1] = nodes2[matching1[i]];
			}
			if (treeOrder)
				_xtree1.addMatching(nodes1[i], _matchp);
			else
				_xtree2.addMatching(nodes1[i], _matchp);
		}

		for (int i = 0; i < count2; i++)
		{
			if (matching2[i] == XTree.NO_MATCH)
				_matchp[0] = XTree.NO_MATCH;
			else
			{
				_matchp[0] = XTree.CHANGE;
				_matchp[1] = nodes1[matching2[i]];
			}
			if (treeOrder)
				_xtree2.addMatching(nodes2[i], _matchp);
			else
				_xtree1.addMatching(nodes2[i], _matchp);
		}

		for (int i = 0; i < count1; i++)
		{
			if (matching1[i] != XTree.NO_MATCH)
			{
				int	todo1 = nodes1[i];
				int	todo2 = nodes2[matching1[i]];
				if (treeOrder)
				{
					if (_xtree1.isElement(todo1) &&
					    _xtree2.isElement(todo2))
						xdiff(todo1, todo2, true);
				}
				else
				{
					if (_xtree1.isElement(todo2) &&
					    _xtree2.isElement(todo1))
						xdiff(todo2, todo1, true);
				}
			}
		}
	}

	/**
	  * Compute (minimal-editing) distance between two nodes.
	  * @param	eid1		element id #1
	  * @param	eid2		element id #2
	  * @param	toRecord	whether or not to keep the result
	  * @param	threshold	No need to return a distance higher
	  *				than this threshold
	  * @return	the distance
	  */
	private int distance(int eid1, int eid2, boolean toRecord,
			     int threshold)
	{
		boolean	isE1 = _xtree1.isElement(eid1);
		boolean isE2 = _xtree2.isElement(eid2);
		if (isE1 && isE2)
		{
			if (_xtree1.getTag(eid1).compareTo(_xtree2.getTag(eid2)) != 0)
				return XTree.NO_CONNECTION;
			else 
			{
				int	dist = _xdiff(eid1, eid2, threshold);
				if (toRecord && (dist < XTree.NO_CONNECTION))
					_xlut.add(eid1, eid2, dist);
				return dist;
			}
		}
		else if (!isE1 && !isE2)
			return 1;
		else
			return XTree.NO_CONNECTION;
	}

	/**
	  * To compute the editing distance between two nodes
	  * @param	pid1		parent id #1
	  * @param	pid2		parent id #2
	  * @param	threshold	No need to return a distance higher
	  *				than this threshold
	  * @return	the distance
	  */
	private int _xdiff(int pid1, int pid2, int threshold)
	{
		int	dist = 0;

		// diff attributes.
		int	attrCount1 = 0;
		int	attrCount2 = 0;
		int	attr1 = _xtree1.getFirstAttribute(pid1);
		while (attr1 != XTree.NULL_NODE)
		{
			_attrList1[attrCount1++] = attr1;
			attr1 = _xtree1.getNextAttribute(attr1);
		}
		int	attr2 = _xtree2.getFirstAttribute(pid2);
		while (attr2 != XTree.NULL_NODE)
		{
			_attrList2[attrCount2++] = attr2;
			attr2 = _xtree2.getNextAttribute(attr2);
		}

		if (attrCount1 == 0)
			dist = attrCount2 * 2;
		else if (attrCount2 == 0)
			dist = attrCount1 * 2;
		else
			dist = _diffAttributes(attrCount1, attrCount2);
		if (_gFlag && (dist >= threshold))
			return XTree.NO_CONNECTION;

		// Match second level nodes first.
		int	count1 = _xtree1.getChildrenCount(pid1) - attrCount1;
		int	count2 = _xtree2.getChildrenCount(pid2) - attrCount2;

		if (count1 == 0)
		{
			int	node2 = _xtree2.getFirstChild(pid2);
			while (node2 != XTree.NULL_NODE)
			{
				dist += _xtree2.getDecendentsCount(node2) + 1;
				if (_gFlag && (dist >= threshold))
					return XTree.NO_CONNECTION;
				node2 = _xtree2.getNextSibling(node2);
			}
		}
		else if (count2 == 0)
		{
			int	node1 = _xtree1.getFirstChild(pid1);
			while (node1 != XTree.NULL_NODE)
			{
				dist += _xtree1.getDecendentsCount(node1) + 1;
				if (_gFlag && (dist >= threshold))
					return XTree.NO_CONNECTION;
				node1 = _xtree1.getNextSibling(node1);
			}
		}
		else if ((count1 == 1) && (count2 == 1))
		{
			int	node1 = _xtree1.getFirstChild(pid1);
			int	node2 = _xtree2.getFirstChild(pid2);

			if (_xtree1.getHashValue(node1) == _xtree2.getHashValue(node2))
				return dist;

			boolean	isE1 = _xtree1.isElement(node1);
			boolean isE2 = _xtree2.isElement(node2);

			if (isE1 && isE2)
			{
				String	tag1 = _xtree1.getTag(node1);
				String	tag2 = _xtree2.getTag(node2);
				if (tag1.compareTo(tag2) == 0)
					dist += _xdiff(node1, node2, threshold - dist);
				else
					dist += _xtree1.getDecendentsCount(node1) + _xtree2.getDecendentsCount(node2) + 2;
			}
			else if (!isE1 && !isE2)
				dist++;
			else
				dist += _xtree1.getDecendentsCount(node1) + _xtree2.getDecendentsCount(node2) + 2;
		}
		else
		{
			int[]		elements1 = new int[count1];
			int[]		elements2 = new int[count2];
			int		elementCount1 = 0, textCount1 = 0;
			int		elementCount2 = 0, textCount2 = 0;

			int	child1 = _xtree1.getFirstChild(pid1);
			if (_xtree1.isElement(child1))
				elements1[elementCount1++] = child1;
			else
				_textList1[textCount1++] = child1;
			for (int i = 1; i < count1; i++)
			{
				child1 = _xtree1.getNextSibling(child1);
				if (_xtree1.isElement(child1))
					elements1[elementCount1++] = child1;
				else
					_textList1[textCount1++] = child1;
			}

			int	child2 = _xtree2.getFirstChild(pid2);
			if (_xtree2.isElement(child2))
				elements2[elementCount2++] = child2;
			else
				_textList2[textCount2++] = child2;
			for (int i = 1; i < count2; i++)
			{
				child2 = _xtree2.getNextSibling(child2);
				if (_xtree2.isElement(child2))
					elements2[elementCount2++] = child2;
				else
					_textList2[textCount2++] = child2;
			}

			// Match text nodes.
			if (textCount1 == 0)
			{
				dist += textCount2;
			}
			else if (textCount2 == 0)
			{
				dist += textCount1;
			}
			else
				dist += _diffText(textCount1, textCount2);

			if (_gFlag && (dist >= threshold))
				return XTree.NO_CONNECTION;

			boolean[]	matched1 = new boolean[elementCount1];
			boolean[]	matched2 = new boolean[elementCount2];
			int	mcount = _matchFilter(elements1, elementCount1,
						      elements2, elementCount2,
						      matched1, matched2);

			if ((elementCount1 == mcount) &&
			    (elementCount2 == mcount))
				return dist;
			if (elementCount1 == mcount)
			{
				for (int i = 0; i < elementCount2; i++)
				{
					if (!matched2[i])
					{
						dist += _xtree2.getDecendentsCount(elements2[i]) + 1;
						if (_gFlag && (dist >= threshold))
							return XTree.NO_CONNECTION;
					}
				}
				return dist;
			}
			if (elementCount2 == mcount)
			{
				for (int i = 0; i < elementCount1; i++)
				{
					if (!matched1[i])
					{
						dist += _xtree1.getDecendentsCount(elements1[i]) + 1;
						if (_gFlag && (dist >= threshold))
							return XTree.NO_CONNECTION;
					}
				}
				return dist;
			}

			// Write the list of unmatched nodes.
			int	ucount1 = elementCount1 - mcount;
			int	ucount2 = elementCount2 - mcount;
			int[]	unmatched1 = new int[ucount1];
			int[]	unmatched2 = new int[ucount2];
			int	muc1 = 0, muc2 = 0;
			int	start = 0;

			while ((muc1 < ucount1) && (muc2 < ucount2))
			{
				for (; (start < elementCount1) && matched1[start]; start++);
				String	startTag = _xtree1.getTag(elements1[start]);
				int	uele1 = 0, uele2 = 0;
				muc1++;
				unmatched1[uele1++] = elements1[start];
				matched1[start++] = true;

				for (int i = start; (i < elementCount1) && (muc1 < ucount1); i++)
				{
					if (!matched1[i] && startTag.equals(_xtree1.getTag(elements1[i])))
					{
						matched1[i] = true;
						muc1++;
						unmatched1[uele1++] = elements1[i];
					}
				}

				for (int i = 0; (i < elementCount2) && (muc2 < ucount2); i++)
				{
					if (!matched2[i] && startTag.equals(_xtree2.getTag(elements2[i])))
					{
						matched2[i] = true;
						muc2++;
						unmatched2[uele2++] = elements2[i];
					}
				}

				if (uele2 == 0)
				{
					for (int i = 0; i < uele1; i++)
						dist += _xtree1.getDecendentsCount(unmatched1[i]);
				}
				else
				{
/*
   					if ((uele1 == 1) && (uele2 == 1))
					{
						dist += _xdiff(unmatched1[0],
							       unmatched2[0],
							       threshold-dist);
					}
					else if (uele1 >= uele2)
					*/
					// To find minimal-cost matching between those unmatched.
					if (uele1 >= uele2)
					{
						if ((uele2 <= _sampleCount) || !_gFlag)
							dist += _matchListO(unmatched1, unmatched2, uele1, uele2, true);
						else
							dist += _matchList(unmatched1, unmatched2, uele1, uele2, true, threshold - dist);
					}	
					else
					{
						if ((uele1 <= _sampleCount) || !_gFlag)
							dist += _matchListO(unmatched2, unmatched1, uele2, uele1, false);
						else
							dist += _matchList(unmatched2, unmatched1, uele2, uele1, false, threshold - dist);
					}
				}

				if (_gFlag && (dist >= threshold))
					return XTree.NO_CONNECTION;
			}

			if (muc1 < ucount1)
			{
				for (int i = start; i < elementCount1; i++)
				{
					if (!matched1[i])
						dist += _xtree1.getDecendentsCount(elements1[i]);
				}
			}
			else if (muc2 < ucount2)
			{
				for (int i = 0; i < elementCount2; i++)
				{
					if (!matched2[i])
						dist += _xtree2.getDecendentsCount(elements2[i]);
				}
			}
		}

		if (!_gFlag || (dist < threshold))
			return dist;
		else
			return XTree.NO_CONNECTION;
	}

	/**
	  * Diff two lists of attributes
	  * @param	attrCount1	number of attributes in the 1st list
	  * @param	attrCount2	number of attributes in the 2nd list
	  * @return	the distance
	  */
	private int _diffAttributes(int attrCount1, int attrCount2)
	{
		if ((attrCount1 == 1) && (attrCount2 == 1))
		{
			long	ah1 = _xtree1.getHashValue(_attrList1[0]);
			long	ah2 = _xtree2.getHashValue(_attrList2[0]);
			if (ah1 == ah2)
				return 0;

			String	tag1 = _xtree1.getTag(_attrList1[0]);
			String	tag2 = _xtree2.getTag(_attrList2[0]);
			if (tag1.compareTo(tag2) == 0)
				return 1;
			else
				return 2;
		}

		int	dist = 0;
		for (int i = 0; i < attrCount2; i++)
		{
			_attrHash[i] = _xtree2.getHashValue(_attrList2[i]);
			_attrTag[i] = _xtree2.getTag(_attrList2[i]);
			_attrMatch[i] = false;
		}

		int	matchCount = 0;
		for (int i = 0; i < attrCount1; i++)
		{
			long	ah1 = _xtree1.getHashValue(_attrList1[i]);
			String	tag1 = _xtree1.getTag(_attrList1[i]);
			boolean	found = false;

			for (int j = 0; j < attrCount2; j++)
			{
				if (_attrMatch[j])
					continue;
				else if (ah1 == _attrHash[j])
				{
					_attrMatch[j] = true;
					found = true;
					matchCount++;
					break;
				}
				else if (tag1.compareTo(_attrTag[j]) == 0)
				{
					_attrMatch[j] = true;
					dist++;
					found = true;
					matchCount++;
					break;
				}
			}

			if (!found)
				dist += 2;
		}

		dist += (attrCount2 - matchCount) * 2;
		return dist;
	}

	/**
	  * Diff and match two lists of text nodes.
	  * XXX This is just a hack that treats text nodes as unordered, to
	  * be consistent with the entire algorithm.
	  * @param	textCount1	number of text nodes in the 1st list
	  * @param	textCount2	number of text nodes in the 2nd list
	  * @return the "distance" between these two lists.
	  */
	private int _diffText(int textCount1, int textCount2)
	{
		for (int i = 0; i < textCount2; i++)
		{
			_textMatch2[i] = false;
			_textHash[i] = _xtree2.getHashValue(_textList2[i]);
		}

		int	mcount = 0;
		for (int i = 0; i < textCount1; i++)
		{
			long	hash1 = _xtree1.getHashValue(_textList1[i]);
			for (int j = 0; j < textCount2; j++)
			{
				if (!_textMatch2[j] && (hash1 == _textHash[j]))
				{
					_textMatch2[j] = true;
					mcount++;
					break;
				}
			}

			if (mcount == textCount2)
				break;
		}

		if (textCount1 >= textCount2)
			return textCount1 - mcount;
		else
			return textCount2 - mcount;
	}

	/**
	  * Find minimal cost matching between two node lists;
	  * Using the original algorithm
	  * @param	nodes1		node list #1
	  * @param	nodes2		node list #2
	  * @param	count1		# of nodes in node list #1
	  * @param	count2		# of nodes in node list #2
	  * @param	treeOrder	true for original, false for inverse
	  */
	private int _matchListO(int nodes1[], int nodes2[], int count1,
				int count2, boolean treeOrder)
	{
		int[][]	distance = new int[count1+1][];
		int[]	matching1 = new int[count1];
		int[]	matching2 = new int[count2];

		// insert cost.
		distance[count1] = new int[count2+1];
		for (int i = 0; i < count2; i++)
			distance[count1][i] = (treeOrder ? _xtree2.getDecendentsCount(nodes2[i]) : _xtree1.getDecendentsCount(nodes2[i])) + 1;

		for (int i = 0; i < count1; i++)
		{
			distance[i] = new int[count2+1];
			int	deleteCost = (treeOrder ? _xtree1.getDecendentsCount(nodes1[i]) : _xtree2.getDecendentsCount(nodes1[i])) + 1;
			for (int j = 0; j < count2; j++)
			{
				int	dist = treeOrder ? distance(nodes1[i], nodes2[j], true, XTree.NO_CONNECTION) : distance(nodes2[j], nodes1[i], true, XTree.NO_CONNECTION);
				// the default mode.
				if (!_oFlag && (dist > 1) &&
				    (dist < XTree.NO_CONNECTION) &&
				    (dist >= _NO_MATCH_THRESHOLD *
					(deleteCost + distance[count1][j])))
					dist = XTree.NO_CONNECTION;

				if (dist < XTree.NO_CONNECTION)
				{
					if (treeOrder)
						_xlut.add(nodes1[i], nodes2[j],
							  dist);
			       		else
					       	_xlut.add(nodes2[j], nodes1[i],
							  dist);
				}
				distance[i][j] = dist;
			}
			// delete cost.
			distance[i][count2] = deleteCost;
		}

		// compute the minimal cost matching.
		return findMatching(count1, count2, distance, matching1,
				    matching2);
	}

	/**
	  * Find minimal cost matching between two node lists;
	  * Do sampling
	  * @param	nodes1		node list #1
	  * @param	nodes2		node list #2
	  * @param	count1		# of nodes in node list #1
	  * @param	count2		# of nodes in node list #2
	  * @param	treeOrder	true for original, false for inverse
	  * @param	threshold	No need to return a distance higher
	  *				than this threshold
	  */
	private int _matchList(int nodes1[], int nodes2[], int count1,
			       int count2, boolean treeOrder, int threshold)
	{
		int[]	matching1 = new int[count1];
		int[]	matching2 = new int[count2];
		for (int i = 0; i < count1; i++)
			matching1[i] = XTree.NO_MATCH;
		for (int i = 0; i < count2; i++)
			matching2[i] = XTree.NO_MATCH;

		int	distance = 0;
		Random	r = new Random(System.currentTimeMillis());
		int	scount1 = 0;
		int	scount2 = 0;
		int	matchingThreshold = 0;

		for (int i = 0; ((i < _sampleCount) && (scount2 < count2)); scount2++)
		{
			int	snode = r.nextInt(count2 - scount2) + scount2;
			int	dist = XTree.NO_CONNECTION;
			int	bestmatch = XTree.NO_MATCH;
			for (int j = scount1; j < count1; j++)
			{
				int	d = treeOrder ? distance(nodes1[j], nodes2[snode], false, threshold - distance) : distance(nodes2[snode], nodes1[j], false, threshold - distance);
				if (d < dist)
				{
					dist = d;
					bestmatch = j;
					if (d == 1)
						break;
				}
			}

			int	deleteCost = (treeOrder ? _xtree2.getDecendentsCount(nodes2[snode]) : _xtree1.getDecendentsCount(nodes2[snode])) + 1;

			if ((dist > 1) &&
			    (dist > (_NO_MATCH_THRESHOLD * deleteCost)))
			{
				int	tmp = nodes2[snode];
				nodes2[snode] = nodes2[scount2];
				nodes2[scount2] = tmp;
				distance += deleteCost;
			}
			else
			{
				int	tmp = nodes1[bestmatch];
				nodes1[bestmatch] = nodes1[scount1];
				nodes1[scount1] = tmp;
				tmp = nodes2[snode];
				nodes2[snode] = nodes2[scount2];
				nodes2[scount2] = tmp;

				if (treeOrder)
					_xlut.add(nodes1[scount1], nodes2[scount2], dist);
				else
					_xlut.add(nodes2[scount2], nodes1[scount1], dist);
				matching1[scount1] = scount2;
				matching2[scount2] = scount1;

				i++;
				scount1++;
				if (matchingThreshold < dist)
					matchingThreshold = dist;
				distance += dist;
			}

			if (distance >= threshold)
				return XTree.NO_CONNECTION;
		}

		for (;scount2 < count2; scount2++)
		{
			int	deleteCost = (treeOrder ? _xtree2.getDecendentsCount(nodes2[scount2]) : _xtree1.getDecendentsCount(nodes2[scount2])) + 1;
			int	dist = XTree.NO_CONNECTION;
			int	bestmatch = XTree.NO_MATCH;
			for (int i = scount1; i < count1; i++)
			{
				int	d = treeOrder ? distance(nodes1[i], nodes2[scount2], false, threshold - distance) : distance(nodes2[scount2], nodes1[i], false, threshold - distance);
				if (d <= matchingThreshold)
				{
					dist = d;
					bestmatch = i;
					break;
				}
				else if ((d == 1) || ( d < (_NO_MATCH_THRESHOLD * dist)))
				{
					dist = d;
					bestmatch = i;
				}
			}

			if (bestmatch == XTree.NO_MATCH)
			{
				distance += deleteCost;
			}
			else
			{
				int	tmp = nodes1[bestmatch];
				nodes1[bestmatch] = nodes1[scount1];
				nodes1[scount1] = tmp;

				if (treeOrder)
					_xlut.add(nodes1[scount1], nodes2[scount2], dist);
				else
					_xlut.add(nodes2[scount2], nodes1[scount1], dist);

				matching1[scount1] = scount2;
				matching2[scount2] = scount1;
				scount1++;
				distance += dist;
			}

			if (distance >= threshold)
				return XTree.NO_CONNECTION;
		}

		for (int i = 0; i < count1; i++)
		{
			if (matching1[i] == XTree.NO_MATCH)
			{
				distance += (treeOrder ? _xtree1.getDecendentsCount(nodes1[i]) : _xtree2.getDecendentsCount(nodes1[i])) + 1;
				if (distance >= threshold)
					return XTree.NO_CONNECTION;
			}
		}

		return distance;
	}

	/**
	  * Perform minimal-cost matching between two node lists #1
	  * Trivial part.
	  * @param	count1	length of node list #1
	  * @param	count2	length of node list #2
	  * @param	dist	distance matrix
	  * @param	matching1	matching list (for node list #1)
	  * @param	matching2	matching list (for node list #2)
	  * @return	distance
	  */
	private int findMatching(int count1, int count2, int dist[][],
				 int matching1[], int matching2[])
	{
		if (count1 == 1)
		{
			// count2 == 1
			if (dist[0][0] < XTree.NO_CONNECTION)
			{
				matching1[0] = 0;
				matching2[0] = 0;
			}
			else
			{
				matching1[0] = XTree.DELETE;
				matching2[0] = XTree.DELETE;
			}

			return dist[0][0];
		}
		else if (count2 == 1)
		{
			int     distance = 0;
			int     mate = 0;
			int     mindist = XTree.NO_CONNECTION;
			matching2[0] = XTree.DELETE;

			for (int i = 0; i < count1; i++)
			{
				matching1[i] = XTree.DELETE;
				if (mindist > dist[i][0])
				{
					mindist = dist[i][0];
					mate = i;
				}

				// Suppose we delete every node on list1.
				distance += dist[i][1];
			}

			if (mindist < XTree.NO_CONNECTION)
			{
				matching1[mate] = 0;
				matching2[0] = mate;
				distance += mindist - dist[mate][1];
			}
			else
			{
				// Add the delete cost of the single node
				// on list2.
				distance += dist[count1][0];
			}

			return distance;
		}
		else if ((count1 == 2) && (count2 == 2))
		{
			int	distance1 = dist[0][0] + dist[1][1];
			int	distance2 = dist[0][1] + dist[1][0];
			if (distance1 < distance2)
			{
				if (dist[0][0] < XTree.NO_CONNECTION)
				{
					matching1[0] = 0;
					matching2[0] = 0;
					distance1 = dist[0][0];
				}
				else
				{
					matching1[0] = XTree.DELETE;
					matching2[0] = XTree.DELETE;
					distance1 = dist[0][2] + dist[2][0];
				}

				if (dist[1][1] < XTree.NO_CONNECTION)
				{
					matching1[1] = 1;
					matching2[1] = 1;
					distance1 += dist[1][1];
				}
				else
				{
					matching1[1] = XTree.DELETE;
					matching2[1] = XTree.DELETE;
					distance1 += dist[1][2] + dist[2][1];
				}

				return distance1;
			}
			else
			{
				if (dist[0][1] < XTree.NO_CONNECTION)
				{
					matching1[0] = 1;
					matching2[1] = 0;
					distance2 = dist[0][1];
				}
				else
				{
					matching1[0] = XTree.DELETE;
					matching2[1] = XTree.DELETE;
					distance2 = dist[0][2] + dist[2][1];
				}

				if (dist[1][0] < XTree.NO_CONNECTION)
				{
					matching1[1] = 0;
					matching2[0] = 1;
					distance2 += dist[1][0];
				}
				else
				{
					matching1[1] = XTree.DELETE;
					matching2[0] = XTree.DELETE;
					distance2 += dist[1][2] + dist[2][0];
				}

				return distance2;
			}
		}
		else
		{
			return optimalMatching(count1, count2, dist,
					       matching1, matching2);
		}
	}

	/**
	  * Perform minimal-cost matching between two node lists
	  * @param	count1	length of node list #1
	  * @param	count2	length of node list #2
	  * @param	dist	distance matrix
	  * @param	matching1	matching list (for node list #1)
	  * @param	matching2	matching list (for node list #2)
	  * @return	distance
	  */
	private int optimalMatching(int count1, int count2, int dist[][],
				    int matching1[], int matching2[])
	{
		// Initialize matching. 
		// Initial guess will be pair-matching between two lists.
		// Others will be insertion or deletion
		for (int i = 0; i < count2; i++)
			matching1[i] = i;
		for (int i = count2; i < count1; i++)
			matching1[i] = XTree.DELETE;

		// Three artificial nodes: "start", "end" and "delete".
		int count = count1 + count2 + 3;

		// Initialize least cost matrix and path matrix.
		// Both have been initialized at the very beginning.

		// Start algorithm.
		while (true)
		{
			// Construct least cost matrix.
			constructLCM(dist, matching1, count1, count2);

			// Initialize path matrix.
			for (int i = 0; i < count; i++)
				for (int j = 0; j < count; j++)
					_pathMatrix[i][j] = i;

			// Search negative cost circuit.
			int	clen = searchNCC(count);
			if (clen > 0)
			{
				// Modify matching.
				for (int i = 0, next = 0; i < clen - 1; i++)
				{
					int	n1 = _circuit[next];
					next = _circuit[next+1];
					// Node in node list 1.
					if ((n1 > 0) && (n1 <= count1))
					{
						int	nid1 = n1 - 1;
						int	nid2 = _circuit[next] - count1 - 1;
						if (nid2 == count2)
							nid2 = XTree.DELETE;

						matching1[nid1] = nid2;
					}
				}
			}
			else // Stop.
				break;
		}

		int	distance = 0;
		// Suppose all insertion on list2
		for (int i = 0; i < count2; i++)
		{
			matching2[i] = XTree.INSERT;
			distance += dist[count1][i];
		}

		// update distance by looking at matching pairs.
		for (int i = 0; i < count1; i++)
		{
			int	mmm = matching1[i];
			if (mmm == XTree.DELETE)
				distance += dist[i][count2];
			else
			{
				matching2[mmm] = i;
				distance += dist[i][mmm] -
					    dist[count1][mmm];
			}
		}

		return distance;
	}

	/**
	  * Construct a least cost matrix (of the flow network) based on
	  * the cost matrix
	  * @param	costMatrix	cost matrix
	  * @param	matching	matching information
	  * @param	nodeCount1	# of nodes in node list 1
	  * @param	nodeCount2	# of nodes in node list 2
	  */
	private void constructLCM(int costMatrix[][], int matching[],
				  int nodeCount1, int nodeCount2)
	{
		// Three artificial nodes: "start", "end" and "delete".
		int nodeCount = nodeCount1 + nodeCount2 + 3;

		// Initialize.
		for (int i = 0; i < nodeCount; i++)
		{
			for (int j = 0; j < nodeCount; j++)
			_leastCostMatrix[i][j] = XTree.NO_CONNECTION;

			// self.
			_leastCostMatrix[i][i] = 0;
		}

		// Between start node and nodes in list 1.
		// Start -> node1 = Infinity; node1 -> Start = -0.
		for (int i = 0; i < nodeCount1; i++)
			_leastCostMatrix[i+1][0] = 0;

		// Between nodes in list2 and the end node.
		// Unless matched (later), node2 -> end = 0;
		// end -> node2 = Infinity.
		for (int i = 0; i < nodeCount2; i++)
			_leastCostMatrix[i+nodeCount1+1][nodeCount-1] = 0;

		int deleteCount = 0;

		// Between nodes in list1 and nodes in list2.
		// For matched, node1 -> node2 = Infinity;
		// node2 -> node1 = -1 * distance
		// For unmatched, node1 -> node2 = distance;
		// node2 -> node1 = Infinity
		for (int i = 0; i < nodeCount1; i++)
		{
			int node1 = i + 1;
			int node2;

			// According to cost matrix.
			for (int j = 0; j < nodeCount2; j++)
			{
				node2 = j + nodeCount1 + 1;
				_leastCostMatrix[node1][node2] = costMatrix[i][j];
			}

			// According to matching.
			if (matching[i] == XTree.DELETE)
			{
				deleteCount++;

				// node1 -> Delete = Infinity;
				// Delete -> node1 = -1 * DELETE_COST
				_leastCostMatrix[nodeCount-2][node1] = -1 * costMatrix[i][nodeCount2];
			}
			else
			{
				node2 = matching[i] + nodeCount1 + 1;

				// Between node1 and node2.
				_leastCostMatrix[node1][node2] = XTree.NO_CONNECTION;
				_leastCostMatrix[node2][node1] = costMatrix[i][matching[i]] * -1;

				// Between node1 and delete.
				_leastCostMatrix[node1][nodeCount-2] = costMatrix[i][nodeCount2];

				// Between node2 and end.
				_leastCostMatrix[node2][nodeCount-1] = XTree.NO_CONNECTION;
				_leastCostMatrix[nodeCount-1][node2] = costMatrix[nodeCount1][matching[i]];
			}
		}

		// Between the "Delete" and the "End".
		// If delete all, delete -> end = Infinity; end -> delete = 0.
		if (deleteCount == nodeCount1)
			_leastCostMatrix[nodeCount-1][nodeCount-2] = 0;
		// if no delete, delete -> end = 0; end -> delete = Infinity.
		else if (deleteCount == 0)
			_leastCostMatrix[nodeCount-2][nodeCount-1] = 0;
		// else, both 0;
		else
		{
			_leastCostMatrix[nodeCount-2][nodeCount-1] = 0;
			_leastCostMatrix[nodeCount-1][nodeCount-2] = 0;
		}
	}

	/**
	  * Search for negative cost circuit in the least cost matrix.
	  * @param	nodeCount	node count
	  * @return	the length of the path if found; otherwise 0
	  */
	private int searchNCC(int nodeCount)
	{
	  for (int k = 0; k < nodeCount; k++)
	  {
	    for (int i = 0; i < nodeCount; i++)
	    {
	      if ((i != k) && (_leastCostMatrix[i][k] != XTree.NO_CONNECTION))
	      {
		for (int j = 0; j < nodeCount; j++)
		{
		  if ((j != k) && (_leastCostMatrix[k][j] != XTree.NO_CONNECTION))
		  {
		    int	less = _leastCostMatrix[i][k] + _leastCostMatrix[k][j];
		    if (less < _leastCostMatrix[i][j])
		    {
		      _leastCostMatrix[i][j] = less;
		      _pathMatrix[i][j] = k;

		      // Found!
		      if ((i == j) && (less < 0))
		      {
			int	clen = 0; // the length of the circuit.

			// Locate the circuit.
			//circuit.addElement(new Integer(i));
			_circuit[0] = i;
			_circuit[1] = 2;

			//circuit.addElement(new Integer(pathMatrix[i][i]));
			_circuit[2] = _pathMatrix[i][i];
			_circuit[3] = 4;

			//circuit.addElement(new Integer(i));
			_circuit[4] = i;
			_circuit[5] = -1;

			clen = 3;

			boolean	finish;
			
			do
			{
			  finish = true;
			  for (int cit = 0, n = 0; cit < clen - 1; cit++)
			  {
			    int	left = _circuit[n];
			    int	next = _circuit[n+1];
			    int	right = (next == -1)?-1:_circuit[next];

			    //int middle = pathMatrix[circuit[n-1]][circuit[n]];
			    int	middle = _pathMatrix[left][right];

			    if (middle != left)
			    {
			      //circuit.insert( cit, middle );
			      _circuit[clen*2] = middle;
			      _circuit[clen*2+1] = next;
			      _circuit[n+1] = clen*2;
			      clen++;

			      finish = false;
			      break;
			    }
			    n = next;
			  }
			} while (!finish);

			return clen;
		      }
		    }
		  }
		}
	      }
	    }
	  }

	  return 0;
	}

	// For testing purpose -- print out matrixes
	private void printMatrix(int nodeCount)
	{
		System.out.println("Cost Matrix:");
		for (int i = 0; i < nodeCount; i++)
		{
			for (int j = 0; j < nodeCount; j++)
			{
				if (_leastCostMatrix[i][j] < XTree.NO_CONNECTION)
					System.out.print(_leastCostMatrix[i][j] + "\t");
				else
					System.out.print("\t");
			}
			System.out.println();
		}

		System.out.println("\nPath Matrix:");
		for (int i = 0; i < nodeCount; i++)
		{
			for (int j = 0; j < nodeCount - 1; j++)
				System.out.print(_pathMatrix[i][j] + "\t");
			System.out.println(_pathMatrix[i][nodeCount-1]);
		}
	}

	/**
	  * Write out the diff result -- how doc1 is changed to doc2
	  * @param	input		the first/old xml document
	  * @param	output		output file name
	  */
	private void writeDiff(String input, String output)
	{
		try
		{
			BufferedReader	br = new BufferedReader(new FileReader(input));
			FileOutputStream	fos =
				new FileOutputStream(output);
			OutputStreamWriter	out =
				new OutputStreamWriter(fos, _encoding);

			int	root1 = _xtree1.getRoot();
			int	root2 = _xtree2.getRoot();

			// XXX <root > is as valid as <root>,
			// but < root> is NOT!
			String	rootTag = "<" + _xtree1.getTag(root1);
			String	line = br.readLine();
			while (line != null)
			{
				if (line.indexOf(rootTag) >= 0)
					break;
				out.write(line + "\n");
				line = br.readLine();
			}

			_xtree1.getMatching(root1, _matchp);
			if (_matchp[0] == XTree.DELETE)
			{
				writeDeleteNode(out, root1);
				writeInsertNode(out, root2);
			}
			else
				writeDiffNode(out, root1, root2);

			out.close();
		}
		catch (IOException ioe)
		{
			System.err.println(ioe.getMessage());
		}
	}

	/**
	  * Write an element that has been deleted from the old document.
	  * @param	out	output file writer
	  * @param	node	element id
	  */
	private void writeDeleteNode(OutputStreamWriter out,
				     int node) throws IOException
	{
		if (_xtree1.isElement(node))
		{
			String	tag = _xtree1.getTag(node);
			out.write("<" + tag);

			// Attributes.
			int	attr = _xtree1.getFirstAttribute(node);
			while (attr > 0)
			{
				String	atag = _xtree1.getTag(attr);
				String	value = _xtree1.getAttributeValue(attr);
				out.write(" " + atag + "=\"" + value + "\"");
				attr = _xtree1.getNextAttribute(attr);
			}

			// Child nodes.
			int	child = _xtree1.getFirstChild(node);

			if (child < 0)
			{
				out.write("/><?DELETE " + tag + "?>\n");
				_needNewLine = false;
				return;
			}

			out.write("><?DELETE " + tag + "?>\n");
			_needNewLine = false;

			while (child > 0)
			{
				writeMatchNode(out, _xtree1, child);
				child = _xtree1.getNextSibling(child);
			}

			if (_needNewLine)
			{
				out.write("\n");
				_needNewLine = false;
			}

			out.write("</" + tag + ">\n");
		}
		else
		{
			out.write("<?DELETE \"" + constructText(_xtree1, node) +
				  "\"?>\n");
			_needNewLine = false;
		}
	}

	/**
	  * Write an element that has been inserted from the new document.
	  * @param	out	output file writer
	  * @param	node	element id
	  */
	private void writeInsertNode(OutputStreamWriter out,
				     int node) throws IOException
	{
		if (_xtree2.isElement(node))
		{
			String	tag = _xtree2.getTag(node);
			out.write("<" + tag);

			// Attributes.
			int	attr = _xtree2.getFirstAttribute(node);
			while (attr > 0)
			{
				String	atag = _xtree2.getTag(attr);
				String	value = _xtree2.getAttributeValue(attr);
				out.write(" " + atag + "=\"" + value + "\"");
				attr = _xtree2.getNextAttribute(attr);
			}

			// Child nodes.
			int	child = _xtree2.getFirstChild(node);
			if (child < 0)
			{
				out.write("/><?INSERT " + tag + "?>\n");
				_needNewLine = false;
				return;
			}

			out.write("><?INSERT " + tag + "?>\n");
			_needNewLine = false;

			while (child > 0)
			{
				writeMatchNode(out, _xtree2, child);
				child = _xtree2.getNextSibling(child);
			}

			if (_needNewLine)
			{
				out.write("\n");
				_needNewLine = false;
			}

			out.write("</" + tag + ">\n");
		}
		else
		{
			out.write(constructText(_xtree2, node) +
				  "<?INSERT?>\n");
			_needNewLine = false;
		}
	}

	/**
	  * Write an element that is unchanged or in a deleted node or in
	  * an inserted node.
	  * @param	out	output file writer
	  * @param	xtree	the document tree
	  * @param	node	element id
	  */
	private void writeMatchNode(OutputStreamWriter out, XTree xtree,
				    int node) throws IOException
	{
		if (xtree.isElement(node))
		{
			String	tag = xtree.getTag(node);
			if (_needNewLine)
				out.write("\n");

			out.write("<" + tag);

			// Attributes.
			int	attr = xtree.getFirstAttribute(node);
			while (attr > 0)
			{
				String	atag = xtree.getTag(attr);
				String	value = xtree.getAttributeValue(attr);
				out.write(" " + atag + "=\"" + value + "\"");
				attr = xtree.getNextAttribute(attr);
			}

			// Child nodes.
			int	child = xtree.getFirstChild(node);
			if (child < 0)
			{
				out.write("/>\n");
				_needNewLine = false;
				return;
			}

			out.write(">");
			_needNewLine = true;

			while (child > 0)
			{
				writeMatchNode(out, xtree, child);
				child = xtree.getNextSibling(child);
			}

			if (_needNewLine)
			{
				out.write("\n");
				_needNewLine = false;
			}

			out.write("</" + tag + ">\n");
		}
		else
		{
			out.write(constructText(xtree, node));
			_needNewLine = false;
		}
	}

	/**
	  * Write one node in the diff result.
	  * @param	out	output file writer
	  * @param	node1	the node in the first tree
	  * @param	node2	node1's conterpart in the second tree
	  */
	private void writeDiffNode(OutputStreamWriter out, int node1,
				   int node2) throws IOException
	{
		if (_xtree1.isElement(node1))
		{
			String	tag = _xtree1.getTag(node1);
			if (_needNewLine)
				out.write("\n");
			out.write("<" + tag);

			// Attributes.
			int	attr1 = _xtree1.getFirstAttribute(node1);
			String	diffff = "";
			while (attr1 > 0)
			{
				String	atag = _xtree1.getTag(attr1);
				String	value = _xtree1.getAttributeValue(attr1);
				_xtree1.getMatching(attr1, _matchp);
				if (_matchp[0] == XTree.MATCH)
					out.write(" " + atag + "=\"" +
						  value + "\"");
				else if (_matchp[0] == XTree.DELETE)
				{
					out.write(" " + atag + "=\"" +
						  value + "\"");
					diffff += "<?DELETE " + atag + "?>";
				}
				else
				{
					String	value2 = _xtree2.getAttributeValue(_matchp[1]);
					out.write(" " + atag + "=\"" +
						  value2 + "\"");
					diffff += "<?UPDATE " + atag +
						  " FROM \"" + value + "\"?>";
				}

				attr1 = _xtree1.getNextAttribute(attr1);
			}

			int	attr2 = _xtree2.getFirstAttribute(node2);
			while (attr2 > 0)
			{
				_xtree2.getMatching(attr2, _matchp);
				if (_matchp[0] == XTree.INSERT)
				{
					String	atag = _xtree2.getTag(attr2);
					String	value = _xtree2.getAttributeValue(attr2);
					out.write(" " + atag + "=\"" +
						  value + "\"");
					diffff += "<?INSERT " + atag + "?>";
				}

				attr2 = _xtree2.getNextAttribute(attr2);
			}

			// Child nodes.
			int	child1 = _xtree1.getFirstChild(node1);
			if (child1 < 0)
			{
				out.write("/>" + diffff + "\n");
				_needNewLine = false;
				return;
			}

			out.write(">" + diffff);
			_needNewLine = true;

			while (child1 > 0)
			{
				_xtree1.getMatching(child1, _matchp);
				if (_matchp[0] == XTree.MATCH)
					writeMatchNode(out, _xtree1, child1);
				else if (_matchp[0] == XTree.DELETE)
					writeDeleteNode(out, child1);
				else
					writeDiffNode(out, child1, _matchp[1]);

				child1 = _xtree1.getNextSibling(child1);
			}

			int	child2 = _xtree2.getFirstChild(node2);
			while (child2 > 0)
			{
				_xtree2.getMatching(child2, _matchp);
				if (_matchp[0] == XTree.INSERT)
					writeInsertNode(out, child2);

				child2 = _xtree2.getNextSibling(child2);
			}

			if (_needNewLine)
			{
				out.write("\n");
				_needNewLine = false;
			}

			out.write("</" + tag + ">\n");
		}
		else
		{
			out.write(constructText(_xtree2, node2) +
				  "<?UPDATE FROM \"" +
				  constructText(_xtree1, node1) + "\"?>");
			_needNewLine = false;
		}
	}

	/**
	  * Construct the text node -- to handle the possible CDATA sections.
	  */
	private String constructText(XTree xtree, int eid)
	{
		String	text = xtree.getText(eid);
		Vector	cdatalist = xtree.getCDATA(eid);
		if (cdatalist == null)
			return text;

		StringBuffer	buf = new StringBuffer();
		int		count = cdatalist.size();
		int		lastEnd = 0;
		for (int i = 0; i < count; i += 2)
		{
			int	cdataStart =
				((Integer)cdatalist.elementAt(i)).intValue();
			int	cdataEnd =
				((Integer)cdatalist.elementAt(i+1)).intValue();

			if (cdataStart > lastEnd)
				buf.append(text.substring(lastEnd, cdataStart));
			buf.append("<![CDATA[" +
				   text.substring(cdataStart, cdataEnd) +
				   "]]>");
			lastEnd = cdataEnd;
		}
		if (lastEnd < text.length())
			buf.append(text.substring(lastEnd));

		return buf.toString();
	}

	public static void main(String args[])
	{
		Vector	parameters = new Vector();
		if (!readParameters(args, parameters))
		{
			System.err.println(_usage);
			return;
		}

		XDiff	mydiff = new XDiff((String)parameters.elementAt(0),
					   (String)parameters.elementAt(1),
					   (String)parameters.elementAt(2));
	}

	private static boolean readParameters(String args[], Vector parameters)
	{
		int	opid = 0;
		if (args.length < 3)
			return false;
		else if (args[0].equals("-o"))
		{
			_oFlag = true;
			opid++;
		}
		else if (args[0].equals("-g"))
		{
			_gFlag = true;
			opid++;
		}

		if (args[opid].equals("-p"))
		{
			opid++;
			double	p = 0;
			try
			{
				p = Double.valueOf(args[opid++]).doubleValue();
			}
			catch (NumberFormatException nfe)
			{
				return false;
			}

			if ((p <= 0) || (p > 1))
				return false;
			_NO_MATCH_THRESHOLD = p;
		}

		if (args[opid].equals("-e"))
		{
			opid++;
			_encoding = args[opid++];
		}

		if ((args.length - opid) != 3)
			return false;
		parameters.add(args[opid++]);
		parameters.add(args[opid++]);
		parameters.add(args[opid]);

		return true;
	}
}
