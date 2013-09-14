import java.io.*;
import java.util.*;

public abstract class Node {
	// evaluated as integer or lambda by interpreter.
	private Object value;
	protected Environment env;

	public Object getValue()  {
		return value;
	}

	public void setValue(Object v)  {
		if (!(v instanceof Integer || v instanceof ArrayList || v instanceof Lambda)) {
			throw new IllegalArgumentException("Invalid value type of Node! Observe: " + v.getClass());
		}
		this.value = v;
	}

	public void setEnvironment(Environment env) {
		this.env = env;
	}

	public Environment getEnvironment() {
		return this.env;
	}

	protected Node copy(Node node) {
		if (node instanceof Lambda) {
			return new Lambda((Lambda)node);
		}
		else if (node instanceof Addition) {
			return new Addition((Addition)node);
		}
		else if (node instanceof Application) {
			return new Application((Application)node);
		}
		else if (node instanceof Expression) {
			return new Expression((Expression)node);
		}
		else if (node instanceof If) {
			return new If((If)node);
		}
		else {
			throw new RuntimeException("Unknown node to copy!");
		}
	}

	protected void copyEnvironment(Node node) {
		this.env = new Environment(node.getEnvironment());
	}
}

class Lambda extends Node {
	private String id;
	private Node exp;
	private boolean rec = false; // whether it's a recursive function definition.

	Lambda(String id, Node exp) {
		this.exp = exp;
		this.id = id;
	}

	Lambda(Lambda l) {
		this.id = l.getId();
		this.exp = copy(l.getExpression());

		copyEnvironment(l);
	}

	public void setRec() {
		this.rec = true;
	}

	public boolean isRec() {
		return this.rec;
	}

	public String getId() {
		return id;
	}

	public Node getExpression() {
		return exp;
	}
}

class If extends Node {
	private Expression cond, baseCase, recCase;
	private boolean ifZero;
	
	public If(Expression cond, Expression baseCase, Expression recCase, boolean ifZero) {
		this.cond = cond;
		this.baseCase = baseCase;
		this.recCase = recCase;
		this.ifZero = ifZero;
	}

	public If(If i) {
		this.cond = new Expression(i.getCond());
		this.baseCase = new Expression(i.getBaseCase());
		this.recCase = new Expression(i.getRecCase());
		this.ifZero = i.getIfZero();
		copyEnvironment(i);
	}

	public Expression getCond()  {
		return this.cond;
	}
	
	public Expression getBaseCase()  {
		return this.baseCase;
	}
	
	public Expression getRecCase()  {
		return this.recCase;
	}

	public boolean getIfZero() {
		return this.ifZero;
	}
}

class Application extends Node {
	private Node lambda, exp;
	private boolean rec = false;

	Application(Node lambda, Node exp) {
		this.lambda = lambda;
		this.exp = exp;
	}

	Application(Application app) {
		this.lambda = copy(app.getLambda());
		this.exp = copy(app.getExpression());
		
		copyEnvironment(app);
	}

	public void setRec() {
		this.rec = true;
	}

	public boolean isRec() {
		return this.rec;
	}

	public Node getLambda() {
		return lambda;
	}

	public Node getExpression() {
		return exp;
	}
}

class Addition extends Node {
	private List<Expression> operands;

	Addition(List<Expression> exps) {
		operands = exps;
	}

	Addition(Addition add) {
		this.operands = new ArrayList<Expression>();

		for (Expression exp : add.getOperands()) {
			this.operands.add(new Expression(exp));
		}

		copyEnvironment(add);
	}

	public List<Expression> getOperands() {
		return operands;
	}
}

class Expression extends Node {
	private Object content;
	private String action; // "car"

	Expression() {
	}

	Expression(Expression exp) {
		this.action = exp.getAction();
		if (this.action != null) {
			return;
		}

		Object c = exp.getContent();
		if (c instanceof Application || c instanceof Addition || c instanceof If) {
			// needs deep copy
			this.content = copy((Node)c);
		}
		else {
			this.content = c;
		}

		copyEnvironment(exp);
	}

	Expression(Application app) {
		this.content = app;
	}

	Expression(Addition add) {
		this.content = add;
	}

	Expression(int num) {
		this.content = (Integer)num;
	}

	Expression(String id) {
		this.content = id;
	}

	Expression(If i) {
		this.content = i;
	}

	Expression( ArrayList<Expression> list )
	{
		this.content = list;
	}
	
	public void setAction(String act) {
		if (act != "car" && act != "cdr" && act != "cons") {
			throw new IllegalArgumentException("Unknown action in Expression.");
		}

		this.action = act;
	}

	public String getAction() {
		return this.action;
	}

	/**
	 * Get literal value.
	 */
	public Object getContent() {
		if (this.content == null) {
			throw new IllegalArgumentException("Content undefined for Expression.");
		}
		else {
			return this.content;
		}
	}
}
