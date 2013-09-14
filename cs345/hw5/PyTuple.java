/*
Name : Shun Zhang
EID  : sz4554
*/
// Copyright (c) Corporation for National Research Initiatives
package org.python.core;

//added these imports
//import com.sun.org.apache.bcel.internal.generic.Select;

import java.io.StringReader;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.*;
import net.sf.jsqlparser.statement.select.*;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.update.Update;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.drop.Drop;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.ExpressionVisitor;

import java.sql.ResultSet;
import java.sql.DatabaseMetaData;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import oracle.jdbc.OracleConnection;
import oracle.jdbc.OracleResultSet;
import oracle.jdbc.pool.OracleDataSource;

import java.math.BigDecimal;

import java.sql.Statement;

import java.util.*;
//end imports
import java.math.BigInteger;
//prolog
import alice.tuprolog.*;

import java.io.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import java.lang.reflect.Array;
import java.lang.*;

import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedType;
import org.python.expose.MethodType;

import org.python.util.SQLVisitor;

/**
 * A builtin python tuple.
 */
@ExposedType(name = "tuple", base = PyObject.class, doc = BuiltinDocs.tuple_doc)
public class PyTuple extends PySequenceList implements List {

    public static final PyType TYPE = PyType.fromClass(PyTuple.class);

    // private final PyObject[] array;
    private PyObject[] array = null;

    private volatile List<PyObject> cachedList = null;

    private static final PyTuple EMPTY_TUPLE = new PyTuple();

    private static int guid = 1;
    private static int linenumber = 1;
    private static String website = "www.example.org/";
    //Boolean to know if user going to make asp facts
    private static boolean aspFlag = false;
    //Boolean to convert a select statement to an rdf database
    private static boolean rdfFlag = false;
    //Boolean to make RDF database
    private static boolean makeRDF = false;
    private static boolean remote = false;
    private static boolean verbose = false;
		//initialized and used when SPARQL is called
		private static SPARQLDoer sDoer;

    public PyTuple() {
        this(TYPE, Py.EmptyObjects);
    }

    public PyTuple(PyObject... elements) {
        this(TYPE, elements);
    }

    public PyTuple(PyType subtype, PyObject[] elements) {
        super(subtype);
        if (elements == null) {
            array = new PyObject[0];
        } else {
            array = new PyObject[elements.length];
            System.arraycopy(elements, 0, array, 0, elements.length);
        }
    }

    public PyTuple(PyObject[] elements, boolean copy) {
        this(TYPE, elements, copy);
    }
    //Replace Grounder and Solver to point to your respective executables
    //Examples
    //Linux:
    //public String grounder = "/u/fgarcia/Desktop/newJython/gringo-3.0.4-x86-linux/gringo";
    //public String solver = "/u/fgarcia/Desktop/newJython/clasp-2.0.5/clasp-2.0.5-st-x86-linux";
    private static boolean asp_select = false;
    private static String fact_name = "";
    private static String fact_type = "";
    private static boolean prolog_select = false;

    //Windows Example:
    public String grounder = "";
    public String solver = "";

    public PyTuple(PyObject[] elements, String asp_stmt, String appendStr, String asporprolog,
                   String path) throws Exception {
        super(TYPE);
        if (asp_stmt.contains("ASP_PATH")) {
            String clasp = "";
            String identifier = "linux";
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                identifier = "win";
            }
            String[] splitPath = path.split(";");
            if (asp_stmt.contains("SOLVER")) {
                grounder = splitPath[0] + "/gringo";
                path = splitPath[1];
            } else {
                grounder = splitPath[0] + "/gringo";
                path = splitPath[0];
            }
            File dir = new File(path);
            for (File file : dir.listFiles()) {
                String temp = file.getName();
                if (temp.toLowerCase().contains("clasp") && temp.toLowerCase().contains(identifier)) {
                    clasp = temp;
                    break;
                }
            }
            solver = path + "/" + clasp.replace(".exe", "");

            //System.out.println(grounder);
            //System.out.println(solver);
            FileWriter settings = new FileWriter("aspsettings");
            settings.write(grounder + "\n");
            settings.write(solver);
            settings.close();
        }
        //System.out.println(grounder);
        //System.out.println(solver);

        String[] as_attr = asp_stmt.split(";");
        String as_stmt = "";
        Boolean append = Boolean.valueOf(appendStr);
        int elementcount = 0;
        for (int i = 1; i < as_attr.length; i++) {
            if (as_attr[i].equals("@ASPREPLACESTRINGHERE@")) {
                if (elements[elementcount].getType().pyGetName().toString() == "str") {
                    as_stmt += "\'" + elements[elementcount].toString() + "\'";
                } else {
                    as_stmt += "" + elements[elementcount].toString() + "";
                }
                elementcount++;
            } else if (as_attr[i].contains("!@&@!")) {
                as_stmt += as_attr[i].replace("!@&@!", ";");
            } else {
                as_stmt += as_attr[i];
            }
        }
        if (as_attr[0].equals("?-") || as_attr[0].equals("?-.")) {
            Scanner scan = new Scanner(new File("aspsettings"));
            if (scan.hasNextLine())
                grounder = scan.nextLine();
            if (scan.hasNextLine())
                solver = scan.nextLine();
            scan.close();
        }

        ArrayList<PyObject> rtn = new ArrayList<PyObject>();
        as_stmt = as_stmt.replace("\"", "").replace("'", "");
        FileWriter f;
        FileReader fin;
        if (as_attr[0].equals("?[")) { //ASP Load Database
            try {
                f = new FileWriter("aspdb.jas", true);
                fin = new FileReader(as_stmt);
                int c;
                while ((c = fin.read()) != -1)
                    f.write(c);
                f.write("\n");
                f.close();
                fin.close();
                if (append)
                    as_stmt = "appended to " + as_stmt;
                else
                    as_stmt = "loaded " + as_stmt;
            } catch (Exception e) {
                System.out.println("File error at load.");
            }
        }

        else if (as_attr[0].equals("?-.")) { //ASP Blank Query
            String cmd = grounder + " aspdb.jas";
            Runtime run = Runtime.getRuntime();
            Process pr = null;
            pr = run.exec(cmd);
            InputStreamReader in = new InputStreamReader(pr.getInputStream());
            BufferedReader buf = new BufferedReader(in);
            String line1 = "";
            BufferedWriter out;
            out = new BufferedWriter(new FileWriter("toSolver.sol"));
            while ((line1 = buf.readLine()) != null)
                out.append(line1 + "\n");
            out.close();


            cmd = solver + " 0 " + "toSolver.sol";
            run = Runtime.getRuntime();
            pr = null;

            pr = run.exec(cmd);

            in = new InputStreamReader(pr.getInputStream());
            buf = new BufferedReader(in);
            line1 = "";
            as_stmt = "";
            boolean nextLine = false;
            while ((line1 = buf.readLine()) != null) {
                if (nextLine) {
                    line1 = line1.replace("Stable Model: ", ""); //if solver is SModels
                    as_stmt = as_stmt + line1 + ";";
                    nextLine = false;
                } else {
                    if (line1.contains("Answer:")) {
                        nextLine = true;
                    }
                }
            }

            buf.close();
            if (as_stmt.equals("")) {
                as_stmt = "false";
            }


        } else if (as_attr[0].equals("?-")) { //ASP Query
            f = new FileWriter("aspdbquery.jas", false);
            fin = new FileReader("aspdb.jas");
            int c;
            while ((c = fin.read()) != -1)
                f.write(c);
            f.write("\n");
            f.write(as_stmt);
            f.close();
            fin.close();
            String cmd = grounder + " aspdbquery.jas";
            Runtime run = Runtime.getRuntime();
            Process pr = null;

            pr = run.exec(cmd);
            InputStreamReader in = new InputStreamReader(pr.getInputStream());
            BufferedReader buf = new BufferedReader(in);
            String line1 = "";
            BufferedWriter out;

            out = new BufferedWriter(new FileWriter("toSolver.sol"));

            while ((line1 = buf.readLine()) != null)
                out.append(line1 + "\n");

            out.close();


            cmd = solver + " 0 " + "toSolver.sol";
            run = Runtime.getRuntime();
            pr = null;

            pr = run.exec(cmd);

            in = new InputStreamReader(pr.getInputStream());
            buf = new BufferedReader(in);
            line1 = "";

            as_stmt = "";
            boolean nextLine = false;
            while ((line1 = buf.readLine()) != null) {
                if (nextLine) {
                    line1 = line1.replace("Stable Model: ", ""); //if solver is SModels
                    as_stmt = as_stmt + line1 + ";";
                    nextLine = false;
                } else {
                    if (line1.contains("Answer:")) {
                        nextLine = true;
                    }
                }
            }
            buf.close();
            if (as_stmt.equals("")) {
                as_stmt = "false";
            }


        } else if (as_attr[0].equals("?->")) {
            try {
                f = new FileWriter("aspdb.jas", true);
                f.write(as_stmt + "\n");
                f.close();
            } catch (Exception e) {
                System.out.println("File error at insert.");
            }
        } else if (as_attr[0].equals("PRINT")) {
            try {
                BufferedReader buf = new BufferedReader(new FileReader("aspdb.jas"));
                System.out.println("CURRENT ASP DATABASE:");
                String line1;
                while ((line1 = buf.readLine()) != null)
                    System.out.println(line1);
                buf.close();
            } catch (Exception e) {
                System.out.println("File error while printing database.");
            }
        }

