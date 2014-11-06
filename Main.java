import jasmin.ClassFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

public class Main extends MoneyBaseVisitor<String> {
	
	private Scope currentScope = null;
	private Map<String, Function> functionTable = new HashMap<>();
	
	private int labelCount = 0;
	private int scopeCount = 0;
	private static String className = null;
	private static String fileName = null;
	private static boolean debug = false;
	
	private static void usage()
	{
		System.out.println("Usage: java -jar Compiler.jar filename debugTrueOrFalse");
	}
	
	public static void main(String []args)
	{
		if (args.length < 1)
		{
			usage();
			System.exit(0);
		}
		
		String jasminSource = null;
		try {
			
			if (args.length >= 2)
			{
				debug = Boolean.valueOf(args[1]);
			}
			
			jasminSource = parse(args[0]);
		} catch (IOException e) {

			e.printStackTrace();
			System.out.println("Error while creating source file!");
			System.exit(0);
		}
		
		if (debug)
		{
			System.out.println(jasminSource);
			System.out.println("---------------");	
		}
		
		createClassFile(jasminSource);
		executeClass();
	}
	
	private static String parse(String file) throws IOException
	{
		File f = new File(file);
		fileName = f.getName();
		className = fileName.substring(0, fileName.indexOf('.'));

		// create a CharStream that reads from standard input
		FileInputStream fileIn = new FileInputStream(file);
		ANTLRInputStream input = new ANTLRInputStream(fileIn);
		// create a lexer that feeds off of input CharStream
		MoneyLexer lexer = new MoneyLexer(input);
		// create a buffer of tokens pulled from the lexer
		CommonTokenStream tokens = new CommonTokenStream(lexer);
		// create a parser that feeds off the tokens buffer
		MoneyParser parser = new MoneyParser(tokens);
		ParseTree tree = parser.start(); // begin parsing at init rule
		
		String jasminClass = new Main().visit(tree);
		return jasminClass;
	}

	private static void createClassFile(String jasminSource)
	{
		try {
			ClassFile classFile = new ClassFile();
			classFile.readJasmin(new StringReader(jasminSource), "", !debug);
			
			String outputFileName = Main.class.getResource("/").getFile() + classFile.getClassName() + ".class";
			outputFileName = outputFileName.replace("%20", " ");
			
			FileOutputStream classOut = new FileOutputStream(outputFileName);
			classFile.write(classOut);
			classOut.close();
		} catch (Exception e)
		{
			e.printStackTrace();
			System.err.println("Error while creating class file!");
			System.exit(0);
		}
	}
		
	private static void executeClass() 
	{
		try {
			
			Class<?> myClass = Class.forName(className);
			System.out.println(myClass.getMethod("main", String[].class));
			System.out.println("invoking main...");
			myClass.getMethod("main", String[].class).invoke(null, (Object)null);
			System.out.println("done");
		} catch (Exception e)
		{
			e.printStackTrace();
			System.err.println("Error while executing class!");
			System.exit(0);
		}
	}

	@Override
	public String visitScope(MoneyParser.ScopeContext ctx) {
	
		currentScope = new Scope(currentScope);
		
		if (debug)
			currentScope.setScopeLabel(scopeCount++);
		
		String ret = "";
		if (debug)
			ret += "scope_" + currentScope.getScopeLabel() + ":\n";
		
		
		if (ctx.parent instanceof MoneyParser.FunctionContext)
		{
			ret += visit(((MoneyParser.FunctionContext)ctx.parent).params); // register parameters
		}
		
		
		for (int i = 0; i != ctx.getChildCount(); ++i)
		{
			ParseTree p = ctx.getChild(i);
			if (p instanceof MoneyParser.ActionContext)
			{
				ret += visit(p);
			}
		}

		if (debug)
			ret += "scope_end_" + currentScope.getScopeLabel() + ":\n";
		
		currentScope = currentScope.getParent();
		
		return ret;
	}

