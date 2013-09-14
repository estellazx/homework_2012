import java.io.*;
import java.util.*;

/*
 * Superclass of all the visitors
 */
public abstract class Visitor {
	public void visit(Node node, int depth) {
		if (node instanceof Lambda) {
			visitLambda((Lambda) node, depth + 1);
		}
		else if (node instanceof Application) {
			visitApplication((Application) node, depth + 1);
		}
		else if (node instanceof Addition) {
			visitAddition((Addition) node, depth + 1);
		}
		else if (node instanceof Expression) {
			visitExpression((Expression) node, depth + 1);
		}
		else if (node instanceof If)  {
			visitIf((If) node, depth + 1);
		}
		else {
			throw new IllegalArgumentException("Illegal argument!!!");
		}
	}

	public abstract void visitLambda(Lambda node, int depth);

	public abstract void visitApplication(Application node, int depth);

	public abstract void visitAddition(Addition node, int depth);

	public abstract void visitIf(If node, int depth);

	public abstract void visitExpression(Expression node, int depth);

	protected void printIndent(int length) {
		for (int i = 0; i < length; i++) {
			System.out.print("  ");
		}
	}
}

/*
 * AstVisitor would visit the AST nodes and print them out.
 */
class AstVisitor extends Visitor {
	public AstVisitor(Node root) {
		System.out.println("* AST Printer Visitising..");
		System.out.println();
		visit(root, 0); // depth from 0
		System.out.println();
	}

	public void visitLambda(Lambda lnode, int depth) {
		printIndent(depth);
		System.out.println("lambda");
		printIndent(depth + 1);
		System.out.println(lnode.getId()); // indented
		visit(lnode.getExpression(), depth);
	}

	public void visitApplication(Application anode, int depth) {
		printIndent(depth);
		System.out.println("app");
		visit(anode.getLambda(), depth);
		visit(anode.getExpression(), depth);
	}

	public void visitAddition(Addition adnode, int depth) {
		printIndent(depth);
		System.out.println("+");
		for (Expression operand : adnode.getOperands()) {
			visit(operand, depth);
		}
	}

	public void visitIf(If ifnode, int depth) {
		printIndent(depth);
		System.out.println("if");
		visit(ifnode.getCond(), depth);
		visit(ifnode.getBaseCase(), depth);
		visit(ifnode.getRecCase(), depth);
	}

	public void visitExpression(Expression enode, int depth) {
		Object value = enode.getContent();

		if (value instanceof Integer || value instanceof String) {
			printIndent(depth);
			System.out.println(value);
		}
		else if (value instanceof ArrayList) {
			printIndent(depth);
			System.out.println(value);
		}
		else {
			visit((Node)value, depth - 1); // not going deeper
		}
	}
}

/*
 * InterpVisitor would visit the AST nodes and return the result and environment.
 */
class InterpVisitor extends Visitor {
	public void visit(Node node, int depth) {
		if (! Parser.noEnvPrint) {
			if (node.getEnvironment() != null && (node instanceof Application || node instanceof Addition || node instanceof Lambda)) {
				printIndent(depth);
				System.out.print(node.getClass() + ": ");
				node.getEnvironment().print();
				System.out.println();
			}
		}
		super.visit(node, depth);
	}

	public InterpVisitor(Node root, ArrayList<ASub> subs) {
		System.out.println("* Interpreter Visitising..");
		System.out.println();

		Environment env = new PreloadedEnvironment(subs);
		root.setEnvironment(env);

		if (subs != null) {
			for (ASub sub : subs) {
				sub.getLambda().setEnvironment(env);
				visit(sub.getLambda(), 0);
			}
		}

		visit(root,0);

		System.out.println();
		System.out.print("Result: ");
		System.out.println(root.getValue());
		System.out.println();
	}
	
	public void visitLambda(Lambda lnode, int depth) {
		lnode.setValue(lnode);

		lnode.getExpression().setEnvironment(lnode.getEnvironment());
	}

