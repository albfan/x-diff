#
# Copyright (c) 2001 - 2005
# 	Yuan Wang. All rights reserved.
#
# Redistribution and use in source and binary forms, with or without
# modification, are permitted provided that the following conditions
# are met:
# 1. Redistributions of source code must retain the above copyright notice,
# this list of conditions and the following disclaimer.
# 2. Redistributions in binary form must reproduce the above copyright
# notice, this list of conditions and the following disclaimer in the 
# documentation and/or other materials provided with the distribution.
# 3. Redistributions in any form must be accompanied by information on
# how to obtain complete source code for the X-Diff software and any
# accompanying software that uses the X-Diff software.  The source code
# must either be included in the distribution or be available for no 
# more than the cost of distribution plus a nominal fee, and must be
# freely redistributable under reasonable conditions.  For an executable
# file, complete source code means the source code for all modules it
# contains.  It does not include source code for modules or files that 
# typically accompany the major components of the operating system on
# which the executable file runs.
#
# THIS SOFTWARE IS PROVIDED BY YUAN WANG "AS IS" AND ANY EXPRESS OR IMPLIED
# WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF 
# MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, OR NON-INFRINGEMENT,
# ARE DISCLAIMED.  IN NO EVENT SHALL YUAN WANG BE LIABLE FOR ANY DIRECT,
# INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
# (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR 
# SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
# HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, 
# STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING 
# IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
# POSSIBILITY OF SUCH DAMAGE.
#
#

##############################################################################
#
#  Makefile to build X-Diff
#
#  make:             builds all
#  make install:     builds all
#                    create a 'xdiff.jar' file, and clean class files
#             
#  make clean:       removes class files
#
##############################################################################

#export JAVA_HOME   = 
#export JAVAC	= $(JAVA_HOME)javac
#export JAVA	= $(JAVA_HOME)java
#export JAR	= $(JAVA_HOME)jar

export JAVAC	= javac
export JAVA	= java
export JAR	= jar

export SOURCES	= XHash.java XTree.java XParser.java XLut.java XDiff.java
export CLASSES	= $(SOURCES:.java=.class)
export JARFILE	= xdiff.jar

xdiff:	$(CLASSES)

%.class: %.java
	$(JAVAC) $<

install: 
	$(JAR) cf $(JARFILE) $(CLASSES) ; \
	rm -f $(CLASSES)

clean:
	rm -f $(CLASSES) $(JARFILE)