	@Override
	public String visitCondition(MoneyParser.ConditionContext ctx) {
		
		String ret = "";
		ret += visit(ctx.arg1);
		ret += visit(ctx.arg2);
		
		String cmp = ctx.cmp.getText();
		if (cmp.equals("=="))
		{
			ret += "if_icmpeq ";
		} else if (cmp.equals("!="))
		{
			ret += "if_icmpne ";
		} else if (cmp.equals("<"))
		{
			ret += "if_icmplt ";
		} else if (cmp.equals(">"))
		{
			ret += "if_icmpgt ";
		} else if (cmp.equals("<="))
		{
			ret += "if_icmple ";
		} else if (cmp.equals(">="))
		{
			ret += "if_icmpge ";
		}
		
		currentScope.subStack();
		
		return ret;
	}

	@Override
	public String visitPrint(MoneyParser.PrintContext ctx) {

		String ret = "";
		boolean isInteger = true;
		
		if (debug)
			ret += ".line " + ctx.getStart().getLine() + "\n";
		
		if (ctx.expr.getChildCount() == 1) // is single token, function call or read call
		{
			if (ctx.expr.getChild(0) instanceof MoneyParser.TokenContext)
			{
				if (ctx.expr.getText().startsWith("'"))
					isInteger = false;
				else if (ctx.expr.getText().matches("[0-9]+"))
				{
					isInteger = true;
				} else
				{
					isInteger = currentScope.getVariable(ctx.expr.getText()).isInteger();
				}
				
			} else if (ctx.expr.getChild(0) instanceof MoneyParser.FunctionCallContext)
			{
				MoneyParser.FunctionCallContext funcCall = (MoneyParser.FunctionCallContext)ctx.expr.getChild(0);
				Function f = functionTable.get(funcCall.name.getText());
				
				if (f.isReturns())
				{
					isInteger = f.isInteger();
				} else
				{
					System.err.println("ERROR! Function " + f.getName() + " DOES NOT RETURN ANY VALUE!");
					System.exit(0);
				}
			}
		}
		
		ret += visit(ctx.expr);
		ret += "invokestatic " + className + "/print(" + (isInteger ? "I" : "C") + ")V\n";

		currentScope.subStack();
		
		return ret;
	}
	
	@Override
	public String visitRead(MoneyParser.ReadContext ctx) {
		
		String ret = "";
		
		if (debug)
			ret += ".line " + ctx.getStart().getLine() + "\n";
		
		if (ctx.str != null)
		{
			System.out.println(ctx.str.getText());
			currentScope.addStack();
			ret += "ldc " + ctx.str.getText() + "\n";
			ret += "invokestatic " + className + "/print(Ljava/lang/String;)V\n";
			currentScope.subStack();
		}
			
		ret += "invokestatic " + className + "/read()I\n";
		currentScope.addStack();
		
		return ret;
	}

	@Override
	public String visitFunction(MoneyParser.FunctionContext ctx) {

		String name = ctx.name.getText();
		int parameterCount = ctx.params.getText().isEmpty() ? 0 : ctx.params.getText().split(",").length;
		Function f = new Function(name, parameterCount, !"bankrupt".equals(ctx.ret.getText()));
		f.setIsInteger(ctx.ret.getText().equals("dollar"));
		
		//currentScope.addOffset(f.getArgc());
		
		if (functionTable.get(name) != null)
		{
			System.err.println("ERROR, Function " + name + " already exists");
			System.exit(0);
		} else
		{
			functionTable.put(name, f);
		}

		String ret = "";
		ret += "\n\n.method public static " + f.getName() + "(";
		
		for (int i = 0; i != f.getArgc(); ++i)
		{
			ret += "I";
		}
		
		ret += ")" + (!f.isReturns() ? "V" : (f.isInteger() ? "I" : "C")) +"\n" +
				".limit stack %d\n.limit locals %d\n";
		
		if (debug)
			ret += ".line " + ctx.getStart().getLine() + "\n";
		
		ret += visitScope(ctx.code) +
				"return" + "\n" + //add return value here, fix parser (say!)
				".end method";
		
		return String.format(ret, currentScope.getStack(), currentScope.getLocals() + 1);
	}

