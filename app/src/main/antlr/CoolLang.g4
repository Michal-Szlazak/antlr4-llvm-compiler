grammar CoolLang;

program : statement* ;

statement : (
    declaration
    | writeOperation
    | readOperation
    | assignment
    | ifStatement
    | loopStatement
    | functionDeclaration
    | functionCall
    | structDeclaration
    | struct
    | structVariableAssignment
    ) ';' ;

ifStatement: 'if' '(' boolExpression ')' '{' ifBody '}' ;
ifBody: statement* ;

loopStatement: 'loop' '(' loopCondition ')' '{' loopBody '}' ;
loopCondition: ID | boolExpression | INT ;
loopBody: statement* ;

functionDeclaration: 'fun' functionName '{' functionBody '}' ;

functionName: ID ;
functionBody: statement* ;

functionCall: 'call' ID ;

declaration : type ID ;

type : ID ;

writeOperation : 'write' (expression | STRING | boolExpression) ;

structDeclaration: 'struct' structName ID ;

struct: 'struct' ID '{' (structVariableDeclaration ';')* '}' ;
structVariableDeclaration: type ID ;

structVariableCall: structName ':' structVariable ;
structName: ID ;
structVariable: ID ;

assignment: ID '=' (expression | boolExpression);

structVariableAssignment: structVariableCall '=' (expression | boolExpression);

expression: expression op=('*'|'/') expression
    | expression op=('+'|'-') expression
    | '(' expression ')'
    | ID
    | INT
    | REAL
    | structVariableCall
    ;

boolExpression
    : boolOrExpr
    ;

boolOrExpr
    : boolXorExpr ('OR' boolXorExpr)*
    ;

boolXorExpr
    : boolAndExpr ('XOR' boolAndExpr)*
    ;

boolAndExpr
    : boolNotExpr ('AND' boolNotExpr)*
    ;

boolNotExpr
    : 'NEG' boolNotExpr
    | boolPrimary
    ;

boolPrimary: '(' boolExpression ')'
    | boolean
    | ID
    | structVariableCall
    ;


readOperation : 'read' ID ;

boolean : TRUE | FALSE;

// Lexer Rules

TRUE : 'true';
FALSE : 'false';
AND : 'AND' ;
OR : 'OR' ;
XOR : 'XOR' ;
NEG : 'NEG' ;

ID : [a-zA-Z]+ [a-zA-Z0-9]*;

WS : [ \t\r\n]+ -> skip ;

STRING : '"' (~["\r\n])* '"' ;

INT : [+-]? [0-9]+ ;

REAL : [0-9]+ '.' [0-9]+ ;