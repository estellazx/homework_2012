Formatter.class: Formatter.java
	javac *java
	jar cmf mainClass Formatter.jar *class
	jar tf Formatter.jar

Formatter.java: Formatter.jj 
	javacc Formatter.jj

clean:
	rm *.class Formatter.java FormatterConstants.java FormatterTokenManager.java ParseException.java SimpleCharStream.java Token.java TokenMgrError.java

