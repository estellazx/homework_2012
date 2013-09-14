Parser.class: Parser.java
	javac *java

Parser.java: Parser.jj 
	javacc Parser.jj

clean:
	rm *.class ParseException.java Parser.java ParserConstants.java ParserTokenManager.java SimpleCharStream.java Token.java TokenMgrError.java