	public void visitApplication(Application anode, int depth) {
		ASub aSub;

		anode.getLambda().setEnvironment(anode.getEnvironment());
		visit(anode.getLambda(), depth);

		anode.getExpression().setEnvironment(anode.getEnvironment());
		visit(anode.getExpression(), depth);

		if ( ! (anode.getLambda().getValue() instanceof Lambda)) {
			// (f 3) while f = 1 in environment.
			throw new RuntimeException("Illegal to apply Integer " + anode.getLambda().getValue());
		}

		// get the Lambda object from the left child - can be converted from ID, Application
		Lambda leftLambda = (Lambda)(anode.getLambda().getValue());

		if (anode.getExpression() instanceof Expression) {
			// construct simple ASub
			Object value = anode.getExpression().getValue();

			if (value instanceof Integer) {
				aSub = new ASub(leftLambda.getId(), (Integer)(value));
			}
			else if (value instanceof ArrayList) {
				aSub = new ASub(leftLambda.getId(), (ArrayList)(value));
			}
			else if (value instanceof Lambda) {
				aSub = new ASub(leftLambda.getId(), (Lambda)value, anode.getEnvironment());
			}
			else {
				throw new RuntimeException("Found " + value.getClass() + " in Expression value.");
			}
		}
		else {
			// construct ASub with closure
			aSub = new ASub(leftLambda.getId(), (Lambda)(anode.getExpression().getValue()), anode.getEnvironment());
		}

		// add the sub at this level to the evironment
		Environment envTmp;
		if (Parser.staticScoping) {
			envTmp = new Environment(leftLambda.getEnvironment());
		}
		else {
			envTmp = new Environment(anode.getEnvironment());
		}
		envTmp.append(aSub);
		leftLambda.getExpression().setEnvironment(envTmp);

		// for recursive function definition, add the function definition itself to its environment
		if (leftLambda.isRec() || anode.isRec()) {
			anode.getExpression().setEnvironment(envTmp);
		}

		visit(leftLambda.getExpression(), depth);
		anode.setValue(leftLambda.getExpression().getValue());
	}

	public void visitIf(If ifNode, int depth)  {
		ifNode.getCond().setEnvironment(new Environment(ifNode.getEnvironment()));
		ifNode.getBaseCase().setEnvironment(new Environment(ifNode.getEnvironment()));
		ifNode.getRecCase().setEnvironment(new Environment(ifNode.getEnvironment()));

		visit(ifNode.getCond(), depth);
		boolean condition = (ifNode.getIfZero() && ((Integer)(ifNode.getCond().getValue()) == 0))
										||	(!ifNode.getIfZero() && (((ArrayList)ifNode.getCond().getValue()).size() == 0));

		if (condition) {
			visit(ifNode.getBaseCase(), depth);
			ifNode.setValue(ifNode.getBaseCase().getValue());
		}
		else {
			visit(ifNode.getRecCase(), depth);
			ifNode.setValue(ifNode.getRecCase().getValue());
		}
	}

	public void visitAddition(Addition adnode, int depth) {
		int sum = 0;
		for (Expression e : adnode.getOperands())  {
			e.setEnvironment(new Environment(adnode.getEnvironment()));
			visit(e, depth);

			if (e.getValue() instanceof Lambda) {
				throw new RuntimeException("Illegal to add lambda expression.");
			}

			sum += (Integer)(e.getValue());
		}

		adnode.setValue(sum);
	}

	public void visitExpression(Expression enode, int depth) {
		String action = enode.getAction();

		// expression body for preloaded functions
		if (action != null) {
			if (action == "car") {
				ArrayList list = (ArrayList)(enode.getEnvironment().lookup("list"));
				enode.setValue(list.get(0));
			}
			else if (action == "cdr") {
				ArrayList<Expression> list = (ArrayList)(enode.getEnvironment().lookup("list"));
				ArrayList<Expression> newList = (ArrayList)(list.clone());
				newList.remove(0);
				enode.setValue(newList);
			}
			else if (action == "cons") {
				Object elem = enode.getEnvironment().lookup("elem");
				ArrayList list = (ArrayList)(enode.getEnvironment().lookup("list"));

				ArrayList newList = (ArrayList)(list.clone());
				newList.add(0, elem);
				enode.setValue(newList);
			}

			return;
		}

		Object value = enode.getContent();

		if (value instanceof Integer ) {
			enode.setValue((Integer)value);
		}
		else if (value instanceof String) {
			// evaluate variable
			enode.setValue(enode.getEnvironment().lookup((String)value));
		}
		else if (value instanceof ArrayList) {
			ArrayList valueList = new ArrayList();
			for (Expression e : (ArrayList<Expression>)value) {
				visit(e, depth);
				valueList.add(e.getValue());
			}
			enode.setValue(valueList);
		}
		else {
			Node valueNode = (Node)value;
			valueNode.setEnvironment(new Environment(enode.getEnvironment()));
			visit(valueNode, depth);
			enode.setValue(valueNode.getValue());
		}
	}
}
