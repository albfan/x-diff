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


/**
  * <code>XHash</code> is an implementaion of DES
  */
class XHash
{
	private static final int	_initialPermutation[] =
	{
 		57, 49, 41, 33 , 25, 17, 9, 1, 59, 51, 43, 35, 27, 19, 11, 3,
 		61, 53, 45, 37, 29, 21, 13, 5, 63, 55, 47, 39, 31, 23, 15, 7,
 		56, 48, 40, 32 ,24, 16, 8, 0, 58, 50, 42, 34, 26, 18, 10, 2,
 		60, 52, 44, 36, 28, 20, 12, 4, 62, 54, 46, 38, 30, 22, 14, 6
	};

	private static final int	_finalPermutation[] =
	{
		39, 7, 47, 15, 55, 23, 63, 31, 38, 6, 46, 14, 54, 22, 62, 30,
		37, 5, 45, 13, 53, 21, 61, 29, 36, 4, 44, 12, 52, 20, 60, 28,
		35, 3, 43, 11, 51, 19, 59, 27, 34, 2, 42, 10, 50, 18, 58, 26,
		33, 1, 41, 9,  49, 17, 57, 25, 32, 0, 40,  8, 48, 16, 56, 24
	};

	private static final int	_keyReducePermutation[] =
	{
		60, 52, 44, 36, 59, 51, 43, 35, 27, 19, 11, 3, 58, 50,
		42, 34, 26, 18, 10, 2, 57, 49, 41, 33, 25, 17, 9, 1,
		28, 20, 12, 4, 61, 53, 45, 37, 29, 21, 13, 5, 62, 54,
		46, 38, 30, 22, 14, 6, 63, 55, 47, 39, 31, 23, 15, 7
	};
 
	private static final int	_keyCompressPermutation[] =
	{
		24, 27, 20, 6, 14, 10, 3, 22, 0, 17, 7, 12,
		8, 23,  11, 5, 16, 26, 1, 9, 19, 25, 4, 15,
		54, 43 ,36, 29, 49, 40, 48, 30, 52, 44, 37, 33,
		46, 35, 50, 41, 28, 53, 51, 55, 32, 45, 39, 42
	};

	private static final int	_keyRot[] =
	{
		1, 2, 4, 6, 8, 10, 12, 14, 15, 17, 19, 21, 23, 25, 27, 28
	};