	@Override
	public String visitFunctionCall(MoneyParser.FunctionCallContext ctx) {
		
		Function f = functionTable.get(ctx.name.getText());
		
		if (f == null)
		{
			System.err.println("FUNCTION " + ctx.name.getText() + " NOT FOUND!");
			System.exit(0);
			return null;
		}
		
		int argc = ctx.params.getText().isEmpty() ? 0 : ctx.params.getText().split(",").length;
		if (argc != f.getArgc())
		{
			System.out.println("FUNCTION CALL FOR " + f.getName() + " HAS ERRORS!");
			System.exit(0);
			return null;
		}
		
		String ret = "";
		
		if (debug)
			ret += ".line " + ctx.getStart().getLine() + "\n";
		
		ret += visit(ctx.params) + "invokestatic " + className + "." + f.getName() + "("; 
		
		for (int i = 0; i != argc; ++i)
		{
			ret += "I";
		}
		
		ret += ")";
		ret += f.isReturns() ? (f.isInteger() ? "I" : "C") : "V";
		ret += "\n";
		
		for (int i = 0; i != argc; ++i)
		{
			currentScope.subStack();
		}
		
		return ret;
	}

	@Override
	public String visitStart(MoneyParser.StartContext ctx) {
		
		String ret = ".source " + fileName + "\n" +
					".class public " + className + "\n" +
					".super java/lang/Object";
		
		for (int i = 0; i != ctx.getChildCount(); ++i)
		{
			ParseTree node = ctx.getChild(i);
			if (node instanceof MoneyParser.CommandContext)
			{
				node = node.getChild(0);

				if (node instanceof MoneyParser.FunctionContext)
				{
					currentScope = new Scope(null);
					ret += visitFunction((MoneyParser.FunctionContext)node);
				}
			}
		}
		
		ret += "\n\n.method public static print(I)V\n";
		ret += ".limit stack 2\n.limit locals 2\n";
		ret += "getstatic java/lang/System/out Ljava/io/PrintStream;\n";
		ret += "iload 0";
		ret += "\ninvokevirtual java/io/PrintStream/println(I)V\n";
		ret += "return\n.end method";
		
		ret += "\n\n.method public static print(C)V\n";
		ret += ".limit stack 2\n.limit locals 2\n";
		ret += "getstatic java/lang/System/out Ljava/io/PrintStream;\n";
		ret += "iload 0";
		ret += "\ninvokevirtual java/io/PrintStream/println(C)V\n";
		ret += "return\n.end method";
		
		ret += "\n\n.method public static print(Ljava/lang/String;)V\n";
		ret += ".limit stack 2\n.limit locals 2\n";
		ret += "getstatic java/lang/System/out Ljava/io/PrintStream;\n";
		ret += "aload 0";
		ret += "\ninvokevirtual java/io/PrintStream/println(Ljava/lang/String;)V\n";
		ret += "return\n.end method";
		
		ret += "\n\n.method public static read()I\n";
		ret += ".limit stack 4\n.limit locals 0\n";
		ret += "new java/util/Scanner\n";
		ret += "dup\n";
		ret += "getstatic java/lang/System/in Ljava/io/InputStream;\n";
		ret += "invokespecial java/util/Scanner/<init>(Ljava/io/InputStream;)V\n";
		ret += "invokevirtual java/util/Scanner/nextInt()I\n";
		ret += "ireturn\n.end method";
		
		ret += "\n\n.method public static main([Ljava/lang/String;)V\n" +
		".limit stack %d\n.limit locals %d\n"; // insert values from Scope class after parsing
		
		currentScope = new Scope(null);
		currentScope.putVariable("args", true);
		currentScope.setScopeLabel(scopeCount++);
		
		ret += "scope_" + currentScope.getScopeLabel() + ":\n";
		
		for (int i = 0; i != ctx.getChildCount(); ++i)
		{
			ParseTree node = ctx.getChild(i);
			if (node instanceof MoneyParser.CommandContext)
			{
				node = node.getChild(0);
				
				if (!(node instanceof MoneyParser.FunctionContext))
				{
					ret += visit(node);
				}
			}
		}
		
		ret += "scope_end_" + currentScope.getScopeLabel() + ":\n";
		ret += "return\n.end method";
		
		return String.format(ret, currentScope.getStack(), currentScope.getLocals());
	}

