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


import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.ext.LexicalHandler;

/**
  * <code>XParser</code> parses an input XML document and constructs an
  * <code>XTree</code>
  */
class XParser extends DefaultHandler implements LexicalHandler
{
	private static final String	_PARSER_NAME = "org.apache.xerces.parsers.SAXParser";
	private static boolean	_setValidation = false;
	private static boolean	_setNameSpaces = true;
	private static boolean	_setSchemaSupport = true;
	private static boolean	_setSchemaFullSupport = false;
 	private static boolean  _setNameSpacePrefixes = true;

	private static int	_STACK_SIZE = 100;

	private XMLReader	_parser;
	private XTree		_xtree;
	private int		_idStack[], _lsidStack[]; // id and left sibling
	private long		_valueStack[];
	private int		_stackTop, _currentNodeID;
	private boolean		_readElement;
	private StringBuffer	_elementBuffer;

	/**
	  * Constructor.
	  */
	public XParser()
	{
		XHash.initialize();
		try
		{
			_parser = (XMLReader)Class.forName(_PARSER_NAME).newInstance();
			_parser.setFeature("http://xml.org/sax/features/validation", _setValidation);
			_parser.setFeature("http://xml.org/sax/features/namespaces", _setNameSpaces);
			_parser.setFeature("http://apache.org/xml/features/validation/schema", _setSchemaSupport);
			_parser.setFeature("http://apache.org/xml/features/validation/schema-full-checking", _setSchemaFullSupport);
 			_parser.setFeature("http://xml.org/sax/features/namespace-prefixes", _setNameSpacePrefixes);

			_parser.setContentHandler(this);
			_parser.setErrorHandler(this);
			_parser.setProperty("http://xml.org/sax/properties/lexical-handler", this);
		}
		catch (Exception e)
		{
			System.err.println(e.getMessage());
			System.exit(1);
		}

		_idStack = new int[_STACK_SIZE];
		_lsidStack = new int[_STACK_SIZE];
		_valueStack = new long[_STACK_SIZE];
		_stackTop = 0;
		_currentNodeID = XTree.NULL_NODE;
		_elementBuffer = new StringBuffer();
	}

	/**
	  * Parse an XML document
	  * @param	uri	input XML document
	  * @return	the created XTree
	  */
	public XTree parse(String uri)
	{
		_xtree = new XTree();
		_idStack[_stackTop] = XTree.NULL_NODE;
		_lsidStack[_stackTop] = XTree.NULL_NODE;

		try
		{
			_parser.parse(uri);
		}
		catch (Exception e)
		{
			System.err.println(e.getMessage());
			System.exit(1);
		}

		return _xtree;
	}

	// Document handler methods

	public void startElement(String uri, String local, String raw,
				 Attributes attrs)
	{
		// if text is mixed with elements
		if (_elementBuffer.length() > 0)
		{
			String	text = _elementBuffer.toString().trim();
			if (text.length() > 0)
			{
				long	value = XHash.hash(text);
				int	tid = _xtree.addText(_idStack[_stackTop], _lsidStack[_stackTop], text, value);
				_lsidStack[_stackTop] = tid;
				_currentNodeID = tid;
				_valueStack[_stackTop] += value;
			}
		}

		int	eid = _xtree.addElement(_idStack[_stackTop],
						_lsidStack[_stackTop], local);

		// Update last sibling info.
		_lsidStack[_stackTop] = eid;

		// Push
		_stackTop++;
		_idStack[_stackTop] = eid;
		_currentNodeID = eid;
		_lsidStack[_stackTop] = XTree.NULL_NODE;
		_valueStack[_stackTop] = XHash.hash(local);

		// Take care of attributes
		if ((attrs != null) && (attrs.getLength() > 0))
		{
			for (int i = 0; i < attrs.getLength(); i++)
			{
				String	name = attrs.getQName(i);
				String	value = attrs.getValue(i);
				long	namehash = XHash.hash(name);
				long	valuehash = XHash.hash(value);
				long	attrhash = namehash * namehash +
						   valuehash * valuehash;
				int	aid = _xtree.addAttribute(eid, _lsidStack[_stackTop], name, value, namehash, attrhash);

				_lsidStack[_stackTop] = aid;
				_currentNodeID = aid + 1;
				_valueStack[_stackTop] += attrhash * attrhash;
			}
		}

		_readElement = true;
		_elementBuffer = new StringBuffer();
	}

	public void characters(char ch[], int start, int length)
	{
		_elementBuffer.append(ch, start, length);
	}

	public void endElement(String uri, String local, String raw)
	{
		if (_readElement)
		{
			if (_elementBuffer.length() > 0)
			{
				String	text = _elementBuffer.toString();
				long	value = XHash.hash(text);
				_currentNodeID =
					_xtree.addText(_idStack[_stackTop],
						       _lsidStack[_stackTop],
						       text, value);
				_valueStack[_stackTop] += value;
			}
			else	// an empty element
			{
				_currentNodeID =
					_xtree.addText(_idStack[_stackTop],
						       _lsidStack[_stackTop],
						       "", 0);
			}
			_readElement = false;
		}
		else
		{
			if (_elementBuffer.length() > 0)
			{
				String	text = _elementBuffer.toString().trim();
				// More text nodes before end of the element.
				if (text.length() > 0)
				{
					long	value = XHash.hash(text);
					_currentNodeID =
					  _xtree.addText(_idStack[_stackTop],
							 _lsidStack[_stackTop],
							 text, value);
					_valueStack[_stackTop] += value;
				}
			}
		}

		_elementBuffer = new StringBuffer();
		_xtree.addHashValue(_idStack[_stackTop],
				    _valueStack[_stackTop]);
		_valueStack[_stackTop-1] += _valueStack[_stackTop] *
					    _valueStack[_stackTop];
		_lsidStack[_stackTop-1] = _idStack[_stackTop];

		// Pop
		_stackTop--;
	}

	// End of document handler methods

	// Lexical handler methods.

	public void startCDATA()
	{
		// The text node id should be the one next to the current
		// node id.
		int	textid = _currentNodeID + 1;
		String	text = _elementBuffer.toString();
		_xtree.addCDATA(textid, text.length());
	}

	public void endCDATA()
	{
		int	textid = _currentNodeID + 1;
		String	text = _elementBuffer.toString();
		_xtree.addCDATA(textid, text.length());
	}

	// Following functions are not implemented.
	public void comment(char[] ch, int start, int length)
	{
	}

	public void startDTD(String name, String publicId, String systemId)
	{
	}

	public void endDTD()
	{
	}

	public void startEntity(String name)
	{
	}

	public void endEntity(String name)
	{
	}

	// End of lexical handler methods.
}