 	private static final int	_sBoxP[][] =
	{
		{
			0x00808200, 0x00000000, 0x00008000, 0x00808202,
			0x00808002, 0x00008202, 0x00000002, 0x00008000,
			0x00000200, 0x00808200, 0x00808202, 0x00000200,
			0x00800202, 0x00808002, 0x00800000, 0x00000002,
			0x00000202, 0x00800200, 0x00800200, 0x00008200,
			0x00008200, 0x00808000, 0x00808000, 0x00800202,
			0x00008002, 0x00800002, 0x00800002, 0x00008002,
			0x00000000, 0x00000202, 0x00008202, 0x00800000,
			0x00008000, 0x00808202, 0x00000002, 0x00808000,
			0x00808200, 0x00800000, 0x00800000, 0x00000200,
			0x00808002, 0x00008000, 0x00008200, 0x00800002,
			0x00000200, 0x00000002, 0x00800202, 0x00008202,
			0x00808202, 0x00008002, 0x00808000, 0x00800202,
			0x00800002, 0x00000202, 0x00008202, 0x00808200,
			0x00000202, 0x00800200, 0x00800200, 0x00000000,
			0x00008002, 0x00008200, 0x00000000, 0x00808002
		},
		{
			0x40084010, 0x40004000, 0x00004000, 0x00084010,
			0x00080000, 0x00000010, 0x40080010, 0x40004010,
			0x40000010, 0x40084010, 0x40084000, 0x40000000,
			0x40004000, 0x00080000, 0x00000010, 0x40080010,
			0x00084000, 0x00080010, 0x40004010, 0x00000000,
			0x40000000, 0x00004000, 0x00084010, 0x40080000,
			0x00080010, 0x40000010, 0x00000000, 0x00084000,
			0x00004010, 0x40084000, 0x40080000, 0x00004010,
			0x00000000, 0x00084010, 0x40080010, 0x00080000,
			0x40004010, 0x40080000, 0x40084000, 0x00004000,
			0x40080000, 0x40004000, 0x00000010, 0x40084010,
			0x00084010, 0x00000010, 0x00004000, 0x40000000,
			0x00004010, 0x40084000, 0x00080000, 0x40000010,
			0x00080010, 0x40004010, 0x40000010, 0x00080010,
			0x00084000, 0x00000000, 0x40004000, 0x00004010,
			0x40000000, 0x40080010, 0x40084010, 0x00084000
		},
		{
			0x00000104, 0x04010100, 0x00000000, 0x04010004,
			0x04000100, 0x00000000, 0x00010104, 0x04000100,
			0x00010004, 0x04000004, 0x04000004, 0x00010000,
			0x04010104, 0x00010004, 0x04010000, 0x00000104,
			0x04000000, 0x00000004, 0x04010100, 0x00000100,
			0x00010100, 0x04010000, 0x04010004, 0x00010104,
			0x04000104, 0x00010100, 0x00010000, 0x04000104,
			0x00000004, 0x04010104, 0x00000100, 0x04000000,
			0x04010100, 0x04000000, 0x00010004, 0x00000104,
			0x00010000, 0x04010100, 0x04000100, 0x00000000,
			0x00000100, 0x00010004, 0x04010104, 0x04000100,
			0x04000004, 0x00000100, 0x00000000, 0x04010004,
			0x04000104, 0x00010000, 0x04000000, 0x04010104,
			0x00000004, 0x00010104, 0x00010100, 0x04000004,
			0x04010000, 0x04000104, 0x00000104, 0x04010000,
			0x00010104, 0x00000004, 0x04010004, 0x00010100
		},
		{
			0x80401000, 0x80001040, 0x80001040, 0x00000040,
			0x00401040, 0x80400040, 0x80400000, 0x80001000,
			0x00000000, 0x00401000, 0x00401000, 0x80401040,
			0x80000040, 0x00000000, 0x00400040, 0x80400000,
			0x80000000, 0x00001000, 0x00400000, 0x80401000,
			0x00000040, 0x00400000, 0x80001000, 0x00001040,
			0x80400040, 0x80000000, 0x00001040, 0x00400040,
			0x00001000, 0x00401040, 0x80401040, 0x80000040,
			0x00400040, 0x80400000, 0x00401000, 0x80401040,
			0x80000040, 0x00000000, 0x00000000, 0x00401000,
			0x00001040, 0x00400040, 0x80400040, 0x80000000,
			0x80401000, 0x80001040, 0x80001040, 0x00000040,
			0x80401040, 0x80000040, 0x80000000, 0x00001000,
			0x80400000, 0x80001000, 0x00401040, 0x80400040,
			0x80001000, 0x00001040, 0x00400000, 0x80401000,
			0x00000040, 0x00400000, 0x00001000, 0x00401040
		},
		{
			0x00000080, 0x01040080, 0x01040000, 0x21000080,
			0x00040000, 0x00000080, 0x20000000, 0x01040000,
			0x20040080, 0x00040000, 0x01000080, 0x20040080,
			0x21000080, 0x21040000, 0x00040080, 0x20000000,
			0x01000000, 0x20040000, 0x20040000, 0x00000000,
			0x20000080, 0x21040080, 0x21040080, 0x01000080,
			0x21040000, 0x20000080, 0x00000000, 0x21000000,
			0x01040080, 0x01000000, 0x21000000, 0x00040080,
			0x00040000, 0x21000080, 0x00000080, 0x01000000,
			0x20000000, 0x01040000, 0x21000080, 0x20040080,
			0x01000080, 0x20000000, 0x21040000, 0x01040080,
			0x20040080, 0x00000080, 0x01000000, 0x21040000,
			0x21040080, 0x00040080, 0x21000000, 0x21040080,
			0x01040000, 0x00000000, 0x20040000, 0x21000000,
			0x00040080, 0x01000080, 0x20000080, 0x00040000,
			0x00000000, 0x20040000, 0x01040080, 0x20000080
		},
		{
			0x10000008, 0x10200000, 0x00002000, 0x10202008,
			0x10200000, 0x00000008, 0x10202008, 0x00200000,
			0x10002000, 0x00202008, 0x00200000, 0x10000008,
			0x00200008, 0x10002000, 0x10000000, 0x00002008,
			0x00000000, 0x00200008, 0x10002008, 0x00002000,
			0x00202000, 0x10002008, 0x00000008, 0x10200008,
			0x10200008, 0x00000000, 0x00202008, 0x10202000,
			0x00002008, 0x00202000, 0x10202000, 0x10000000,
			0x10002000, 0x00000008, 0x10200008, 0x00202000,
			0x10202008, 0x00200000, 0x00002008, 0x10000008,
			0x00200000, 0x10002000, 0x10000000, 0x00002008,
			0x10000008, 0x10202008, 0x00202000, 0x10200000,
			0x00202008, 0x10202000, 0x00000000, 0x10200008,
			0x00000008, 0x00002000, 0x10200000, 0x00202008,
			0x00002000, 0x00200008, 0x10002008, 0x00000000,
			0x10202000, 0x10000000, 0x00200008, 0x10002008
		},
		{
			0x00100000, 0x02100001, 0x02000401, 0x00000000,
			0x00000400, 0x02000401, 0x00100401, 0x02100400,
			0x02100401, 0x00100000, 0x00000000, 0x02000001,
			0x00000001, 0x02000000, 0x02100001, 0x00000401,
			0x02000400, 0x00100401, 0x00100001, 0x02000400,
			0x02000001, 0x02100000, 0x02100400, 0x00100001,
			0x02100000, 0x00000400, 0x00000401, 0x02100401,
			0x00100400, 0x00000001, 0x02000000, 0x00100400,
			0x02000000, 0x00100400, 0x00100000, 0x02000401,
			0x02000401, 0x02100001, 0x02100001, 0x00000001,
			0x00100001, 0x02000000, 0x02000400, 0x00100000,
			0x02100400, 0x00000401, 0x00100401, 0x02100400,
			0x00000401, 0x02000001, 0x02100401, 0x02100000,
			0x00100400, 0x00000000, 0x00000001, 0x02100401,
			0x00000000, 0x00100401, 0x02100000, 0x00000400,
			0x02000001, 0x02000400, 0x00000400, 0x00100001
		},
		{
			0x08000820, 0x00000800, 0x00020000, 0x08020820,
			0x08000000, 0x08000820, 0x00000020, 0x08000000,
			0x00020020, 0x08020000, 0x08020820, 0x00020800,
			0x08020800, 0x00020820, 0x00000800, 0x00000020,
			0x08020000, 0x08000020, 0x08000800, 0x00000820,
			0x00020800, 0x00020020, 0x08020020, 0x08020800,
			0x00000820, 0x00000000, 0x00000000, 0x08020020,
			0x08000020, 0x08000800, 0x00020820, 0x00020000,
			0x00020820, 0x00020000, 0x08020800, 0x00000800,
			0x00000020, 0x08020020, 0x00000800, 0x00020820,
			0x08000800, 0x00000020, 0x08000020, 0x08020000,
			0x08020020, 0x08000000, 0x00020000, 0x08000820,
			0x00000000, 0x08020820, 0x00020020, 0x08000020,
			0x08020000, 0x08000800, 0x08000820, 0x00000000,
			0x08020820, 0x00020800, 0x00020800, 0x00000820,
			0x00000820, 0x00020020, 0x08000000, 0x08020800
		}
	};

