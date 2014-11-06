
public class Variable {

	private int identifier = 0;
	private boolean integer = false;
	
	public Variable(int identifier, boolean integer)
	{
		this.identifier = identifier;
		this.integer = integer;
	}
	
	public int getIdentifier()
	{
		return identifier;
	}
	
	public boolean isInteger()
	{
		return integer;
	}
}
