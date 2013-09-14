import java.io.*;
import java.util.*;

// (aSub x 3 (aSub x 4 (mtSub)))
// -> {{String:'x', 3}, {String:'y', 4}}
public class Environment {
	protected ArrayList<ASub> subs;
	
	public Environment() {
		this.subs = new ArrayList<ASub>();
	}

	public Environment(Environment e) {
		this.subs = new ArrayList<ASub>();
		if (e != null && e.copySubs() != null)  {
			for (ASub sb : e.copySubs() )  {
				this.subs.add(new ASub(sb));
			}
		}
	}

	public void append(ASub sub) {
		subs.add(sub);
	}
	
	public void append(ArrayList subList) {
		subs.addAll(subList);
	}

	public Object lookup(String id)  {
		for (int i = subs.size()-1 ; i >= 0 ; i--)  {
			if (subs.get(i).getToken().equals(id))  {
				Object value = subs.get(i).getValue();
				if (value instanceof Lambda) {
					// make a copy of that lambda
					return new Lambda((Lambda)value);
				}
				else {
					return value;
				}
			}
		}
		throw new RuntimeException("Can't find the key named " + id + " in Env! If this should be a preloaded function, please use -p.");
	}

	// make a copy of subs
	public ArrayList<ASub> copySubs() {
		return (ArrayList<ASub>)(subs.clone());
	}

	public void print() {
		printhelp(subs.size()-1 ,subs);
		System.out.println(); 
	}

	private void printhelp(int index, List<ASub> subs )  {
		if (index == -1)  {
			System.out.print("(mtSub)");
		}
		else {
			if (subs.get(index).isClosure()) {
				System.out.print("(aSub " + subs.get(index).getToken() + " (closureV " + subs.get(index).getLambda().getId() + " <#procedure> ");
				if (subs.get(index).getSubs() != null) {
					printhelp(subs.get(index).getSubs().size() - 1, subs.get(index).getSubs());
				}
				System.out.print(" ) ");
			}
			else {
				System.out.print("(aSub " + subs.get(index).getToken() + " " + subs.get(index).getValue() + " ");
			}

			printhelp(index - 1, subs);
			System.out.print(")");
		}
	}
}

class ASub {
	private String token;
	private Integer value;
	private ArrayList list;
	private Lambda lambda;
	private ArrayList<ASub> subs;
	private boolean isClosure;

	// like (aSub x 3)
	public ASub(String token, int v) {
		this.token = token;
		this.value = v;
		this.isClosure = false;
	}

	public ASub(String token, ArrayList l) {
		this.token = token;
		this.list = l;
		this.isClosure = false;
	}

	// like (aSub f (lambda (x) (+ x y)) (aSub x ))
	public ASub(String token, Lambda l, Environment e) {
		this.token = token;
		this.lambda = l;
		if (e != null) {
			this.subs = e.copySubs();
		}
		this.isClosure = true;
	}

	public ASub(String token, Lambda l) {
		// no environment
		this(token, l, null);
	}

	public ASub(ASub sb)  {
		if (sb.isClosure())  {
			this.token = sb.getToken();
			this.lambda = sb.getLambda();
			if (sb.getSubs() != null)
				this.subs = new ArrayList<ASub>(sb.getSubs());
			this.isClosure = sb.isClosure();
		}
		else if (sb.getValue() instanceof Integer) {
			this.value = (Integer)(sb.getValue());
			this.token = sb.getToken();
			this.isClosure = sb.isClosure();
		}
		else if (sb.getValue() instanceof ArrayList)  {
			this.token = sb.getToken();
			this.isClosure = sb.isClosure();
			this.list = (ArrayList)(sb.getValue());
		}
	}

	public String getToken() {
		return token;
	}

	public Object getValue() {
		if (isClosure) {
			return lambda;
		}
		else if (value != null) {
			return value;
		}
		else {
			return list;
		}
	}

	public ArrayList<ASub> getSubs()  {
		return this.subs;
	}

	public Lambda getLambda() {
		return this.lambda;
	}

	public boolean isClosure() {
		return this.isClosure;
	}
}

class PreloadedEnvironment extends Environment {
	public PreloadedEnvironment(ArrayList<ASub> subs) {
		this.subs = new ArrayList<ASub>();

		if (Parser.preload) {
			this.subs.addAll(subs);
			listFunc();
			//combinator();
		}
	}
		
	private void listFunc() {
		// add car, cdr, cons
		Expression carBody = new Expression();
		carBody.setAction("car");
		this.subs.add(new ASub("car" , new Lambda("list", carBody)));

		Expression cdrBody = new Expression();
		cdrBody.setAction("cdr");
		this.subs.add(new ASub("cdr" , new Lambda("list", cdrBody)));

		Expression consBody = new Expression();
		consBody.setAction("cons");
		this.subs.add(new ASub("cons" , new Lambda("elem", new Lambda("list", consBody))));
	}

	private void combinator() {
		Expression f = new Expression("f"), x = new Expression("x"), g = new Expression("g"), y = new Expression("y");

		// s
		this.subs.add(new ASub("s", new Lambda("f", new Lambda("g", new Lambda("x", new Application(new Application(f, x), new Application(g, x)))))));

		// k
		this.subs.add(new ASub("k", new Lambda("x", new Lambda("y", x))));

		// b
		this.subs.add(new ASub("b", new Lambda("f", new Lambda("g", new Lambda("x", new Application(f, new Application(g, x)))))));
		
		// c
		this.subs.add(new ASub("c", new Lambda("f", new Lambda("g", new Lambda("x", new Application(new Application(f, x), g))))));

		// y
		this.subs.add(new ASub("y", new Lambda("f", new Application(f, new Application(y, f)))));
	}
}