	private static long	_keys[];
	private static char	_word[];

	private static long	_initialKey = 1007360890380L;

	/**
	  * Initialization #1.
	  */
	public static void initialize()
	{
		makeKeys(_initialKey);
		_word = new char[64];
	}

	/**
	  * Initialization #2.
	  */
	public static void initialize(long key)
	{
		makeKeys(key);
		_word = new char[64];
	}
	
	public static long hash(String word)
	{
		int	len = word.length();
		long	value = 0L;
		for (int start = 0; start < len; start += 64)
		{
			if (len - start > 64)
				value += (_hash(word.substring(start, start + 64)) ^ 0xffffffffL);
			else
			{
				value += (_hash(word.substring(start)) ^ 0xffffffffL);
				break;
			}
		}
		return value;
	}

	/**
	  * The actual hash function.
	  */
	private static long _hash(String word)
	{
		int	len = word.length();
		for (int i = 0; i < len; i++)
			_word[i] = word.charAt(i);
		int	round = len / 8;
		int	rest = len % 8;
		if (rest > 0)
		{
			for (int i = 0; i < 8 - rest; i++)
				_word[len+i] = 0;
			round++;
		}

		int	value = 0;
		for (int i = 0, pos = 0; i < round; i++, pos += 8)
		{
			long	todo = 0L + (byte)_word[pos];
			for (int j = 1; j < 8; j++)
				todo = (todo << 8) + (byte)_word[pos+j];
			value += des(todo);
		}

		return value;
	}