        else if (as_attr[0].equals("reg_fact")) {
            asp_select = true;
            fact_type = as_attr[0];
            fact_name = as_attr[1];
        } else if (as_attr[0].equals("group_fact")) {
            asp_select = true;
            fact_type = as_attr[0];
            fact_name = as_attr[1];
        } else {
            if (as_stmt.equals("CLEAR")) {
                try {
                    f = new FileWriter("aspdb.jas");
                    f.write("");
                    f.close();
                    as_stmt = "Database cleared.";
                } catch (Exception e) {
                    System.out.println("File error at CLEAR.");
                }
            } else {
                try {
                    f = new FileWriter(as_stmt, append);
                    fin = new FileReader("aspdb.jas");
                    int c;
                    while ((c = fin.read()) != -1) {
                        f.write(c);
                    }
                    f.close();
                    fin.close();
                    as_stmt = "Saved database to " + as_stmt;
                } catch (Exception e) {
                    System.out.println("File error at SAVE.");
                }
            }
        }
        as_attr = as_stmt.split(";");
        for (String s : as_attr) {
            rtn.add(new PyString(s));
        }
        PyObject[] temp = listtoarray(rtn);
        array = new PyObject[temp.length];
        System.arraycopy(temp, 0, array, 0, temp.length);
    }

    //prolog constructor
    public Prolog tupro;

    public PyTuple(PyObject[] elements, String prolog_stmt, String appendStr) throws SQLException {
        super(TYPE);
        /*
        String userHome = System.getProperty("user.home");
        if(userHome == null) {
            throw new IllegalStateException("user.home==null");
        }
        File home = new File(userHome);
        File settingsFile = new File(home, ".aspjylog");
        if(!settingsFile.exists()) {
            if(!settingsFile.mkdir()) {
                throw new IllegalStateException(settingsFile.toString());
            }
        }

        try {
            Scanner configs = new Scanner (settingsFile);
            while(configs.hasNextLine()){
                String a = configs.nextLine();
                if (a.contains("=")){
                    if (a.startsWith(new String("grounder")))
                    {grounder = a.substring(a.indexOf(new String("=")) + 1);}
                    if (a.startsWith(new String("solver")))
                    {solver = a.substring(a.indexOf(new String("=")) + 1);}
                }
            }
            configs.close();
        }
        catch (Exception e){
            System.out.println ("ERROR setting grounder and solver");
        } */
        String[] pro_attr = prolog_stmt.split(";");
        int size = Math.max(pro_attr.length - 1, elements.length);
        String pro_stmt = "";
        Boolean append = Boolean.valueOf(appendStr);
        for (int i = 0; i < size; i++) {
            if (i < elements.length) {
                if (elements[i].getType().pyGetName().toString() == "str")
                    pro_stmt += "\'" + elements[i].toString() + "\'";
                else
                    pro_stmt += "" + elements[i].toString() + "";
            }
            if (i + 1 < pro_attr.length) {
                pro_stmt += pro_attr[i + 1];
            }
        }
        ArrayList<PyObject> rtn = new ArrayList<PyObject>();
        pro_stmt = pro_stmt.replace("\"", "").replace("'", "");
        FileWriter f;
        FileReader fin;
        if (tupro == null)
            tupro = new Prolog();
        if (pro_attr[0].equals("?[")) {
            try {
                f = new FileWriter("prologdb.p", true);
                fin = new FileReader(pro_stmt);
                int c;
                while ((c = fin.read()) != -1)
                    f.write(c);
                f.write("\n");
                f.close();
                fin.close();
                if (append)
                    pro_stmt = "appended to " + pro_stmt;
                else
                    pro_stmt = "loaded " + pro_stmt;
            } catch (Exception e) {
                System.out.println("File error at load.");
            }
        } else if (pro_attr[0].equals("?-")) {
            try {
                Theory t = new Theory(new FileInputStream("prologdb.p"));
                tupro.setTheory(t);
                SolveInfo answer = tupro.solve(pro_stmt);
                if (answer.toString().equals("no."))
                    pro_stmt = "false.";
                else {
                    pro_stmt = answer.getSolution().toString() + ";";
                    while (tupro.hasOpenAlternatives()) {
                        answer = tupro.solveNext();
                        if (!(answer.toString().equals("no.")))
                            pro_stmt = pro_stmt + answer.getSolution().toString() + ";";
                    }
                }
            } catch (Exception e) {
                System.out.println("Query error.");
            }
        } else if (pro_attr[0].equals("?->")) {
            try {
                f = new FileWriter("prologdb.p", true);
                f.write(pro_stmt + "\n");
                f.close();
            } catch (Exception e) {
                System.out.println("File error at insert.");
            }
        } else if (pro_attr[0].equals("PRINT")) {
            try {
                BufferedReader buf = new BufferedReader(new FileReader("prologdb.p"));
                System.out.println("CURRENT PROLOG DATABASE:");
                String line1;
                while ((line1 = buf.readLine()) != null)
                    System.out.println(line1);
                buf.close();
            } catch (Exception e) {
                System.out.println("File error while printing database.");
            }
        } else if (pro_attr[0].equals("PRO_SELECT")) {
            prolog_select = true;
            fact_name = pro_attr[1];
        } else {
            if (pro_stmt.equals("CLEAR")) {
                try {
                    f = new FileWriter("prologdb.p");
                    f.write("");
                    f.close();
                    pro_stmt = "Database cleared.";
                } catch (Exception e) {
                    System.out.println("File error at CLEAR.");
                }
            } else {
                try {
                    f = new FileWriter(pro_stmt, append);
                    fin = new FileReader("prologdb.p");
                    int c;
                    while ((c = fin.read()) != -1) {
                        f.write(c);
                    }
                    f.close();
                    fin.close();
                    pro_stmt = "Saved database to " + pro_stmt;
                } catch (Exception e) {
                    System.out.println("File error at SAVE.");
                }
            }
        }
        pro_attr = pro_stmt.split(";");
        for (String s : pro_attr) {
            rtn.add(new PyString(s));
        }
        PyObject[] temp = listtoarray(rtn);
        array = new PyObject[temp.length];
        System.arraycopy(temp, 0, array, 0, temp.length);
    }


    //SQl stuff

    public static int nTuple; //Count how many tuples(columns) theere are

    public PyTuple(PyObject[] elements, String sqlstrings, String mode, String server, String uname, String pword,
                   String ctype) throws SQLException {
        this(TYPE, elements, sqlstrings, mode, server, uname, pword, ctype);
    }

    public PyTuple(PyType subtype, PyObject[] elements, String sqlstrings, String mode, String server, String uname,
    		String pword, String ctype) throws SQLException {
    	super(subtype);

    	String url = server;
    	OracleConnection conn = null;
    	Statement stmt = null;
    	try {

    		String url1 = System.getProperty("JDBC_URL");
    		if (url1 != null)
    			url = url1;
    		conn = createConnection(uname, pword, url);
    		stmt = conn.createStatement();

    		System.out.println("Checking mode to determine which code to use.");
    		if (mode.equals("SIM")) {                                                      // SIM PROCESSING
    			String sparql = parseSIM(elements, sqlstrings, server, uname, pword, ctype, conn);
    			if ( ! sparql.equals(""))
    				runAndOutputTuples(sparql, stmt);
    		}
		else if (mode.equals("SPARQL")) {
			this.sDoer = new SPARQLDoer(conn, uname);

			sDoer.executeStatement("BEGIN \n" +

                                       "sem_apis.create_entailment( \n" +

                                       "  'RDF_ENT_CS345_sz4554', \n" +

                                       "  models_in         => sem_models('RDF_MODEL_CS345_sz4554'), \n" +

                                       "  rulebases_in      => sem_rulebases('owl2rl'), \n" +

                                       "  passes            => SEM_APIS.REACH_CLOSURE, \n" +

                                       "  options           => 'LOCAL_NG_INF=T' \n" +

                                       "); \n" +

                                       "END; \n");
		}
		else if (mode.equals("SQL") && !(asp_select || prolog_select)) {                                               // SQL PROCESSING
    			String[] strings = sqlstrings.split(";");
    			int size = Math.max(strings.length - 1, elements.length);
    			//Parsing the SQL statement into one string
    			String sqlstmt = strings[0];
    			for (int i = 0; i < size; i++) {
    				if (i + 1 < strings.length) {
    					sqlstmt += strings[i + 1];
    				}
    				if (i < elements.length) {
    					if (elements[i].getType().pyGetName().toString() == "str")
    						sqlstmt += " \'" + elements[i].toString() + "\' ";
    					else
    						sqlstmt += " " + elements[i].toString() + " ";
    				}
    			}
    			CCJSqlParserManager pm = new CCJSqlParserManager();
    			net.sf.jsqlparser.statement.Statement statement = null;
    			try {
    				statement = (net.sf.jsqlparser.statement.Statement)pm.parse(new StringReader(sqlstmt));
    			} catch (Exception e) {
    				System.out.println(e);
    			}
    			ArrayList<PyObject> rows = new ArrayList<PyObject>();
    			sqlstmt = statement.toString();

    			boolean isRemote = false;
    			if (ctype.equalsIgnoreCase("remote")) isRemote = true;

    			if (statement instanceof CreateTable) {
    				if (ctype.equalsIgnoreCase("remote")) {
    					stmt.execute(sqlstmt);
    				} else {
    					try {
    						net.sf.jsqlparser.statement.create.table.CreateTable caststmt =
    								(net.sf.jsqlparser.statement.create.table.CreateTable)statement;
    						SQLVisitor visitor = new SQLVisitor(conn, server, uname, pword);
    						visitor.getCreateTable(caststmt, conn);
    					} catch (Exception e) {
    						System.out.println(e);
    						e.printStackTrace();
    						PyObject[] temp = new PyObject[1];
    						temp[0] = new PyString(e.toString());
    						rows.add(new PyTuple(temp));
    					}
    				}
    			} else if (statement instanceof Insert) {
    				if (ctype.equalsIgnoreCase("remote")) {
    					//If connection type is remote do SQL
    					stmt.execute(sqlstmt);
    				} else if (ctype.equalsIgnoreCase("local")) {
    					System.out.println("Entering local insert section.");
    					net.sf.jsqlparser.statement.insert.Insert caststmt =
    							(net.sf.jsqlparser.statement.insert.Insert)statement;
    					SQLVisitor visitor = new SQLVisitor(conn, server, uname, pword);
    					visitor.getInsert(caststmt, conn);
    				}
    			} else if (statement instanceof Select) {
    				try {
    					net.sf.jsqlparser.statement.select.Select caststmt =
    							(net.sf.jsqlparser.statement.select.Select)statement;
    					if (ctype.equalsIgnoreCase("local")) {
    						SQLVisitor visitor = new SQLVisitor(conn, server, uname, pword);
    						sqlstmt = visitor.getSelect(caststmt);
    					}
    					runAndOutputTuples(sqlstmt, stmt);
    				} catch (Exception e) {
    					PyObject[] temp = new PyObject[1];
    					temp[0] = new PyString(e.toString());
    					rows.add(new PyTuple(temp));
    				}
    			} else if (statement instanceof Delete) {

    			}
    			else if (statement instanceof Update)
    			{

    			} else if (statement instanceof Drop) {
    				if (ctype.equalsIgnoreCase("remote")) {
    					System.out.println(sqlstmt);
    					stmt.execute(sqlstmt);
    				} else if (ctype.equalsIgnoreCase("local")) {
    					System.out.println("Entering local drop section.");
    					net.sf.jsqlparser.statement.drop.Drop caststmt = (net.sf.jsqlparser.statement.drop.Drop)statement;
    					SQLVisitor visitor = new SQLVisitor(conn, server, uname, pword);
    					visitor.doDrop(caststmt, conn);
    				}
    			}
    			stmt.close();
    			if (conn != null)
    				conn.close();
    		} else {                                                                        // PROLOG and ASP PROCESSING
    			//replace elements with result of sql expression
    			String[] strings = sqlstrings.split(";");
    			int size = Math.max(strings.length - 1, elements.length);
    			//if we're attempting ASP operations
    			aspFlag = false;
    			if (strings[0].contains("ASPSELECT")) {
    				strings[0] = "SELECT ";
    				aspFlag = true;
    			} else if (strings[0].contains("RDFSELECT")) {
    				strings[0] = "SELECT ";
    				rdfFlag = true;
    			}

    			//Parsing the SQL statement into one string
    			String sqlstmt = strings[0];
    			for (int i = 0; i < size; i++) {
    				if (i + 1 < strings.length) {
    					sqlstmt += strings[i + 1];
    				}
    				if (i < elements.length) {
    					if (elements[i].getType().pyGetName().toString() == "str")
    						sqlstmt += " \'" + elements[i].toString() + "\' ";

    					else
    						sqlstmt += " " + elements[i].toString() + " ";
    				}
    				//System.out.println("Partial SLSTMT:"+sqlstmt);
    			}
    			//System.out.println("SLSTMT:"+sqlstmt);


    			// Try JsqlParser
    			CCJSqlParserManager pm = new CCJSqlParserManager();
    			/*
				String sql =
				"SELECT * FROM MY_TABLE1, MY_TABLE2, (SELECT * FROM MY_TABLE3) LEFT OUTER JOIN MY_TABLE4 " +
				" WHERE ID = (SELECT MAX(ID) FROM MY_TABLE5) AND ID2 IN (SELECT * FROM MY_TABLE6)";
    			 */
    			net.sf.jsqlparser.statement.Statement statement = null;
    			try {
    				statement = (net.sf.jsqlparser.statement.Statement)pm.parse(new StringReader(sqlstmt));
    			} catch (Exception e) {
    				System.out.println(e);
    			}

    			ArrayList<PyObject> rows = new ArrayList<PyObject>();
    			sqlstmt = statement.toString();

    			//Use only if have to create RDF table from SQL table
    			//Holds the converted insert statements
    			ArrayList<String> insertStatements = new ArrayList<String>();
    			//If the RDF table does not exists we are to create one
    			String createTable = "";
    			makeRDF = false; //A flag to indicate the code we are to make a RDF table
    			boolean exit = false, isRemote = false;
    			if (ctype.equalsIgnoreCase("remote"))
    				isRemote = true;

    			if (statement instanceof Select) {
    				try {
    					net.sf.jsqlparser.statement.select.Select caststmt =
    							(net.sf.jsqlparser.statement.select.Select)statement;
    					SQLVisitor visitor = new SQLVisitor(conn, server, uname, pword);
    					//visitor.site = website;//visitor.subscript = "_CS345_fjg344";//System.out.println("TEST");

    					System.out.println(caststmt);
    					if (ctype.equalsIgnoreCase("local"))
    						sqlstmt = visitor.getSelect(caststmt);

    					// check if  table is there
    					//ResultSet tables;
    					if (asp_select || prolog_select) {
    						rows = convertNonRDF(sqlstmt, conn, ctype, server, uname, pword);
    					} else if (aspFlag && ctype.equalsIgnoreCase("local"))
    						rows = ASPSELECT(sqlstmt, conn);
    					else if (rdfFlag && ctype.equalsIgnoreCase("remote")) {
    						rows = RDFSELECT(caststmt, sqlstmt, conn, uname);
    						makeRDF = true;
    					}
    					//Need else, as ASP_SELECT, RDF_SELECT etc,. have their own way to process data
    					else {
    						if (ctype.equalsIgnoreCase("remote")) {
    							stmt.execute(sqlstmt);
    						}
    						OracleResultSet rs = (OracleResultSet)stmt.executeQuery(sqlstmt);
    						ResultSetMetaData rd = rs.getMetaData();
    						DatabaseMetaData dbm = conn.getMetaData();
    						//This is the way frank originally had it, but adding new lines, so we can create RDF table from SQL table
    						//format results
    						int cc = rd.getColumnCount();
    						PyObject[] temp;
    						ArrayList<PyObject> columns = new ArrayList<PyObject>();
    						nTuple = cc;
    						for (int i = 1; i <= cc; i++) {
    							columns.add(new PyString(rd.getColumnName(i)));
    						}
    						temp = listtoarray(columns);
    						rows.add(new PyTuple(temp));
    						while (rs.next()) {
    							ArrayList<PyObject> items = new ArrayList<PyObject>();
    							for (int i = 1; i <= cc; i++) {
    								Object obj = rs.getObject(i);
    								//all objects are set to strings, can change this later
    								if (obj != null) {
    									//check classes and convert to pytypes accordingly
    									//System.out.println(obj.getClass());
    									if (String.class.isInstance(obj)) {
    										items.add(new PyString(obj.toString()));
    										//seems all numbers are bigdecimals here. . . probably a orcale thing
    										//this is a start, will have to look into ints, floats, longs, etc
    									} else if (BigDecimal.class.isInstance(obj)) {
    										items.add(new PyLong(((BigDecimal)obj).toBigInteger()));
    										//could add more types
    										//default to string representation if type not supported
    									} else {
    										items.add(new PyString(obj.toString()));
    									}
    								} else {
    									items.add(new PyNone());
    								}
    							}
    							temp = listtoarray(items);
    							rows.add(new PyTuple(temp));
    						}
    						rs.close();
    					}
    				} catch (Exception e) {
    					//System.out.println(e);
    					//e.printStackTrace();
    					PyObject[] temp = new PyObject[1];
    					temp[0] = new PyString(e.toString());
    					rows.add(new PyTuple(temp));
    				}
    			}

    			//a lot of conversion going on here. . .
    			PyObject[] results = listtoarray(rows);
    			//put results in array for this tuple object
    			array = new PyObject[results.length];
    			System.arraycopy(results, 0, array, 0, results.length);
    		}
    	} catch (Exception e) {
        	System.out.println(e);
        	e.printStackTrace();
        } finally {
        	if (stmt != null) {
        		stmt.close();
        	}
        	if (conn != null) {
        		conn.close();
        	}
        }
        
    }

    public void runAndOutputTuples(String sqlstmt, Statement stmt) {
        ArrayList<PyObject> rows = new ArrayList<PyObject>();
        try {
            stmt.execute(sqlstmt);
            OracleResultSet rs = (OracleResultSet)stmt.executeQuery(sqlstmt);
            ResultSetMetaData rd = rs.getMetaData();
            int cc = rd.getColumnCount();
            PyObject[] temp;
            ArrayList<PyObject> columns = new ArrayList<PyObject>();
            nTuple = cc;
            for (int i = 1; i <= cc; i++) {
                columns.add(new PyString(rd.getColumnName(i)));
            }
            temp = listtoarray(columns);
            rows.add(new PyTuple(temp));
            while (rs.next()) {
                ArrayList<PyObject> items = new ArrayList<PyObject>();
                for (int i = 1; i <= cc; i++) {
                    Object obj = rs.getObject(i);
                    //all objects are set to strings, can change this later
                    if (obj != null) {
                        //check classes and convert to pytypes accordingly
                        //System.out.println(obj.getClass());
                        if (String.class.isInstance(obj)) {
                            items.add(new PyString(obj.toString()));
                            //seems all numbers are bigdecimals here. . . probably a orcale thing
                            //this is a start, will have to look into ints, floats, longs, etc
                        } else if (BigDecimal.class.isInstance(obj)) {
                            items.add(new PyLong(((BigDecimal)obj).toBigInteger()));
                            //could add more types
                            //default to string representation if type not supported
                        } else {
                            items.add(new PyString(obj.toString()));
                        }
                    } else {
                        items.add(new PyNone());
                    }
                }
                temp = listtoarray(items);
                rows.add(new PyTuple(temp));
            }
            rs.close();
        } catch (Exception e) {
            PyObject[] temp = new PyObject[1];
            temp[0] = new PyString(e.toString());
            rows.add(new PyTuple(temp));
        }

        //a lot of conversion going on here. . .
        PyObject[] results = listtoarray(rows);
        //put results in array for this tuple object
        array = new PyObject[results.length];
        System.arraycopy(results, 0, array, 0, results.length);
    }

    public ArrayList<PyObject> ASPSELECT(String sqlstmt, OracleConnection conn) {
        // Execute the SQL statement.
        ArrayList<PyObject> rows = new ArrayList<PyObject>(); //rows will contain tuples to be used later
        try {
            Statement stmt = conn.createStatement();
            //Get renspose from database
            OracleResultSet rs = (OracleResultSet)stmt.executeQuery(sqlstmt);
            ResultSetMetaData rd = rs.getMetaData();
            //Here processing aspfacts
            int sizeOfTuple = 0;
            String identifier = "";
            OracleResultSet rs2 = (OracleResultSet)stmt.executeQuery(sqlstmt);
            HashMap<String, Integer> m = new HashMap<String, Integer>();
            while (rs2.next()) {
                Object obj = rs2.getObject(1);
                //String[] aspTemp = obj.toString().split("/");

                if (identifier.equals("")) {
                    identifier = rs2.getObject(1).toString();

                } else if (!identifier.equals(rs2.getObject(1).toString()))
                    break;
                m.put(rs2.getObject(2).toString(), sizeOfTuple);
                sizeOfTuple++;
            }

            //verbose use for testing porpuses...

            if (verbose) {
                System.out.println(m);
                System.out.println("Size of tuple: " + sizeOfTuple + rd.getColumnCount());
            }

            ArrayList<PyObject> strs = new ArrayList<PyObject>();
            String aspStatement = "";
            //format results
            int cc = rd.getColumnCount();
            PyObject[] temp;
            int count = 0;
            ArrayList<PyObject> allItems = new ArrayList<PyObject>();
            ArrayList<PyObject> items = new ArrayList<PyObject>();

            //remember to make sure that this works with the bottom 2 lines of code
            //that involve the original items arraylist
            rs = (OracleResultSet)stmt.executeQuery(sqlstmt);
            //Here parsing RDF, to return it as tuples without the RDF identifiers
            while (rs.next()) {

                //Getting the third object: RS returns in a weird format
                Object obj = rs.getObject(3);

                //Obj will be in form random/random/data

                if (verbose) {
                    System.out.println(obj.toString());
                    System.out.println("Object to String: " + obj.toString());
                }

                //Store the important data
                items.add(new PyString(rs.getObject(2).toString() + ":" + obj.toString()));

                if (verbose)
                    System.out.println("Count is " + count + ", size of tuple is " + sizeOfTuple);

                //If we've gathered enough data to fill a sizeOfTuple-sized tuple
                if (count == sizeOfTuple - 1) {
                    //Time to process tuples
                    count = 0;

                    String s = "";
                    String[] aspOrder = new String[sizeOfTuple];
                    for (int i = 0; i < sizeOfTuple; i++) {
                        String[] aspTemp = items.get(i).toString().split(":");
                        aspOrder[sizeOfTuple - m.get(aspTemp[0]) - 1] = aspTemp[1];
                    }
                    for (String i : aspOrder) {
                        s += i + ",";
                    }
                    s = s.substring(0, s.length() - 1);

                    if (verbose)
                        System.out.println(s);

                    strs.add(new PyString(s.toString()));

                    if (verbose)
                        System.out.println("STRS: " + strs.get(0).toString());

                    items = new ArrayList<PyObject>();

                    temp = listtoarray(strs);
                    if (verbose)
                        System.out.println(Arrays.toString(temp));

                    rows.add(new PyTuple(temp));
                    strs = new ArrayList<PyObject>();
                } else
                    count++;
            }
            rs.close();
        } catch (Exception e) {
            //System.out.println(e);
            //e.printStackTrace();
            PyObject[] temp = new PyObject[1];
            temp[0] = new PyString(e.toString());
            rows.add(new PyTuple(temp));
        }
        return rows;
    }

    public ArrayList<PyObject> RDFSELECT(net.sf.jsqlparser.statement.Statement statement, String sqlstmt,
                                         OracleConnection conn, String uname) {
        // Execute the SQL statement.
        ArrayList<PyObject> rows = new ArrayList<PyObject>(); //rows will contain tuples to be used later
        net.sf.jsqlparser.statement.select.Select caststmt = (net.sf.jsqlparser.statement.select.Select)statement;
        //Get renspose from database

        try {
            Statement stmt = conn.createStatement();
            ArrayList<String> insertStatements = new ArrayList<String>();
            String createTable = "";
            DatabaseMetaData dbm = conn.getMetaData();
            ResultSet tables = dbm.getTables(null, null, "RDF_DATA_TABLE", null);
            //if (!tables.next()) {
            //System.out.println("TABLE HERE");
            //Get renspose from database
            OracleResultSet rs = (OracleResultSet)stmt.executeQuery(sqlstmt);
            ResultSetMetaData rd = rs.getMetaData();
            String vals = "";
            String[] tempCreateTable;
            //format results

            int cc = rd.getColumnCount();
            PyObject[] temp;
            ArrayList<PyObject> columns = new ArrayList<PyObject>();

            //FOR RDFSELECT
            tempCreateTable =
                    new String[cc * 2]; //Storing name of column and type as of right now, only varchar(255), and NUMBER
            //Ex., CREATE TABLE TEST (SUBJECT varchar(255), PREDICATE varchar(255), OBJECT NUMBER);
            nTuple = cc;
            String insertStm = "INSERT INTO " + ((PlainSelect)caststmt.getSelectBody()).getFromItem() + " (";

            for (int i = 1; i <= cc; i++) {
                tempCreateTable[(i - 1) * 2] = rd.getColumnName(i).toString();
                insertStm += tempCreateTable[(i - 1) * 2] + ","; //Only used on RDF
                columns.add(new PyString(rd.getColumnName(i)));
            }
            insertStm = insertStm.substring(0, insertStm.length() - 1) + ") VALUES (";
            temp = listtoarray(columns);
            rows.add(new PyTuple(temp));
            //int tempI = 1;
            String insertTemp = insertStm;
            while (rs.next()) {
                ArrayList<PyObject> items = new ArrayList<PyObject>();
                for (int i = 1; i <= cc; i++) {
                    Object obj = rs.getObject(i);
                    //all objects are set to strings, can change this later
                    if (obj != null) {
                        //check classes and convert to pytypes accordingly
                        //System.out.println(obj.getClass());
                        if (String.class.isInstance(obj)) {
                            items.add(new PyString(obj.toString()));

                            //added fo RDFSELECT
                            insertStm += "'" + obj.toString() + "',";
                            //if(tempI<=cc)
                            //	tempCreateTable[(tempI-1)*2+1] = "varchar(255)";

                        }
                        //seems all numbers are bigdecimals here. . . probably a orcale thing
                        //this is a start, will have to look into ints, floats, longs, etc
                        else if (BigDecimal.class.isInstance(obj)) {
                            items.add(new PyLong(((BigDecimal)obj).toBigInteger()));
                            //added for RDFSELECT
                            insertStm += obj.toString() + ",";
                            //if(tempI<=cc)
                            //	tempCreateTable[(tempI-1)*2+1] = "NUMBER";
                        }
                        //could add more types
                        //default to string representation if type not supported
                        else {
                            items.add(new PyString(obj.toString()));
                            //Added for RDFSELECT
                            //if(tempI<=cc)
                            //	tempCreateTable[(tempI-1)*2+1] = "varchar(255)";
                        }
                        //tempI++;
                    } else {
                        items.add(new PyNone());
                        //Empty
                        //if(tempI<=cc)
                        //	tempCreateTable[(tempI-1)*2+1] = "";
                    }
                }
                //Done creating insert statement, use only if user uses RDFSELECT
                insertStm = insertStm.substring(0, insertStm.length() - 1) + ")";

                //Add current insertStm to array of insert statements
                insertStatements.add(insertStm);
                insertStm = insertTemp;

                temp = listtoarray(items);
                rows.add(new PyTuple(temp));
            }
            rs.close();
            //if we are creating an RDF table, fix createTable statement
            //if rdfFlag and table does not exist, then need to create it.
            //createTable = "CREATE TABLE " + ((PlainSelect)caststmt.getSelectBody()).getFromItem() + " (";
            //for (int i=1;i<=cc;i++){
            //	createTable += tempCreateTable[(i-1)*2] + " " + tempCreateTable[(i-1)*2+1] + " , ";
            //}
            //Done creating CREATE TABLE statements...
            //createTable = createTable.substring(0, createTable.length()-2) + " )";
            // Try JsqlParser
            CCJSqlParserManager pm = new CCJSqlParserManager();
            //statement = (net.sf.jsqlparser.statement.Statement)pm.parse(new StringReader(createTable));
            //net.sf.jsqlparser.statement.create.table.CreateTable caststmt1 = (net.sf.jsqlparser.statement.create.table.CreateTable) statement;
            //Creating Sequence only used on RDF data base
            //stmt.executeQuery("CREATE SEQUENCE "+caststmt1.getTable()+"_RDF_DATA_sqnc MINVALUE 1 START WITH 1 INCREMENT BY 1 NOCACHE");
            stmt.executeQuery("CREATE SEQUENCE RDF_DATA_TABLE_sqnc MINVALUE 1 START WITH 1 INCREMENT BY 1 NOCACHE");

            //System.out.println();
            //System.out.println(sqlstmt);
            //System.out.println();
            sqlstmt = "";

            //sqlstmt = "CREATE TABLE " + caststmt1.getTable() + "_RDF_DATA(id NUMBER, triple SDO_RDF_TRIPLE_S)";
            sqlstmt = "CREATE TABLE RDF_DATA_TABLE (id NUMBER, triple SDO_RDF_TRIPLE_S)";
            System.out.println("|" + sqlstmt + "| <<< create RDF table");

            stmt.execute(sqlstmt);

            //System.out.println(sqlstmt);
            //sqlstmt = "BEGIN\nSEM_APIS.CREATE_RDF_MODEL('" + caststmt1.getTable() + "_CS345_fjg344', '" + caststmt1.getTable() + "_RDF_DATA', 'triple');\nEND;";	
            sqlstmt = "BEGIN\nSEM_APIS.CREATE_RDF_MODEL('RDF_MODEL_" + uname + "', 'RDF_DATA_TABLE', 'triple');\nEND;";
            System.out.println("|" + sqlstmt + "| <<< create RDF model");
            stmt.execute(sqlstmt);
            //System.out.println(sqlstmt);

            net.sf.jsqlparser.statement.insert.Insert caststmt2 = null;
            int sizei = insertStatements.size();
            //System.out.printl
            //Need to iterate over insert staments if making RDF, else just iteratate once.
            for (int j = 0; j < sizei; j++) {
                try {
                    statement =
                            (net.sf.jsqlparser.statement.Statement)pm.parse(new StringReader(insertStatements.get(j)));
                } catch (Exception e) {
                    System.out.println(e);
                }
                caststmt2 = (net.sf.jsqlparser.statement.insert.Insert)statement;

                List cols = caststmt2.getColumns();
                List vals1 = ((ExpressionList)caststmt2.getItemsList()).getExpressions();

                String tableName = caststmt2.getTable().toString();
                //Oracle Sequence
                //select mySeq.nextval from dual
                //OracleResultSet rs2 = (OracleResultSet)stmt.executeQuery("SELECT " + tableName + "_RDF_DATA_sqnc.nextval FROM dual");
                OracleResultSet rs2 =
                    (OracleResultSet)stmt.executeQuery("SELECT RDF_DATA_TABLE_sqnc.nextval FROM dual");
                //Getting next gui using Oracle Sequence
                while (rs2.next()) {
                    guid = Integer.parseInt((rs2.getObject(1)).toString()); //There is only one item
                }
                rs2.close();
                for (int i = 0; i < cols.size(); ++i) {
                    sqlstmt = "";
                    String bob = vals1.get(i).toString();
                    if (bob.charAt(0) == '\'')
                        bob = bob.substring(1, bob.length() - 1);
                    //sqlstmt += "INSERT INTO " + tableName + "_RDF_DATA VALUES (" + linenumber++ + ", SDO_RDF_TRIPLE_S('" + tableName + "_CS345_fjg344', '" + website
                    //+ caststmt2.getTable() + "/" + guid + "', '" + website + tableName + "/" + cols.get(i) + "', '" +  website + caststmt2.getTable() + "/" + bob + "'))";
                    sqlstmt +=
                            "INSERT INTO RDF_DATA_TABLE VALUES (" + linenumber++ + "," + " SDO_RDF_TRIPLE_S('RDF_MODEL_" +
                            uname + ":<" + website + tableName + ">'," + " '<http://" + guid + ">'," +
                            // !!! make sure these guid's are the same for each entry from a specific line of an SQL table -Oldag 22 June 2012
                            " '<" + website + "people.owl#" + cols.get(i) + ">'," + " '\"" + bob + "\"' ) )";
                    System.out.println(sqlstmt);
                    stmt.executeUpdate(sqlstmt);
                }
            }
            //}
        } catch (Exception e) {
            //System.out.println(e);
            //e.printStackTrace();
            PyObject[] temp = new PyObject[1];
            temp[0] = new PyString(e.toString());
            rows.add(new PyTuple(temp));
        }
        return rows;
    }

    public ArrayList<PyObject> convertNonRDF(String statement,
                                             OracleConnection conn, String ctype, String server, String uname,
                                             String pword) {
        // Execute the SQL statement.
        ArrayList<PyObject> rows = new ArrayList<PyObject>(); //rows will contain tuples to be used later
        // Remote
        if (ctype.equals("remote")) {
            try {
                String sqlstmt = statement;
                System.out.println(sqlstmt);
                Statement stmt = conn.createStatement();
				try {
					OracleResultSet rs = (OracleResultSet)stmt.executeQuery(sqlstmt);
					ResultSetMetaData rd = rs.getMetaData();
					//format results
					int cc = rd.getColumnCount();
					PyObject[] temp;
					ArrayList<PyObject> columns = new ArrayList<PyObject>();
					for (int i = 1; i <= cc; i++) {
						columns.add(new PyString(rd.getColumnName(i)));
					}
					temp = listtoarray(columns);
					rows.add(new PyTuple(temp));

					String asp_fact = fact_name + "(";
					boolean first_item = true;
					FileWriter f = new FileWriter("aspdb.jas", true);
					FileWriter pro = new FileWriter("prologdb.p", true);
					String tempString = "";
					while (rs.next()) {
						ArrayList<PyObject> items = new ArrayList<PyObject>();
						for (int i = 1; i <= cc; i++) {
							Object obj = rs.getObject(i);
							//all objects are set to strings, can change this later
							if (obj != null) {
								//check classes and convert to pytypes accordingly
								//System.out.println(obj.getClass());
								if (String.class.isInstance(obj)) {
									items.add(new PyString(obj.toString()));
								}
								//seems all numbers are bigdecimals here. . . probably a orcale thing
								//this is a start, will have to look into ints, floats, longs, etc
								else if (BigDecimal.class.isInstance(obj)) {
									items.add(new PyLong(((BigDecimal)obj).toBigInteger()));
								}
								//could add more types
								//default to string representation if type not supported
								else {
									items.add(new PyString(obj.toString()));
								}
							} else {
								items.add(new PyNone());
							}
						}
						temp = listtoarray(items);

						rows.add(new PyTuple(temp));

						if (asp_select) {
							if (fact_type.equals("reg_fact")) {
								if (temp[0] instanceof PyNone) {
									asp_fact += 0;
								} else if (temp[0] instanceof PyLong) {
									tempString = ((PyLong)temp[0]).toString();
									asp_fact += tempString.substring(0, tempString.length() - 1);

								} else {
									asp_fact += ((PyString)temp[0]).toString();
								}

								for (int i = 1; i < temp.length; i++) {
									if (temp[i] instanceof PyNone) {
										asp_fact += "," + 0;
									} else if (temp[i] instanceof PyLong) {
										tempString = ((PyLong)temp[i]).toString();
										asp_fact += "," + tempString.substring(0, tempString.length() - 1);
									} else {
										asp_fact += "," + ((PyString)temp[i]).toString();
									}
								}
							} else {
								if (first_item) {
									if (temp[0] instanceof PyNone) {
										asp_fact += 0;
									} else if (temp[0] instanceof PyLong) {
										tempString = ((PyLong)temp[0]).toString();
										asp_fact += tempString.substring(0, tempString.length() - 1);
									} else {
										asp_fact += ((PyString)temp[0]).toString();
									}
									first_item = false;
								} else {
									if (temp[0] instanceof PyNone) {
										asp_fact += ";" + 0;
									} else if (temp[0] instanceof PyLong) {
										tempString = ((PyLong)temp[0]).toString();
										asp_fact += ";" + tempString.substring(0, tempString.length() - 1);

									} else {
										asp_fact += ";" + ((PyString)temp[0]).toString();
									}
								}
							}
							if (fact_type.equals("reg_fact")) {
								asp_fact += ").";
								f.write(asp_fact);
								f.write("\n");
								asp_fact = fact_name + "(";
							}
						} else if (prolog_select) {
							if (temp[0] instanceof PyNone) {
								asp_fact += 0;
							} else if (temp[0] instanceof PyLong) {
								tempString = ((PyLong)temp[0]).toString();
								asp_fact += tempString.substring(0, tempString.length() - 1);
							} else {
								asp_fact += "\'" + ((PyString)temp[0]).toString() + "\'";
							}
							for (int i = 1; i < temp.length; i++) {
								if (temp[i] instanceof PyNone) {
									asp_fact += "," + 0;
								} else if (temp[i] instanceof PyLong) {
									tempString = ((PyLong)temp[i]).toString();
									asp_fact += "," + tempString.substring(0, tempString.length() - 1);
								} else {
									asp_fact += "," + "\'" + ((PyString)temp[i]).toString() + "\'";
								}
							}
							//finish up the fact and add it to the database
							asp_fact += ").";
							pro.write(asp_fact);
							pro.write("\n");
							asp_fact = fact_name + "(";
						}
					}
					if (asp_select && fact_type.equals("group_fact")) {
						asp_fact += ").";
						f.write(asp_fact);
						f.write("\n");
					}
					if (asp_select) {
						asp_select = false;
						fact_type = "";
						fact_name = "";
					} else if (prolog_select) {
						prolog_select = false;
						fact_name = "";
					}
					f.close();
					pro.close();
					rs.close();
				} catch (Exception e) {
					System.out.println(e);
					e.printStackTrace();
					PyObject[] temp = new PyObject[1];
					temp[0] = new PyString(e.toString());
					rows.add(new PyTuple(temp));
				}
            } catch (Exception e) {
                System.out.println(e);
            }
        } else if (ctype.equals("local")) {
            rows = convertRDF(statement, conn, server, uname, pword);
        }

        return rows;

    }

    public ArrayList<PyObject> convertRDF(String statement, OracleConnection conn,
                                            String server, String uname, String pword) {
        ArrayList<PyObject> rows = new ArrayList<PyObject>();
        try {
            Statement stmt = conn.createStatement();
            String sqlstmt = "";
			sqlstmt = statement;
			System.out.println(sqlstmt);
			try {
				OracleResultSet rs = (OracleResultSet)stmt.executeQuery(sqlstmt);

				ResultSetMetaData rd = rs.getMetaData();
				//format results
				int cc = rd.getColumnCount();
				PyObject[] temp;
				ArrayList<PyObject> columns = new ArrayList<PyObject>();
				for (int i = 1; i <= cc; i++) {
					columns.add(new PyString(rd.getColumnName(i)));
				}
				temp = listtoarray(columns);
				rows.add(new PyTuple(temp));
				String asp_fact = fact_name + "(";
				String element = fact_name;
				boolean first_item = true;
				FileWriter f = new FileWriter("aspdb.jas", true);
				FileWriter pro = new FileWriter("prologdb.p", true);
				String fact_element = "";
				int slash_index = 0;
				while (rs.next()) {
					ArrayList<PyObject> items = new ArrayList<PyObject>();
					for (int i = 1; i <= cc; i++) {
						Object obj = rs.getObject(i);
						//System.out.println(obj.toString());
						//all objects are set to strings, can change this later
						if (obj != null) {
							//check classes and convert to pytypes accordingly
							//System.out.println(obj.getClass());
							if (String.class.isInstance(obj)) {
								items.add(new PyString(obj.toString()));
							}
							//seems all numbers are bigdecimals here. . . probably a orcale thing
							//this is a start, will have to look into ints, floats, longs, etc
							else if (BigDecimal.class.isInstance(obj)) {
								items.add(new PyLong(((BigDecimal)obj).toBigInteger()));
							}
							//could add more types
							//default to string representation if type not supported
							else {
								items.add(new PyString(obj.toString()));
							}
						} else {
							items.add(new PyNone());
						}
					}
					temp = listtoarray(items);
					rows.add(new PyTuple(temp));
					if (asp_select) {
						if (fact_type.equals("reg_fact")) {
							fact_element = ((PyString)temp[0]).toString();
							slash_index = fact_element.lastIndexOf("/");
							asp_fact += fact_element.substring(slash_index + 1);
							for (int i = 1; i < temp.length; i++) {
								fact_element = ((PyString)temp[i]).toString();
								slash_index = fact_element.lastIndexOf("/");
								if(!(fact_element.toLowerCase().equals(element.toLowerCase())))
									asp_fact += "," + fact_element.substring(slash_index + 1);
							}
						} else {
							fact_element = ((PyString)temp[0]).toString();
							slash_index = fact_element.lastIndexOf("/");
							if (first_item) {
								asp_fact += fact_element.substring(slash_index + 1);
								first_item = false;
							} else {
								asp_fact += ";" + fact_element.substring(slash_index + 1);
							}
						}
						if (fact_type.equals("reg_fact")) {
							asp_fact += ").";
							f.write(asp_fact);
							f.write("\n");
							asp_fact = fact_name + "(";
						}
					} else if (prolog_select) {
						fact_element = ((PyString)temp[0]).toString();
						slash_index = fact_element.lastIndexOf("/");
						asp_fact += fact_element.substring(slash_index + 1);

						for (int i = 1; i < temp.length; i++) {
							fact_element = ((PyString)temp[i]).toString();
							slash_index = fact_element.lastIndexOf("/");
							asp_fact += "," + fact_element.substring(slash_index + 1);
						}
						asp_fact += ").";
						pro.write(asp_fact);
						pro.write("\n");
						asp_fact = fact_name + "(";
					}
				}
				if (asp_select && fact_type.equals("group_fact")) {
					asp_fact += ").";
					f.write(asp_fact);
					f.write("\n");
				}
				if (asp_select) {
					asp_select = false;
					fact_type = "";
					fact_name = "";
				} else if (prolog_select) {
					prolog_select = false;
					fact_name = "";
				}
				f.close();
				pro.close();
				rs.close();
			} catch (Exception e) {
				System.out.println(e);
				e.printStackTrace();
				PyObject[] temp = new PyObject[1];
				temp[0] = new PyString(e.toString());
				rows.add(new PyTuple(temp));

			}
            
            //close stmt
            stmt.close();
        } catch (Exception e) {
            System.out.println(e);
        }
        return rows;
    }

    private OracleConnection createConnection(String user, String pass, String url) throws SQLException {
        // Create a OracleDataSource instance and set properties
        OracleDataSource ods = new OracleDataSource();
        ods.setUser(user);
        ods.setPassword(pass);
        ods.setURL(url);
        // Connect to the database
        return (OracleConnection)ods.getConnection();
    }

    public String parseSIM(PyObject[] elements, String sql, String server, String uname, String pword,
                         String conntype, OracleConnection conn) {

    	try {
    		
    		SIMHelper simHelper = new SIMHelper(conn, uname, pword);
    		String[] sqlTokens = sql.split(";");

    		System.out.println("****** Parsing SIM: " + sql);
    		if (sqlTokens[0].contains("MODIFY")) {
    			return parseSimModify(elements, server, uname, pword, simHelper, sqlTokens);

    		}else if (sqlTokens[0].contains("FROM")) {
    			return parseSimFrom(elements, server, uname, pword, simHelper, sqlTokens);

    		} else if (sqlTokens[0].contains("INSERT")) {
    			return parseSimInsert(elements, server, uname, pword, simHelper, sqlTokens);

    		} else if (sqlTokens[0].contains("CLASS") || sqlTokens[0].contains("SUBCLASS")) {
    			return parseSimClass(elements, server, uname, pword, simHelper, sqlTokens);
    		}
        
    	} catch (SQLException e) {
    		while (e != null) {
    			// Retrieve a human-readable message identifying the reason for the exception
    			System.out.println(e.getMessage());

    			// This vendor-independent string contains a code that identifies
    			// the reason for the exception.
    			// The code follows the Open Group SQL conventions.
    			System.out.println(e.getSQLState());

    			// Retrieve a vendor-specific code identifying the reason for the exception.
    			System.out.println(e.getErrorCode());

    			// The exception may have been chained; process the next chained exception
    			e = e.getNextException();
    		}
    	} catch (net.sf.jsqlparser.JSQLParserException e) {
    		System.out.println(e);
    	} catch (Exception e) {
    		e.fillInStackTrace();
    		System.out.println(e);
    	}

    	return "";
    }

	private String parseSimClass(PyObject[] elements, String server,
			String uname, String pword, SIMHelper simHelper, String[] sqlTokens)
			throws SQLException {
		int subclass = 0;
		String scname = null;
		if (sqlTokens[0].contains("SUBCLASS"))
		    subclass = 1;
		String className = sqlTokens[1];
		int strIndex = 2;
		int elemIndex = 0;
		int nextToken = -1;
		String dvaVal = "";
		String evaVal = "";
		List<String> attributeNames = new ArrayList<String>();
		List<String> attributeValues = new ArrayList<String>();
		if (sqlTokens[strIndex].trim().equals("COMMENT")) {
		    strIndex++;
		    attributeNames.add("ClassComment");
		    attributeValues.add(sqlTokens[strIndex++].trim().replaceAll("\'", " "));
		}
		if (subclass == 1) {
		    if (sqlTokens[strIndex].trim().equals("SUPERCLASS")) {
		        strIndex++;
		        scname = sqlTokens[strIndex++];
		    }

		}

		while (strIndex < sqlTokens.length) {
		    sqlTokens[strIndex] = sqlTokens[strIndex].trim();
		    if (sqlTokens[strIndex].equals("")) {
		        strIndex++;
		        continue;
		    }

		    if (sqlTokens[strIndex].equals("dvaAttribute")) {
		        attributeNames.add("dvaAttribute");
		        strIndex++;
		        dvaVal = "";
		        dvaVal += "dva:" + sqlTokens[strIndex++].trim() + ":";
		        //attributeValues.add(strings[strIndex++];
		        nextToken = 0;
		        continue;
		    } else if (sqlTokens[strIndex].equals("evaAttribute")) {
		        attributeNames.add("evaAttribute");
		        strIndex++;
		        evaVal = "";
		        evaVal += "eva:" + sqlTokens[strIndex++].trim() + ":";
		        //attributeValues.add(strings[strIndex++]);
		        nextToken = 1;
		        continue;
		    }

		    else {
		        assert (nextToken != -1);
		        if (nextToken == 0) {
		            while (strIndex < sqlTokens.length &&
		                   !(sqlTokens[strIndex].trim().equals("dvaAttribute") || sqlTokens[strIndex].trim().equals("evaAttribute"))) {
		                //System.out.println("inside nexttoken = 0 " +strIndex +" " + strings[strIndex]);
		                sqlTokens[strIndex] = sqlTokens[strIndex].trim();
		                if (sqlTokens[strIndex].equals("")) {
		                    strIndex++;
		                    continue;
		                }

		                if (sqlTokens[strIndex].equals("COMMENT")) {
		                    strIndex++;
		                    dvaVal += sqlTokens[strIndex++].trim().replaceAll("\'", " ");
		                    //attributeNames.add("dvaComment"); strIndex++;
		                    //attributeValues.add(strings[strIndex++]);
		                    continue;
		                }

		                if (sqlTokens[strIndex].equals("targetType")) {
		                    strIndex++;
		                    dvaVal += ":" + sqlTokens[strIndex++].trim();
		                    //attributeNames.add("attributeType"); strIndex++;
		                    //attributeValues.add(strings[strIndex++]);
		                    continue;
		                }

		                if (sqlTokens[strIndex].equals("DVAOPTIONS")) {
		                    strIndex++;
		                    String dvaoptval = "";
		                    while (strIndex < sqlTokens.length &&
		                           !(sqlTokens[strIndex].trim().equals("dvaAttribute") ||
		                             sqlTokens[strIndex].trim().equals("evaAttribute"))) {
		                        sqlTokens[strIndex] = sqlTokens[strIndex].trim();
		                        if (sqlTokens[strIndex].equals("")) {
		                            strIndex++;
		                            continue;
		                        }

		                        if (sqlTokens[strIndex].equals("REQUIRED")) {
		                            dvaoptval += " REQUIRED";
		                            strIndex++;
		                            continue;
		                        }

		                        else if (sqlTokens[strIndex].equals("INITIALVAL")) {
		                            dvaoptval += " INITIALVAL=" + getElementValue(elements[elemIndex++]);
		                            strIndex++;
		                            continue;
		                        }
		                    }
		                    dvaVal += ":" + dvaoptval.trim().replaceAll("\\s+", ":");
		                    //attributeValues.add(dvaoptval);
		                    break;
		                }
		            }
		            attributeValues.add(dvaVal);
		        } else if (nextToken == 1) {
		            while (strIndex < sqlTokens.length &&
		                   !(sqlTokens[strIndex].trim().equals("dvaAttribute") || sqlTokens[strIndex].trim().equals("evaAttribute"))) {
		                //System.out.println("inside nexttoken = 1 " +strIndex +" " + strings[strIndex]);
		                sqlTokens[strIndex] = sqlTokens[strIndex].trim();
		                if (sqlTokens[strIndex].equals("")) {
		                    strIndex++;
		                    continue;
		                }

		                if (sqlTokens[strIndex].equals("COMMENT")) {
		                    //attributeNames.add("evaComment");
		                    strIndex++;
		                    evaVal += sqlTokens[strIndex++].trim().replaceAll("\'", " ");
		                    // attributeValues.add(strings[strIndex++]);
		                    continue;
		                }
		                if (sqlTokens[strIndex].equals("targetClass")) {
		                    //attributeNames.add("attributeClass");
		                    strIndex++;
		                    evaVal += ":" + sqlTokens[strIndex++].trim();
		                    //attributeValues.add(strings[strIndex++]);
		                    continue;
		                }
		                if (sqlTokens[strIndex].equals("EVAOPTIONS")) {
		                    //attributeNames.add("EVAOPTIONS"); strIndex++;
		                    strIndex++;
		                    String evaoptval = "";
		                    while (strIndex < sqlTokens.length &&
		                           !(sqlTokens[strIndex].trim().equals("dvaAttribute") ||
		                             sqlTokens[strIndex].trim().equals("evaAttribute"))) {
		                        //System.out.println("inside evaOptions while " +strIndex +" " + strings[strIndex]);
		                        sqlTokens[strIndex] = sqlTokens[strIndex].trim();
		                        if (sqlTokens[strIndex].equals("")) {
		                            strIndex++;
		                            continue;
		                        }
		                        if (sqlTokens[strIndex].equals("SV")) {
		                            evaoptval += " SV";
		                            strIndex++;
		                            continue;
		                        }
		                        if (sqlTokens[strIndex].equals("MV")) {
		                            evaoptval += " MV";
		                            strIndex++;
		                            if (sqlTokens[strIndex].trim().equals("MVOPTIONS")) {
		                                String mvoptionsVal = "";
		                                //evaoptval += " MVOPTIONS";
		                                strIndex++;
		                                while (!sqlTokens[strIndex].trim().equals("MVOPTIONSEND")) {
		                                    sqlTokens[strIndex] = sqlTokens[strIndex].trim();
		                                    if (sqlTokens[strIndex].equals("")) {
		                                        strIndex++;
		                                        continue;
		                                    }
		                                    if (sqlTokens[strIndex].equals("DISTINCT")) {
		                                        mvoptionsVal += " DISTINCT";
		                                        strIndex++;
		                                        continue;
		                                    }
		                                    if (sqlTokens[strIndex].equals("MAXVAL")) {
		                                        mvoptionsVal += " MAXVAL(";
		                                        strIndex++;
		                                        mvoptionsVal += sqlTokens[strIndex++] + ")";
		                                        continue;
		                                    }

		                                }
		                                strIndex++;
		                                evaoptval += "=" + mvoptionsVal.trim().replaceAll("\\s", "_");
		                            }
		                            continue;
		                        }
		                        if (sqlTokens[strIndex].equals("INVERSEIS")) {
		                            evaoptval += " INVERSEIS=";
		                            strIndex++;
		                            evaoptval += sqlTokens[strIndex++].trim();
		                            continue;
		                        }
		                    }
		                    evaVal += ":" + evaoptval.trim().replaceAll("\\s+", ":");
		                    break;
		                }
		            }
		            attributeValues.add(evaVal);
		        }
		    }
		}

		linenumber =
				simHelper.executeClass(className, attributeNames, attributeValues, server, uname, pword, linenumber,
						guid, scname);
		    System.out.println("Returned from simHelper.executeClass");
		guid++;
		return "";
	}

	private String parseSimInsert(PyObject[] elements, String server,
			String uname, String pword, SIMHelper simHelper, String[] sqlTokens)
			throws SQLException, JSQLParserException {
		String className = sqlTokens[1];
		//System.out.println("Elements size:" + elements.length);
		//int size = Math.max(strings.length, elements.length);
		int strIndex = 2;
		int elemIndex = 0;
		int nextToken = -1; // 0 - attibute name; 1 - EvaValue; 2 - DvaValue; 3 - Where clause attributes
		String evaValue = "";
		List<String> attributeNames = new ArrayList<String>();
		List<String> attributeValues = new ArrayList<String>();
		String fromClause = "";
		//System.out.println("XXXXXXXXXX"+Arrays.toString(strings));
		while (strIndex < sqlTokens.length) {
		    // System.out.println(strIndex+" "+strings[strIndex] + nextToken);
		    //System.out.println(elemIndex+" "+elements[elemIndex]);
		    sqlTokens[strIndex] = sqlTokens[strIndex].trim();
		    if (sqlTokens[strIndex].equals("")) {
		        strIndex++;
		        continue;
		    }

		    if (sqlTokens[strIndex].equals("attributeToChange")) {
		        nextToken = 0;
		        strIndex++;
		        continue;
		    } else if (sqlTokens[strIndex].equals("evaValue")) {
		        nextToken = 1;
		        strIndex++;
		        evaValue = "";
		        evaValue += sqlTokens[strIndex++];
		        evaValue += " WITH ";
		        while (strIndex < sqlTokens.length &&
		               !(sqlTokens[strIndex].trim().equals("WHERE") || sqlTokens[strIndex].trim().equals("attributeToChange"))) {
		            if (sqlTokens[strIndex].trim().equals("")) {
		                //System.out.println("whattttt");
		                strIndex++;
		                continue;
		            }
		            //System.out.println("strIndex" + strIndex + " str length: " + strings.length + " " + strings[strIndex]);
		            evaValue += " " + sqlTokens[strIndex++];
		            //System.out.println("elementIndex evaValue: " + elemIndex);
		            evaValue += " " + getElementValue(elements[elemIndex++]).replaceAll("'", "");
		        }
		        attributeValues.add(evaValue);
		        continue;
		    } else if (sqlTokens[strIndex].equals("dvaValue")) {
		        nextToken = 2;
		        strIndex++;
		        //System.out.println("elementIndex dvaValue: " + elemIndex);

		        attributeValues.add(getElementValue(elements[elemIndex++]).replaceAll("'", ""));
		        continue;
		    }

		    else if (sqlTokens[strIndex].equals("FROM")) {
		        nextToken = 3;
		        fromClause = "FROM ";
		        strIndex++;
		        fromClause += sqlTokens[strIndex];
		        strIndex++;
		        fromClause += " " + sqlTokens[strIndex];
		        strIndex++;
		        continue;
		    }

		    else {
		        assert (nextToken != -1);
		        if (nextToken == 0) {
		            attributeNames.add(sqlTokens[strIndex++]);
		        } else if (nextToken == 3) {

		            while (!(sqlTokens[strIndex].trim().equals("attributeToChange"))) {
		                //System.out.println("fromClause evaValue: " + elemIndex);
		                fromClause +=
		                        " " + sqlTokens[strIndex++] + "  " + getElementValue(elements[elemIndex++]).replaceAll("'",
		                                                                                                             "");
		            }
		        }
		    }
		}

		linenumber =
				simHelper.executeInsert(className, attributeNames, attributeValues, fromClause, server, uname,
						pword, linenumber, guid);
		guid++;
		return "";
	}

	private String parseSimFrom(PyObject[] elements, String server,
			String uname, String pword, SIMHelper simHelper, String[] sqlTokens)
			throws SQLException, JSQLParserException {
		String className = sqlTokens[1];
		//                        System.out.println("IN FROM "+className);
		List<String> dvaAttribs = new ArrayList<String>();
		List<String> evaAttribs = new ArrayList<String>();
		String whereClause = "";

		assert (sqlTokens[2].contains("RETRIEVE"));
		int index = 3;
		int exprIndex = 0;
		boolean isAttribute = true;
		String currentEvaAttrib = null;
		Map<String, String> whereAttrValues = new HashMap<String, String>();
		while (index < sqlTokens.length) {
		    //                              System.out.println("*"+strings[index]+"*");
		    String s = sqlTokens[index].trim();
		    if (s.equals("") || s.equals("WHERE")) {
		        isAttribute = false;
		        index++;
		        continue;
		    } else if (isAttribute == false) {
		    	String attrName = sqlTokens[index].trim();
		    	String attrValue = elements[exprIndex].toString().trim();
		    	whereAttrValues.put(attrName, attrValue);
		        index++;
		        exprIndex++;
		    } else {
		        assert ((index + 1) <= sqlTokens.length);
		        if (s.startsWith("OF")) {
		            //System.out.println(currentEvaAttrib+" "+strings[index]);
		            assert (currentEvaAttrib != null);
		            if (currentEvaAttrib != null) {
		                currentEvaAttrib += " " + sqlTokens[index];
		            }
		            if (sqlTokens[index + 1].startsWith(" OF") == false) {
		                evaAttribs.add(currentEvaAttrib);
		                currentEvaAttrib = null;
		            }
		        } else if (sqlTokens[index + 1].startsWith(" OF")) {
		            currentEvaAttrib = sqlTokens[index];
		        } else {
		            dvaAttribs.add(sqlTokens[index].trim());
		        }
		        index++;
		    }
		}

		String sparql = "";
		    sparql = simHelper.executeFrom(className, dvaAttribs, evaAttribs, whereAttrValues, server, uname, pword);
		return sparql;
	}

	private String parseSimModify(PyObject[] elements, String server,
			String uname, String pword, SIMHelper simHelper, String[] sqlTokens)
			throws NumberFormatException, SQLException, JSQLParserException {
		
		int limit = Integer.parseInt(elements[0].toString());

		String className = sqlTokens[2];

		int strIndex = 3;
		int elemIndex = 1;
		int nextToken = -1; // 0 - attibute name; 1 - EvaValue; 2 - DvaValue; 3 - Where clause attributes
		String evaValue = "";
		List<String> attributeNames = new ArrayList<String>();
		List<String> attributeValues = new ArrayList<String>();
		Map<String, String> attrValues = new HashMap<String, String>();
		Map<String, String> whereAttrValues = new HashMap<String, String>();
		String attrName = null;
		while (strIndex < sqlTokens.length) {
		    //System.out.println(strIndex+" "+strings[strIndex] + nextToken);
		    //System.out.println(elemIndex+" "+elements[elemIndex]);
		    sqlTokens[strIndex] = sqlTokens[strIndex].trim();
		    if (sqlTokens[strIndex].equals("")) {
		        strIndex++;
		        continue;
		    }

		    if (sqlTokens[strIndex].equals("attributeToChange")) {
		        nextToken = 0;
		        strIndex++;
		        continue;
		    } else if (sqlTokens[strIndex].equals("evaValue")) {
		        nextToken = 1;
		        strIndex++;
		        if (sqlTokens[strIndex].trim().equals("INCLUDE")) {
		            strIndex++;
		            evaValue += " INCLUDE";
		        }
		        evaValue += sqlTokens[strIndex++];
		        evaValue += " WITH ";
		        while (strIndex < sqlTokens.length &&
		               !(sqlTokens[strIndex].trim().equals("WHERE")
		            		   || sqlTokens[strIndex].trim().equals("attributeToChange"))) {
		            evaValue += " " + sqlTokens[strIndex++];
		            evaValue += " " + getElementValue(elements[elemIndex++]);
		        }
		        attrValues.put(attrName, evaValue);
		        continue;
		    } else if (sqlTokens[strIndex].equals("dvaValue")) {
		        nextToken = 2;
		        strIndex++;
		        //attributeValues.add(getElementValue(elements[elemIndex++]));
		        attrValues.put(attrName, getElementValue(elements[elemIndex++]));
		        continue;
		    } else if (sqlTokens[strIndex].equals("WHERE")) {
		        nextToken = 3;
		        strIndex++;
		        continue;
		    } else {
		        assert (nextToken != -1);
		        if (nextToken == 0) {
		            //attributeNames.add(strings[strIndex++]);
		        	attrName = sqlTokens[strIndex++];
		        } else if (nextToken == 3) {
		        	whereAttrValues.put(sqlTokens[strIndex++], getElementValue(elements[elemIndex++]));
		        }
		    }
		}

		simHelper.executeModify(className, attrValues, whereAttrValues, 
				limit, server, uname, pword, linenumber, guid);
		guid++;
		return "";
		
	}

    public static String getElementValue(PyObject element) {
        if (element == null)
            return "";
        if (element.getType().pyGetName().toString() == "str") {
            return "'" + element.toString() + "'";
        }
        return element.toString();
    }

    //helper to convert lists to arrays

    private PyObject[] listtoarray(ArrayList<PyObject> a) {
        PyObject[] results = new PyObject[a.size()];
        int iter = 0;
        for (PyObject pt : a) {
            results[iter] = pt;
            iter++;
        }
        return results;

    }
    //end sql stuff

    public PyTuple(PyType subtype, PyObject[] elements, boolean copy) {
        super(subtype);

        if (copy) {
            array = new PyObject[elements.length];
            System.arraycopy(elements, 0, array, 0, elements.length);
        } else {
            array = elements;
        }
    }

    private static PyTuple fromArrayNoCopy(PyObject[] elements) {
        return new PyTuple(elements, false);
    }

    List<PyObject> getList() {
        if (cachedList == null) {
            cachedList = Arrays.asList(array);
        }
        return cachedList;
    }

    @ExposedNew
    final static PyObject tuple_new(PyNewWrapper new_, boolean init, PyType subtype, PyObject[] args,
                                    String[] keywords) {
        ArgParser ap = new ArgParser("tuple", args, keywords, new String[] { "sequence" }, 0);
        PyObject S = ap.getPyObject(0, null);
        if (new_.for_type == subtype) {
            if (S == null) {
                return EMPTY_TUPLE;
            }
            if (S instanceof PyTupleDerived) {
                return new PyTuple(((PyTuple)S).getArray());
            }
            if (S instanceof PyTuple) {
                return S;
            }
            return fromArrayNoCopy(Py.make_array(S));
        } else {
            if (S == null) {
                return new PyTupleDerived(subtype, Py.EmptyObjects);
            }
            return new PyTupleDerived(subtype, Py.make_array(S));
        }
    }

    /**
     * Return a new PyTuple from an iterable.
     *
     * Raises a TypeError if the object is not iterable.
     *
     * @param iterable an iterable PyObject
     * @return a PyTuple containing each item in the iterable
     */
    public static PyTuple fromIterable(PyObject iterable) {
        return fromArrayNoCopy(Py.make_array(iterable));
    }

    protected PyObject getslice(int start, int stop, int step) {
        if (step > 0 && stop < start) {
            stop = start;
        }
        int n = sliceLength(start, stop, step);
        PyObject[] newArray = new PyObject[n];

        if (step == 1) {
            System.arraycopy(array, start, newArray, 0, stop - start);
            return fromArrayNoCopy(newArray);
        }
        for (int i = start, j = 0; j < n; i += step, j++) {
            newArray[j] = array[i];
        }
        return fromArrayNoCopy(newArray);
    }

    protected PyObject repeat(int count) {
        if (count < 0) {
            count = 0;
        }
        int size = size();
        if (size == 0 || count == 1) {
            if (getType() == TYPE) {
                // Since tuples are immutable, we can return a shared copy in this case
                return this;
            }
            if (size == 0) {
                return EMPTY_TUPLE;
            }
        }

        int newSize = size * count;
        if (newSize / size != count) {
            throw Py.MemoryError("");
        }

        PyObject[] newArray = new PyObject[newSize];
        for (int i = 0; i < count; i++) {
            System.arraycopy(array, 0, newArray, i * size, size);
        }
        return fromArrayNoCopy(newArray);
    }

    @Override
    public int __len__() {
        return tuple___len__();
    }

    @ExposedMethod(doc = BuiltinDocs.tuple___len___doc)
    final int tuple___len__() {
        return size();
    }

    @ExposedMethod(doc = BuiltinDocs.tuple___contains___doc)
    final boolean tuple___contains__(PyObject o) {
        return super.__contains__(o);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.tuple___ne___doc)
    final PyObject tuple___ne__(PyObject o) {
        return super.__ne__(o);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.tuple___eq___doc)
    final PyObject tuple___eq__(PyObject o) {
        return super.__eq__(o);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.tuple___gt___doc)
    final PyObject tuple___gt__(PyObject o) {
        return super.__gt__(o);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.tuple___ge___doc)
    final PyObject tuple___ge__(PyObject o) {
        return super.__ge__(o);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.tuple___lt___doc)
    final PyObject tuple___lt__(PyObject o) {
        return super.__lt__(o);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.tuple___le___doc)
    final PyObject tuple___le__(PyObject o) {
        return super.__le__(o);
    }

    @Override
    public PyObject __add__(PyObject generic_other) {
        return tuple___add__(generic_other);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.tuple___add___doc)
    final PyObject tuple___add__(PyObject generic_other) {
        PyTuple sum = null;
        if (generic_other instanceof PyTuple) {
            PyTuple other = (PyTuple)generic_other;
            PyObject[] newArray = new PyObject[array.length + other.array.length];
            System.arraycopy(array, 0, newArray, 0, array.length);
            System.arraycopy(other.array, 0, newArray, array.length, other.array.length);
            sum = fromArrayNoCopy(newArray);
        }
        return sum;
    }

    @Override
    public PyObject __mul__(PyObject o) {
        return tuple___mul__(o);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.tuple___mul___doc)
    final PyObject tuple___mul__(PyObject o) {
        if (!o.isIndex()) {
            return null;
        }
        return repeat(o.asIndex(Py.OverflowError));
    }

    @Override
    public PyObject __rmul__(PyObject o) {
        return tuple___rmul__(o);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.tuple___rmul___doc)
    final PyObject tuple___rmul__(PyObject o) {
        if (!o.isIndex()) {
            return null;
        }
        return repeat(o.asIndex(Py.OverflowError));
    }

    @Override
    public PyObject __iter__() {
        return tuple___iter__();
    }

    @ExposedMethod(doc = BuiltinDocs.tuple___iter___doc)
    public PyObject tuple___iter__() {
        return new PyFastSequenceIter(this);
    }

    @ExposedMethod(defaults = "null", doc = BuiltinDocs.tuple___getslice___doc)
    final PyObject tuple___getslice__(PyObject s_start, PyObject s_stop, PyObject s_step) {
        return seq___getslice__(s_start, s_stop, s_step);
    }

    @ExposedMethod(doc = BuiltinDocs.tuple___getitem___doc)
    final PyObject tuple___getitem__(PyObject index) {
        PyObject ret = seq___finditem__(index);
        if (ret == null) {
            throw Py.IndexError("index out of range: " + index);
        }
        return ret;
    }

    @ExposedMethod(doc = BuiltinDocs.tuple___getnewargs___doc)
    final PyTuple tuple___getnewargs__() {
        return new PyTuple(new PyTuple(getArray()));
    }

    @Override
    public PyTuple __getnewargs__() {
        return tuple___getnewargs__();
    }

    @Override
    public int hashCode() {
        return tuple___hash__();
    }

    @ExposedMethod(doc = BuiltinDocs.tuple___hash___doc)
    final int tuple___hash__() {
        // strengthened hash to avoid common collisions. from CPython
        // tupleobject.tuplehash. See http://bugs.python.org/issue942952
        int y;
        int len = size();
        int mult = 1000003;
        int x = 0x345678;
        while (--len >= 0) {
            y = array[len].hashCode();
            x = (x ^ y) * mult;
            mult += 82520 + len + len;
        }
        return x + 97531;
    }

    private String subobjRepr(PyObject o) {
        if (o == null) {
            return "null";
        }
        //if Long and ASP then need to remove L that apears at the end of number
        if ((PyLong.class.isInstance(o)) && aspFlag)
            return o.__repr__().toString().replaceFirst("L", "");
        return o.__repr__().toString();
    }

    @Override
    public String toString() {
        return tuple___repr__();
    }

    @ExposedMethod(doc = BuiltinDocs.tuple___repr___doc)
    final String tuple___repr__() {
        StringBuilder buf;
        if ((!aspFlag || makeRDF) && array != null) {
            //Process non asp and RDF's
            //Process table
            buf = new StringBuilder("(");
            for (int i = 0; i < array.length - 1; i++) {
                buf.append(subobjRepr(array[i]));
                buf.append(", ");
            }
            if (array.length > 0) {
                buf.append(subobjRepr(array[array.length - 1]));
            }
            if (array.length == 1) {
                buf.append(",");
            }
            buf.append(")");
        } else {
            //If doing remote and ASP
            if (remote) {
                buf = new StringBuilder("'");
                for (int i = 0; i < array.length - 1; i++) {
                    buf.append(subobjRepr(array[i]).replaceAll("'", ""));
                    buf.append(", ");
                }
                if (array.length > 0) {
                    buf.append(subobjRepr(array[array.length - 1]).replaceAll("'", ""));
                }
                if (array.length == 1) {
                    buf.append(",");
                }
                buf.append("'");
            } else {

                //Return tuples for use of ASP facts
                buf = new StringBuilder("");
                if (array != null) {
                    for (int i = 0; i < array.length; i++) {
                        buf.append(subobjRepr(array[i]).replaceFirst("'SUB''PRED''OBJ'", ""));
                    }
                }

            }
        }
        return buf.toString();
    }

    public List subList(int fromIndex, int toIndex) {
        if (fromIndex < 0 || toIndex > size()) {
            throw new IndexOutOfBoundsException();
        } else if (fromIndex > toIndex) {
            throw new IllegalArgumentException();
        }
        PyObject elements[] = new PyObject[toIndex - fromIndex];
        for (int i = 0, j = fromIndex; i < elements.length; i++, j++) {
            elements[i] = array[j];
        }
        return new PyTuple(elements);
    }

    public Iterator iterator() {
        return new Iterator() {

            private final Iterator<PyObject> iter = getList().iterator();

            public void remove() {
                throw new UnsupportedOperationException();
            }

            public boolean hasNext() {
                return iter.hasNext();
            }

            public Object next() {
                return iter.next().__tojava__(Object.class);
            }
        };
    }

    public boolean add(Object o) {
        throw new UnsupportedOperationException();
    }

    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    public boolean addAll(Collection coll) {
        throw new UnsupportedOperationException();
    }

    public boolean removeAll(Collection coll) {
        throw new UnsupportedOperationException();
    }

    public boolean retainAll(Collection coll) {
        throw new UnsupportedOperationException();
    }

    public void clear() {
        throw new UnsupportedOperationException();
    }

    public Object set(int index, Object element) {
        throw new UnsupportedOperationException();
    }

    public void add(int index, Object element) {
        throw new UnsupportedOperationException();
    }

    public Object remove(int index) {
        throw new UnsupportedOperationException();
    }

    public boolean addAll(int index, Collection c) {
        throw new UnsupportedOperationException();
    }

    public ListIterator listIterator() {
        return listIterator(0);
    }

    public ListIterator listIterator(final int index) {
        return new ListIterator() {

            private final ListIterator<PyObject> iter = getList().listIterator(index);

            public boolean hasNext() {
                return iter.hasNext();
            }

            public Object next() {
                return iter.next().__tojava__(Object.class);
            }

            public boolean hasPrevious() {
                return iter.hasPrevious();
            }

            public Object previous() {
                return iter.previous().__tojava__(Object.class);
            }

            public int nextIndex() {
                return iter.nextIndex();
            }

            public int previousIndex() {
                return iter.previousIndex();
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }

            public void set(Object o) {
                throw new UnsupportedOperationException();
            }

            public void add(Object o) {
                throw new UnsupportedOperationException();
            }
        };
    }

    protected String unsupportedopMessage(String op, PyObject o2) {
        if (op.equals("+")) {
            return "can only concatenate tuple (not \"{2}\") to tuple";
        }
        return super.unsupportedopMessage(op, o2);
    }

    public void pyset(int index, PyObject value) {
        throw Py.TypeError("'tuple' object does not support item assignment");
    }

    @Override
    public boolean contains(Object o) {
        return getList().contains(Py.java2py(o));
    }

    @Override
    public boolean containsAll(Collection c) {
        if (c instanceof PyList) {
            return getList().containsAll(((PyList)c).getList());
        } else if (c instanceof PyTuple) {
            return getList().containsAll(((PyTuple)c).getList());
        } else {
            return getList().containsAll(new PyList(c));
        }
    }

    public int count(PyObject value) {
        return tuple_count(value);
    }

    @ExposedMethod(doc = BuiltinDocs.tuple_count_doc)
    final int tuple_count(PyObject value) {
        int count = 0;
        for (PyObject item : array) {
            if (item.equals(value)) {
                count++;
            }
        }
        return count;
    }

    public int index(PyObject value) {
        return index(value, 0);
    }

    public int index(PyObject value, int start) {
        return index(value, start, size());
    }

    public int index(PyObject value, int start, int stop) {
        return tuple_index(value, start, stop);
    }

    @ExposedMethod(defaults = { "null", "null" }, doc = BuiltinDocs.tuple_index_doc)
    final int tuple_index(PyObject value, PyObject start, PyObject stop) {
        int startInt = start == null ? 0 : PySlice.calculateSliceIndex(start);
        int stopInt = stop == null ? size() : PySlice.calculateSliceIndex(stop);
        return tuple_index(value, startInt, stopInt);
    }

    final int tuple_index(PyObject value, int start, int stop) {
        int validStart = boundToSequence(start);
        int validStop = boundToSequence(stop);
        for (int i = validStart; i < validStop; i++) {
            if (array[i].equals(value)) {
                return i;
            }
        }
        throw Py.ValueError("tuple.index(x): x not in list");
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (other instanceof PyObject) {
            return _eq((PyObject)other).__nonzero__();
        }
        if (other instanceof List) {
            return other.equals(this);
        }
        return false;
    }

    @Override
    public Object get(int index) {
        return array[index].__tojava__(Object.class);
    }

    @Override
    public PyObject[] getArray() {
        return array;
    }

    @Override
    public int indexOf(Object o) {
        return getList().indexOf(Py.java2py(o));
    }

    @Override
    public boolean isEmpty() {
        return array.length == 0;
    }

    @Override
    public int lastIndexOf(Object o) {
        return getList().lastIndexOf(Py.java2py(o));
    }

    @Override
    public void pyadd(int index, PyObject element) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean pyadd(PyObject o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public PyObject pyget(int index) {
        return array[index];
    }

    @Override
    public void remove(int start, int stop) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int size() {
        return array.length;
    }

    @Override
    public Object[] toArray() {
        Object[] converted = new Object[array.length];
        for (int i = 0; i < array.length; i++) {
            converted[i] = array[i].__tojava__(Object.class);
        }
        return converted;
    }

    @Override
    public Object[] toArray(Object[] converted) {
        Class<?> type = converted.getClass().getComponentType();
        if (converted.length < array.length) {
            converted = (Object[])Array.newInstance(type, array.length);
        }
        for (int i = 0; i < array.length; i++) {
            converted[i] = type.cast(array[i].__tojava__(type));
        }
        if (array.length < converted.length) {
            for (int i = array.length; i < converted.length; i++) {
                converted[i] = null;
            }
        }
        return converted;
    }
}
