
public class Function {
	
	private String name = null;
	private int argc = 0;
	private boolean returns = false;
	private boolean integer = false;
	
	public Function(String name, int argc, boolean returns)
	{
		this.name = name;
		this.argc = argc;
		this.returns = returns;
	}
	
	public String getName()
	{
		return name;
	}
	
	public int getArgc()
	{
		return argc;
	}
	
	public boolean isReturns()
	{
		return returns;
	}
	
	public void setIsInteger(boolean integer)
	{
		this.integer = integer;
	}
	
	public boolean isInteger()
	{
		return integer;
	}
}