	private static void makeKeys(long key)
	{
		long reduced = permutate(key, _keyReducePermutation);
		int l = (int)(reduced >> 28);
		int r = (int)(reduced & 0xfffffff);
		_keys = new long[16];
		for (int i = 0; i < 16; i++)
			_keys[i] = permutate(rotate(l, r, _keyRot[i]),
					    _keyCompressPermutation);
	}

	private static long des(long w)
	{
		long	x = permutate(w, _initialPermutation);
		int	l = (int)(x >>> 32);
		int	r = (int)x;
		for (int i = 0; i < 16; i++)
		{
			int	tmp = desFunc(r, _keys[i]) ^ l;
			l = r;
			r = tmp;
		}
		long	y = ((long)r << 32) | ((long)l & 0xffffffffL);
		return permutate(y, _finalPermutation);
	}

	private static long permutate(long k, int p[])
	{
		long	s = 0;
		for (int i = 0; i < p.length; i++)
		{
			if ((k & (1L << p[i])) != 0)
				s |= 1L << i;
		}

		return s;
	}

	private static long rotate(int l, int r, int s)
	{
		return ((long)(((l<<s) & 0xfffffff) | (l>>>(28 - s))) << 28) |
			((r<<s) & 0xfffffff) | (r>> (28 - s));
	}

	private static int desFunc(int x, long k)
	{
		int p = x >>> 27;
		int q = (p & 3) << 4;
		int r = x << 5;
		p |=  r;
		r = _sBoxP[0][(int)((k >> 42) ^ p) & 0x3f];
		p >>>= 4;
		r |= _sBoxP[7][(int)((k >> 0) ^ p) & 0x3f];
		p >>>= 4;
		r |= _sBoxP[6][(int)((k >> 6) ^ p) & 0x3f];
		p >>>= 4;
		r |= _sBoxP[5][(int)((k >> 12) ^ p) & 0x3f];
		p >>>= 4;
		r |= _sBoxP[4][(int)((k >> 18) ^ p) & 0x3f];
		p >>>= 4;
		r |= _sBoxP[3][(int)((k >> 24) ^ p) & 0x3f];
		p >>>= 4;
		r |= _sBoxP[2][(int)((k >> 30) ^ p) & 0x3f];
		p >>>= 4;
		r |= _sBoxP[1][(int)((k >> 36) ^ (p | q)) & 0x3f];
		return r;
	}
}