	@Override
	public String visitToken(MoneyParser.TokenContext ctx) {

		String ret = "";
		if (ctx.getText().matches("[0-9]+"))
		{
			ret += "ldc " + ctx.getText() + "\n";
		}
		else if (ctx.getText().startsWith("'"))
		{
			ret += "ldc " + Integer.valueOf(ctx.getText().charAt(1)) + "\n";
		} else
		{
			Variable identifier = currentScope.getVariable(ctx.getText());
			if (identifier == null)
			{
				System.err.println("VAR " + ctx.getText() + " DOES NOT EXIST IN CONTEXT");
				System.exit(0);
			}
			
			ret += "iload " + identifier.getIdentifier() + "\n";
		}
		
		currentScope.addStack();
		
		return ret;
	}

	@Override
	public String visitCommand(MoneyParser.CommandContext ctx) {
		
		return visit(ctx.getChild(0));
	}

	@Override
	public String visitAction(MoneyParser.ActionContext ctx) {
		return visit(ctx.getChild(0));
	}

	@Override
	public String visitVarInit(MoneyParser.VarInitContext ctx) {

		// register var
		String identifier = ctx.assignment.var.getText();
		if (!currentScope.varExists(identifier))
		{
			currentScope.putVariable(identifier, ctx.DATATYPE().getText().equals("dollar"));
		} else
		{
			System.err.println("ERROR, VAR " + identifier + " EXISTS AT LINE " + ctx.getStart().getLine() + "!");
			System.exit(0);
		}
		
		String ret = "";
		
		if (debug)
			ret += ".var " + currentScope.getVariable(identifier).getIdentifier() + 
				" is " + identifier + " " + (currentScope.getVariable(identifier).isInteger() ? "I" : "C") + " from scope_" + currentScope.getScopeLabel()
				+ " to scope_end_" + currentScope.getScopeLabel() + "\n";
		
		return ret + visit(ctx.assignment);
	}

	@Override
	public String visitUntilBlock(MoneyParser.UntilBlockContext ctx) {

		int tempLabelCount = labelCount++;
		String condition_label = "condition_label_" + tempLabelCount;
		String loop_label = "loop_label_" + tempLabelCount;
		String exit_label = "exit_label_" + tempLabelCount;
		
		String ret = "";
		
		if (debug)
			ret += ".line " + ctx.getStart().getLine() + "\n";
		
		ret += condition_label + ":\n";
		ret += visitCondition(ctx.cond);
		ret += loop_label + "\n";
		ret += "goto " + exit_label + "\n";
		ret += loop_label + ":\n";
		
		ret += visitScope(ctx.code);
		
		ret += "goto " + condition_label + "\n";
		ret += exit_label + ":\n";
		
		return ret;
	}

	@Override
	public String visitIfBlock(MoneyParser.IfBlockContext ctx) {
		
		int tempLabelCount = labelCount++;
		String if_label = "if_label_" + tempLabelCount;
		String exit_label = "exit_label_" + tempLabelCount;
		String else_label = "else_label_" + tempLabelCount;
		
		String ret = "";
		
		if (debug)
			ret += ".line " + ctx.getStart().getLine() + "\n";
		
		ret += visitCondition(ctx.cond);
		ret += if_label + "\n";
		ret += "goto " + else_label + "\n";
		ret += if_label + ":\n";
		
		ret += visitScope(ctx.code);
		ret += "goto " + exit_label + "\n";
		
		ret += else_label +":\n";

		if (ctx.getChildCount() > 4)
		{
			ret += visit(ctx.getChild(5));
		}
		
		ret += exit_label + ":\n";
		
		return ret;
	}

