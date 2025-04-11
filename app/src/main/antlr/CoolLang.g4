grammar CoolLang;

program : statement* ;

statement : (declaration | writeOperation | readOperation | assignment) ';' ;

declaration : type ID ;

type : ID ;

writeOperation : 'write' (expression | STRING) ;

assignment: ID '=' expression;

expression
    : expression op=('*'|'/') expression
    | expression op=('+'|'-') expression
    | '(' expression ')'
    | ID
    | INT
    | REAL
    ;

readOperation : 'read' ID ;

// Lexer Rules

ID : [a-zA-Z]+ [a-zA-Z0-9]*;

WS : [ \t\r\n]+ -> skip ;

STRING : '"' (~["\r\n])* '"' ;

INT : [+-]? [0-9]+ ;

REAL : [0-9]+ '.' [0-9]+ ;