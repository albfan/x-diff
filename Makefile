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
#  make package:     builds all
#                    create a 'xdiff.jar' file, and clean class files
#             
#  make clean:       removes class files
#
##############################################################################

#JAVAC_ARGS=-Xlint:unchecked

JAVAC = $(JDK_HOME)/bin/javac $(JAVAC_ARGS)
JAVA = $(JDK_HOME)/bin/java
JAR = $(JDK_HOME)/bin/jar

SOURCES = XHash.java XTree.java XParser.java XLut.java XDiff.java
CLASSES = $(SOURCES:.java=.class)
DEST_CLASSES = $(patsubst %,$(DEST_DIR)/%,$(subst :, ,$(SOURCES:.java=.class)))
JARFILE = xdiff.jar

DEST_DIR = build

xdiff:	$(DEST_CLASSES)

$(DEST_DIR)/%.class: %.java
	mkdir -p $(DEST_DIR)
	$(JAVAC) -d $(DEST_DIR) $<

package: 
	$(JAR) cf $(DEST_DIR)/$(JARFILE) -C $(DEST_DIR) . ; \
	rm -f $(DEST_CLASSES)

clean:
	rm -rf $(DEST_DIR)

