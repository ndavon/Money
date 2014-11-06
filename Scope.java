import java.util.HashMap;
import java.util.Map;

public class Scope {

	private Scope parent;
	private Map<String, Variable> variables = new HashMap<>();
	private int baseOffset = 0;
	private int locals = 0;
	private int stack = 0;
	private int maxStack = 0;
	private int scopeLabel = 0;
	
	public Scope(Scope parent) {
		this.parent = parent;
		baseOffset = getOffset();
	}
	
	public Scope getParent() {
		return parent;
	}

	public void putVariable(String varName, boolean isInt)
	{
		variables.put(varName, new Variable(variables.size() + baseOffset, isInt));
		addLocal();
	}
	
	public Variable getVariable(String varName) {
		
		Variable result = variables.get(varName);
		if(result == null) {
			
			if(parent == null) {
				
				return null;
			}
			
			return parent.getVariable(varName);
		}
		
		return result;
	}
	
	public boolean varExists(String varName)
	{
		return variables.get(varName) != null;
	}
	
	private int getOffset()
	{
		if (parent == null)
		{
			return 0;
		} else
		{
			return parent.variables.size() + baseOffset;
		}
	}
	
	/*
	public void addOffset(int offset)
	{
		this.baseOffset += offset;
	}
	*/
	
	public void addLocal()
	{
		if (parent == null)
			++locals;
		else
			parent.addLocal();
	}
	
	public int getLocals()
	{
		if (parent == null)
			return locals;
		else
			return parent.getLocals();
	}
	
	public void addStack()
	{
		if (parent == null)
		{	
			++stack;
			if (stack > maxStack)
				maxStack = stack;
		}else
			parent.addStack();
	}
	
	public void subStack()
	{
		if (parent == null)
			--stack;
		else
			parent.subStack();
	}
	
	public int getStack()
	{
		if (parent == null)
			return maxStack;
		else
			return parent.getStack();
	}

	public int getScopeLabel() 
	{	
		return scopeLabel;
	}

	public void setScopeLabel(int scopeLabel) 
	{	
		this.scopeLabel = scopeLabel;
	}
}
