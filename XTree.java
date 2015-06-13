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


import java.util.Hashtable;
import java.util.Vector;

/**
  * <code>XTree</code> provides a DOM-like interface but somehow simplified;
  * Ideally, it can be replaced by any other DOM parser output tree structures.
  */
class XTree {

	public static final int	MATCH = 0;
	public static final int	CHANGE = 1;
	public static final int	NO_MATCH = -1;
	public static final int	INSERT = -1;
	public static final int	DELETE = -1;
	public static final int	NULL_NODE = -1;
	public static final int NO_CONNECTION = 1048576;

	private static int	_TOP_LEVEL_CAPACITY = 16384;
	private static int	_BOT_LEVEL_CAPACITY = 4096;
	private static int	_root = 0;

	private int		_topCap, _botCap;
	private int		_elementIndex, _tagIndex, _valueCount;
	private	int		_firstChild[][], _nextSibling[][];
	private int		_childrenCount[][], _valueIndex[][];
	private boolean		_isAttribute[][];
	private int		_matching[][];
	private long		_hashValue[][];
	private String		_value[][];
	private Hashtable	_tagNames, _cdataTable;

	/**
	  * Default constructor
	  */
	XTree()
	{
		_topCap = _TOP_LEVEL_CAPACITY;
		_botCap = _BOT_LEVEL_CAPACITY;
		_initialize();
	}

	/**
	  * Constructor that allows users to modify settings.
	  */
	XTree(int topcap, int botcap)
	{
		_topCap = topcap;
		_botCap = botcap;
		_initialize();
	}

	// Initialization.
	private void _initialize()
	{
		_firstChild	= new int[_topCap][];
		_nextSibling	= new int[_topCap][];
		_isAttribute	= new boolean[_topCap][];
		_valueIndex	= new int[_topCap][];
		_matching	= new int[_topCap][];
		_childrenCount	= new int[_topCap][];
		_hashValue	= new long[_topCap][];
		_value		= new String[_topCap][];

		_value[0]	= new String[_botCap];
		_tagNames	= new Hashtable(_botCap);

		// This hashtable is used to record CDATA section info.
		// The key is the text node id, the value is the list of 
		// (start,end) position pair of each CDATA section.
		_cdataTable	= new Hashtable(_botCap);

		_elementIndex	= -1;
		_tagIndex	= -1;
		_valueCount	= _botCap - 1;
	}

	/**
	  * ID Expansion
	  */
	private void _expand(int topid)
	{
		_firstChild[topid]	= new int[_botCap];
		_nextSibling[topid]	= new int[_botCap];
		_childrenCount[topid]	= new int[_botCap];
		_matching[topid]	= new int[_botCap];
		_valueIndex[topid]	= new int[_botCap];
		_hashValue[topid]	= new long[_botCap];
		_isAttribute[topid]	= new boolean[_botCap];

		for (int i = 0; i < _botCap; i++)
		{
			_firstChild[topid][i]	= NULL_NODE;
			_nextSibling[topid][i]	= NULL_NODE;
			_childrenCount[topid][i]= 0;
			_matching[topid][i]	= MATCH;
			_valueIndex[topid][i]	= -1;
			_isAttribute[topid][i]	= false;
		}
	}

	// Start  -- methods for constructing a tree.
	/**
	  * Add a new element to the tree.
	  * @param	pid		parent id
	  * @param	lsid		left-side sibling id
	  * @param	tagName		element name
	  * @return	the element id in the tree.
	  */
	public int addElement(int pid, int lsid, String tagName)
	{
		_elementIndex++;

		int	topid = _elementIndex / _botCap;
		int	botid = _elementIndex % _botCap;
		if (botid == 0)
			_expand(topid);

		// Check if we've already had the tag
		Integer	tagID = (Integer)_tagNames.get(tagName);
		if (tagID != null)
			_valueIndex[topid][botid] = tagID.intValue();
		else
		{
			_tagIndex++;
			tagID = new Integer(_tagIndex);
			_value[0][_tagIndex] = tagName;
			_tagNames.put(tagName, tagID);
			_valueIndex[topid][botid] = _tagIndex;
		}

		if (pid == NULL_NODE)
			return _elementIndex;

		int	ptopid = pid / _botCap;
		int	pbotid = pid % _botCap;
		// parent-child relation or sibling-sibling relation
		if (lsid == NULL_NODE)
			_firstChild[ptopid][pbotid] = _elementIndex;
		else
			_nextSibling[lsid/_botCap][lsid%_botCap] = _elementIndex;

		// update children count
		_childrenCount[ptopid][pbotid]++;

		return _elementIndex;
	}

