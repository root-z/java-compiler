JC = javac
JFLAGS = -g -d ${classpath}


SUBDIR = exceptions scanner parser weeder ast environment static_analysis utility code_generation joosc
SOURCES = Joosc.java

ifndef classpath
export classpath = ${PWD}/../class
endif

.SUFFIXES: .java .class
.PHONY: all clean ${SUBDIR}

default: all

all: ${SUBDIR} 
#${SOURCES:.java=.class}

${SUBDIR}:
	${JC} ${JFLAGS} $@/*.java

.java.class:
	${JC} ${JFLAGS} $*.java

clean:
	find . -type f -name '*.class' -delete
