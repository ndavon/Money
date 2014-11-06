grammar Money;

start: command* ;
command : function | action ; 
action :  (block | print '!'| varAssignment '!' | varInit '!'| functionCall '!' | returnStatement '!');
scope : SCOPE_BEGIN action* SCOPE_END ; 
print : 'print' expr=values;
read : 'read' str=STRING? ; 
block : ifBlock | untilBlock | repeatUntilBlock | scope;

token : IDENTIFIER | VALUE ;
functionCall : name=IDENTIFIER '(' params=parameters ')' ;
returnStatement : 'pay' expr=values? ;

values : values ('*' | '/') values
		| values ('+' | '-') values
 		| token | functionCall  | read ;

function : name=IDENTIFIER '(' params=parameterDefinitions ')' '->' ret=(DATATYPE | 'bankrupt') code=scope ;

parameterDefinitions : (parameterDefinition)? (',' parameterDefinition)* ;
parameters : values? (',' values)* ;
parameterDefinition : type=DATATYPE name=IDENTIFIER ;

varInit : DATATYPE assignment=varAssignment ;
varAssignment :  var=IDENTIFIER assign=ASSIGN val=values ;

ifBlock : IF_CLOSURE cond=condition IF_CLOSURE code=scope ('...' scope)? ;
untilBlock : UNTIL_BEGIN cond=condition UNTIL_END code=scope ;
repeatUntilBlock: REPEAT code=scope UNTIL_BEGIN cond=condition UNTIL_END ;

condition : arg1=values cmp=COMPARE arg2=values ;

ASSIGN : '=' ;
ARITHMETICS : '+' | '-' | '*' | '/' ; 
DATATYPE : 'dollar' | 'quarter' ;
RETURNTYPE : 'bankrupt' | 'dollar' | 'quarter' ;
COMPARE : '==' | '!=' | '<' | '>' | '<=' | '>=' ;
IF_CLOSURE : '?' ;
SCOPE_BEGIN : '{' ;
SCOPE_END : '}' ;
REPEAT : 'repeat' ;
UNTIL_BEGIN : 'until (' ;
UNTIL_END : ')' ;
IDENTIFIER : [a-zA-Z_][a-zA-Z0-9_]* ;
STRING : '\"'(.)*?'\"' ;
WHITE : [\r\n\t ]+ -> skip ;
COMMENT: '#' .*? '\n' -> skip ; 
VALUE: [0-9]+ | '\''.'\'' ;