	/**
	  * Add a text node.
	  * @param	eid	element id
	  * @param	lsid	the sibling id on the left
	  * @param	text	text value
	  * @param	value	hash value
	  */
	public int addText(int eid, int lsid, String text, long value)
	{
		_elementIndex++;
		int	topid = _elementIndex / _botCap;
		int	botid = _elementIndex % _botCap;
		if (botid == 0)
			_expand(topid);

		int	etopid = eid / _botCap;
		int	ebotid = eid % _botCap;
		if (lsid == NULL_NODE)
			_firstChild[etopid][ebotid] = _elementIndex;
		else
			_nextSibling[lsid/_botCap][lsid%_botCap] = _elementIndex;

		_childrenCount[etopid][ebotid]++;
		_hashValue[topid][botid] = value;

		_valueCount++;
		int	vtopid = _valueCount / _botCap;
		int	vbotid = _valueCount % _botCap;
		if (vbotid == 0)
			_value[vtopid] = new String[_botCap];

		_value[vtopid][vbotid] = text;
		_valueIndex[topid][botid] = _valueCount;

		return _elementIndex;
	}

	/**
	  * Add an attribute.
	  * @param	eid	element id
	  * @param	lsid	the sibling id on the left
	  * @param	name	attribute name
	  * @param	value	attribute value
	  * @param	valuehash	hash value of the value
	  * @param	attrhash	hash value of the entire attribute
	  * @return	the element id of the attribute
	  */
	public int addAttribute(int eid, int lsid, String name, String value,
				long valuehash, long attrhash)
	{
		// attribute name first.
		int	aid = addElement(eid, lsid, name);

		// attribute value second.
		addText(aid, NULL_NODE, value, valuehash);

		// hash value third
		int	atopid = aid / _botCap;
		int	abotid = aid % _botCap;
		_isAttribute[atopid][abotid] = true;
		_hashValue[atopid][abotid] = attrhash;

		return aid;
	}

	/**
	  * Add more information (hash value) to an element node.
	  * @param	eid	element id
	  * @param	value	extra hash value
	  */
	public void addHashValue(int eid, long value)
	{
		_hashValue[eid/_botCap][eid%_botCap] = value;
	}

	/**
	  * Add a CDATA section (either a start or an end) to the CDATA
	  * hashtable, in which each entry should have an even number of
	  * position slots.
	  * @param	eid		The text node id
	  * @param	position	the section tag position
	  */
	public void addCDATA(int eid, int position)
	{
		Integer	key = new Integer(eid);
		Object	value = _cdataTable.get(key);
		if (value == null)
		{
			Vector	list = new Vector(2);
			list.addElement(new Integer(position));
			_cdataTable.put(key, list);
		}
		else
		{
			Vector	list = (Vector)value;
			list.addElement(new Integer(position));
			_cdataTable.put(key, list);
		}
	}

	/**
	  * Add matching information.
	  * @param	eid	element id
	  * @param	match	?match and matched element id
	  */
	public void addMatching(int eid, int[] match)
	{
		if (match[0] == NO_MATCH)
			_matching[eid/_botCap][eid%_botCap] = NO_MATCH;
		else if (match[0] == MATCH)
			_matching[eid/_botCap][eid%_botCap] = MATCH;
		else
			_matching[eid/_botCap][eid%_botCap] = match[1] + 1;
	}

	// End  -- methods for constructing a tree.

	// Start -- methods for accessing a tree.

	/**
	  * Get matching information.
	  * @param	eid	element id
	  * @param	match	?change and matched element id 
	  */
	public void getMatching(int eid, int[] match)
	{
		int	mid = _matching[eid/_botCap][eid%_botCap];
		if (mid == NO_MATCH)
			match[0] = NO_MATCH;
		else if (mid == MATCH)
			match[0] = MATCH;
		else
		{
			match[0] = CHANGE;
			match[1] = mid - 1;
		}
	}

	/**
	  * Get the root element id.
	  */
	public int getRoot()
	{
		return _root;
	}

	/**
	  * Get the first child of a node.
	  * @param	eid	element id
	  */
	public int getFirstChild(int eid)
	{
		int	cid = _firstChild[eid/_botCap][eid%_botCap];
		while (cid > _root)
		{
			int	ctopid = cid / _botCap;
			int	cbotid = cid % _botCap;
			if (_isAttribute[ctopid][cbotid])
				cid = _nextSibling[ctopid][cbotid];
			else
				return cid;
		}

		return NULL_NODE;
	}

	/**
	  * Get the next sibling of a node.
	  * @param	eid	element id
	  */
	public int getNextSibling(int eid)
	{
		return _nextSibling[eid/_botCap][eid%_botCap];
	}

	/**
	  * Get the first attribute of a node.
	  * @param	eid	element id
	  */
	public int getFirstAttribute(int eid)
	{
		int	aid = _firstChild[eid/_botCap][eid%_botCap];
		if ((aid > _root) && (_isAttribute[aid/_botCap][aid%_botCap]))
			return aid;
		else
			return NULL_NODE;
	}

	/**
	  * Get the next attribute of a node.
	  * @param	aid	attribute id
	  */
	public int getNextAttribute(int aid)
	{
		int	aid1 = _nextSibling[aid/_botCap][aid%_botCap];
		if ((aid1 > _root) && (_isAttribute[aid1/_botCap][aid1%_botCap]))
			return aid1;
		else
			return NULL_NODE;
	}

