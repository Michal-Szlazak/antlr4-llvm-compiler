grammar CoolLang;

program : statement* ;

statement : (declaration | writeOperation | readOperation) ';' ;

declaration : type ID;

value : ID | STRING ;

type : 'i32' | 'i64' | 'f32' | 'f64' ;

writeOperation : 'write' value ;

readOperation : 'read' ID ;

// Lexer Rules

ID : [a-zA-Z]+ ;

WS : [ \t\r\n]+ -> skip ;

STRING : '"' (~["\r\n])* '"' ;