JFLAGS = -g
JC = javac
JARFILE = DNSLookupService.jar
SRC = $(shell find src -iname '*.java')
all: $(JARFILE)

.SUFFIXES: .java .class
bin/%.class: $(SRC)
	mkdir -p bin/
	$(JC) -sourcepath src -d bin/ $(JFLAGS) src/$*.java

$(JARFILE): bin/DNSLookupService.class
	jar cvfe $(JARFILE) DNSLookupService -C bin ca/

run: $(JARFILE)
	java -jar $(JARFILE) 199.7.83.42

clean:
	-rm -rf  $(JARFILE) bin/*
