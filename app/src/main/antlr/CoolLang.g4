grammar CoolLang;

program : statement* ;

statement : (declaration | writeOperation | readOperation) ';' ;

declaration : type ID ;

value : ID | STRING ;

type : ID ;

writeOperation : 'write' value ;

readOperation : 'read' ID ;

// Lexer Rules

ID : [a-zA-Z]+ [a-zA-Z0-9]*;

WS : [ \t\r\n]+ -> skip ;

STRING : '"' (~["\r\n])* '"' ;