	@Override
	public String visitVarAssignment(MoneyParser.VarAssignmentContext ctx) {

		Variable var = currentScope.getVariable(ctx.var.getText());
		
		if (var == null)
		{
			System.err.println("VAR " + ctx.var.getText() + " DOES NOT EXIST!");
			System.exit(0);
		}
		
		String ret = "";
		
		if (debug)
			ret += ".line " + ctx.getStart().getLine() + "\n";
		
		ret += visit(ctx.val);
		ret += "istore " + var.getIdentifier() + "\n";
		
		return ret;
	}

	@Override
	public String visitBlock(MoneyParser.BlockContext ctx) {

		return visit(ctx.getChild(0));
	}

	@Override
	public String visitParameterDefinitions(MoneyParser.ParameterDefinitionsContext ctx) {

		String ret = "";
		for (int i = 0; i != ctx.getChildCount(); ++i)
		{			
			if (ctx.getChild(i) instanceof MoneyParser.ParameterDefinitionContext)
			{
				ret += visit(ctx.getChild(i));
			}
		}
		
		return ret;
	}
	
	@Override
	public String visitParameterDefinition(MoneyParser.ParameterDefinitionContext ctx)
	{
		String ret = "";
		
		currentScope.putVariable(ctx.name.getText(), ctx.type.getText().equals("dollar"));
		
		if (debug)
			ret += ".var " + currentScope.getVariable(ctx.name.getText()).getIdentifier() + 
				" is " + ctx.name.getText() + " " + (ctx.type.getText().equals("dollar") ? "I" : "C") + " from scope_" + currentScope.getScopeLabel()
				+ " to scope_end_" + currentScope.getScopeLabel() + "\n";
		
		return ret;
	}

	@Override
	public String visitRepeatUntilBlock(MoneyParser.RepeatUntilBlockContext ctx) {

		String loop_label = "loop_label_" + labelCount++;
		String ret = "";
		
		if (debug)
			ret += ".line " + ctx.getStart().getLine() + "\n";
		
		ret += loop_label +":\n";
		ret += visitScope(ctx.code);
		
		ret += visitCondition(ctx.cond);
		ret += loop_label + "\n";
		
		return ret;	
	}

	@Override
	public String visitValues(MoneyParser.ValuesContext ctx) {

		String ret = "";
		
		if (ctx.getChildCount() > 1) // values (+, -, *, /) values
		{
			ret += visit(ctx.getChild(0));
			ret += visit(ctx.getChild(2));
			
			String operator = ctx.getChild(1).getText();
			
			if ("+".equals(operator))
			{
				ret += "iadd\n";
			} else if ("-".equals(operator))
			{
				ret += "isub\n";
			} else if ("*".equals(operator))
			{
				ret += "imul\n";
			} else
			{
				ret += "idiv\n";
			}
			
			currentScope.subStack();
			
		} else
		{
			ret += visit(ctx.getChild(0));
		}

		return ret;
	}
	
	@Override
	public String visitReturnStatement(MoneyParser.ReturnStatementContext ctx)
	{
		return ctx.expr == null ? "return\n" : visit(ctx.expr) + "ireturn\n";
	}

	@Override
	public String visitParameters(MoneyParser.ParametersContext ctx) {

		String ret = "";
		for (int i = 0; i != ctx.getChildCount(); ++i)
		{
			if (ctx.getChild(i) instanceof MoneyParser.ValuesContext)
			{
				ret += visit(ctx.getChild(i));
			}
		}
		
		return ret;
	}
}
