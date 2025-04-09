grammar CoolLang;

program : statement* ;

statement : (declaration | writeOperation | readOperation) ';' ;

declaration : type ID ;

type : ID ;

writeOperation : 'write' expression ;

expression
    : expression op=('*'|'/') expression
    | expression op=('+'|'-') expression
    | '(' expression ')'
    | ID
    | STRING
    ;

readOperation : 'read' ID ;

// Lexer Rules

ID : [a-zA-Z]+ [a-zA-Z0-9]*;

WS : [ \t\r\n]+ -> skip ;

STRING : '"' (~["\r\n])* '"' ;