	/**
	  * Get the attribute value.
	  * @param	aid	attribute id
	  */
	public String getAttributeValue(int aid)
	{
		int	cid = _firstChild[aid/_botCap][aid%_botCap];
		int	index = _valueIndex[cid/_botCap][cid%_botCap];
		if (index > 0)
			return _value[index/_botCap][index%_botCap];
		else
			return "";
	}

	/**
	  * Get the hash value of a node.
	  * @param	eid	element id
	  */
	public long getHashValue(int eid)
	{
		return _hashValue[eid/_botCap][eid%_botCap];
	}

	/**
	  * Get the CDATA section position list of a text node.
	  * @param	eid	element id
	  * @return	position list which is a vector or null if no CDATA
	  */
	public Vector getCDATA(int eid)
	{
		return (Vector)_cdataTable.get(new Integer(eid));
	}

	/**
	  * Get the childern count of a node.
	  * @param	eid	element id
	  */
	public int getChildrenCount(int eid)
	{
		return _childrenCount[eid/_botCap][eid%_botCap];
	}

	/**
	  * Get the # of all decendents of a node.
	  * @param	eid	element id
	  */
	public int getDecendentsCount(int eid)
	{
		int	topid = eid / _botCap;
		int	botid = eid % _botCap;
		int	count = _childrenCount[topid][botid];
		if (count == 0)
			return 0;

		int	cid = _firstChild[topid][botid];
		while (cid > NULL_NODE)
		{
			count += getDecendentsCount(cid);
			cid = _nextSibling[cid/_botCap][cid%_botCap];
		}

		return count;
	}

	/**
	  * Get the value index of a node
	  * @param	eid	element id
	  */
	public int getValueIndex(int eid)
	{
		return _valueIndex[eid/_botCap][eid%_botCap];
	}

	/**
	  * Get the value of a leaf node
	  * @param	index	value index
	  */
	public String getValue(int index)
	{
		return _value[index/_botCap][index%_botCap];
	}

	/**
	  * Get the tag of an element node
	  * @param	eid	element id
	  */
	public String getTag(int eid)
	{
		int	index = _valueIndex[eid/_botCap][eid%_botCap];
		return	_value[0][index];
	}

	/**
	  * Get the text value of a leaf node
	  * @param	eid	element id
	  */
	public String getText(int eid)
	{
		int	index = _valueIndex[eid/_botCap][eid%_botCap];
		if (index >= _botCap)
			return _value[index/_botCap][index%_botCap];
		else
			return "";
	}

	/**
	  * Check if a node an element node.
	  * @param	eid	element id
	  */
	public boolean isElement(int eid)
	{
		int	vindex = _valueIndex[eid/_botCap][eid%_botCap];
		if (vindex < _botCap)
			return true;
		else
			return false;
	}

	/**
	  * Check if a node is an attribute node.
	  * @param	eid	element id
	  */
	public boolean isAttribute(int eid)
	{
		return _isAttribute[eid/_botCap][eid%_botCap];
	}

	/**
	  * Check if a node an leaf text node.
	  * @param	edi	element id
	  */
	public boolean isLeaf(int eid)
	{
		int	index = _valueIndex[eid/_botCap][eid%_botCap];
		if (index < _botCap)
			return false;
		else
			return true;
	}

	// End  -- methods for accessing a tree.

	/**
	  * For testing purpose.
	  */
	public void dump()
	{
		System.out.println("eid\tfirstC\tnextS\tattr?\tcCount\thash\tmatch\tvalue\n");
		for (int i = _root; i <= _elementIndex; i++)
		{
			int	topid = i / _botCap;
			int	botid = i % _botCap;
			int	vid = _valueIndex[topid][botid];
			int	vtopid = vid / _botCap;
			int	vbotid = vid % _botCap;
			System.out.println(i + "\t" +
					   _firstChild[topid][botid] + "\t" +
					   _nextSibling[topid][botid] + "\t" +
					   _isAttribute[topid][botid] + "\t" +
					   _childrenCount[topid][botid] + "\t" +
					   _hashValue[topid][botid] + "\t" +
					   _matching[topid][botid] + "\t" +
					   _value[vtopid][vbotid]);
		}
	}
	public void dump(int eid)
	{
		int	topid = eid / _botCap;
		int	botid = eid % _botCap;
		int	vid = _valueIndex[topid][botid];
		int	vtopid = vid / _botCap;
		int	vbotid = vid % _botCap;
		System.out.println(eid + "\t" +
				   _firstChild[topid][botid] + "\t" +
				   _nextSibling[topid][botid] + "\t" +
				   _isAttribute[topid][botid] + "\t" +
				   _childrenCount[topid][botid] + "\t" +
				   _hashValue[topid][botid] + "\t" +
				   _matching[topid][botid] + "\t" +
				   _value[vtopid][vbotid]);
	}
}
