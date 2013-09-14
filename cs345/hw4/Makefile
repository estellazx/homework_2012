Parser.class: Parser.java
	javac Parser.java
	rm *.java
	cat Input | java Parser

Parser.java: Parser.jj 
	javacc Parser.jj
